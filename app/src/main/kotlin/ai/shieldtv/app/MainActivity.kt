package ai.shieldtv.app

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.Button
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
import ai.shieldtv.app.navigation.AppDestination
import ai.shieldtv.app.ui.DetailsScreenRenderer
import ai.shieldtv.app.ui.EpisodePickerScreenRenderer
import ai.shieldtv.app.ui.HomeScreenRenderer
import ai.shieldtv.app.ui.PlayerScreenRenderer
import ai.shieldtv.app.ui.ResultsScreenRenderer
import ai.shieldtv.app.ui.ScreenViewFactory
import ai.shieldtv.app.ui.SearchScreenRenderer
import ai.shieldtv.app.ui.SettingsScreenRenderer
import ai.shieldtv.app.ui.SourcesScreenRenderer
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
    private lateinit var viewFactory: ScreenViewFactory
    private lateinit var homeRenderer: HomeScreenRenderer
    private lateinit var searchRenderer: SearchScreenRenderer
    private lateinit var resultsRenderer: ResultsScreenRenderer
    private lateinit var detailsRenderer: DetailsScreenRenderer
    private lateinit var episodesRenderer: EpisodePickerScreenRenderer
    private lateinit var sourcesRenderer: SourcesScreenRenderer
    private lateinit var playerRenderer: PlayerScreenRenderer
    private lateinit var settingsRenderer: SettingsScreenRenderer

    private lateinit var root: LinearLayout
    private lateinit var statusText: android.widget.TextView
    private lateinit var loadingView: ProgressBar
    private lateinit var screenHost: LinearLayout
    private lateinit var playerView: PlayerView

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewFactory = ScreenViewFactory(this)
        setContentView(buildContentView())
        initializeRenderers()
        attachPlayerView()
        observePlaybackState()
        refreshAuthState()
        renderCurrentScreen()
    }

    override fun onDestroy() {
        authPollingJob?.cancel()
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
        }

        val sidebar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.34f)
            setPadding(0, 0, 32, 0)
        }

        val contentPane = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 0.66f)
        }

        val title = viewFactory.title("Asahi")
        val subtitle = viewFactory.body("TV-first search → details → sources → playback")
        val buildInfo = viewFactory.body("${BuildConfig.VERSION_NAME} · ${BuildConfig.GIT_SHA}")

        loadingView = ProgressBar(this).apply {
            visibility = View.GONE
        }

        statusText = android.widget.TextView(this).apply {
            text = "Ready"
            textSize = 15f
        }

        screenHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        sidebar.addView(title)
        sidebar.addView(subtitle)
        sidebar.addView(buildInfo)
        sidebar.addView(viewFactory.spacer())
        sidebar.addView(loadingView)
        sidebar.addView(statusText)

        contentPane.addView(
            ScrollView(this).apply {
                isFillViewport = true
                addView(screenHost)
            },
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
        homeRenderer = HomeScreenRenderer(screenHost, viewFactory)
        searchRenderer = SearchScreenRenderer(this, screenHost, viewFactory)
        resultsRenderer = ResultsScreenRenderer(this, screenHost, viewFactory)
        detailsRenderer = DetailsScreenRenderer(screenHost, viewFactory)
        episodesRenderer = EpisodePickerScreenRenderer(this, screenHost, viewFactory)
        sourcesRenderer = SourcesScreenRenderer(this, screenHost, viewFactory)
        playerRenderer = PlayerScreenRenderer(screenHost, viewFactory)
        settingsRenderer = SettingsScreenRenderer(this, screenHost, viewFactory)
    }

    private fun createPlayerView(): PlayerView {
        return PlayerView(this).apply {
            useController = true
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                720
            )
        }
    }

    private fun attachPlayerView() {
        val engine = AppContainer.playbackEngine as? Media3PlaybackEngine ?: return
        playerView = createPlayerView().apply {
            player = engine.attach(this@MainActivity)
        }
    }

    private fun handleBackPress(): Boolean {
        return when (coordinator.currentState().destination) {
            AppDestination.HOME -> false
            AppDestination.SEARCH,
            AppDestination.RESULTS,
            AppDestination.SETTINGS -> {
                coordinator.openHome()
                renderCurrentScreen()
                true
            }
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
        screenHost.removeAllViews()
        when (coordinator.currentState().destination) {
            AppDestination.HOME -> homeRenderer.render(
                settingsLabel = settingsLabel(),
                onMovies = {
                    coordinator.openSearch(SearchMode.MOVIES)
                    renderCurrentScreen()
                },
                onShows = {
                    coordinator.openSearch(SearchMode.SHOWS)
                    renderCurrentScreen()
                },
                onSettings = {
                    coordinator.openSettings()
                    renderCurrentScreen()
                },
                onFirstFocusTarget = ::focusView
            )
            AppDestination.SEARCH -> searchRenderer.render(
                state = coordinator.currentState(),
                onSearch = ::runSearch,
                onBack = {
                    coordinator.openHome()
                    renderCurrentScreen()
                },
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
                        loadSourcesFor(
                            details.mediaRef,
                            coordinator.currentState().selectedSeasonNumber ?: 1,
                            coordinator.currentState().selectedEpisodeNumber ?: 1
                        )
                    }
                },
                onFirstFocusTarget = ::focusView
            )
            AppDestination.SOURCES -> sourcesRenderer.render(
                state = coordinator.currentState(),
                diagnostics = latestSourceDiagnostics,
                error = latestSourcesError,
                onSourceSelected = { source ->
                    preparePlayback(
                        source = source,
                        seasonNumber = coordinator.currentState().selectedSeasonNumber,
                        episodeNumber = coordinator.currentState().selectedEpisodeNumber
                    )
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
                    playbackControls = buildPlaybackControls()
                )
            }
            AppDestination.SETTINGS -> settingsRenderer.render(
                authState = authState,
                activeDeviceFlow = activeDeviceFlow,
                playbackModeLabel = currentRenderModeLabel(),
                buildAuthUrl = ::buildRealDebridAuthUrl,
                onStartLink = ::startRealDebridLink,
                onPoll = ::pollRealDebridLink,
                onTogglePlaybackMode = ::toggleRenderMode,
                onCopyDebugInfo = ::copyDebugInfoToClipboard,
                onFirstFocusTarget = ::focusView
            )
        }
    }

    private fun settingsLabel(): String {
        return if (authState.isLinked) {
            "Settings / Accounts (RD linked)"
        } else {
            "Settings / Accounts (RD not linked)"
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
            latestSourceDiagnostics = state.diagnostics ?: buildSourceDiagnostics(state.sources)
            latestSourcesError = state.error
            coordinator.showSources(
                mediaRef = mediaRef,
                details = coordinator.currentState().selectedDetails,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                sources = state.sources
            )
            setLoading(false, state.error ?: "Found ${state.sources.size} source(s) for $searchLabel.")
            renderCurrentScreen()
        }
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

    private fun pollRealDebridLink(flow: DeviceCodeFlow) {
        setLoading(true, "Polling Real-Debrid link status…")
        lifecycleScope.launch {
            runCatching {
                AppContainer.pollRealDebridDeviceFlowUseCase(flow)
            }.onSuccess { state ->
                authState = state
                if (state.isLinked) {
                    authPollingJob?.cancel()
                    activeDeviceFlow = null
                    setLoading(false, "Real-Debrid linked successfully.")
                } else {
                    setLoading(false, "Real-Debrid still waiting for authorization.")
                }
                refreshAuthUiOnly()
            }.onFailure { error ->
                authState = RealDebridAuthState(isLinked = false, authInProgress = true, lastError = error.message)
                setLoading(false, "Polling Real-Debrid link failed.")
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
        episodeNumber: Int? = source.episodeNumber
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
                episodeNumber = episodeNumber
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
                coordinator.showPlayer(source)
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

    private fun buildPlaybackControls(): View {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(Button(this@MainActivity).apply {
                text = "Play"
                setOnClickListener {
                    AppContainer.playbackEngine.play()
                    statusText.text = "Playback state: playing"
                }
            })
            addView(Button(this@MainActivity).apply {
                text = "Pause"
                setOnClickListener {
                    AppContainer.playbackEngine.pause()
                    statusText.text = "Playback state: paused"
                }
            })
            addView(Button(this@MainActivity).apply {
                text = "Stop"
                setOnClickListener {
                    AppContainer.playbackEngine.stop()
                    statusText.text = "Playback state: stopped"
                }
            })
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
