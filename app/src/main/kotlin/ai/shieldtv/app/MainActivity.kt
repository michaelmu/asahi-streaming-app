package ai.shieldtv.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import ai.shieldtv.app.auth.RealDebridAuthCoordinator
import ai.shieldtv.app.auth.RealDebridLinkStartResult
import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.di.AppContainer
import ai.shieldtv.app.settings.SourcePreferences
import ai.shieldtv.app.domain.repository.SourceFetchProgress
import ai.shieldtv.app.feature.details.presentation.DetailsPresenter
import ai.shieldtv.app.feature.details.presentation.DetailsViewModel
import ai.shieldtv.app.feature.player.presentation.PlayerPresenter
import ai.shieldtv.app.feature.player.presentation.PlayerViewModel
import ai.shieldtv.app.feature.search.presentation.SearchPresenter
import ai.shieldtv.app.feature.search.presentation.SearchViewModel
import ai.shieldtv.app.feature.sources.presentation.SourcesPresenter
import ai.shieldtv.app.feature.sources.presentation.SourcesViewModel
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState
import ai.shieldtv.app.integration.playback.media3.engine.Media3PlaybackEngine
import ai.shieldtv.app.integration.playback.media3.engine.Media3PlaybackEngine.RenderMode
import ai.shieldtv.app.playback.ContinueWatchingHydrator
import ai.shieldtv.app.playback.PlaybackRestoreDecider
import ai.shieldtv.app.playback.PlaybackResumeDecider
import ai.shieldtv.app.playback.RestoreTarget
import ai.shieldtv.app.playback.PlaybackSessionRecord
import ai.shieldtv.app.navigation.AppDestination
import ai.shieldtv.app.update.ApkDownloadManager
import ai.shieldtv.app.update.ApkInstaller
import ai.shieldtv.app.sources.ProviderHealthTracker
import ai.shieldtv.app.sources.SourceLoadRequest
import ai.shieldtv.app.sources.SourceLoadingCoordinator
import ai.shieldtv.app.ui.DetailsScreenRenderer
import ai.shieldtv.app.ui.EpisodePickerScreenRenderer
import ai.shieldtv.app.ui.HomeScreenRenderer
import ai.shieldtv.app.ui.ModalDefaultAction
import ai.shieldtv.app.ui.NavigationRailRenderer
import ai.shieldtv.app.ui.OverlayPopup
import ai.shieldtv.app.ui.PlayerScreenRenderer
import ai.shieldtv.app.ui.ResultsScreenRenderer
import ai.shieldtv.app.ui.ScreenViewFactory
import ai.shieldtv.app.ui.SearchScreenRenderer
import ai.shieldtv.app.ui.SettingsScreenRenderer
import ai.shieldtv.app.ui.SourcesScreenRenderer
import ai.shieldtv.app.update.AppUpdateInfo
import ai.shieldtv.app.update.GitHubReleaseUpdateChecker
import ai.shieldtv.app.update.UpdateCoordinator
import ai.shieldtv.app.update.UpdateInstallUiResult
import ai.shieldtv.app.update.UpdateUiCoordinator
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val searchViewModel by lazy {
        SearchViewModel(SearchPresenter(AppContainer.searchTitlesUseCase))
    }
    private val detailsViewModel by lazy {
        DetailsViewModel(DetailsPresenter(AppContainer.getTitleDetailsUseCase))
    }
    private val sourcesViewModel by lazy {
        SourcesViewModel(SourcesPresenter(AppContainer.findSourcesUseCase))
    }
    private val playerViewModel by lazy {
        PlayerViewModel(
            PlayerPresenter(
                resolveSourceUseCase = AppContainer.resolveSourceUseCase,
                buildPlaybackItemUseCase = AppContainer.buildPlaybackItemUseCase,
                playbackEngine = AppContainer.playbackEngine
            )
        )
    }

    private val coordinator = AppCoordinator()
    private val apkDownloadManager by lazy { ApkDownloadManager() }
    private val apkInstaller by lazy { ApkInstaller(this) }
    private lateinit var viewFactory: ScreenViewFactory
    private lateinit var navigationRailRenderer: NavigationRailRenderer
    private lateinit var homeRenderer: HomeScreenRenderer
    private lateinit var searchRenderer: SearchScreenRenderer
    private lateinit var resultsRenderer: ResultsScreenRenderer
    private lateinit var detailsRenderer: DetailsScreenRenderer
    private lateinit var episodesRenderer: EpisodePickerScreenRenderer
    private lateinit var sourcesRenderer: SourcesScreenRenderer
    private lateinit var playerRenderer: PlayerScreenRenderer
    private lateinit var settingsRenderer: SettingsScreenRenderer
    private lateinit var overlayPopup: OverlayPopup

    private val realDebridAuthCoordinator by lazy {
        RealDebridAuthCoordinator(
            scope = lifecycleScope,
            startDeviceFlow = { AppContainer.startRealDebridDeviceFlowUseCase() },
            pollDeviceFlow = { flow -> AppContainer.pollRealDebridDeviceFlowUseCase(flow) },
            clearAuth = { AppContainer.clearRealDebridAuth() },
            buildAuthUrl = ::buildRealDebridAuthUrl,
            buildStartFailureMessage = ::buildRealDebridStartFailureMessage
        )
    }

    private val updateUiCoordinator by lazy {
        UpdateUiCoordinator(
            updateCoordinator = UpdateCoordinator(
                updateCheckerFactory = {
                    GitHubReleaseUpdateChecker(
                        owner = "michaelmu",
                        repo = "asahi-streaming-app",
                        currentVersionName = BuildConfig.VERSION_NAME,
                        currentVersionCode = BuildConfig.VERSION_CODE
                    )
                },
                apkDownloadManager = apkDownloadManager,
                apkInstaller = apkInstaller
            ),
            cacheDirProvider = { cacheDir }
        )
    }

    private lateinit var root: LinearLayout
    private lateinit var sidebar: LinearLayout
    private lateinit var contentPane: LinearLayout
    private lateinit var railHost: LinearLayout
    private lateinit var contentScrollView: ScrollView
    private lateinit var statusText: android.widget.TextView
    private lateinit var loadingView: ProgressBar
    private lateinit var screenHost: LinearLayout
    private lateinit var modalHost: android.widget.FrameLayout
    private lateinit var playerView: PlayerView

    private val sourceLoadingCoordinator by lazy {
        SourceLoadingCoordinator(
            lifecycleScope = lifecycleScope,
            sourcesViewModel = sourcesViewModel
        )
    }
    private val providerHealthTracker = ProviderHealthTracker()

    private val playerControllerVisibilityTimeoutMs = 3500

    private var authState: RealDebridAuthState = RealDebridAuthState(isLinked = false)
    private var activeDeviceFlow: DeviceCodeFlow? = null
    private var authPollingJob: Job? = null
    private var latestPlaybackState = ai.shieldtv.app.core.model.playback.PlaybackState(
        isBuffering = false,
        isPlaying = false,
        positionMs = 0,
        durationMs = 0
    )
    private var latestSourceDiagnostics: String? = null
    private var latestSourcesError: String? = null
    private var latestPlaybackMessage: String? = null
    private var latestPlaybackError: String? = null
    private var latestUpdateInfo: AppUpdateInfo? = null
    private var latestUpdateMessage: String? = null
    private var persistedPlaybackSession: PlaybackSessionRecord? = null
    private var activeModalView: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewFactory = ScreenViewFactory(this)
        setContentView(buildContentView())
        initializeRenderers()
        attachPlayerView()
        observePlaybackState()
        refreshAuthState()
        persistedPlaybackSession = AppContainer.playbackSessionStore.load()
        val restored = savedInstanceState?.getStringArrayList("appStateEntries")
            ?.mapNotNull {
                val parts = it.split('=', limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else null
            }
            ?.toMap()
        if (restored != null) {
            coordinator.restoreState(appStateFromBundleMap(restored))
        } else {
            coordinator.openHome()
        }
        hydrateContinueWatchingFromPersistedSession()
        reconcileRestoredState()
        renderCurrentScreen()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val entries = ArrayList(
            coordinator.currentState().toBundleMap().map { (key, value) -> "$key=$value" }
        )
        outState.putStringArrayList("appStateEntries", entries)
    }

    override fun onDestroy() {
        authPollingJob?.cancel()
        realDebridAuthCoordinator.cancel()
        sourceLoadingCoordinator.cancel()
        AppContainer.playbackEngine.getCurrentItem()?.let { currentItem ->
            AppContainer.playbackSessionStore.save(
                item = currentItem,
                state = latestPlaybackState,
                seasonNumber = coordinator.currentState().selectedSeasonNumber,
                episodeNumber = coordinator.currentState().selectedEpisodeNumber
            )
        }
        super.onDestroy()
        AppContainer.playbackEngine.release()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && dismissModal()) {
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && handleBackPress()) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun buildContentView(): View {
        val shellRoot = android.widget.FrameLayout(this).apply {
            setBackgroundResource(ai.shieldtv.app.R.drawable.asahi_app_bg)
        }

        root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(48, 32, 48, 32)
        }

        sidebar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.21f)
            setPadding(0, 0, 28, 0)
            setBackgroundResource(ai.shieldtv.app.R.drawable.asahi_panel_bg)
            setPadding(22, 22, 22, 22)
        }

        contentPane = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.79f)
            setBackgroundResource(ai.shieldtv.app.R.drawable.asahi_panel_elevated_bg)
            setPadding(28, 28, 28, 28)
        }

        val title = viewFactory.railTitle("Asahi")
        val subtitle = viewFactory.caption("TV-first streaming shell")
        val buildInfo = viewFactory.caption("v${BuildConfig.VERSION_NAME} • #${BuildConfig.VERSION_CODE} • ${BuildConfig.GIT_SHA}")

        railHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        loadingView = ProgressBar(this).apply {
            visibility = View.GONE
            indeterminateTintList = android.content.res.ColorStateList.valueOf(viewFactory.accentColor)
        }

        statusText = android.widget.TextView(this).apply {
            text = "Ready"
            textSize = 15f
            setTextColor(viewFactory.textSecondaryColor)
        }

        screenHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        sidebar.addView(title)
        sidebar.addView(viewFactory.spacer(6))
        sidebar.addView(subtitle)
        sidebar.addView(viewFactory.spacer(4))
        sidebar.addView(buildInfo)
        sidebar.addView(viewFactory.spacer(18))
        sidebar.addView(railHost)
        sidebar.addView(viewFactory.spacer())
        sidebar.addView(loadingView)
        sidebar.addView(statusText)

        contentScrollView = ScrollView(this).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            addView(screenHost)
        }
        contentPane.addView(
            contentScrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        )

        root.addView(sidebar)
        root.addView(contentPane)

        modalHost = android.widget.FrameLayout(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            isClickable = true
            isFocusable = true
            isFocusableInTouchMode = true
        }

        shellRoot.addView(root)
        shellRoot.addView(modalHost)
        return shellRoot
    }

    private fun initializeRenderers() {
        navigationRailRenderer = NavigationRailRenderer(railHost, viewFactory)
        homeRenderer = HomeScreenRenderer(screenHost, viewFactory)
        searchRenderer = SearchScreenRenderer(this, screenHost, viewFactory)
        resultsRenderer = ResultsScreenRenderer(this, screenHost, viewFactory)
        detailsRenderer = DetailsScreenRenderer(screenHost, viewFactory)
        episodesRenderer = EpisodePickerScreenRenderer(this, screenHost, viewFactory)
        sourcesRenderer = SourcesScreenRenderer(screenHost, viewFactory)
        playerRenderer = PlayerScreenRenderer(screenHost, viewFactory)
        settingsRenderer = SettingsScreenRenderer(this, screenHost, viewFactory)
        overlayPopup = OverlayPopup(this, viewFactory)
    }

    private fun createPlayerView(): PlayerView {
        return PlayerView(this).apply {
            useController = true
            controllerAutoShow = true
            controllerHideOnTouch = false
            controllerShowTimeoutMs = playerControllerVisibilityTimeoutMs
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            visibility = View.GONE
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun attachPlayerView() {
        val engine = AppContainer.playbackEngine as? Media3PlaybackEngine ?: return
        playerView = createPlayerView().apply {
            player = engine.attach(this@MainActivity)
            showController()
        }
    }

    private fun handleBackPress(): Boolean {
        return when (coordinator.currentState().destination) {
            AppDestination.HOME -> false
            AppDestination.SEARCH,
            AppDestination.RESULTS,
            AppDestination.SETTINGS -> false
            AppDestination.DETAILS -> {
                coordinator.showResults(
                    query = coordinator.currentState().query,
                    results = coordinator.currentState().searchResults
                )
                renderCurrentScreen()
                true
            }
            AppDestination.EPISODES -> {
                coordinator.currentState().selectedDetails?.let {
                    coordinator.showDetails(it.mediaRef, it)
                    renderCurrentScreen()
                    true
                } ?: false
            }
            AppDestination.SOURCES -> {
                val state = coordinator.currentState()
                val details = state.selectedDetails
                if (details != null && details.mediaRef.mediaType == ai.shieldtv.app.core.model.media.MediaType.SHOW) {
                    coordinator.showEpisodes(details, state.selectedSeasonNumber, state.selectedEpisodeNumber)
                } else if (details != null) {
                    coordinator.showDetails(details.mediaRef, details)
                } else {
                    coordinator.showResults(state.query, state.searchResults)
                }
                renderCurrentScreen()
                true
            }
            AppDestination.PLAYER -> {
                AppContainer.playbackEngine.stop()
                coordinator.currentState().selectedMedia?.let { mediaRef ->
                    coordinator.showSources(
                        mediaRef = mediaRef,
                        details = coordinator.currentState().selectedDetails,
                        seasonNumber = coordinator.currentState().selectedSeasonNumber,
                        episodeNumber = coordinator.currentState().selectedEpisodeNumber,
                        sources = coordinator.currentState().selectedSources
                    )
                    renderCurrentScreen()
                    true
                } ?: false
            }
        }
    }

    private fun hydrateContinueWatchingFromPersistedSession() {
        val record = persistedPlaybackSession ?: return
        val item = ContinueWatchingHydrator.fromActiveResume(record) ?: return
        coordinator.recordContinueWatching(
            mediaRef = coordinator.currentState().selectedMedia
                ?: ai.shieldtv.app.core.model.media.MediaRef(
                    mediaType = coordinator.currentState().searchMode.mediaType,
                    ids = ai.shieldtv.app.core.model.media.MediaIds(tmdbId = null, imdbId = null, tvdbId = null),
                    title = item.mediaTitle,
                    year = null
                ),
            artworkUrl = item.artworkUrl,
            seasonNumber = record.seasonNumber,
            episodeNumber = record.episodeNumber,
            progressPercent = item.progressPercent
        )
    }

    private fun resumePositionFor(source: SourceResult, seasonNumber: Int?, episodeNumber: Int?): Long {
        return PlaybackResumeDecider.resumePositionFor(
            record = persistedPlaybackSession,
            source = source,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )
    }

    private fun reconcileRestoredState() {
        val state = coordinator.currentState()
        when (PlaybackRestoreDecider.decide(state)) {
            RestoreTarget.KEEP_PLAYER -> return
            RestoreTarget.SOURCES -> {
                val fallbackSource = state.selectedSources.firstOrNull() ?: return
                coordinator.showSources(
                    mediaRef = state.selectedMedia ?: fallbackSource.mediaRef,
                    details = state.selectedDetails,
                    seasonNumber = state.selectedSeasonNumber,
                    episodeNumber = state.selectedEpisodeNumber,
                    sources = state.selectedSources
                )
            }
            RestoreTarget.EPISODES -> coordinator.showEpisodes(
                details = state.selectedDetails ?: return,
                seasonNumber = state.selectedSeasonNumber ?: 1,
                episodeNumber = state.selectedEpisodeNumber ?: 1
            )
            RestoreTarget.DETAILS -> coordinator.showDetails(
                mediaRef = state.selectedDetails?.mediaRef ?: return,
                details = state.selectedDetails
            )
            RestoreTarget.RESULTS -> coordinator.showResults(state.query, state.searchResults)
            RestoreTarget.HOME -> coordinator.openHome()
        }
        latestPlaybackError = null
        latestPlaybackMessage = null
        AppContainer.playbackEngine.stop()
    }

    private fun refreshAuthState() {
        lifecycleScope.launch {
            authState = AppContainer.getRealDebridAuthStateUseCase()
            renderCurrentScreen()
        }
    }

    private fun refreshAuthUiOnly() {
        renderCurrentScreen()
    }

    private fun showModal(view: View) {
        modalHost.removeAllViews()
        modalHost.addView(view)
        modalHost.visibility = View.VISIBLE
        activeModalView = view
        modalHost.post { modalHost.requestFocus() }
    }

    private fun dismissModal(): Boolean {
        if (activeModalView == null) return false
        modalHost.removeAllViews()
        modalHost.visibility = View.GONE
        activeModalView = null
        return true
    }

    private fun showInfoModal(
        title: String,
        message: String,
        primaryLabel: String = "OK",
        onPrimary: (() -> Unit)? = null,
        secondaryLabel: String? = null,
        onSecondary: (() -> Unit)? = null,
        tertiaryLabel: String? = null,
        onTertiary: (() -> Unit)? = null,
        dismissOnBack: Boolean = true,
        defaultAction: ModalDefaultAction = ModalDefaultAction.PRIMARY,
        customContent: View? = null
    ) {
        showModal(
            overlayPopup.build(
                title = title,
                message = message,
                primaryLabel = primaryLabel,
                onPrimary = {
                    dismissModal()
                    onPrimary?.invoke()
                },
                secondaryLabel = secondaryLabel,
                onSecondary = {
                    dismissModal()
                    onSecondary?.invoke()
                },
                tertiaryLabel = tertiaryLabel,
                onTertiary = {
                    dismissModal()
                    onTertiary?.invoke()
                },
                dismissOnBack = dismissOnBack,
                defaultAction = defaultAction,
                customContent = customContent
            )
        )
    }

    private fun renderCurrentScreen() {
        val destination = coordinator.currentState().destination
        if (destination == AppDestination.PLAYER) {
            showPlayerFullscreenShell()
        } else {
            showStandardShell()
        }

        railHost.removeAllViews()
        if (destination != AppDestination.PLAYER) {
            navigationRailRenderer.render(
                inHome = destination == AppDestination.HOME,
                inSearch = destination == AppDestination.SEARCH,
                selectedMode = coordinator.currentState().searchMode,
                inSettings = destination == AppDestination.SETTINGS,
                onHome = {
                    AppContainer.playbackEngine.stop()
                    coordinator.openHome()
                    renderCurrentScreen()
                },
                onMovies = {
                    AppContainer.playbackEngine.stop()
                    if (coordinator.currentState().searchMode == SearchMode.MOVIES && coordinator.currentState().destination == AppDestination.SEARCH) {
                        renderCurrentScreen()
                    } else {
                        coordinator.openSearch(SearchMode.MOVIES)
                        renderCurrentScreen()
                    }
                },
                onShows = {
                    AppContainer.playbackEngine.stop()
                    if (coordinator.currentState().searchMode == SearchMode.SHOWS && coordinator.currentState().destination == AppDestination.SEARCH) {
                        renderCurrentScreen()
                    } else {
                        coordinator.openSearch(SearchMode.SHOWS)
                        renderCurrentScreen()
                    }
                },
                onSettings = {
                    AppContainer.playbackEngine.stop()
                    coordinator.openSettings()
                    renderCurrentScreen()
                },
                onQuit = {
                    AppContainer.playbackEngine.stop()
                    finishAffinity()
                },
                onFirstFocusTarget = ::focusView
            )
        }

        screenHost.removeAllViews()
        when (destination) {
            AppDestination.HOME -> {
                contentScrollView.post {
                    contentScrollView.scrollTo(0, 0)
                }
                homeRenderer.render(
                    state = coordinator.currentState(),
                    authLinked = authState.isLinked,
                    statusMessage = statusText.text?.toString().orEmpty(),
                    movieFavorites = AppContainer.favoritesCoordinator.listByType(ai.shieldtv.app.core.model.media.MediaType.MOVIE),
                    showFavorites = AppContainer.favoritesCoordinator.listByType(ai.shieldtv.app.core.model.media.MediaType.SHOW),
                    movieHistory = AppContainer.watchHistoryCoordinator.listResultsByType(ai.shieldtv.app.core.model.media.MediaType.MOVIE),
                    showHistory = AppContainer.watchHistoryCoordinator.listResultsByType(ai.shieldtv.app.core.model.media.MediaType.SHOW),
                    onBrowseMovies = {
                        coordinator.openSearch(SearchMode.MOVIES)
                        renderCurrentScreen()
                    },
                    onMovieFavorites = {
                        coordinator.showFavorites(
                            SearchMode.MOVIES,
                            AppContainer.favoritesCoordinator.listByType(ai.shieldtv.app.core.model.media.MediaType.MOVIE)
                        )
                        statusText.text = "Movie favorites"
                        renderCurrentScreen()
                    },
                    onMovieHistory = {
                        coordinator.showHistory(
                            SearchMode.MOVIES,
                            AppContainer.watchHistoryCoordinator.listResultsByType(ai.shieldtv.app.core.model.media.MediaType.MOVIE)
                        )
                        statusText.text = "Movie watch history"
                        renderCurrentScreen()
                    },
                    onBrowseShows = {
                        coordinator.openSearch(SearchMode.SHOWS)
                        renderCurrentScreen()
                    },
                    onShowFavorites = {
                        coordinator.showFavorites(
                            SearchMode.SHOWS,
                            AppContainer.favoritesCoordinator.listByType(ai.shieldtv.app.core.model.media.MediaType.SHOW)
                        )
                        statusText.text = "TV favorites"
                        renderCurrentScreen()
                    },
                    onShowHistory = {
                        coordinator.showHistory(
                            SearchMode.SHOWS,
                            AppContainer.watchHistoryCoordinator.listResultsByType(ai.shieldtv.app.core.model.media.MediaType.SHOW)
                        )
                        statusText.text = "TV watch history"
                        renderCurrentScreen()
                    },
                    onOpenSettings = {
                        coordinator.openSettings()
                        renderCurrentScreen()
                    },
                    onResumeSearch = coordinator.currentState().takeIf {
                        it.query.isNotBlank() && (it.searchResults.isNotEmpty() || it.selectedMedia != null)
                    }?.let {
                        {
                            coordinator.openSearch(it.searchMode)
                            renderCurrentScreen()
                        }
                    },
                    onQuickPick = { quickPick ->
                        coordinator.openSearch(
                            if (quickPick.mediaRef.mediaType == ai.shieldtv.app.core.model.media.MediaType.SHOW) SearchMode.SHOWS else SearchMode.MOVIES
                        )
                        renderCurrentScreen()
                        runSearch(
                            if (quickPick.mediaRef.mediaType == ai.shieldtv.app.core.model.media.MediaType.SHOW) SearchMode.SHOWS else SearchMode.MOVIES,
                            quickPick.mediaRef.title
                        )
                    },
                    onRecentQuery = { recentQuery ->
                        runSearch(coordinator.currentState().searchMode, recentQuery)
                    },
                    onContinueWatching = { item ->
                        runSearch(coordinator.currentState().searchMode, item.queryHint)
                    },
                    onFavoriteSelected = { result ->
                        onSearchResultSelected(result)
                    },
                    onHistorySelected = { result ->
                        onSearchResultSelected(result)
                    },
                    onFirstFocusTarget = ::focusView
                )
            }
            AppDestination.SEARCH -> searchRenderer.render(
                state = coordinator.currentState(),
                onSearch = ::runSearch,
                onOpenFavorites = {
                    if (coordinator.currentState().searchMode == SearchMode.MOVIES) {
                        coordinator.showFavorites(
                            SearchMode.MOVIES,
                            AppContainer.favoritesCoordinator.listByType(ai.shieldtv.app.core.model.media.MediaType.MOVIE)
                        )
                        statusText.text = "Movie favorites"
                    } else {
                        coordinator.showFavorites(
                            SearchMode.SHOWS,
                            AppContainer.favoritesCoordinator.listByType(ai.shieldtv.app.core.model.media.MediaType.SHOW)
                        )
                        statusText.text = "TV favorites"
                    }
                    renderCurrentScreen()
                },
                onOpenHistory = {
                    if (coordinator.currentState().searchMode == SearchMode.MOVIES) {
                        coordinator.showHistory(
                            SearchMode.MOVIES,
                            AppContainer.watchHistoryCoordinator.listResultsByType(ai.shieldtv.app.core.model.media.MediaType.MOVIE)
                        )
                        statusText.text = "Movie watch history"
                    } else {
                        coordinator.showHistory(
                            SearchMode.SHOWS,
                            AppContainer.watchHistoryCoordinator.listResultsByType(ai.shieldtv.app.core.model.media.MediaType.SHOW)
                        )
                        statusText.text = "TV watch history"
                    }
                    renderCurrentScreen()
                },
                onBack = {},
                onFirstFocusTarget = ::focusView
            )
            AppDestination.RESULTS -> resultsRenderer.render(
                state = coordinator.currentState(),
                emptyMessage = latestSourcesError ?: "No results.",
                onResultSelected = ::onSearchResultSelected,
                onResultLongPress = ::showResultActions,
                onNewSearch = {
                    coordinator.openSearch(coordinator.currentState().searchMode)
                    renderCurrentScreen()
                },
                onFirstFocusTarget = ::focusView
            )
            AppDestination.DETAILS -> detailsRenderer.render(
                state = coordinator.currentState(),
                onBrowseEpisodes = {
                    coordinator.currentState().selectedDetails?.let { details ->
                        coordinator.showEpisodes(
                            details = details,
                            seasonNumber = coordinator.currentState().selectedSeasonNumber ?: 1,
                            episodeNumber = coordinator.currentState().selectedEpisodeNumber ?: 1
                        )
                        renderCurrentScreen()
                    }
                },
                onFindSources = {
                    coordinator.currentState().selectedDetails?.let { details ->
                        if (!ensureRealDebridLinkedForSources()) return@let
                        loadSourcesFor(details.mediaRef, null, null)
                    }
                },
                onFirstFocusTarget = ::focusView
            )
            AppDestination.EPISODES -> episodesRenderer.render(
                state = coordinator.currentState(),
                watchedEpisodeKeys = coordinator.currentState().selectedDetails?.mediaRef?.ids?.let {
                    AppContainer.watchHistoryCoordinator.watchedEpisodeKeys(it)
                }.orEmpty(),
                onSeasonSelected = { season ->
                    coordinator.currentState().selectedDetails?.let { details ->
                        coordinator.showEpisodes(details, season, 1)
                        renderCurrentScreen()
                    }
                },
                onEpisodeSelected = { episode ->
                    coordinator.currentState().selectedDetails?.let { details ->
                        coordinator.showEpisodes(
                            details,
                            coordinator.currentState().selectedSeasonNumber ?: 1,
                            episode
                        )
                        renderCurrentScreen()
                    }
                },
                onFindSources = {
                    coordinator.currentState().selectedDetails?.let { details ->
                        if (!ensureRealDebridLinkedForSources()) return@let
                        loadSourcesFor(
                            details.mediaRef,
                            coordinator.currentState().selectedSeasonNumber ?: 1,
                            coordinator.currentState().selectedEpisodeNumber ?: 1
                        )
                    }
                },
                onEpisodePlay = { episode ->
                    coordinator.currentState().selectedDetails?.let { details ->
                        if (!ensureRealDebridLinkedForSources()) return@let
                        val season = coordinator.currentState().selectedSeasonNumber ?: 1
                        coordinator.showEpisodes(details, season, episode)
                        loadSourcesFor(details.mediaRef, season, episode)
                    }
                },
                onFirstFocusTarget = ::focusView
            )
            AppDestination.SOURCES -> sourcesRenderer.render(
                state = coordinator.currentState(),
                diagnostics = latestSourceDiagnostics,
                error = latestSourcesError,
                onSourceSelected = { source ->
                    val resumePosition = resumePositionFor(
                        source,
                        coordinator.currentState().selectedSeasonNumber,
                        coordinator.currentState().selectedEpisodeNumber
                    )
                    if (resumePosition > 0L) {
                        preparePlayback(
                            source = source,
                            seasonNumber = coordinator.currentState().selectedSeasonNumber,
                            episodeNumber = coordinator.currentState().selectedEpisodeNumber
                        )
                    } else {
                        preparePlayback(
                            source = source,
                            seasonNumber = coordinator.currentState().selectedSeasonNumber,
                            episodeNumber = coordinator.currentState().selectedEpisodeNumber
                        )
                    }
                },
                onFirstFocusTarget = ::focusView
            )
            AppDestination.PLAYER -> {
                playerView.visibility = View.VISIBLE
                detachPlayerFromParent()
                playerRenderer.render(
                    state = coordinator.currentState(),
                    playbackMessage = latestPlaybackMessage,
                    playbackError = latestPlaybackError,
                    playerView = playerView,
                    playbackState = latestPlaybackState
                )
            }
            AppDestination.SETTINGS -> settingsRenderer.render(
                authState = authState,
                activeDeviceFlow = activeDeviceFlow,
                updateSummary = latestUpdateMessage,
                providerSummary = buildProviderSummary(),
                sourcePreferencesSummary = buildSourcePreferencesSummary(),
                movieMaxSizeLabel = currentSourcePreferences().movieMaxSizeGb?.let { "${it}GB" } ?: "No limit",
                tvMaxSizeLabel = currentSourcePreferences().episodeMaxSizeGb?.let { "${it}GB" } ?: "No limit",
                providerSelectionLabel = buildProviderSelectionLabel(),
                buildAuthUrl = ::buildRealDebridAuthUrl,
                onStartLink = ::startRealDebridLink,
                onResetAuth = ::resetRealDebridAuth,
                onCopyDebugInfo = ::copyDebugInfoToClipboard,
                onCheckForUpdates = ::checkForUpdates,
                onOpenLatestUpdate = latestUpdateInfo?.let { { openLatestUpdate(it) } },
                onConfigureMovieMaxSize = ::showMovieMaxSizePicker,
                onConfigureTvMaxSize = ::showTvMaxSizePicker,
                onToggleProviders = ::showProviderSelectionModal,
                onResetSourcePreferences = ::resetSourcePreferences,
                onBackToHome = {
                    coordinator.openHome()
                    renderCurrentScreen()
                },
                onFirstFocusTarget = ::focusView
            )
        }
    }

    private fun runSearch(mode: SearchMode, rawQuery: String) {
        val query = rawQuery.trim()
        latestSourcesError = null
        latestSourceDiagnostics = null
        latestPlaybackMessage = null
        latestPlaybackError = null
        AppContainer.playbackEngine.stop()
        setLoading(true, "Searching for \"$query\"…")

        lifecycleScope.launch {
            val state = searchViewModel.search(query)
            val filteredResults = state.results.filter { it.mediaRef.mediaType == mode.mediaType }
            val decoratedResults = AppContainer.watchHistoryCoordinator.applyWatchedBadges(filteredResults)
            coordinator.showResults(query = query, results = decoratedResults)
            setLoading(false, state.error ?: "Found ${filteredResults.size} result(s) for \"$query\".")
            if (state.error != null && filteredResults.isEmpty()) {
                latestSourcesError = state.error
            }
            renderCurrentScreen()
        }
    }

    private fun onSearchResultSelected(result: SearchResult) {
        latestSourcesError = null
        latestSourceDiagnostics = null
        latestPlaybackMessage = null
        latestPlaybackError = null
        AppContainer.playbackEngine.stop()
        setLoading(true, "Loading details for ${result.mediaRef.title}…")

        lifecycleScope.launch {
            val state = detailsViewModel.load(result.mediaRef)
            val details = state.item
            if (details == null) {
                setLoading(false, state.error ?: "No details available.")
                latestSourcesError = state.error ?: "No details available."
                renderCurrentScreen()
                return@launch
            }

            coordinator.showDetails(result.mediaRef, details)
            setLoading(false, "Loaded details for ${details.mediaRef.title}.")
            renderCurrentScreen()
        }
    }

    private fun showResultActions(result: SearchResult) {
        val isFavorite = AppContainer.favoritesCoordinator.isFavorited(result)
        val isFavoritesBrowse = coordinator.currentState().favoritesBrowseMode != null
        val isHistoryBrowse = coordinator.currentState().historyBrowseMode != null
        showInfoModal(
            title = result.mediaRef.title,
            message = when {
                isFavoritesBrowse -> "Choose what to do with this favorite."
                isHistoryBrowse -> "Choose what to do with this history item."
                else -> "Open details or manage this item from here."
            },
            primaryLabel = "Open",
            onPrimary = {
                onSearchResultSelected(result)
            },
            secondaryLabel = when {
                isHistoryBrowse -> "Remove from History"
                isFavorite -> "Remove Favorite"
                else -> "Add Favorite"
            },
            onSecondary = {
                when {
                    isHistoryBrowse -> {
                        AppContainer.watchHistoryCoordinator.removeByResult(result)
                        refreshHistoryBrowse()
                    }
                    else -> {
                        val nowFavorite = AppContainer.favoritesCoordinator.toggle(result)
                        if (coordinator.currentState().favoritesBrowseMode != null) {
                            refreshFavoritesBrowse()
                        } else {
                            statusText.text = if (nowFavorite) {
                                "Added ${result.mediaRef.title} to favorites"
                            } else {
                                "Removed ${result.mediaRef.title} from favorites"
                            }
                            renderCurrentScreen()
                        }
                    }
                }
            },
            tertiaryLabel = when {
                isHistoryBrowse -> "Clear History"
                isFavoritesBrowse -> "Back to Search"
                else -> null
            },
            onTertiary = when {
                isHistoryBrowse -> {
                    {
                        clearCurrentHistoryBrowse()
                    }
                }
                isFavoritesBrowse -> {
                    {
                        coordinator.openSearch(coordinator.currentState().searchMode)
                        renderCurrentScreen()
                    }
                }
                else -> null
            }
        )
    }

    private fun refreshFavoritesBrowse() {
        val mode = coordinator.currentState().favoritesBrowseMode ?: return
        val mediaType = if (mode == SearchMode.SHOWS) ai.shieldtv.app.core.model.media.MediaType.SHOW else ai.shieldtv.app.core.model.media.MediaType.MOVIE
        coordinator.showFavorites(mode, AppContainer.favoritesCoordinator.listByType(mediaType))
        statusText.text = if (mode == SearchMode.SHOWS) "TV favorites" else "Movie favorites"
        renderCurrentScreen()
    }

    private fun refreshHistoryBrowse() {
        val mode = coordinator.currentState().historyBrowseMode ?: return
        val mediaType = if (mode == SearchMode.SHOWS) ai.shieldtv.app.core.model.media.MediaType.SHOW else ai.shieldtv.app.core.model.media.MediaType.MOVIE
        coordinator.showHistory(mode, AppContainer.watchHistoryCoordinator.listResultsByType(mediaType))
        statusText.text = if (mode == SearchMode.SHOWS) "TV watch history" else "Movie watch history"
        renderCurrentScreen()
    }

    private fun clearCurrentHistoryBrowse() {
        val mode = coordinator.currentState().historyBrowseMode ?: return
        val mediaType = if (mode == SearchMode.SHOWS) ai.shieldtv.app.core.model.media.MediaType.SHOW else ai.shieldtv.app.core.model.media.MediaType.MOVIE
        AppContainer.watchHistoryCoordinator.clearByType(mediaType)
        refreshHistoryBrowse()
    }

    private fun ensureRealDebridLinkedForSources(): Boolean {
        if (authState.isLinked) return true
        latestSourcesError = "Real-Debrid link required before finding sources."
        statusText.text = "Link Real-Debrid in Settings before finding sources."
        coordinator.openSettings()
        renderCurrentScreen()
        return false
    }

    private fun loadSourcesFor(
        mediaRef: MediaRef,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ) {
        latestSourcesError = null
        latestSourceDiagnostics = null
        latestPlaybackMessage = null
        latestPlaybackError = null
        AppContainer.playbackEngine.stop()

        val searchLabel = if (seasonNumber != null && episodeNumber != null) {
            "${mediaRef.title} S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
        } else {
            mediaRef.title
        }

        providerHealthTracker.reset()
        setLoading(true, "Finding sources for $searchLabel…")
        coordinator.showSources(
            mediaRef = mediaRef,
            details = coordinator.currentState().selectedDetails,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            sources = emptyList()
        )
        renderCurrentScreen()
        val prefs = currentSourcePreferences()
        sourceLoadingCoordinator.load(
            request = SourceLoadRequest(
                mediaRef = mediaRef,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                authLinked = authState.isLinked,
                preferences = prefs,
                availableProviderIds = AppContainer.availableProviderIds().toSet(),
                filters = currentSourceFilters(),
                searchLabel = searchLabel
            ),
            onStarted = {
                showSourceProgressModal(searchLabel)
            },
            onProgressUpdated = { progressItems ->
                progressItems.lastOrNull()?.let(providerHealthTracker::record)
                runOnUiThread {
                    if (activeModalView != null) {
                        showSourceProgressModal(searchLabel)
                    }
                }
            },
            onIncrementalUpdate = { update ->
                runOnUiThread {
                    coordinator.showSources(
                        mediaRef = mediaRef,
                        details = coordinator.currentState().selectedDetails,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        sources = update.sources
                    )
                    latestSourceDiagnostics = buildSourceDiagnostics(update.sources) +
                        " | progress=${update.completedProviders}/${update.totalProviders}" +
                        " | ${providerHealthTracker.summary()}"
                    latestSourcesError = null
                    renderCurrentScreen()
                }
            },
            onCompleted = { result ->
                latestSourceDiagnostics = (result.diagnostics ?: buildSourceDiagnostics(result.sources)) +
                    " | ${providerHealthTracker.summary()}"
                latestSourcesError = result.error
                coordinator.showSources(
                    mediaRef = mediaRef,
                    details = coordinator.currentState().selectedDetails,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    sources = result.sources
                )
                dismissModal()
                setLoading(false, result.error ?: "Found ${result.sources.size} source(s) for $searchLabel.")
                renderCurrentScreen()
            }
        )
    }

    private fun showSourceProgressModal(searchLabel: String) {
        val progressItems = sourceLoadingCoordinator.currentProgress()
        val lines = if (progressItems.isEmpty()) {
            listOf("Preparing providers…")
        } else {
            progressItems.map { progress ->
                when (progress.state) {
                    SourceFetchProgress.State.STARTED -> "${progress.providerDisplayName}: querying…"
                    SourceFetchProgress.State.COMPLETED -> "${progress.providerDisplayName}: ${progress.resultCount ?: 0} result(s)"
                    SourceFetchProgress.State.FAILED -> "${progress.providerDisplayName}: failed${progress.message?.let { " (${it.take(60)})" } ?: ""}"
                }
            }
        }
        showInfoModal(
            title = "Finding Sources",
            message = buildString {
                appendLine(searchLabel)
                appendLine()
                append(lines.joinToString("\n"))
            },
            primaryLabel = "Keep Waiting",
            onPrimary = { showSourceProgressModal(searchLabel) },
            secondaryLabel = "Cancel",
            onSecondary = {
                sourceLoadingCoordinator.cancel()
                dismissModal()
                setLoading(false, "Source lookup cancelled.")
            },
            dismissOnBack = false,
            defaultAction = ModalDefaultAction.PRIMARY
        )
    }

    private fun resetRealDebridAuth() {
        activeDeviceFlow = null
        authState = realDebridAuthCoordinator.resetAuth()
        latestSourcesError = null
        statusText.text = "Real-Debrid auth reset."
        renderCurrentScreen()
    }

    private fun startRealDebridLink() {
        dismissModal()
        setLoading(true, "Starting Real-Debrid link flow…")
        lifecycleScope.launch {
            when (val result = realDebridAuthCoordinator.startLink()) {
                is RealDebridLinkStartResult.Success -> {
                    activeDeviceFlow = result.value.flow
                    authState = result.value.authState
                    setLoading(false, result.value.statusMessage)
                    showRealDebridFlowModal(result.value.flow)
                    refreshAuthUiOnly()
                    startAutoPolling(result.value.flow)
                }
                is RealDebridLinkStartResult.Failure -> {
                    authPollingJob?.cancel()
                    authState = result.value.authState
                    setLoading(false, result.value.statusMessage)
                    showInfoModal(
                        title = "Real-Debrid Link Failed",
                        message = "${result.value.errorType}: ${result.value.debugMessage}",
                        primaryLabel = "OK",
                        secondaryLabel = "Copy Debug Info",
                        onSecondary = ::copyDebugInfoToClipboard,
                        defaultAction = ModalDefaultAction.PRIMARY
                    )
                    refreshAuthUiOnly()
                }
            }
        }
    }

    private fun buildRealDebridStartFailureMessage(error: Throwable): String {
        val errorType = error::class.java.simpleName
        val errorMessage = error.message?.takeIf { it.isNotBlank() } ?: "No exception message"
        val apiError = RealDebridDebugState.lastStartDeviceFlowError.takeIf { it.isNotBlank() }
        val responsePreview = RealDebridDebugState.lastStartDeviceFlowResponse
            .replace("\n", " ")
            .replace("\r", " ")
            .take(220)
            .takeIf { it.isNotBlank() }

        return buildString {
            appendLine("Step: start device flow")
            appendLine("Error: $errorType")
            appendLine("Message: $errorMessage")
            apiError?.let {
                appendLine()
                appendLine("API error: $it")
            }
            responsePreview?.let {
                appendLine()
                appendLine("Response preview:")
                append(it)
            }
        }.trim()
    }

    private fun showRealDebridFlowModal(flow: DeviceCodeFlow) {
        val authUrl = buildRealDebridAuthUrl(flow)
        showInfoModal(
            title = "Link Real-Debrid",
            message = buildString {
                appendLine("Open: $authUrl")
                appendLine()
                append("Code: ${flow.userCode}")
            },
            primaryLabel = "Open Link Page",
            onPrimary = {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)))
                showRealDebridFlowModal(flow)
            },
            secondaryLabel = "Copy Debug Info",
            onSecondary = ::copyDebugInfoToClipboard,
            tertiaryLabel = "Close",
            onTertiary = {},
            defaultAction = ModalDefaultAction.PRIMARY,
            customContent = buildQrCodeView(flow.qrCodeUrl ?: authUrl)
        )
    }

    private fun startAutoPolling(flow: DeviceCodeFlow) {
        authPollingJob?.cancel()
        realDebridAuthCoordinator.startAutoPolling(
            flow = flow,
            currentAuthState = { authState },
            onStateUpdated = { result ->
                authState = result.authState
                activeDeviceFlow = result.activeDeviceFlow
                result.statusMessage?.let { statusText.text = it }
                result.errorType?.let { latestPlaybackMessage = "Auth flow warning: $it" }
                result.linkedMessage?.let { linkedMessage ->
                    dismissModal()
                    showInfoModal(
                        title = "Real-Debrid Linked",
                        message = linkedMessage,
                        primaryLabel = "Back to Settings",
                        defaultAction = ModalDefaultAction.PRIMARY
                    )
                }
                refreshAuthUiOnly()
            },
            onTimeout = { timeout ->
                authState = timeout.authState
                statusText.text = timeout.statusMessage
                showInfoModal(
                    title = "Real-Debrid Link Timed Out",
                    message = "${timeout.errorType}: ${timeout.timeoutMessage}",
                    primaryLabel = "Try Again",
                    onPrimary = ::startRealDebridLink,
                    secondaryLabel = "Close",
                    onSecondary = {},
                    defaultAction = ModalDefaultAction.PRIMARY
                )
                refreshAuthUiOnly()
            }
        )
    }

    private fun buildQrCodeView(value: String): View {
        val size = viewFactory.dp(180)
        val image = android.widget.ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply {
                gravity = android.view.Gravity.CENTER_HORIZONTAL
            }
            setImageBitmap(generateQrBitmap(value, size))
            setBackgroundColor(Color.WHITE)
            setPadding(viewFactory.dp(8), viewFactory.dp(8), viewFactory.dp(8), viewFactory.dp(8))
        }
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = android.view.Gravity.CENTER_HORIZONTAL
            addView(image)
            addView(viewFactory.spacer(10))
            addView(viewFactory.caption("Scan to open the Real-Debrid link on your phone."))
        }
    }

    private fun generateQrBitmap(value: String, size: Int): Bitmap {
        val matrix = QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[y * size + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.RGB_565)
    }

    private fun buildRealDebridAuthUrl(flow: DeviceCodeFlow): String {
        return flow.directVerificationUrl
            ?.takeIf { it.isNotBlank() }
            ?: flow.verificationUrl.ifBlank { "https://real-debrid.com/device" }
    }

    private fun currentRenderModeLabel(): String {
        val engine = AppContainer.playbackEngine as? Media3PlaybackEngine ?: return "unknown"
        return when (engine.getRenderMode()) {
            RenderMode.SURFACE_VIEW -> "SurfaceView"
            RenderMode.TEXTURE_VIEW -> "TextureView"
        }
    }

    private fun toggleRenderMode() {
        val engine = AppContainer.playbackEngine as? Media3PlaybackEngine ?: return
        val nextMode = when (engine.getRenderMode()) {
            RenderMode.SURFACE_VIEW -> RenderMode.TEXTURE_VIEW
            RenderMode.TEXTURE_VIEW -> RenderMode.SURFACE_VIEW
        }
        engine.setRenderMode(nextMode)
        attachPlayerView()
        statusText.text = "Playback render mode: ${currentRenderModeLabel()}"
        renderCurrentScreen()
    }

    private fun observePlaybackState() {
        lifecycleScope.launch {
            AppContainer.playbackEngine.observeState().collectLatest { state ->
                latestPlaybackState = state
            }
        }
    }

    private fun preparePlayback(
        source: SourceResult,
        seasonNumber: Int? = source.seasonNumber,
        episodeNumber: Int? = source.episodeNumber,
        forceStartAtZero: Boolean = false
    ) {
        if (source.debridService == DebridService.REAL_DEBRID && !authState.isLinked) {
            latestPlaybackError = "Real-Debrid link required before playback."
            latestPlaybackMessage = "This source needs Real-Debrid. Link your account in Settings / Accounts, then try again."
            statusText.text = "Real-Debrid link required before playback."
            coordinator.openSettings()
            renderCurrentScreen()
            return
        }

        latestPlaybackError = null
        latestPlaybackMessage = null
        setLoading(true, "Resolving ${source.displayName}…")

        lifecycleScope.launch {
            val state = playerViewModel.prepare(
                source = source,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                startPositionMs = if (forceStartAtZero) 0L else resumePositionFor(source, seasonNumber, episodeNumber)
            )
            latestPlaybackError = state.error
            latestPlaybackMessage = if (state.prepared) null else buildString {
                appendLine("Playback preparation failed.")
                appendLine("Current item: ${AppContainer.playbackEngine.getCurrentItem()?.title ?: source.displayName}")
                appendLine("Stream URL: ${state.playbackUrl ?: AppContainer.playbackEngine.getCurrentUrl() ?: source.url}")
                appendLine("Player state: ${latestPlaybackState.playerStateLabel}")
                appendLine("Video format: ${latestPlaybackState.videoFormat ?: "unknown"}")
                appendLine("Video size: ${latestPlaybackState.videoSizeLabel ?: "unknown"}")
                append("Playback error type: ${state.errorType ?: "unknown"}")
                appendLine()
                append("Playback error: ${latestPlaybackState.errorMessage ?: state.error ?: "none"}")
            }
            setLoading(false, state.error ?: if (state.prepared) "Playback item prepared." else "Playback not prepared.")
            if (state.prepared) {
                val progressPercent = when {
                    latestPlaybackState.durationMs > 0 -> ((latestPlaybackState.positionMs * 100) / latestPlaybackState.durationMs).toInt()
                    else -> 8
                }
                coordinator.recordContinueWatching(
                    mediaRef = source.mediaRef,
                    artworkUrl = coordinator.currentState().selectedDetails?.backdropUrl
                        ?: coordinator.currentState().selectedDetails?.posterUrl,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    progressPercent = progressPercent
                )
                AppContainer.playbackEngine.getCurrentItem()?.let { currentItem ->
                    AppContainer.playbackSessionStore.saveActiveResume(
                        item = currentItem,
                        state = latestPlaybackState,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber
                    )
                    AppContainer.watchHistoryCoordinator.recordPlayback(
                        item = currentItem,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber
                    )
                    persistedPlaybackSession = AppContainer.playbackSessionStore.loadActiveResume()
                }
                coordinator.showPlayer(source)
                AppContainer.playbackEngine.play()
            }
            renderCurrentScreen()
        }
    }

    private fun buildSourceDiagnostics(sources: List<SourceResult>): String {
        if (sources.isEmpty()) return "providers=none | results=0"
        val providerCounts = sources
            .flatMap { source ->
                source.providerDisplayNames.ifEmpty { setOf(source.providerDisplayName.ifBlank { source.providerId }) }
            }
            .groupingBy { it }
            .eachCount()
        val providerSummary = providerCounts.entries
            .sortedByDescending { it.value }
            .joinToString(", ") { (providerId, count) -> "$providerId:$count" }
        val cachedCount = sources.count { it.cacheStatus == CacheStatus.CACHED }
        val directCount = sources.count { it.cacheStatus == CacheStatus.DIRECT }
        val fallbackCount = sources.count { it.cacheStatus == CacheStatus.UNCACHED || it.cacheStatus == CacheStatus.UNCHECKED }
        val topRankExplanation = sources.firstOrNull()?.let { top ->
            val explanation = AppContainer.explainSourceRanking(top) ?: return@let null
            val topContributions = explanation.contributions
                .sortedByDescending { kotlin.math.abs(it.value) }
                .take(4)
                .joinToString(", ") { contribution ->
                    "${contribution.rule}:${contribution.value}"
                }
            val provenance = top.origins
                .take(3)
                .joinToString(", ") { origin ->
                    val seeders = origin.seeders?.let { "/s$it" }.orEmpty()
                    "${origin.providerDisplayName}:${origin.cacheStatus}$seeders"
                }
                .takeIf { it.isNotBlank() }
            buildString {
                append("top=${top.displayName.take(60)} score=${explanation.totalScore} [$topContributions]")
                provenance?.let {
                    append(" origins=")
                    append(it)
                }
            }
        }
        return listOf(
            "providers=$providerSummary",
            "cached=$cachedCount",
            "direct=$directCount",
            "fallback=$fallbackCount",
            "results=${sources.size}",
            "resumePolicy=${ai.shieldtv.app.playback.PlaybackPersistencePolicy.SUMMARY}",
            topRankExplanation
        ).filterNotNull().joinToString(" | ")
    }

    private fun buildProviderSummary(): String {
        val labels = AppContainer.availableProviderLabels()
        val allProviders = labels.keys.toSet()
        val prefs = currentSourcePreferences()
        val enabled = prefs.providerSelection.effectiveEnabledProviders(allProviders)
        val modeLabel = when (prefs.providerSelection.mode) {
            ai.shieldtv.app.settings.ProviderSelectionMode.ALL_ENABLED -> "all enabled"
            ai.shieldtv.app.settings.ProviderSelectionMode.CUSTOM -> "custom"
        }
        return labels.entries.joinToString(" • ") { (id, label) ->
            val marker = if (id in enabled) "on" else "off"
            "$label:$marker"
        } + " ($modeLabel)"
    }

    private fun currentSourcePreferences(): SourcePreferences = AppContainer.sourcePreferencesStore.load()

    private fun currentSourceFilters(): ai.shieldtv.app.core.model.source.SourceFilters {
        val prefs = currentSourcePreferences()
        return ai.shieldtv.app.core.model.source.SourceFilters(
            movieMaxSizeGb = prefs.movieMaxSizeGb,
            episodeMaxSizeGb = prefs.episodeMaxSizeGb
        )
    }

    private fun buildSourcePreferencesSummary(): String {
        val prefs = currentSourcePreferences()
        val allProviders = AppContainer.availableProviderIds().toSet()
        val effectiveProviders = prefs.providerSelection.effectiveEnabledProviders(allProviders)
        val providerMode = when (prefs.providerSelection.mode) {
            ai.shieldtv.app.settings.ProviderSelectionMode.ALL_ENABLED -> "Providers: all enabled"
            ai.shieldtv.app.settings.ProviderSelectionMode.CUSTOM -> "Providers: ${effectiveProviders.joinToString(", ")}"
        }
        val movieLimit = "Movies max: ${prefs.movieMaxSizeGb?.let { "${it}GB" } ?: "none"}"
        val tvLimit = "TV max: ${prefs.episodeMaxSizeGb?.let { "${it}GB" } ?: "none"}"
        return listOf(providerMode, movieLimit, tvLimit).joinToString(" • ")
    }

    private fun buildProviderSelectionLabel(): String {
        val prefs = currentSourcePreferences()
        val allProviders = AppContainer.availableProviderIds().toSet()
        val effectiveProviders = prefs.providerSelection.effectiveEnabledProviders(allProviders)
        return when (prefs.providerSelection.mode) {
            ai.shieldtv.app.settings.ProviderSelectionMode.ALL_ENABLED -> "All enabled"
            ai.shieldtv.app.settings.ProviderSelectionMode.CUSTOM -> "${effectiveProviders.size} selected"
        }
    }

    private fun showMovieMaxSizePicker() {
        showSizePickerModal(
            title = "Movie Max Size",
            currentValue = currentSourcePreferences().movieMaxSizeGb,
            values = listOf(null, 1, 2, 4, 8, 12, 16, 24, 32, 48),
            valueLabel = { it?.let { gb -> "${gb}GB" } ?: "No limit" },
            onSelected = {
                AppContainer.sourcePreferencesStore.saveMovieMaxSizeGb(it)
                renderCurrentScreen()
            }
        )
    }

    private fun showTvMaxSizePicker() {
        showSizePickerModal(
            title = "TV Max Size",
            currentValue = currentSourcePreferences().episodeMaxSizeGb,
            values = listOf(null, 1, 2, 4, 6, 8, 12, 16, 24),
            valueLabel = { it?.let { gb -> "${gb}GB" } ?: "No limit" },
            onSelected = {
                AppContainer.sourcePreferencesStore.saveEpisodeMaxSizeGb(it)
                renderCurrentScreen()
            }
        )
    }

    private fun showProviderSelectionModal() {
        showProviderSelectionPage(startIndex = 0)
    }

    private fun formatProviderChoiceLabel(label: String, enabled: Boolean): String {
        return if (enabled) "Disable $label" else "Enable $label"
    }

    private fun showProviderSelectionPage(startIndex: Int) {
        val labels = AppContainer.availableProviderLabels()
        val allProviders = labels.keys.toList()
        val current = currentSourcePreferences().providerSelection
        val effectiveEnabled = current.effectiveEnabledProviders(allProviders.toSet())
        val page = allProviders.drop(startIndex).take(3)
        val providerLines = allProviders.joinToString("\n") { id ->
            val enabled = id in effectiveEnabled
            val marker = if (enabled) "[x]" else "[ ]"
            "$marker ${labels[id] ?: id}"
        }
        showInfoModal(
            title = "Choose Providers",
            message = buildString {
                appendLine("Enabled: ${effectiveEnabled.size} / ${allProviders.size}")
                appendLine()
                append(providerLines)
            },
            primaryLabel = page.getOrNull(0)?.let { id -> formatProviderChoiceLabel(labels[id] ?: id, id in effectiveEnabled) } ?: "Enable All",
            onPrimary = {
                val provider = page.getOrNull(0)
                if (provider != null) toggleProvider(provider) else setAllProvidersEnabled()
            },
            secondaryLabel = page.getOrNull(1)?.let { id -> formatProviderChoiceLabel(labels[id] ?: id, id in effectiveEnabled) } ?: "Disable All",
            onSecondary = {
                val provider = page.getOrNull(1)
                if (provider != null) toggleProvider(provider) else setNoProviderOverrides()
            },
            tertiaryLabel = when {
                page.getOrNull(2) != null -> formatProviderChoiceLabel(labels[page[2]] ?: page[2], page[2] in effectiveEnabled)
                startIndex + 3 < allProviders.size -> "More…"
                else -> "Actions"
            },
            onTertiary = {
                val provider = page.getOrNull(2)
                when {
                    provider != null -> toggleProvider(provider)
                    startIndex + 3 < allProviders.size -> showProviderSelectionPage(startIndex + 3)
                    else -> showProviderSelectionActions(allProviders)
                }
            },
            dismissOnBack = true,
            defaultAction = ModalDefaultAction.PRIMARY
        )
    }

    private fun showProviderSelectionActions(allProviders: List<String>) {
        val labels = AppContainer.availableProviderLabels()
        val current = currentSourcePreferences().providerSelection
        val effectiveEnabled = current.effectiveEnabledProviders(allProviders.toSet())
        val providerLines = allProviders.joinToString("\n") { id ->
            val enabled = id in effectiveEnabled
            val marker = if (enabled) "[x]" else "[ ]"
            "$marker ${labels[id] ?: id}"
        }
        showInfoModal(
            title = "Provider Selection",
            message = providerLines,
            primaryLabel = "Enable All",
            onPrimary = { setAllProvidersEnabled() },
            secondaryLabel = "Disable All",
            onSecondary = { setNoProviderOverrides() },
            tertiaryLabel = "Done",
            onTertiary = { renderCurrentScreen() },
            dismissOnBack = true,
            defaultAction = ModalDefaultAction.TERTIARY
        )
    }

    private fun toggleProvider(providerId: String) {
        val allProviders = AppContainer.availableProviderIds().toSet()
        val store = AppContainer.sourcePreferencesStore
        val current = currentSourcePreferences().providerSelection
        val working = current.effectiveEnabledProviders(allProviders).toMutableSet()
        if (!working.add(providerId)) {
            working.remove(providerId)
        }
        store.saveProviderSelection(
            if (working == allProviders) {
                ai.shieldtv.app.settings.ProviderSelectionState(
                    mode = ai.shieldtv.app.settings.ProviderSelectionMode.ALL_ENABLED
                )
            } else {
                ai.shieldtv.app.settings.ProviderSelectionState(
                    mode = ai.shieldtv.app.settings.ProviderSelectionMode.CUSTOM,
                    enabledProviders = working
                )
            }
        )
        showProviderSelectionModal()
        renderCurrentScreen()
    }

    private fun setAllProvidersEnabled() {
        AppContainer.sourcePreferencesStore.saveProviderSelection(
            ai.shieldtv.app.settings.ProviderSelectionState(
                mode = ai.shieldtv.app.settings.ProviderSelectionMode.ALL_ENABLED
            )
        )
        showProviderSelectionModal()
        renderCurrentScreen()
    }

    private fun setNoProviderOverrides() {
        AppContainer.sourcePreferencesStore.saveProviderSelection(
            ai.shieldtv.app.settings.ProviderSelectionState(
                mode = ai.shieldtv.app.settings.ProviderSelectionMode.CUSTOM,
                enabledProviders = emptySet()
            )
        )
        showProviderSelectionModal()
        renderCurrentScreen()
    }

    private fun buildProviderToggleList(
        labels: Map<String, String>,
        allProviders: List<String>,
        effectiveEnabled: Set<String>
    ): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            allProviders.forEachIndexed { index, providerId ->
                val enabled = providerId in effectiveEnabled
                val row = viewFactory.panel(vertical = false, elevated = false).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.CENTER_VERTICAL
                    setPadding(viewFactory.dp(16), viewFactory.dp(12), viewFactory.dp(16), viewFactory.dp(12))
                    isFocusable = true
                    isFocusableInTouchMode = true
                    alpha = 0.97f
                    nextFocusUpId = id
                    nextFocusDownId = id
                    setOnClickListener {
                        toggleProvider(providerId)
                    }
                    setOnFocusChangeListener { view, hasFocus ->
                        view.scaleX = if (hasFocus) 1.01f else 1f
                        view.scaleY = if (hasFocus) 1.01f else 1f
                        view.alpha = if (hasFocus) 1f else 0.97f
                        background = androidx.core.content.ContextCompat.getDrawable(
                            context,
                            if (hasFocus) ai.shieldtv.app.R.drawable.asahi_button_bg else ai.shieldtv.app.R.drawable.asahi_panel_bg
                        )
                    }
                }
                val name = android.widget.TextView(this@MainActivity).apply {
                    text = labels[providerId] ?: providerId
                    setTextColor(viewFactory.textPrimaryColor)
                    textSize = 17f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                val badge = android.widget.TextView(this@MainActivity).apply {
                    text = if (enabled) "ON" else "OFF"
                    setTextColor(if (enabled) viewFactory.textPrimaryColor else viewFactory.textSecondaryColor)
                    textSize = 14f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    background = androidx.core.content.ContextCompat.getDrawable(
                        context,
                        if (enabled) ai.shieldtv.app.R.drawable.asahi_chip_bg else ai.shieldtv.app.R.drawable.asahi_panel_bg
                    )
                    setPadding(viewFactory.dp(12), viewFactory.dp(6), viewFactory.dp(12), viewFactory.dp(6))
                }
                row.addView(name)
                row.addView(badge)
                addView(row)
                if (index < allProviders.lastIndex) {
                    addView(viewFactory.spacer(8))
                }
            }
        }
    }

    private fun showSizePickerModal(
        title: String,
        currentValue: Int?,
        values: List<Int?>,
        valueLabel: (Int?) -> String,
        onSelected: (Int?) -> Unit
    ) {
        showSizePickerPage(
            title = title,
            currentValue = currentValue,
            values = values,
            startIndex = 0,
            valueLabel = valueLabel,
            onSelected = onSelected
        )
    }

    private fun showSizePickerPage(
        title: String,
        currentValue: Int?,
        values: List<Int?>,
        startIndex: Int,
        valueLabel: (Int?) -> String,
        onSelected: (Int?) -> Unit
    ) {
        val page = values.drop(startIndex).take(3)
        showInfoModal(
            title = title,
            message = buildString {
                appendLine("Current: ${valueLabel(currentValue)}")
                appendLine()
                appendLine(values.joinToString("\n") { value ->
                    val marker = if (value == currentValue) "[x]" else "[ ]"
                    "$marker ${valueLabel(value)}"
                })
            },
            primaryLabel = page.getOrNull(0)?.let(valueLabel) ?: "Done",
            onPrimary = {
                val value = page.getOrNull(0)
                if (value != null) {
                    onSelected(value)
                    showSizePickerPage(title, value, values, startIndex, valueLabel, onSelected)
                }
            },
            secondaryLabel = page.getOrNull(1)?.let(valueLabel) ?: if (startIndex + 3 < values.size) "More…" else "Done",
            onSecondary = {
                val value = page.getOrNull(1)
                when {
                    value != null -> {
                        onSelected(value)
                        showSizePickerPage(title, value, values, startIndex, valueLabel, onSelected)
                    }
                    startIndex + 3 < values.size -> showSizePickerPage(title, currentValue, values, startIndex + 3, valueLabel, onSelected)
                    else -> {}
                }
            },
            tertiaryLabel = page.getOrNull(2)?.let(valueLabel) ?: if (startIndex > 0) "Back" else "Done",
            onTertiary = {
                val value = page.getOrNull(2)
                when {
                    value != null -> {
                        onSelected(value)
                        showSizePickerPage(title, value, values, startIndex, valueLabel, onSelected)
                    }
                    startIndex > 0 -> showSizePickerPage(title, currentValue, values, (startIndex - 3).coerceAtLeast(0), valueLabel, onSelected)
                    else -> {}
                }
            },
            defaultAction = ModalDefaultAction.PRIMARY
        )
    }

    private fun resetSourcePreferences() {
        AppContainer.sourcePreferencesStore.reset()
        showInfoModal(
            title = "Source Preferences Reset",
            message = "Movie/TV size limits and provider overrides were reset.",
            primaryLabel = "OK"
        )
        renderCurrentScreen()
    }

    private fun setLoading(isLoading: Boolean, message: String) {
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        statusText.text = message
    }

    private fun showPlayerFullscreenShell() {
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(0, 0, 0, 0)
        sidebar.visibility = View.GONE
        contentPane.setPadding(0, 0, 0, 0)
        contentScrollView.setPadding(0, 0, 0, 0)
        contentPane.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        enterImmersiveFullscreen()
    }

    private fun showStandardShell() {
        root.orientation = LinearLayout.HORIZONTAL
        root.setPadding(48, 32, 48, 32)
        sidebar.visibility = View.VISIBLE
        contentPane.setPadding(28, 28, 28, 28)
        contentScrollView.setPadding(0, 0, 0, 0)
        sidebar.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.26f)
        contentPane.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.74f)
        exitImmersiveFullscreen()
    }

    private fun enterImmersiveFullscreen() {
        window.setDecorFitsSystemWindows(false)
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun exitImmersiveFullscreen() {
        window.setDecorFitsSystemWindows(true)
        window.insetsController?.show(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
    }

    private fun checkForUpdates() {
        dismissModal()
        setLoading(true, "Checking for updates…")
        lifecycleScope.launch {
            val result = updateUiCoordinator.checkForUpdates()
            latestUpdateInfo = result.updateInfo
            latestUpdateMessage = result.statusMessage
            setLoading(false, result.statusMessage)
            if (result.errorMessage != null) {
                showInfoModal(
                    title = "Update Check Failed",
                    message = result.errorMessage,
                    primaryLabel = "OK"
                )
            } else if (result.updateInfo != null) {
                showUpdateAvailableModal(result.updateInfo)
            }
            renderCurrentScreen()
        }
    }

    private fun showUpdateAvailableModal(updateInfo: AppUpdateInfo) {
        showInfoModal(
            title = "Update Available",
            message = buildString {
                append(updateInfo.versionName)
                updateInfo.versionCodeHint?.let {
                    append(" (versionCode ")
                    append(it)
                    append(")")
                }
                updateInfo.publishedAt?.takeIf { it.isNotBlank() }?.let {
                    appendLine()
                    appendLine()
                    append("Published: ")
                    append(it)
                }
            },
            primaryLabel = "Install Update",
            onPrimary = { openLatestUpdate(updateInfo) },
            secondaryLabel = "View Release Notes",
            onSecondary = {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.pageUrl)))
            },
            tertiaryLabel = "Later",
            onTertiary = {},
            defaultAction = ModalDefaultAction.PRIMARY
        )
    }

    private fun openLatestUpdate(updateInfo: AppUpdateInfo) {
        dismissModal()
        lifecycleScope.launch {
            setLoading(true, "Downloading update APK…")
            when (val result = updateUiCoordinator.prepareInstall(updateInfo)) {
                is UpdateInstallUiResult.Ready -> {
                    setLoading(false, result.statusMessage)
                    startActivity(result.readiness.installIntent)
                    showInfoModal(
                        title = "Installer Launched",
                        message = "If Android does not show the installer, check the unknown apps permission for Asahi.",
                        primaryLabel = "Done",
                        secondaryLabel = "Open Settings",
                        onSecondary = {
                            result.readiness.openSettingsIntent?.let(::startActivity)
                                ?: startActivity(apkInstaller.buildManageUnknownAppsIntent())
                        },
                        defaultAction = ModalDefaultAction.PRIMARY
                    )
                }
                is UpdateInstallUiResult.RequiresSettings -> {
                    setLoading(false, result.statusMessage)
                    showInfoModal(
                        title = "Enable APK Installs",
                        message = result.readiness.message ?: "Android is blocking installs from Asahi right now. Allow installs from this app, then try again.",
                        primaryLabel = "Open Settings",
                        onPrimary = {
                            result.readiness.openSettingsIntent?.let(::startActivity)
                        },
                        secondaryLabel = "Not Now",
                        onSecondary = {},
                        defaultAction = ModalDefaultAction.PRIMARY
                    )
                }
                is UpdateInstallUiResult.Unavailable -> {
                    setLoading(false, result.statusMessage)
                    showInfoModal(
                        title = "Installer Unavailable",
                        message = result.readiness.message ?: "No app on this device can handle APK installation intents right now.",
                        primaryLabel = "OK"
                    )
                }
                is UpdateInstallUiResult.Failure -> {
                    setLoading(false, result.statusMessage)
                    showInfoModal(
                        title = "Update Failed",
                        message = result.errorMessage,
                        primaryLabel = "OK"
                    )
                }
            }
        }
    }

    private fun copyDebugInfoToClipboard() {
        val clipboard = getSystemService(ClipboardManager::class.java)
        val state = coordinator.currentState()
        val debugText = buildString {
            appendLine("build=${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) ${BuildConfig.GIT_SHA}")
            appendLine("tmdb_key_embedded=${BuildConfig.TMDB_KEY_EMBEDDED}")
            appendLine("auth_linked=${authState.isLinked}")
            appendLine("auth_user=${authState.username ?: "none"}")
            appendLine("auth_in_progress=${authState.authInProgress}")
            appendLine("auth_error=${authState.lastError ?: "none"}")
            appendLine("token_path=${AppContainer.realDebridTokenStoreDebugPath()}")
            activeDeviceFlow?.let { flow ->
                appendLine("verification_url=${flow.verificationUrl}")
                appendLine("auth_url=${buildRealDebridAuthUrl(flow)}")
                appendLine("user_code=${flow.userCode}")
                appendLine("device_code=${flow.deviceCode}")
            }
            appendLine("destination=${state.destination}")
            appendLine("search_mode=${state.searchMode}")
            appendLine("query=${state.query}")
            appendLine("selected_media=${state.selectedMedia?.title ?: "none"}")
            appendLine("selected_season=${state.selectedSeasonNumber ?: "none"}")
            appendLine("selected_episode=${state.selectedEpisodeNumber ?: "none"}")
            appendLine("selected_source=${state.selectedSource?.displayName ?: "none"}")
            appendLine("status=${statusText.text}")
            appendLine("playback_state=${latestPlaybackState.playerStateLabel}")
            appendLine("playback_is_playing=${latestPlaybackState.isPlaying}")
            appendLine("playback_is_buffering=${latestPlaybackState.isBuffering}")
            appendLine("playback_position_ms=${latestPlaybackState.positionMs}")
            appendLine("playback_duration_ms=${latestPlaybackState.durationMs}")
            appendLine("playback_video_format=${latestPlaybackState.videoFormat ?: "none"}")
            appendLine("playback_video_size=${latestPlaybackState.videoSizeLabel ?: "none"}")
            appendLine("playback_error=${latestPlaybackState.errorMessage ?: latestPlaybackError ?: "none"}")
            appendLine("source_diagnostics=${latestSourceDiagnostics ?: "none"}")
        }
        clipboard?.setPrimaryClip(ClipData.newPlainText("Asahi Debug Info", debugText))
        statusText.text = "Debug info copied to clipboard"
    }

    private fun detachPlayerFromParent() {
        (playerView.parent as? LinearLayout)?.removeView(playerView)
    }

    private fun focusView(view: View) {
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.requestFocus()
    }
}
