package ai.shieldtv.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.media.EpisodeSummary
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.media.TitleDetails
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

    private lateinit var root: LinearLayout
    private lateinit var statusText: TextView
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
        setContentView(buildContentView())
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
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        val title = TextView(this).apply {
            text = "Asahi"
            textSize = 28f
        }
        val subtitle = TextView(this).apply {
            text = "Page-based in-app search → details → sources → playback flow"
            textSize = 16f
        }
        val buildInfo = TextView(this).apply {
            text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · ${BuildConfig.GIT_SHA}"
            textSize = 14f
        }

        loadingView = ProgressBar(this).apply {
            visibility = View.GONE
        }

        statusText = TextView(this).apply {
            text = "Ready"
            textSize = 15f
        }

        screenHost = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(buildInfo)
        root.addView(space())
        root.addView(loadingView)
        root.addView(statusText)
        root.addView(space())
        root.addView(
            ScrollView(this).apply {
                addView(screenHost)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        return root
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
                if (details != null && details.mediaRef.mediaType == MediaType.SHOW) {
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
            AppDestination.HOME -> renderHomeScreen()
            AppDestination.SEARCH -> renderSearchScreen()
            AppDestination.RESULTS -> renderResultsScreen()
            AppDestination.DETAILS -> renderDetailsScreen()
            AppDestination.EPISODES -> renderEpisodesScreen()
            AppDestination.SOURCES -> renderSourcesScreen()
            AppDestination.PLAYER -> renderPlayerScreen()
            AppDestination.SETTINGS -> renderSettingsScreen()
        }
    }

    private fun renderHomeScreen() {
        screenHost.addView(screenTitle("Home"))
        screenHost.addView(bodyText("Start with a content mode, or open settings/accounts."))
        screenHost.addView(primaryButton("Movies") {
            coordinator.openSearch(SearchMode.MOVIES)
            renderCurrentScreen()
        })
        screenHost.addView(primaryButton("TV Shows") {
            coordinator.openSearch(SearchMode.SHOWS)
            renderCurrentScreen()
        })
        screenHost.addView(primaryButton(settingsLabel()) {
            coordinator.openSettings()
            renderCurrentScreen()
        })
    }

    private fun renderSearchScreen() {
        val state = coordinator.currentState()
        screenHost.addView(screenTitle("Search ${state.searchMode.label}"))
        screenHost.addView(bodyText("Enter a title to search ${state.searchMode.label.lowercase()}."))

        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val queryInput = EditText(this).apply {
            setText(state.query)
            hint = when (state.searchMode) {
                SearchMode.MOVIES -> "Search movies"
                SearchMode.SHOWS -> "Search TV shows"
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val searchButton = Button(this).apply {
            text = "Search"
            setOnClickListener { runSearch(state.searchMode, queryInput.text.toString()) }
        }
        searchRow.addView(queryInput)
        searchRow.addView(searchButton)

        screenHost.addView(searchRow)
        screenHost.addView(space())
        screenHost.addView(primaryButton("Back to Home") {
            coordinator.openHome()
            renderCurrentScreen()
        })
    }

    private fun renderResultsScreen() {
        val state = coordinator.currentState()
        screenHost.addView(screenTitle("Results"))
        screenHost.addView(bodyText("Query: ${state.query}"))

        if (state.searchResults.isEmpty()) {
            screenHost.addView(bodyText(latestSourcesError ?: "No results."))
        } else {
            state.searchResults.take(20).forEach { result ->
                screenHost.addView(Button(this).apply {
                    text = buildString {
                        append(result.mediaRef.title)
                        result.mediaRef.year?.let { append(" ($it)") }
                        result.subtitle?.takeIf { it.isNotBlank() }?.let {
                            append(" — ")
                            append(it)
                        }
                    }
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    setOnClickListener { onSearchResultSelected(result) }
                })
            }
        }

        screenHost.addView(space())
        screenHost.addView(primaryButton("New Search") {
            coordinator.openSearch(state.searchMode)
            renderCurrentScreen()
        })
    }

    private fun renderDetailsScreen() {
        val details = coordinator.currentState().selectedDetails
        if (details == null) {
            screenHost.addView(screenTitle("Details"))
            screenHost.addView(bodyText("No details loaded."))
            return
        }

        screenHost.addView(screenTitle(details.mediaRef.title))
        screenHost.addView(
            bodyText(
                buildString {
                    appendLine("Type: ${details.mediaRef.mediaType}")
                    details.mediaRef.year?.let { appendLine("Year: $it") }
                    if (details.genres.isNotEmpty()) appendLine("Genres: ${details.genres.joinToString()}")
                    details.runtimeMinutes?.let { appendLine("Runtime: ${it}m") }
                    details.seasonCount?.let { appendLine("Seasons: $it") }
                    details.episodeCount?.let { appendLine("Episodes: $it") }
                    appendLine()
                    append(details.overview ?: "No overview yet.")
                }
            )
        )

        if (details.mediaRef.mediaType == MediaType.SHOW) {
            screenHost.addView(primaryButton("Browse Episodes") {
                coordinator.showEpisodes(
                    details = details,
                    seasonNumber = coordinator.currentState().selectedSeasonNumber ?: 1,
                    episodeNumber = coordinator.currentState().selectedEpisodeNumber ?: 1
                )
                renderCurrentScreen()
            })
        } else {
            screenHost.addView(primaryButton("Find Sources") {
                loadSourcesFor(details.mediaRef, null, null)
            })
        }
    }

    private fun renderEpisodesScreen() {
        val state = coordinator.currentState()
        val details = state.selectedDetails
        if (details == null) {
            screenHost.addView(screenTitle("Episodes"))
            screenHost.addView(bodyText("No show loaded."))
            return
        }

        screenHost.addView(screenTitle("${details.mediaRef.title} Episodes"))
        screenHost.addView(bodyText("Pick a season and episode, then load sources."))

        val knownSeasonCount = (details.seasonCount ?: 3).coerceAtMost(12)
        val selectedSeason = state.selectedSeasonNumber ?: 1
        val selectedEpisode = state.selectedEpisodeNumber ?: 1

        val seasonStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        (1..knownSeasonCount).forEach { season ->
            seasonStrip.addView(Button(this).apply {
                text = if (season == selectedSeason) {
                    "• S${season.toString().padStart(2, '0')}"
                } else {
                    "S${season.toString().padStart(2, '0')}"
                }
                setOnClickListener {
                    coordinator.showEpisodes(details, season, 1)
                    renderCurrentScreen()
                }
            })
        }
        screenHost.addView(HorizontalScrollView(this).apply { addView(seasonStrip) })

        val realEpisodes = details.episodesBySeason[selectedSeason].orEmpty()
        val episodeChoices = if (realEpisodes.isNotEmpty()) {
            realEpisodes.take(20)
        } else {
            (1..12).map { episode ->
                EpisodeSummary(
                    seasonNumber = selectedSeason,
                    episodeNumber = episode,
                    title = "Episode $episode"
                )
            }
        }

        episodeChoices.forEach { episode ->
            val isSelected = selectedEpisode == episode.episodeNumber
            screenHost.addView(Button(this).apply {
                text = buildString {
                    if (isSelected) append("• ")
                    append("E")
                    append(episode.episodeNumber.toString().padStart(2, '0'))
                    episode.title?.takeIf { it.isNotBlank() }?.let {
                        append(" — ")
                        append(it.take(48))
                    }
                    episode.airDate?.let {
                        append(" [")
                        append(it)
                        append("]")
                    }
                    episode.overview?.takeIf { it.isNotBlank() }?.let {
                        append("\n")
                        append(it.take(140))
                        if (it.length > 140) append("…")
                    }
                }
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setOnClickListener {
                    coordinator.showEpisodes(details, selectedSeason, episode.episodeNumber)
                    renderCurrentScreen()
                }
            })
        }

        screenHost.addView(space())
        screenHost.addView(primaryButton("Find Sources for S${selectedSeason.toString().padStart(2, '0')}E${selectedEpisode.toString().padStart(2, '0')}") {
            loadSourcesFor(details.mediaRef, selectedSeason, selectedEpisode)
        })
    }

    private fun renderSourcesScreen() {
        val state = coordinator.currentState()
        val mediaRef = state.selectedMedia
        screenHost.addView(screenTitle("Sources"))
        if (mediaRef != null) {
            val label = if (state.selectedSeasonNumber != null && state.selectedEpisodeNumber != null) {
                "${mediaRef.title} S${state.selectedSeasonNumber.toString().padStart(2, '0')}E${state.selectedEpisodeNumber.toString().padStart(2, '0')}"
            } else {
                mediaRef.title
            }
            screenHost.addView(bodyText(label))
        }

        latestSourcesError?.let {
            screenHost.addView(bodyText("Source lookup failed: $it"))
        }
        latestSourceDiagnostics?.let {
            screenHost.addView(bodyText("Diagnostics: $it"))
        }

        if (state.selectedSources.isEmpty()) {
            screenHost.addView(bodyText("No sources."))
            return
        }

        state.selectedSources.take(12).forEach { source ->
            screenHost.addView(Button(this).apply {
                text = buildString {
                    append(source.displayName)
                    append("\n")
                    append(source.quality)
                    append(" · ")
                    append(source.cacheStatus)
                    append(" · ")
                    append(source.providerDisplayName)
                    source.sizeLabel?.let {
                        append(" · ")
                        append(it)
                    }
                }
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setOnClickListener {
                    preparePlayback(
                        source = source,
                        seasonNumber = state.selectedSeasonNumber,
                        episodeNumber = state.selectedEpisodeNumber
                    )
                }
            })
        }
    }

    private fun renderPlayerScreen() {
        val state = coordinator.currentState()
        val source = state.selectedSource
        screenHost.addView(screenTitle("Player"))
        if (source == null) {
            screenHost.addView(bodyText("No source selected."))
            return
        }

        screenHost.addView(
            bodyText(
                buildString {
                    appendLine("Selected source: ${source.displayName}")
                    appendLine("Provider: ${source.providerDisplayName}")
                    appendLine("Quality: ${source.quality}")
                    appendLine("Cache: ${source.cacheStatus}")
                    if (state.selectedSeasonNumber != null && state.selectedEpisodeNumber != null) {
                        appendLine(
                            "Episode target: S${state.selectedSeasonNumber.toString().padStart(2, '0')}E${state.selectedEpisodeNumber.toString().padStart(2, '0')}"
                        )
                    }
                    appendLine()
                    append(latestPlaybackMessage ?: "Preparing playback…")
                }
            )
        )
        latestPlaybackError?.let {
            screenHost.addView(bodyText("Playback error: $it"))
        }

        playerView.visibility = if (source != null) View.VISIBLE else View.GONE
        detachPlayerFromParent()
        screenHost.addView(playerView)
        screenHost.addView(space())
        screenHost.addView(buildPlaybackControls())
    }

    private fun renderSettingsScreen() {
        screenHost.addView(screenTitle("Settings / Accounts"))
        screenHost.addView(
            bodyText(
                if (authState.isLinked) {
                    "Real-Debrid linked${authState.username?.let { " as $it" } ?: ""}."
                } else {
                    "Real-Debrid not linked. Debrid-backed playback will fail until you link it."
                }
            )
        )
        screenHost.addView(bodyText("Playback mode: ${currentRenderModeLabel()}"))

        if (!authState.isLinked) {
            screenHost.addView(primaryButton("Start Real-Debrid Link") {
                startRealDebridLink()
            })
        }

        activeDeviceFlow?.let { flow ->
            screenHost.addView(
                bodyText(
                    buildString {
                        appendLine("Open: ${flow.verificationUrl}")
                        appendLine("Code: ${flow.userCode}")
                        append("Polling starts automatically for up to 2 minutes after you begin linking.")
                    }
                )
            )
            screenHost.addView(primaryButton("Open Real-Debrid Link Page") {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(buildRealDebridAuthUrl(flow))))
            })
            screenHost.addView(primaryButton("Poll Link Status") {
                pollRealDebridLink(flow)
            })
        }

        authState.lastError?.takeIf { it.isNotBlank() }?.let {
            screenHost.addView(bodyText("Auth error: $it"))
        }

        screenHost.addView(primaryButton("Toggle Playback Mode") {
            toggleRenderMode()
        })
        screenHost.addView(primaryButton("Copy Debug Info") {
            copyDebugInfoToClipboard()
        })
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
            latestSourceDiagnostics = buildSourceDiagnostics(state.sources)
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

    private fun screenTitle(text: String): View {
        return TextView(this).apply {
            this.text = text
            textSize = 24f
        }
    }

    private fun bodyText(text: String): View {
        return TextView(this).apply {
            this.text = text
            textSize = 16f
        }
    }

    private fun primaryButton(text: String, onClick: () -> Unit): View {
        return Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
        }
    }

    private fun space(): View = TextView(this).apply { text = "" }
}
