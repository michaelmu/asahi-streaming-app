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
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import ai.shieldtv.app.auth.RealDebridAuthCoordinator
import ai.shieldtv.app.auth.RealDebridLinkStartResult
import ai.shieldtv.app.browse.BrowseFlowCoordinator
import ai.shieldtv.app.browse.BrowseSearchResult
import ai.shieldtv.app.browse.BrowseSelectionResult
import ai.shieldtv.app.browse.DetailsNavigationCoordinator
import ai.shieldtv.app.browse.SourceNavigationDecision
import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.debug.PlaybackCrashLogStore
import ai.shieldtv.app.di.AppContainer
import ai.shieldtv.app.settings.SourcePreferences
import ai.shieldtv.app.settings.SourcePreferencesCoordinator
import ai.shieldtv.app.domain.repository.SourceFetchProgress
import ai.shieldtv.app.feature.DetailsFeatureFactory
import ai.shieldtv.app.feature.PlayerFeatureFactory
import ai.shieldtv.app.feature.SearchFeatureFactory
import ai.shieldtv.app.feature.details.presentation.DetailsViewModel
import ai.shieldtv.app.feature.player.presentation.PlayerViewModel
import ai.shieldtv.app.feature.sources.presentation.SourcesPresenter
import ai.shieldtv.app.feature.sources.presentation.SourcesViewModel
import ai.shieldtv.app.integration.debrid.realdebrid.debug.RealDebridDebugState
import ai.shieldtv.app.integration.playback.media3.engine.Media3PlaybackEngine
import ai.shieldtv.app.integration.playback.media3.engine.Media3PlaybackEngine.RenderMode
import ai.shieldtv.app.playback.ContinueWatchingHydrator
import ai.shieldtv.app.playback.PlaybackLaunchCoordinator
import ai.shieldtv.app.playback.PlaybackLaunchResult
import ai.shieldtv.app.playback.PlaybackRestoreDecider
import ai.shieldtv.app.playback.PlaybackResumeDecider
import ai.shieldtv.app.playback.RestoreTarget
import ai.shieldtv.app.playback.PlaybackSessionRecord
import ai.shieldtv.app.navigation.AppDestination
import ai.shieldtv.app.navigation.BackNavigationCoordinator
import ai.shieldtv.app.navigation.BackNavigationResult
import ai.shieldtv.app.update.ApkDownloadManager
import ai.shieldtv.app.update.ApkInstaller
import ai.shieldtv.app.sources.ProviderHealthTracker
import ai.shieldtv.app.sources.SourceLoadRequest
import ai.shieldtv.app.sources.SourceLoadUiCoordinator
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
    private val searchViewModel by lazy { SearchFeatureFactory.createViewModel() }
    private val detailsViewModel by lazy { DetailsFeatureFactory.createViewModel() }
    private val sourcesViewModel by lazy {
        SourcesViewModel(SourcesPresenter(AppContainer.findSourcesUseCase))
    }
    private val playerViewModel by lazy { PlayerFeatureFactory.createViewModel() }

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
    private lateinit var realDebridStatusText: android.widget.TextView
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
    private val browseFlowCoordinator by lazy {
        BrowseFlowCoordinator(
            searchViewModel = searchViewModel,
            detailsViewModel = detailsViewModel,
            watchHistoryCoordinator = AppContainer.watchHistoryCoordinator
        )
    }
    private val detailsNavigationCoordinator = DetailsNavigationCoordinator()
    private val backNavigationCoordinator = BackNavigationCoordinator()
    private val sourceLoadUiCoordinator = SourceLoadUiCoordinator()
    private val playbackLaunchCoordinator by lazy {
        PlaybackLaunchCoordinator(
            playerViewModel = playerViewModel,
            playbackEngine = AppContainer.playbackEngine,
            playbackSessionStore = AppContainer.playbackSessionStore,
            watchHistoryCoordinator = AppContainer.watchHistoryCoordinator
        )
    }
    private val sourcePreferencesCoordinator by lazy {
        SourcePreferencesCoordinator(
            sourcePreferencesStore = AppContainer.sourcePreferencesStore,
            availableProviderIds = { AppContainer.availableProviderIds().toSet() }
        )
    }
    private val settingsCoordinator by lazy {
        ai.shieldtv.app.settings.SettingsCoordinator(
            sourcePreferencesCoordinator = sourcePreferencesCoordinator,
            availableProviderLabels = { AppContainer.availableProviderLabels() },
            realDebridAuthCoordinator = realDebridAuthCoordinator,
            updateUiCoordinator = updateUiCoordinator
        )
    }
    private val settingsModalCoordinator by lazy {
        ai.shieldtv.app.settings.SettingsModalCoordinator(
            sourcePreferencesCoordinator = sourcePreferencesCoordinator,
            availableProviderLabels = { AppContainer.availableProviderLabels() }
        )
    }

    private val playerControllerVisibilityTimeoutMs = 3500
    private val startupUpdateCheckCooldownMs = 24L * 60L * 60L * 1000L

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
    private var hasStartedInitialUpdateCheck = false
    private val updatePrefs by lazy { getSharedPreferences("app_updates", MODE_PRIVATE) }
    private var persistedPlaybackSession: PlaybackSessionRecord? = null
    private var activeModalView: View? = null
    private val playbackCrashLogStore by lazy { PlaybackCrashLogStore(applicationContext) }
    private var lastFatalPlaybackReport: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewFactory = ScreenViewFactory(this)
        setContentView(buildContentView())
        initializeRenderers()
        attachPlayerView()
        updateSidebarStatus(authLinked = false)
        lastFatalPlaybackReport = playbackCrashLogStore.consumeFatalReport()
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
        runStartupUpdateCheckIfNeeded()
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
        if (keyCode == KeyEvent.KEYCODE_BACK && activeModalView != null) {
            return dismissModal()
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && handleBackPress()) {
            return true
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            showExitConfirmation()
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
            setPadding(44, 28, 44, 28)
        }

        sidebar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.21f)
            setPadding(0, 0, 24, 0)
            setBackgroundResource(ai.shieldtv.app.R.drawable.asahi_panel_bg)
            setPadding(24, 24, 24, 24)
        }

        contentPane = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.79f)
            setBackgroundResource(ai.shieldtv.app.R.drawable.asahi_panel_elevated_bg)
            setPadding(30, 30, 30, 30)
        }

        val title = viewFactory.railTitle("Asahi")
        val buildInfo = viewFactory.caption("v${BuildConfig.VERSION_NAME} • #${BuildConfig.VERSION_CODE} • ${BuildConfig.GIT_SHA}")

        railHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        loadingView = ProgressBar(this).apply {
            visibility = View.GONE
            indeterminateTintList = android.content.res.ColorStateList.valueOf(viewFactory.accentColor)
        }

        realDebridStatusText = android.widget.TextView(this).apply {
            text = "Real-Debrid: Not linked"
            textSize = 14f
            setTextColor(viewFactory.textSecondaryColor)
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
        sidebar.addView(realDebridStatusText)
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
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            setKeepContentOnPlayerReset(true)
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
            post { focusPlayerSeekBar() }
            postDelayed({ focusPlayerSeekBar() }, 120)
            postDelayed({ focusPlayerSeekBar() }, 300)
        }
    }

    private fun focusPlayerSeekBar() {
        playerView.showController()
        val progressView = playerView.findViewById<View?>(androidx.media3.ui.R.id.exo_progress) ?: return
        progressView.isFocusable = true
        progressView.isFocusableInTouchMode = true
        progressView.requestFocus()
        progressView.requestFocusFromTouch()
    }

    private fun teardownPlaybackSession() {
        playerView.player = null
        detachPlayerFromParent()
        AppContainer.playbackEngine.release()
        attachPlayerView()
        latestPlaybackState = ai.shieldtv.app.core.model.playback.PlaybackState(
            isBuffering = false,
            isPlaying = false,
            positionMs = 0,
            durationMs = 0,
            playerStateLabel = "idle",
            videoFormat = null,
            videoSizeLabel = null,
            errorMessage = null
        )
        latestPlaybackError = null
        latestPlaybackMessage = null
    }

    private fun handleBackPress(): Boolean {
        return when (
            val result = backNavigationCoordinator.handleBack(
                coordinator = coordinator,
                state = coordinator.currentState()
            )
        ) {
            BackNavigationResult.Unhandled -> false
            is BackNavigationResult.Handled -> {
                if (result.stopPlayback) {
                    teardownPlaybackSession()
                }
                if (result.renderRequired) {
                    renderCurrentScreen()
                }
                true
            }
        }
    }

    private fun showExitConfirmation() {
        showInfoModal(
            title = "Exit Asahi?",
            message = "Are you sure you want to exit?",
            primaryLabel = "Yes",
            onPrimary = {
                AppContainer.playbackEngine.stop()
                finishAffinity()
            },
            secondaryLabel = "No",
            onSecondary = {},
            dismissOnBack = true,
            defaultAction = ModalDefaultAction.SECONDARY
        )
    }

    private fun hydrateContinueWatchingFromPersistedSession() {
        val persistedItems = AppContainer.continueWatchingStore.load().map { it.toUiItem() }
        if (persistedItems.isNotEmpty()) {
            coordinator.hydrateContinueWatching(persistedItems)
            return
        }

        val record = persistedPlaybackSession ?: return
        val fallbackMediaRef = coordinator.currentState().selectedMedia
            ?: ai.shieldtv.app.core.model.media.MediaRef(
                mediaType = coordinator.currentState().searchMode.mediaType,
                ids = ai.shieldtv.app.core.model.media.MediaIds(tmdbId = null, imdbId = null, tvdbId = null),
                title = record.mediaTitle,
                year = null
            )
        val item = ContinueWatchingHydrator.fromActiveResume(record, fallbackMediaRef) ?: return
        coordinator.recordContinueWatching(
            mediaRef = item.mediaRef ?: fallbackMediaRef,
            artworkUrl = item.artworkUrl,
            seasonNumber = record.seasonNumber,
            episodeNumber = record.episodeNumber,
            progressPercent = item.progressPercent
        ).also {
            AppContainer.continueWatchingStore.record(coordinator.persistedContinueWatchingItem(it))
        }
    }

    private fun resumePositionFor(source: SourceResult, seasonNumber: Int?, episodeNumber: Int?): Long {
        return PlaybackResumeDecider.resumePositionFor(
            record = persistedPlaybackSession,
            remembered = AppContainer.playbackMemoryStore.find(
                mediaRef = source.mediaRef,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            ),
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
            updateSidebarStatus()
            renderCurrentScreen()
        }
    }

    private fun refreshAuthUiOnly() {
        updateSidebarStatus()
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
                    showExitConfirmation()
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
                        statusText.text = settingsCoordinator.favoritesStatusLabel(ai.shieldtv.app.core.model.media.MediaType.MOVIE)
                        renderCurrentScreen()
                    },
                    onMovieHistory = {
                        coordinator.showHistory(
                            SearchMode.MOVIES,
                            AppContainer.watchHistoryCoordinator.listResultsByType(ai.shieldtv.app.core.model.media.MediaType.MOVIE)
                        )
                        statusText.text = settingsCoordinator.historyStatusLabel(ai.shieldtv.app.core.model.media.MediaType.MOVIE)
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
                        statusText.text = settingsCoordinator.favoritesStatusLabel(ai.shieldtv.app.core.model.media.MediaType.SHOW)
                        renderCurrentScreen()
                    },
                    onShowHistory = {
                        coordinator.showHistory(
                            SearchMode.SHOWS,
                            AppContainer.watchHistoryCoordinator.listResultsByType(ai.shieldtv.app.core.model.media.MediaType.SHOW)
                        )
                        statusText.text = settingsCoordinator.historyStatusLabel(ai.shieldtv.app.core.model.media.MediaType.SHOW)
                        renderCurrentScreen()
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
                        item.mediaRef?.let { mediaRef ->
                            lifecycleScope.launch {
                                when (
                                    val selection = browseFlowCoordinator.openResult(
                                        coordinator = coordinator,
                                        result = ai.shieldtv.app.core.model.media.SearchResult(
                                            mediaRef = mediaRef,
                                            subtitle = item.subtitle,
                                            posterUrl = item.artworkUrl,
                                            backdropUrl = item.artworkUrl
                                        )
                                    )
                                ) {
                                    is ai.shieldtv.app.browse.BrowseSelectionResult.Completed -> {
                                        statusText.text = selection.statusMessage
                                        selection.errorMessage?.let { latestPlaybackMessage = it }
                                        renderCurrentScreen()
                                    }
                                }
                            }
                        } ?: runSearch(coordinator.currentState().searchMode, item.queryHint)
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
                        statusText.text = settingsCoordinator.favoritesStatusLabel(ai.shieldtv.app.core.model.media.MediaType.MOVIE)
                    } else {
                        coordinator.showFavorites(
                            SearchMode.SHOWS,
                            AppContainer.favoritesCoordinator.listByType(ai.shieldtv.app.core.model.media.MediaType.SHOW)
                        )
                        statusText.text = settingsCoordinator.favoritesStatusLabel(ai.shieldtv.app.core.model.media.MediaType.SHOW)
                    }
                    renderCurrentScreen()
                },
                onOpenHistory = {
                    if (coordinator.currentState().searchMode == SearchMode.MOVIES) {
                        coordinator.showHistory(
                            SearchMode.MOVIES,
                            AppContainer.watchHistoryCoordinator.listResultsByType(ai.shieldtv.app.core.model.media.MediaType.MOVIE)
                        )
                        statusText.text = settingsCoordinator.historyStatusLabel(ai.shieldtv.app.core.model.media.MediaType.MOVIE)
                    } else {
                        coordinator.showHistory(
                            SearchMode.SHOWS,
                            AppContainer.watchHistoryCoordinator.listResultsByType(ai.shieldtv.app.core.model.media.MediaType.SHOW)
                        )
                        statusText.text = settingsCoordinator.historyStatusLabel(ai.shieldtv.app.core.model.media.MediaType.SHOW)
                    }
                    renderCurrentScreen()
                },
                onBack = {},
                onFirstFocusTarget = ::focusView
            )
            AppDestination.RESULTS -> resultsRenderer.render(
                state = coordinator.currentState(),
                emptyMessage = latestSourcesError ?: "No results.",
                favoriteKeys = AppContainer.favoritesCoordinator.favoriteKeys(),
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
                isFavorite = coordinator.currentState().selectedDetails?.let { details ->
                    AppContainer.favoritesCoordinator.isFavorited(
                        ai.shieldtv.app.core.model.media.SearchResult(
                            mediaRef = details.mediaRef,
                            subtitle = details.overview,
                            posterUrl = details.posterUrl,
                            backdropUrl = details.backdropUrl
                        )
                    )
                } ?: false,
                onToggleFavorite = {
                    coordinator.currentState().selectedDetails?.let { details ->
                        val toggledFavorite = AppContainer.favoritesCoordinator.toggle(
                            ai.shieldtv.app.core.model.media.SearchResult(
                                mediaRef = details.mediaRef,
                                subtitle = details.overview,
                                posterUrl = details.posterUrl,
                                backdropUrl = details.backdropUrl
                            )
                        )
                        statusText.text = if (toggledFavorite) {
                            "Added ${details.mediaRef.title} to favorites"
                        } else {
                            "Removed ${details.mediaRef.title} from favorites"
                        }
                        renderCurrentScreen()
                    }
                },
                onBrowseEpisodes = {
                    coordinator.currentState().selectedDetails?.let { details ->
                        val availableSeasonNumbers = buildList {
                            val reportedSeasonCount = details.seasonCount ?: 0
                            if (reportedSeasonCount > 0) {
                                addAll(1..reportedSeasonCount)
                            }
                            addAll(details.episodesBySeason.keys)
                        }.distinct().sortedDescending()
                        val defaultSeason = availableSeasonNumbers.firstOrNull() ?: (details.seasonCount ?: 1).coerceAtLeast(1)
                        detailsNavigationCoordinator.openEpisodes(
                            coordinator = coordinator,
                            details = details,
                            seasonNumber = coordinator.currentState().selectedSeasonNumber ?: defaultSeason,
                            episodeNumber = coordinator.currentState().selectedEpisodeNumber ?: 1
                        )
                        renderCurrentScreen()
                    }
                },
                onFindSources = {
                    coordinator.currentState().selectedDetails?.let { details ->
                        when (
                            val decision = detailsNavigationCoordinator.requestMovieSources(
                                authLinked = authState.isLinked,
                                mediaRef = details.mediaRef
                            )
                        ) {
                            SourceNavigationDecision.RequiresAuth -> {
                                ensureRealDebridLinkedForSources()
                            }
                            is SourceNavigationDecision.LoadSources -> {
                                loadSourcesFor(
                                    decision.mediaRef,
                                    decision.seasonNumber,
                                    decision.episodeNumber
                                )
                            }
                        }
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
                        detailsNavigationCoordinator.selectEpisode(
                            coordinator = coordinator,
                            details = details,
                            seasonNumber = season,
                            episodeNumber = 1
                        )
                        renderCurrentScreen()
                    }
                },
                onEpisodeSelected = { episode ->
                    coordinator.currentState().selectedDetails?.let { details ->
                        detailsNavigationCoordinator.selectEpisode(
                            coordinator = coordinator,
                            details = details,
                            seasonNumber = coordinator.currentState().selectedSeasonNumber ?: 1,
                            episodeNumber = episode
                        )
                        renderCurrentScreen()
                    }
                },
                onFindSources = {
                    coordinator.currentState().selectedDetails?.let { details ->
                        when (
                            val decision = detailsNavigationCoordinator.requestEpisodeSources(
                                authLinked = authState.isLinked,
                                details = details,
                                seasonNumber = coordinator.currentState().selectedSeasonNumber ?: 1,
                                episodeNumber = coordinator.currentState().selectedEpisodeNumber ?: 1,
                                autoSelectEpisode = false,
                                coordinator = coordinator
                            )
                        ) {
                            SourceNavigationDecision.RequiresAuth -> {
                                ensureRealDebridLinkedForSources()
                            }
                            is SourceNavigationDecision.LoadSources -> {
                                loadSourcesFor(
                                    decision.mediaRef,
                                    decision.seasonNumber,
                                    decision.episodeNumber
                                )
                            }
                        }
                    }
                },
                onEpisodePlay = { episode ->
                    coordinator.currentState().selectedDetails?.let { details ->
                        when (
                            val decision = detailsNavigationCoordinator.requestEpisodeSources(
                                authLinked = authState.isLinked,
                                details = details,
                                seasonNumber = coordinator.currentState().selectedSeasonNumber ?: 1,
                                episodeNumber = episode,
                                autoSelectEpisode = true,
                                coordinator = coordinator
                            )
                        ) {
                            SourceNavigationDecision.RequiresAuth -> {
                                ensureRealDebridLinkedForSources()
                            }
                            is SourceNavigationDecision.LoadSources -> {
                                loadSourcesFor(
                                    decision.mediaRef,
                                    decision.seasonNumber,
                                    decision.episodeNumber
                                )
                            }
                        }
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
                providerSummary = settingsCoordinator.buildProviderSummary(),
                sourcePreferencesSummary = settingsCoordinator.buildSourcePreferencesSummary(),
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
            when (val result = browseFlowCoordinator.runSearch(coordinator, mode, query)) {
                is BrowseSearchResult.Completed -> {
                    setLoading(false, result.statusMessage)
                    latestSourcesError = result.errorMessage
                }
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
            when (val selection = browseFlowCoordinator.openResult(coordinator, result)) {
                is BrowseSelectionResult.Completed -> {
                    setLoading(false, selection.statusMessage)
                    latestSourcesError = selection.errorMessage
                }
            }
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
        statusText.text = settingsCoordinator.favoritesStatusLabel(mediaType)
        renderCurrentScreen()
    }

    private fun refreshHistoryBrowse() {
        val mode = coordinator.currentState().historyBrowseMode ?: return
        val mediaType = if (mode == SearchMode.SHOWS) ai.shieldtv.app.core.model.media.MediaType.SHOW else ai.shieldtv.app.core.model.media.MediaType.MOVIE
        coordinator.showHistory(mode, AppContainer.watchHistoryCoordinator.listResultsByType(mediaType))
        statusText.text = settingsCoordinator.historyStatusLabel(mediaType)
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

        val searchLabel = sourceLoadUiCoordinator.buildSearchLabel(mediaRef, seasonNumber, episodeNumber)

        providerHealthTracker.reset()
        setLoading(true, "Finding sources for $searchLabel…")
        sourceLoadUiCoordinator.applyInitialShellState(coordinator, mediaRef, seasonNumber, episodeNumber)
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
                    sourceLoadUiCoordinator.applyIncrementalState(
                        coordinator = coordinator,
                        mediaRef = mediaRef,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        sources = update.sources
                    )
                    latestSourceDiagnostics = sourceLoadUiCoordinator.incrementalDiagnostics(
                        sources = update.sources,
                        completedProviders = update.completedProviders,
                        totalProviders = update.totalProviders,
                        providerSummary = providerHealthTracker.summary(),
                        buildDiagnostics = ::buildSourceDiagnostics
                    )
                    latestSourcesError = null
                    renderCurrentScreen()
                }
            },
            onCompleted = { result ->
                latestSourceDiagnostics = sourceLoadUiCoordinator.completedDiagnostics(
                    resultDiagnostics = result.diagnostics,
                    sources = result.sources,
                    providerSummary = providerHealthTracker.summary(),
                    buildDiagnostics = ::buildSourceDiagnostics
                )
                latestSourcesError = result.error
                sourceLoadUiCoordinator.applyIncrementalState(
                    coordinator = coordinator,
                    mediaRef = mediaRef,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    sources = result.sources
                )
                dismissModal()
                setLoading(false, sourceLoadUiCoordinator.completedStatusMessage(result.sources.size, searchLabel, result.error))
                renderCurrentScreen()
            }
        )
    }

    private fun showSourceProgressModal(searchLabel: String) {
        val spec = sourceLoadUiCoordinator.progressSpec(
            searchLabel = searchLabel,
            progressItems = sourceLoadingCoordinator.currentProgress(),
            onKeepWaiting = { showSourceProgressModal(searchLabel) },
            onCancel = {
                sourceLoadingCoordinator.cancel()
                dismissModal()
                setLoading(false, "Source lookup cancelled.")
            }
        )
        showInfoModal(
            title = spec.title,
            message = spec.message,
            primaryLabel = spec.primaryLabel,
            onPrimary = spec.onPrimary,
            secondaryLabel = spec.secondaryLabel,
            onSecondary = spec.onSecondary,
            tertiaryLabel = spec.tertiaryLabel,
            onTertiary = spec.onTertiary,
            dismissOnBack = spec.dismissOnBack,
            defaultAction = spec.defaultAction,
            customContent = spec.customContent
        )
    }

    private fun resetRealDebridAuth() {
        activeDeviceFlow = null
        authState = settingsCoordinator.resetAuth()
        latestSourcesError = null
        statusText.text = "Real-Debrid auth reset."
        updateSidebarStatus()
        renderCurrentScreen()
    }

    private fun startRealDebridLink() {
        dismissModal()
        setLoading(true, "Starting Real-Debrid link flow…")
        lifecycleScope.launch {
            when (val result = settingsCoordinator.startLink()) {
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
                    val spec = settingsModalCoordinator.realDebridLinkFailedSpec(
                        failure = result.value,
                        onCopyDebugInfo = ::copyDebugInfoToClipboard
                    )
                    showInfoModal(
                        title = spec.title,
                        message = spec.message,
                        primaryLabel = spec.primaryLabel,
                        onPrimary = spec.onPrimary,
                        secondaryLabel = spec.secondaryLabel,
                        onSecondary = spec.onSecondary,
                        tertiaryLabel = spec.tertiaryLabel,
                        onTertiary = spec.onTertiary,
                        dismissOnBack = spec.dismissOnBack,
                        defaultAction = spec.defaultAction,
                        customContent = spec.customContent
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
        val spec = settingsModalCoordinator.realDebridFlowSpec(
            flow = flow,
            authUrl = authUrl,
            qrContent = buildQrCodeView(flow.qrCodeUrl ?: authUrl),
            onOpenLinkPage = {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(authUrl)))
                showRealDebridFlowModal(flow)
            },
            onCopyDebugInfo = ::copyDebugInfoToClipboard
        )
        showInfoModal(
            title = spec.title,
            message = spec.message,
            primaryLabel = spec.primaryLabel,
            onPrimary = spec.onPrimary,
            secondaryLabel = spec.secondaryLabel,
            onSecondary = spec.onSecondary,
            tertiaryLabel = spec.tertiaryLabel,
            onTertiary = spec.onTertiary,
            dismissOnBack = spec.dismissOnBack,
            defaultAction = spec.defaultAction,
            customContent = spec.customContent
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
                updateSidebarStatus()
                result.statusMessage?.let { statusText.text = it }
                result.errorType?.let { latestPlaybackMessage = "Auth flow warning: $it" }
                result.linkedMessage?.let { linkedMessage ->
                    dismissModal()
                    val spec = settingsModalCoordinator.realDebridLinkedSpec(linkedMessage)
                    showInfoModal(
                        title = spec.title,
                        message = spec.message,
                        primaryLabel = spec.primaryLabel,
                        onPrimary = spec.onPrimary,
                        secondaryLabel = spec.secondaryLabel,
                        onSecondary = spec.onSecondary,
                        tertiaryLabel = spec.tertiaryLabel,
                        onTertiary = spec.onTertiary,
                        dismissOnBack = spec.dismissOnBack,
                        defaultAction = spec.defaultAction,
                        customContent = spec.customContent
                    )
                }
                refreshAuthUiOnly()
            },
            onTimeout = { timeout ->
                authState = timeout.authState
                updateSidebarStatus()
                statusText.text = timeout.statusMessage
                val spec = settingsModalCoordinator.realDebridTimedOutSpec(
                    errorType = timeout.errorType,
                    timeoutMessage = timeout.timeoutMessage,
                    onTryAgain = ::startRealDebridLink
                )
                showInfoModal(
                    title = spec.title,
                    message = spec.message,
                    primaryLabel = spec.primaryLabel,
                    onPrimary = spec.onPrimary,
                    secondaryLabel = spec.secondaryLabel,
                    onSecondary = spec.onSecondary,
                    tertiaryLabel = spec.tertiaryLabel,
                    onTertiary = spec.onTertiary,
                    dismissOnBack = spec.dismissOnBack,
                    defaultAction = spec.defaultAction,
                    customContent = spec.customContent
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
        persistPlaybackCrashContext(
            source = source,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber
        )
        playbackLaunchCoordinator.shouldBlockPlayback(source, authState.isLinked)?.let { blocked ->
            latestPlaybackError = blocked.playbackError
            latestPlaybackMessage = blocked.playbackMessage
            statusText.text = blocked.statusMessage
            coordinator.openSettings()
            renderCurrentScreen()
            return
        }

        latestPlaybackError = null
        latestPlaybackMessage = null
        setLoading(true, "Resolving ${source.displayName}…")

        lifecycleScope.launch {
            when (
                val result = playbackLaunchCoordinator.launch(
                    source = source,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    startPositionMs = if (forceStartAtZero) 0L else resumePositionFor(source, seasonNumber, episodeNumber),
                    latestPlaybackState = latestPlaybackState,
                    coordinator = coordinator,
                    artworkUrl = coordinator.currentState().selectedDetails?.backdropUrl
                        ?: coordinator.currentState().selectedDetails?.posterUrl
                )
            ) {
                is PlaybackLaunchResult.Blocked -> {
                    latestPlaybackError = result.playbackError
                    latestPlaybackMessage = result.playbackMessage
                    setLoading(false, result.statusMessage)
                    coordinator.openSettings()
                }
                is PlaybackLaunchResult.Failed -> {
                    latestPlaybackError = result.playbackError
                    latestPlaybackMessage = result.playbackMessage
                    setLoading(false, result.statusMessage)
                }
                is PlaybackLaunchResult.Prepared -> {
                    latestPlaybackError = null
                    latestPlaybackMessage = null
                    setLoading(false, result.statusMessage)
                    persistedPlaybackSession = AppContainer.playbackSessionStore.loadActiveResume()
                    AppContainer.playbackMemoryStore.record(
                        mediaRef = source.mediaRef,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        source = result.selectedSource,
                        positionMs = if (forceStartAtZero) 0L else resumePositionFor(source, seasonNumber, episodeNumber),
                        durationMs = latestPlaybackState.durationMs,
                        progressPercent = if (latestPlaybackState.durationMs > 0) ((latestPlaybackState.positionMs * 100) / latestPlaybackState.durationMs).toInt() else 0
                    )
                    coordinator.showPlayer(result.selectedSource)
                    AppContainer.playbackEngine.play()
                }
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
            val explanation = AppContainer.explainSourceRanking(top)
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

    private fun currentSourcePreferences(): SourcePreferences = sourcePreferencesCoordinator.currentPreferences()

    private fun currentSourceFilters(): ai.shieldtv.app.core.model.source.SourceFilters {
        val prefs = currentSourcePreferences()
        return ai.shieldtv.app.core.model.source.SourceFilters(
            movieMaxSizeGb = prefs.movieMaxSizeGb,
            episodeMaxSizeGb = prefs.episodeMaxSizeGb
        )
    }

    private fun buildProviderSelectionLabel(): String = sourcePreferencesCoordinator.buildProviderSelectionLabel()

    private fun showMovieMaxSizePicker() {
        val spec = settingsModalCoordinator.movieMaxSizeSpec(
            onSelected = {
                sourcePreferencesCoordinator.setMovieMaxSizeGb(it)
                renderCurrentScreen()
            }
        )
        showInfoModal(
            title = spec.title,
            message = spec.message,
            primaryLabel = spec.primaryLabel,
            onPrimary = spec.onPrimary,
            secondaryLabel = spec.secondaryLabel,
            onSecondary = spec.onSecondary,
            tertiaryLabel = spec.tertiaryLabel,
            onTertiary = spec.onTertiary,
            dismissOnBack = spec.dismissOnBack,
            defaultAction = spec.defaultAction,
            customContent = spec.customContent
        )
    }

    private fun showTvMaxSizePicker() {
        val spec = settingsModalCoordinator.tvMaxSizeSpec(
            onSelected = {
                sourcePreferencesCoordinator.setEpisodeMaxSizeGb(it)
                renderCurrentScreen()
            }
        )
        showInfoModal(
            title = spec.title,
            message = spec.message,
            primaryLabel = spec.primaryLabel,
            onPrimary = spec.onPrimary,
            secondaryLabel = spec.secondaryLabel,
            onSecondary = spec.onSecondary,
            tertiaryLabel = spec.tertiaryLabel,
            onTertiary = spec.onTertiary,
            dismissOnBack = spec.dismissOnBack,
            defaultAction = spec.defaultAction,
            customContent = spec.customContent
        )
    }

    private fun showProviderSelectionModal() {
        showProviderSelectionPage(startIndex = 0)
    }

    private fun showProviderSelectionPage(startIndex: Int) {
        val spec = settingsModalCoordinator.providerSelectionSpec(
            startIndex = startIndex,
            onToggleProvider = ::toggleProvider,
            onEnableAll = ::setAllProvidersEnabled,
            onDisableAll = ::setNoProviderOverrides,
            onNextPage = ::showProviderSelectionPage,
            onOpenActions = ::showProviderSelectionActions
        )
        showInfoModal(
            title = spec.title,
            message = spec.message,
            primaryLabel = spec.primaryLabel,
            onPrimary = spec.onPrimary,
            secondaryLabel = spec.secondaryLabel,
            onSecondary = spec.onSecondary,
            tertiaryLabel = spec.tertiaryLabel,
            onTertiary = spec.onTertiary,
            dismissOnBack = spec.dismissOnBack,
            defaultAction = spec.defaultAction,
            customContent = spec.customContent
        )
    }

    private fun showProviderSelectionActions(allProviders: List<String>) {
        val spec = settingsModalCoordinator.providerSelectionActionsSpec(
            allProviders = allProviders,
            onEnableAll = ::setAllProvidersEnabled,
            onDisableAll = ::setNoProviderOverrides,
            onDone = { renderCurrentScreen() }
        )
        showInfoModal(
            title = spec.title,
            message = spec.message,
            primaryLabel = spec.primaryLabel,
            onPrimary = spec.onPrimary,
            secondaryLabel = spec.secondaryLabel,
            onSecondary = spec.onSecondary,
            tertiaryLabel = spec.tertiaryLabel,
            onTertiary = spec.onTertiary,
            dismissOnBack = spec.dismissOnBack,
            defaultAction = spec.defaultAction,
            customContent = spec.customContent
        )
    }

    private fun toggleProvider(providerId: String) {
        sourcePreferencesCoordinator.toggleProvider(providerId)
        showProviderSelectionModal()
        renderCurrentScreen()
    }

    private fun setAllProvidersEnabled() {
        sourcePreferencesCoordinator.enableAllProviders()
        showProviderSelectionModal()
        renderCurrentScreen()
    }

    private fun setNoProviderOverrides() {
        sourcePreferencesCoordinator.disableAllProviders()
        showProviderSelectionModal()
        renderCurrentScreen()
    }


    private fun resetSourcePreferences() {
        sourcePreferencesCoordinator.reset()
        val spec = settingsModalCoordinator.resetSourcePreferencesSpec(onConfirm = { renderCurrentScreen() })
        showInfoModal(
            title = spec.title,
            message = spec.message,
            primaryLabel = spec.primaryLabel,
            onPrimary = spec.onPrimary,
            secondaryLabel = spec.secondaryLabel,
            onSecondary = spec.onSecondary,
            tertiaryLabel = spec.tertiaryLabel,
            onTertiary = spec.onTertiary,
            dismissOnBack = spec.dismissOnBack,
            defaultAction = spec.defaultAction,
            customContent = spec.customContent
        )
    }

    private fun setLoading(isLoading: Boolean, message: String) {
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        statusText.text = message
    }

    private fun updateSidebarStatus(authLinked: Boolean = authState.isLinked) {
        val visibleUsername = authState.username
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.takeUnless {
                val normalized = it.lowercase()
                normalized == "linked" || normalized == "true" || normalized == "yes"
            }

        realDebridStatusText.text = if (authLinked) {
            visibleUsername?.let { "Real-Debrid: Linked as $it" } ?: "Real-Debrid: Linked"
        } else {
            "Real-Debrid: Not linked"
        }
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
        root.setPadding(44, 28, 44, 28)
        sidebar.visibility = View.VISIBLE
        contentPane.setPadding(30, 30, 30, 30)
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
            handleUpdateCheckResult(
                result = settingsCoordinator.checkForUpdates(),
                showFailureModal = true,
                showNoUpdateStatus = true
            )
        }
    }

    private fun runStartupUpdateCheckIfNeeded() {
        if (hasStartedInitialUpdateCheck || !shouldRunStartupUpdateCheck()) return
        hasStartedInitialUpdateCheck = true
        lifecycleScope.launch {
            val result = settingsCoordinator.checkForUpdates()
            recordStartupUpdateCheckAttempt()
            handleUpdateCheckResult(
                result = result,
                showFailureModal = false,
                showNoUpdateStatus = false
            )
        }
    }

    private fun shouldRunStartupUpdateCheck(nowEpochMs: Long = System.currentTimeMillis()): Boolean {
        val lastCheckedAt = updatePrefs.getLong(KEY_LAST_STARTUP_UPDATE_CHECK_AT, 0L)
        return lastCheckedAt <= 0L || nowEpochMs - lastCheckedAt >= startupUpdateCheckCooldownMs
    }

    private fun recordStartupUpdateCheckAttempt(nowEpochMs: Long = System.currentTimeMillis()) {
        updatePrefs.edit().putLong(KEY_LAST_STARTUP_UPDATE_CHECK_AT, nowEpochMs).apply()
    }

    private fun handleUpdateCheckResult(
        result: ai.shieldtv.app.update.UpdateCheckUiResult,
        showFailureModal: Boolean,
        showNoUpdateStatus: Boolean
    ) {
        latestUpdateInfo = result.updateInfo
        latestUpdateMessage = result.statusMessage
        val statusMessage = when {
            result.updateInfo != null -> result.statusMessage
            showNoUpdateStatus -> result.statusMessage
            else -> ""
        }
        setLoading(false, statusMessage)
        if (result.errorMessage != null) {
            if (showFailureModal) {
                val spec = settingsModalCoordinator.updateCheckFailedSpec(result.errorMessage)
                showInfoModal(
                    title = spec.title,
                    message = spec.message,
                    primaryLabel = spec.primaryLabel,
                    onPrimary = spec.onPrimary,
                    secondaryLabel = spec.secondaryLabel,
                    onSecondary = spec.onSecondary,
                    tertiaryLabel = spec.tertiaryLabel,
                    onTertiary = spec.onTertiary,
                    dismissOnBack = spec.dismissOnBack,
                    defaultAction = spec.defaultAction,
                    customContent = spec.customContent
                )
            }
        } else if (result.updateInfo != null) {
            showUpdateAvailableModal(result.updateInfo)
        }
        renderCurrentScreen()
    }

    private fun showUpdateAvailableModal(updateInfo: AppUpdateInfo) {
        val spec = settingsModalCoordinator.updateAvailableSpec(
            updateInfo = updateInfo,
            onInstall = { openLatestUpdate(updateInfo) },
            onViewReleaseNotes = {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateInfo.pageUrl)))
            }
        )
        showInfoModal(
            title = spec.title,
            message = spec.message,
            primaryLabel = spec.primaryLabel,
            onPrimary = spec.onPrimary,
            secondaryLabel = spec.secondaryLabel,
            onSecondary = spec.onSecondary,
            tertiaryLabel = spec.tertiaryLabel,
            onTertiary = spec.onTertiary,
            dismissOnBack = spec.dismissOnBack,
            defaultAction = spec.defaultAction,
            customContent = spec.customContent
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
                    val spec = settingsModalCoordinator.updateInstallSpec(
                        result = result,
                        onOpenSettings = {
                            result.readiness.openSettingsIntent?.let(::startActivity)
                                ?: startActivity(apkInstaller.buildManageUnknownAppsIntent())
                        }
                    )
                    showInfoModal(
                        title = spec.title,
                        message = spec.message,
                        primaryLabel = spec.primaryLabel,
                        onPrimary = spec.onPrimary,
                        secondaryLabel = spec.secondaryLabel,
                        onSecondary = spec.onSecondary,
                        tertiaryLabel = spec.tertiaryLabel,
                        onTertiary = spec.onTertiary,
                        dismissOnBack = spec.dismissOnBack,
                        defaultAction = spec.defaultAction,
                        customContent = spec.customContent
                    )
                }
                is UpdateInstallUiResult.RequiresSettings -> {
                    setLoading(false, result.statusMessage)
                    val spec = settingsModalCoordinator.updateInstallSpec(
                        result = result,
                        onOpenSettings = {
                            result.readiness.openSettingsIntent?.let(::startActivity)
                        }
                    )
                    showInfoModal(
                        title = spec.title,
                        message = spec.message,
                        primaryLabel = spec.primaryLabel,
                        onPrimary = spec.onPrimary,
                        secondaryLabel = spec.secondaryLabel,
                        onSecondary = spec.onSecondary,
                        tertiaryLabel = spec.tertiaryLabel,
                        onTertiary = spec.onTertiary,
                        dismissOnBack = spec.dismissOnBack,
                        defaultAction = spec.defaultAction,
                        customContent = spec.customContent
                    )
                }
                is UpdateInstallUiResult.Unavailable -> {
                    setLoading(false, result.statusMessage)
                    val spec = settingsModalCoordinator.updateInstallSpec(result)
                    showInfoModal(
                        title = spec.title,
                        message = spec.message,
                        primaryLabel = spec.primaryLabel,
                        onPrimary = spec.onPrimary,
                        secondaryLabel = spec.secondaryLabel,
                        onSecondary = spec.onSecondary,
                        tertiaryLabel = spec.tertiaryLabel,
                        onTertiary = spec.onTertiary,
                        dismissOnBack = spec.dismissOnBack,
                        defaultAction = spec.defaultAction,
                        customContent = spec.customContent
                    )
                }
                is UpdateInstallUiResult.Failure -> {
                    setLoading(false, result.statusMessage)
                    val spec = settingsModalCoordinator.updateInstallSpec(result)
                    showInfoModal(
                        title = spec.title,
                        message = spec.message,
                        primaryLabel = spec.primaryLabel,
                        onPrimary = spec.onPrimary,
                        secondaryLabel = spec.secondaryLabel,
                        onSecondary = spec.onSecondary,
                        tertiaryLabel = spec.tertiaryLabel,
                        onTertiary = spec.onTertiary,
                        dismissOnBack = spec.dismissOnBack,
                        defaultAction = spec.defaultAction,
                        customContent = spec.customContent
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
            appendLine("fatal_playback_report_present=${lastFatalPlaybackReport != null}")
            lastFatalPlaybackReport?.let {
                appendLine("fatal_playback_report_start")
                appendLine(it)
                appendLine("fatal_playback_report_end")
            }
        }
        clipboard?.setPrimaryClip(ClipData.newPlainText("Asahi Debug Info", debugText))
        statusText.text = "Debug info copied to clipboard"
    }

    private fun persistPlaybackCrashContext(
        source: SourceResult,
        seasonNumber: Int?,
        episodeNumber: Int?
    ) {
        val state = coordinator.currentState()
        playbackCrashLogStore.savePlaybackContext(
            buildString {
                appendLine("selected_media=${state.selectedMedia?.title ?: "none"}")
                appendLine("selected_season=${seasonNumber ?: state.selectedSeasonNumber ?: "none"}")
                appendLine("selected_episode=${episodeNumber ?: state.selectedEpisodeNumber ?: "none"}")
                appendLine("selected_source=${source.displayName}")
                appendLine("source_provider=${source.providerDisplayName}")
                appendLine("source_quality=${source.quality}")
                appendLine("source_cache=${source.cacheStatus}")
                appendLine("source_size_bytes=${source.sizeBytes ?: "none"}")
                appendLine("source_info_hash=${source.infoHash ?: "none"}")
                appendLine("source_url=${source.url}")
                appendLine(
                    "source_raw_metadata=${source.rawMetadata.entries.joinToString(",") { "${it.key}=${it.value}" }.ifBlank { "none" }}"
                )
                appendLine("playback_url=${AppContainer.playbackEngine.getCurrentUrl() ?: "none"}")
                appendLine("playback_state=${latestPlaybackState.playerStateLabel}")
                appendLine("playback_error=${latestPlaybackState.errorMessage ?: latestPlaybackError ?: "none"}")
            }.trim()
        )
    }

    private fun detachPlayerFromParent() {
        (playerView.parent as? LinearLayout)?.removeView(playerView)
    }

    private fun focusView(view: View) {
        view.isFocusable = true
        view.isFocusableInTouchMode = true
        view.requestFocus()
    }

    companion object {
        private const val KEY_LAST_STARTUP_UPDATE_CHECK_AT = "last_startup_update_check_at"
    }
}
