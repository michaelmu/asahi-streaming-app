package ai.shieldtv.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
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
import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.source.DebridService
import ai.shieldtv.app.core.model.source.SourceResult
import ai.shieldtv.app.core.model.source.SourceSearchRequest
import ai.shieldtv.app.di.AppContainer
import ai.shieldtv.app.feature.details.presentation.DetailsPresenter
import ai.shieldtv.app.feature.details.presentation.DetailsViewModel
import ai.shieldtv.app.feature.player.presentation.PlayerPresenter
import ai.shieldtv.app.feature.player.presentation.PlayerViewModel
import ai.shieldtv.app.feature.search.presentation.SearchPresenter
import ai.shieldtv.app.feature.search.presentation.SearchViewModel
import ai.shieldtv.app.feature.sources.presentation.SourcesPresenter
import ai.shieldtv.app.feature.sources.presentation.SourcesViewModel
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
import ai.shieldtv.app.ui.DetailsScreenRenderer
import ai.shieldtv.app.ui.EpisodePickerScreenRenderer
import ai.shieldtv.app.ui.HomeScreenRenderer
import ai.shieldtv.app.ui.NavigationRailRenderer
import ai.shieldtv.app.ui.PlayerScreenRenderer
import ai.shieldtv.app.ui.ResultsScreenRenderer
import ai.shieldtv.app.ui.ScreenViewFactory
import ai.shieldtv.app.ui.SearchScreenRenderer
import ai.shieldtv.app.ui.SettingsScreenRenderer
import ai.shieldtv.app.ui.SourcesScreenRenderer
import ai.shieldtv.app.update.AppUpdateInfo
import ai.shieldtv.app.update.GitHubReleaseUpdateChecker
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

    private lateinit var root: LinearLayout
    private lateinit var sidebar: LinearLayout
    private lateinit var contentPane: LinearLayout
    private lateinit var railHost: LinearLayout
    private lateinit var contentScrollView: ScrollView
    private lateinit var statusText: android.widget.TextView
    private lateinit var loadingView: ProgressBar
    private lateinit var screenHost: LinearLayout
    private lateinit var playerView: PlayerView

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
        if (keyCode == KeyEvent.KEYCODE_BACK && handleBackPress()) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun buildContentView(): View {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(48, 32, 48, 32)
            setBackgroundResource(ai.shieldtv.app.R.drawable.asahi_app_bg)
        }

        sidebar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.26f)
            setPadding(0, 0, 32, 0)
            setBackgroundResource(ai.shieldtv.app.R.drawable.asahi_panel_bg)
            setPadding(24, 24, 24, 24)
        }

        contentPane = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.74f)
            setBackgroundResource(ai.shieldtv.app.R.drawable.asahi_panel_elevated_bg)
            setPadding(28, 28, 28, 28)
        }

        val title = viewFactory.railTitle("Asahi")
        val subtitle = viewFactory.caption("TV-first streaming shell")
        val buildInfo = viewFactory.caption("${BuildConfig.VERSION_NAME}")

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

        return root
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
        val item = ContinueWatchingHydrator.fromPersistedSession(record) ?: return
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
                selectedMode = coordinator.currentState().searchMode,
                inSettings = destination == AppDestination.SETTINGS,
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
                homeRenderer.render(
                    state = coordinator.currentState(),
                    authLinked = authState.isLinked,
                    statusMessage = statusText.text?.toString().orEmpty(),
                    onOpenMovies = {
                        coordinator.openSearch(SearchMode.MOVIES)
                        renderCurrentScreen()
                    },
                    onOpenShows = {
                        coordinator.openSearch(SearchMode.SHOWS)
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
                    onFirstFocusTarget = ::focusView
                )
            }
            AppDestination.SEARCH -> searchRenderer.render(
                state = coordinator.currentState(),
                onSearch = ::runSearch,
                onBack = {},
                onFirstFocusTarget = ::focusView
            )
            AppDestination.RESULTS -> resultsRenderer.render(
                state = coordinator.currentState(),
                emptyMessage = latestSourcesError ?: "No results.",
                onResultSelected = ::onSearchResultSelected,
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
                playbackModeLabel = currentRenderModeLabel(),
                updateSummary = latestUpdateMessage,
                buildAuthUrl = ::buildRealDebridAuthUrl,
                onStartLink = ::startRealDebridLink,
                onResetAuth = ::resetRealDebridAuth,
                onTogglePlaybackMode = ::toggleRenderMode,
                onCopyDebugInfo = ::copyDebugInfoToClipboard,
                onCheckForUpdates = ::checkForUpdates,
                onOpenLatestUpdate = latestUpdateInfo?.let { { openLatestUpdate(it) } },
                onBackToHome = {
                    coordinator.openHome()
                    renderCurrentScreen()
                },
                onFirstFocusTarget = ::focusView
            )
        }
    }

    private fun runSearch(mode: SearchMode, rawQuery: String) {
        val query = rawQuery.trim().ifEmpty { "Dune" }
        latestSourcesError = null
        latestSourceDiagnostics = null
        latestPlaybackMessage = null
        latestPlaybackError = null
        AppContainer.playbackEngine.stop()
        setLoading(true, "Searching for \"$query\"…")

        lifecycleScope.launch {
            val state = searchViewModel.search(query)
            val filteredResults = state.results.filter { it.mediaRef.mediaType == mode.mediaType }
            coordinator.showResults(query = query, results = filteredResults)
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

        setLoading(true, "Finding sources for $searchLabel…")

        lifecycleScope.launch {
            val state = sourcesViewModel.load(
                SourceSearchRequest(
                    mediaRef = mediaRef,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber
                )
            )
            val filteredSources = state.sources.filter { it.debridService != DebridService.NONE || authState.isLinked }
            latestSourceDiagnostics = state.diagnostics ?: buildSourceDiagnostics(filteredSources)
            latestSourcesError = state.error
            coordinator.showSources(
                mediaRef = mediaRef,
                details = coordinator.currentState().selectedDetails,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                sources = filteredSources
            )
            setLoading(false, state.error ?: "Found ${filteredSources.size} source(s) for $searchLabel.")
            renderCurrentScreen()
        }
    }

    private fun resetRealDebridAuth() {
        AppContainer.clearRealDebridAuth()
        activeDeviceFlow = null
        authPollingJob?.cancel()
        authState = RealDebridAuthState(isLinked = false, authInProgress = false, lastError = null)
        latestSourcesError = null
        statusText.text = "Real-Debrid auth reset."
        renderCurrentScreen()
    }

    private fun startRealDebridLink() {
        setLoading(true, "Starting Real-Debrid link flow…")
        lifecycleScope.launch {
            runCatching {
                AppContainer.startRealDebridDeviceFlowUseCase()
            }.onSuccess { flow ->
                activeDeviceFlow = flow
                authState = RealDebridAuthState(isLinked = false, authInProgress = true, lastError = null)
                setLoading(false, "Real-Debrid device flow started: ${flow.userCode}")
                refreshAuthUiOnly()
                startAutoPolling(flow)
            }.onFailure { error ->
                authPollingJob?.cancel()
                authState = RealDebridAuthState(isLinked = false, authInProgress = false, lastError = error.message)
                setLoading(false, "Failed to start Real-Debrid link flow.")
                refreshAuthUiOnly()
            }
        }
    }

    private fun startAutoPolling(flow: DeviceCodeFlow) {
        authPollingJob?.cancel()
        authPollingJob = lifecycleScope.launch {
            val startedAt = System.currentTimeMillis()
            val timeoutMs = 2 * 60 * 1000L
            while (System.currentTimeMillis() - startedAt < timeoutMs && !authState.isLinked) {
                delay(flow.pollIntervalSeconds.coerceAtLeast(2) * 1000L)
                runCatching {
                    AppContainer.pollRealDebridDeviceFlowUseCase(flow)
                }.onSuccess { state ->
                    authState = state
                    if (state.isLinked) {
                        activeDeviceFlow = null
                        statusText.text = "Real-Debrid linked successfully."
                        refreshAuthUiOnly()
                        return@launch
                    }
                    refreshAuthUiOnly()
                }.onFailure { error ->
                    authState = RealDebridAuthState(
                        isLinked = false,
                        authInProgress = true,
                        lastError = error.message
                    )
                    refreshAuthUiOnly()
                }
            }
            if (!authState.isLinked && activeDeviceFlow != null) {
                authState = authState.copy(
                    authInProgress = false,
                    lastError = authState.lastError ?: "Real-Debrid link timed out after 2 minutes."
                )
                statusText.text = "Real-Debrid link polling timed out."
                refreshAuthUiOnly()
            }
        }
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
            latestPlaybackMessage = buildString {
                appendLine(if (state.prepared) "Playback preparation succeeded." else "Playback preparation failed.")
                appendLine("Current item: ${AppContainer.playbackEngine.getCurrentItem()?.title ?: source.displayName}")
                appendLine("Stream URL: ${state.playbackUrl ?: AppContainer.playbackEngine.getCurrentUrl() ?: source.url}")
                appendLine("Player state: ${latestPlaybackState.playerStateLabel}")
                appendLine("Video format: ${latestPlaybackState.videoFormat ?: "unknown"}")
                appendLine("Video size: ${latestPlaybackState.videoSizeLabel ?: "unknown"}")
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
                    AppContainer.playbackSessionStore.save(
                        item = currentItem,
                        state = latestPlaybackState,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber
                    )
                    persistedPlaybackSession = AppContainer.playbackSessionStore.load()
                }
                coordinator.showPlayer(source)
                AppContainer.playbackEngine.play()
            }
            renderCurrentScreen()
        }
    }

    private fun buildSourceDiagnostics(sources: List<SourceResult>): String {
        if (sources.isEmpty()) return "providers=none | live=0 | fallback=0"
        val providerSummary = sources.groupBy { it.providerId }
            .entries
            .joinToString(",") { (providerId, items) -> "$providerId:${items.size}" }
        val liveCount = sources.count {
            it.providerId == "torrentio" || it.rawMetadata["transport"] == "torrentio"
        }
        val fallbackCount = sources.count { it.rawMetadata["fallbackMode"] == "true" }
        return "providers=$providerSummary | live=$liveCount | fallback=$fallbackCount"
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
        setLoading(true, "Checking for updates…")
        lifecycleScope.launch {
            val result = runCatching {
                GitHubReleaseUpdateChecker(
                    owner = "michaelmu",
                    repo = "asahi-streaming-app",
                    currentVersionName = BuildConfig.VERSION_NAME
                ).check()
            }
            result.onSuccess { checkResult ->
                latestUpdateInfo = checkResult.updateInfo
                latestUpdateMessage = checkResult.statusMessage
                setLoading(false, latestUpdateMessage ?: "Update check complete.")
                renderCurrentScreen()
            }.onFailure { error ->
                latestUpdateInfo = null
                latestUpdateMessage = buildString {
                    append("Update check failed")
                    error::class.simpleName?.let {
                        append(" (")
                        append(it)
                        append(")")
                    }
                    error.message?.takeIf { it.isNotBlank() }?.let {
                        append(": ")
                        append(it.take(180))
                    }
                }
                setLoading(false, latestUpdateMessage ?: "Update check failed.")
                renderCurrentScreen()
            }
        }
    }

    private fun openLatestUpdate(updateInfo: AppUpdateInfo) {
        val url = updateInfo.downloadUrl.ifBlank { updateInfo.pageUrl }
        if (url.isBlank()) {
            setLoading(false, "No APK URL available for this update.")
            return
        }
        lifecycleScope.launch {
            try {
                setLoading(true, "Downloading update APK…")
                val apkFile = java.io.File(cacheDir, "updates/asahi-update.apk")
                apkDownloadManager.download(url, apkFile)
                setLoading(false, "Launching package installer…")
                startActivity(apkInstaller.buildInstallIntent(apkFile))
            } catch (error: Throwable) {
                setLoading(false, "Update download/install failed: ${error.message ?: error::class.simpleName}")
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
