package ai.shieldtv.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private lateinit var statusText: TextView
    private lateinit var loadingView: ProgressBar
    private lateinit var queryInput: EditText
    private lateinit var authContainer: LinearLayout
    private lateinit var resultsContainer: LinearLayout
    private lateinit var detailsContainer: LinearLayout
    private lateinit var episodeSelectionContainer: LinearLayout
    private lateinit var sourcesContainer: LinearLayout
    private lateinit var playbackContainer: LinearLayout
    private lateinit var playbackControlsContainer: LinearLayout
    private lateinit var playerView: PlayerView

    private var selectedMediaRef: MediaRef? = null
    private var selectedSeasonNumber: Int? = null
    private var selectedEpisodeNumber: Int? = null
    private var selectedTitleDetails: TitleDetails? = null
    private var authState: RealDebridAuthState = RealDebridAuthState(isLinked = false)
    private var activeDeviceFlow: DeviceCodeFlow? = null
    private var authPollingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContentView())
        attachPlayerView()
        refreshAuthPanel()
        runInitialSearch()
    }

    override fun onDestroy() {
        authPollingJob?.cancel()
        super.onDestroy()
        AppContainer.playbackEngine.release()
    }

    private fun buildContentView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        val title = TextView(this).apply {
            text = "Asahi"
            textSize = 28f
        }
        val subtitle = TextView(this).apply {
            text = "Minimal in-app search → details → sources → playback flow"
            textSize = 16f
        }
        val buildInfo = TextView(this).apply {
            text = "${ai.shieldtv.app.BuildConfig.VERSION_NAME} (${ai.shieldtv.app.BuildConfig.VERSION_CODE}) · ${ai.shieldtv.app.BuildConfig.GIT_SHA}"
            textSize = 14f
        }

        val searchRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        queryInput = EditText(this).apply {
            setText("Dune")
            hint = "Search title"
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val searchButton = Button(this).apply {
            text = "Search"
            setOnClickListener { runSearch(queryInput.text.toString()) }
        }

        loadingView = ProgressBar(this).apply {
            visibility = View.GONE
        }

        statusText = TextView(this).apply {
            text = "Ready"
            textSize = 15f
        }

        authContainer = verticalSection("Real-Debrid")
        resultsContainer = verticalSection("Search Results")
        detailsContainer = verticalSection("Details")
        episodeSelectionContainer = verticalSection("Episode Selection")
        episodeSelectionContainer.visibility = View.GONE
        sourcesContainer = verticalSection("Sources")
        playbackContainer = verticalSection("Playback")
        playbackControlsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        playerView = PlayerView(this).apply {
            useController = true
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                720
            )
        }

        searchRow.addView(queryInput)
        searchRow.addView(searchButton)

        root.addView(title)
        root.addView(subtitle)
        root.addView(buildInfo)
        root.addView(space())
        root.addView(searchRow)
        root.addView(space())
        root.addView(loadingView)
        root.addView(statusText)
        root.addView(space())

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(authContainer)
            addView(space())
            addView(resultsContainer)
            addView(space())
            addView(detailsContainer)
            addView(space())
            addView(episodeSelectionContainer)
            addView(space())
            addView(sourcesContainer)
            addView(space())
            addView(playbackContainer)
            addView(space())
            addView(playbackControlsContainer)
            addView(space())
            addView(playerView)
        }

        val scrollView = ScrollView(this).apply {
            addView(scrollContent)
        }

        root.addView(
            scrollView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        return root
    }

    private fun attachPlayerView() {
        val engine = AppContainer.playbackEngine as? Media3PlaybackEngine ?: return
        playerView.player = engine.attach(this)
    }

    private fun verticalSection(title: String): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                text = title
                textSize = 20f
            })
        }
    }

    private fun refreshAuthPanel() {
        lifecycleScope.launch {
            authState = AppContainer.getRealDebridAuthStateUseCase()
            renderAuthPanel()
        }
    }

    private fun renderAuthPanel() {
        clearSection(authContainer)

        appendBody(
            authContainer,
            if (authState.isLinked) {
                "Real-Debrid linked${authState.username?.let { " as $it" } ?: ""}."
            } else {
                "Real-Debrid not linked. Magnet/debrid playback will fail until you link it."
            }
        )

        if (!authState.isLinked) {
            authContainer.addView(Button(this).apply {
                text = "Start Real-Debrid Link"
                setOnClickListener { startRealDebridLink() }
            })
        }

        authContainer.addView(Button(this).apply {
            text = "Copy Debug Info"
            setOnClickListener { copyDebugInfoToClipboard() }
        })

        appendBody(
            authContainer,
            buildString {
                append("Token store path: ")
                append(AppContainer.realDebridTokenStoreDebugPath())
            }
        )

        activeDeviceFlow?.let { flow ->
            appendBody(
                authContainer,
                buildString {
                    appendLine("Open: ${flow.verificationUrl}")
                    appendLine("Code: ${flow.userCode}")
                    append("Polling starts automatically for up to 2 minutes after you begin linking.")
                }
            )
            authContainer.addView(Button(this).apply {
                text = "Open Real-Debrid Link Page"
                setOnClickListener {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(buildRealDebridAuthUrl(flow))))
                }
            })
            authContainer.addView(Button(this).apply {
                text = "Poll Link Status"
                setOnClickListener { pollRealDebridLink(flow) }
            })
        }

        authState.lastError?.takeIf { it.isNotBlank() }?.let {
            appendBody(authContainer, "Auth error: $it")
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
                renderAuthPanel()
                startAutoPolling(flow)
            }.onFailure { error ->
                authPollingJob?.cancel()
                authState = RealDebridAuthState(isLinked = false, authInProgress = false, lastError = error.message)
                setLoading(false, "Failed to start Real-Debrid link flow.")
                renderAuthPanel()
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
                renderAuthPanel()
            }.onFailure { error ->
                authState = RealDebridAuthState(isLinked = false, authInProgress = true, lastError = error.message)
                setLoading(false, "Polling Real-Debrid link failed.")
                renderAuthPanel()
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
                        renderAuthPanel()
                        return@launch
                    }
                    renderAuthPanel()
                }.onFailure { error ->
                    authState = RealDebridAuthState(
                        isLinked = false,
                        authInProgress = true,
                        lastError = error.message
                    )
                    renderAuthPanel()
                }
            }
            if (!authState.isLinked && activeDeviceFlow != null) {
                authState = authState.copy(
                    authInProgress = false,
                    lastError = authState.lastError ?: "Real-Debrid link timed out after 2 minutes."
                )
                statusText.text = "Real-Debrid link polling timed out."
                renderAuthPanel()
            }
        }
    }

    private fun buildRealDebridAuthUrl(flow: DeviceCodeFlow): String {
        val base = flow.verificationUrl.ifBlank { "https://real-debrid.com/device" }
        return "$base?user_code=${Uri.encode(flow.userCode)}&device_code=${Uri.encode(flow.deviceCode)}"
    }

    private fun copyDebugInfoToClipboard() {
        val clipboard = getSystemService(ClipboardManager::class.java)
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
            appendLine("selected_media=${selectedMediaRef?.title ?: "none"}")
            appendLine("selected_season=${selectedSeasonNumber ?: "none"}")
            appendLine("selected_episode=${selectedEpisodeNumber ?: "none"}")
            appendLine("status=${statusText.text}")
        }
        clipboard?.setPrimaryClip(ClipData.newPlainText("Asahi Debug Info", debugText))
        statusText.text = "Debug info copied to clipboard"
    }

    private fun runInitialSearch() {
        runSearch(queryInput.text.toString())
    }

    private fun runSearch(rawQuery: String) {
        val query = rawQuery.trim().ifEmpty { "Dune" }
        queryInput.setText(query)
        selectedMediaRef = null
        selectedSeasonNumber = null
        selectedEpisodeNumber = null
        selectedTitleDetails = null
        clearSection(resultsContainer)
        clearSection(detailsContainer)
        clearSection(episodeSelectionContainer)
        episodeSelectionContainer.visibility = View.GONE
        clearSection(sourcesContainer)
        clearSection(playbackContainer)
        clearButtons(playbackControlsContainer)
        playerView.visibility = View.GONE
        setLoading(true, "Searching for \"$query\"…")

        lifecycleScope.launch {
            val state = searchViewModel.search(query)
            setLoading(false, state.error ?: "Found ${state.results.size} result(s) for \"$query\".")
            renderSearchResults(state.results, state.error)
        }
    }

    private fun renderSearchResults(results: List<SearchResult>, error: String?) {
        if (error != null) {
            appendBody(resultsContainer, "Search failed: $error")
            return
        }
        if (results.isEmpty()) {
            appendBody(resultsContainer, "No results.")
            return
        }

        results.take(8).forEachIndexed { index, result ->
            val button = Button(this).apply {
                text = buildString {
                    append(result.mediaRef.title)
                    result.mediaRef.year?.let { append(" ($it)") }
                    result.subtitle?.takeIf { it.isNotBlank() }?.let { append(" — $it") }
                }
                setOnClickListener { onSearchResultSelected(result.mediaRef) }
            }
            resultsContainer.addView(button)
            if (index == 0) {
                button.post { button.performClick() }
            }
        }
    }

    private fun onSearchResultSelected(mediaRef: MediaRef) {
        selectedMediaRef = mediaRef
        selectedSeasonNumber = if (mediaRef.mediaType == MediaType.SHOW) 1 else null
        selectedEpisodeNumber = if (mediaRef.mediaType == MediaType.SHOW) 1 else null
        clearSection(detailsContainer)
        clearSection(episodeSelectionContainer)
        episodeSelectionContainer.visibility = View.GONE
        clearSection(sourcesContainer)
        clearSection(playbackContainer)
        clearButtons(playbackControlsContainer)
        playerView.visibility = View.GONE
        setLoading(true, "Loading details for ${mediaRef.title}…")

        lifecycleScope.launch {
            val state = detailsViewModel.load(mediaRef)
            val details = state.item
            if (details == null) {
                setLoading(false, state.error ?: "No details available.")
                appendBody(detailsContainer, "Details failed: ${state.error ?: "unknown error"}")
                return@launch
            }

            selectedTitleDetails = details
            setLoading(false, "Loaded details for ${details.mediaRef.title}.")
            renderDetails(details)
            renderEpisodeSelection(details)
            loadSourcesFor(
                mediaRef = details.mediaRef,
                seasonNumber = selectedSeasonNumber,
                episodeNumber = selectedEpisodeNumber
            )
        }
    }

    private fun renderDetails(details: TitleDetails) {
        appendBody(
            detailsContainer,
            buildString {
                appendLine(details.mediaRef.title)
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
    }

    private fun renderEpisodeSelection(details: TitleDetails) {
        clearSection(episodeSelectionContainer)
        val mediaRef = details.mediaRef
        if (mediaRef.mediaType != MediaType.SHOW) {
            episodeSelectionContainer.visibility = View.GONE
            return
        }

        episodeSelectionContainer.visibility = View.VISIBLE

        val knownSeasonCount = details.seasonCount ?: 3
        val seasonStrip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        episodeSelectionContainer.addView(TextView(this).apply {
            text = "Pick a season and episode for source lookup and RD resolution."
            textSize = 16f
        })

        val seasonScroll = HorizontalScrollView(this).apply {
            addView(seasonStrip)
        }

        (1..knownSeasonCount.coerceAtMost(12)).forEach { season ->
            seasonStrip.addView(Button(this).apply {
                text = if (season == selectedSeasonNumber) {
                    "• S${season.toString().padStart(2, '0')}"
                } else {
                    "S${season.toString().padStart(2, '0')}"
                }
                setOnClickListener {
                    selectedSeasonNumber = season
                    renderEpisodeSelection(details)
                }
            })
        }

        episodeSelectionContainer.addView(TextView(this).apply {
            text = "Seasons"
            textSize = 18f
        })
        episodeSelectionContainer.addView(seasonScroll)

        val activeSeason = selectedSeasonNumber ?: 1
        episodeSelectionContainer.addView(TextView(this).apply {
            text = "Episodes for season ${activeSeason.toString().padStart(2, '0')}"
            textSize = 18f
        })

        val realEpisodes = details.episodesBySeason[activeSeason].orEmpty()
        val episodeChoices = if (realEpisodes.isNotEmpty()) {
            realEpisodes.take(20)
        } else {
            (1..12).map { episode ->
                EpisodeSummary(
                    seasonNumber = activeSeason,
                    episodeNumber = episode,
                    title = "Episode $episode"
                )
            }
        }

        val episodeList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        episodeChoices.forEach { episode ->
            episodeList.addView(buildEpisodeButton(mediaRef, activeSeason, episode))
        }

        episodeSelectionContainer.addView(episodeList)

        val seasonInput = EditText(this).apply {
            hint = "Season"
            setText(activeSeason.toString())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val episodeInput = EditText(this).apply {
            hint = "Episode"
            setText((selectedEpisodeNumber ?: 1).toString())
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val loadButton = Button(this).apply {
            text = "Load Exact Episode"
            setOnClickListener {
                val season = seasonInput.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
                val episode = episodeInput.text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1
                selectedSeasonNumber = season
                selectedEpisodeNumber = episode
                loadSourcesFor(
                    mediaRef = mediaRef,
                    seasonNumber = season,
                    episodeNumber = episode
                )
            }
        }

        episodeSelectionContainer.addView(LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(seasonInput)
            addView(episodeInput)
            addView(loadButton)
        })
    }

    private fun buildEpisodeButton(
        mediaRef: MediaRef,
        activeSeason: Int,
        episode: EpisodeSummary
    ): Button {
        val isSelected = selectedEpisodeNumber == episode.episodeNumber && selectedSeasonNumber == activeSeason
        return Button(this).apply {
            text = buildString {
                if (isSelected) append("• ")
                append("E")
                append(episode.episodeNumber.toString().padStart(2, '0'))
                episode.title?.takeIf { it.isNotBlank() }?.let {
                    append(" — ")
                    append(it.take(36))
                }
                episode.airDate?.let {
                    append("  [")
                    append(it)
                    append("]")
                }
                episode.overview?.takeIf { it.isNotBlank() }?.let {
                    append("\n")
                    append(it.take(120))
                    if (it.length > 120) append("…")
                }
            }
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setOnClickListener {
                selectedSeasonNumber = activeSeason
                selectedEpisodeNumber = episode.episodeNumber
                loadSourcesFor(
                    mediaRef = mediaRef,
                    seasonNumber = activeSeason,
                    episodeNumber = episode.episodeNumber
                )
            }
        }
    }

    private fun loadSourcesFor(
        mediaRef: MediaRef,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ) {
        clearSection(sourcesContainer)
        clearSection(playbackContainer)
        clearButtons(playbackControlsContainer)
        playerView.visibility = View.GONE

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
            setLoading(false, state.error ?: "Found ${state.sources.size} source(s) for $searchLabel.")
            renderSources(state.sources, state.error, buildSourceDiagnostics(state.sources))
        }
    }

    private fun renderSources(sources: List<SourceResult>, error: String?, diagnostics: String?) {
        if (error != null) {
            appendBody(sourcesContainer, "Source lookup failed: $error")
            return
        }
        diagnostics?.let {
            appendBody(sourcesContainer, "Diagnostics: $it")
        }

        if (sources.isEmpty()) {
            appendBody(sourcesContainer, "No sources.")
            appendBody(sourcesContainer, "Diagnostics: Torrentio enabled, but no live provider results returned for this title/request.")
            return
        }

        val fallbackMode = sources.any { it.rawMetadata["fallbackMode"] == "true" }
        appendBody(
            sourcesContainer,
            if (fallbackMode) {
                "Diagnostics: no live Torrentio results came back, so fallback/demo providers are being shown."
            } else {
                "Diagnostics: showing live provider results."
            }
        )

        sources.take(12).forEach { source ->
            val button = Button(this).apply {
                text = buildString {
                    append(source.displayName)
                    append("\n")
                    append("provider=")
                    append(source.providerId)
                    append("/")
                    append(source.providerDisplayName)
                    append(" · quality=")
                    append(source.quality)
                    append(" · cache=")
                    append(source.cacheStatus)
                    append(" · debrid=")
                    append(source.debridService)
                    if (source.seasonNumber != null && source.episodeNumber != null) {
                        append(" · S")
                        append(source.seasonNumber.toString().padStart(2, '0'))
                        append("E")
                        append(source.episodeNumber.toString().padStart(2, '0'))
                    }
                    source.sizeLabel?.let {
                        append(" · size=")
                        append(it)
                    }
                    append("\n")
                    append("transport=")
                    append(source.rawMetadata["transport"] ?: "none")
                    append(" · fallback=")
                    append(source.rawMetadata["fallbackMode"] ?: "false")
                    append(" · raw_debrid=")
                    append(source.rawMetadata["debrid"] ?: "none")
                    append(" · raw_cache=")
                    append(source.rawMetadata["cache_hint"] ?: "none")
                }
                setOnClickListener {
                    preparePlayback(
                        source = source,
                        seasonNumber = source.seasonNumber,
                        episodeNumber = source.episodeNumber
                    )
                }
            }
            sourcesContainer.addView(button)
        }
    }

    private fun preparePlayback(
        source: SourceResult,
        seasonNumber: Int? = source.seasonNumber,
        episodeNumber: Int? = source.episodeNumber
    ) {
        clearSection(playbackContainer)
        clearButtons(playbackControlsContainer)
        playerView.visibility = View.GONE

        if (source.debridService == DebridService.REAL_DEBRID && !authState.isLinked) {
            appendBody(
                playbackContainer,
                "This source needs Real-Debrid. Link your account in the Real-Debrid section above, then try again."
            )
            statusText.text = "Real-Debrid link required before playback."
            return
        }

        setLoading(true, "Resolving ${source.displayName}…")

        lifecycleScope.launch {
            val state = playerViewModel.prepare(
                source = source,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )
            setLoading(false, state.error ?: if (state.prepared) "Playback item prepared." else "Playback not prepared.")
            appendBody(
                playbackContainer,
                buildString {
                    appendLine("Selected source: ${source.displayName}")
                    appendLine("Provider: ${source.providerDisplayName}")
                    appendLine("Quality: ${source.quality}")
                    appendLine("Cache: ${source.cacheStatus}")
                    if (seasonNumber != null && episodeNumber != null) {
                        appendLine("Episode target: S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}")
                    }
                    appendLine()
                    append(
                        if (state.prepared) {
                            buildString {
                                appendLine("Playback preparation succeeded.")
                                appendLine("Current item: ${AppContainer.playbackEngine.getCurrentItem()?.title ?: source.displayName}")
                                appendLine("Stream URL: ${state.playbackUrl ?: AppContainer.playbackEngine.getCurrentUrl() ?: source.url}")
                                append("Player surface is attached below for direct HTTP playback.")
                            }
                        } else {
                            buildString {
                                appendLine("Playback preparation failed.")
                                append(state.error ?: "unknown error")
                            }
                        }
                    )
                }
            )
            if (state.prepared) {
                playerView.visibility = View.VISIBLE
                renderPlaybackControls()
            }
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

    private fun renderPlaybackControls() {
        clearButtons(playbackControlsContainer)
        playbackControlsContainer.addView(Button(this).apply {
            text = "Play"
            setOnClickListener {
                AppContainer.playbackEngine.play()
                statusText.text = "Playback state: playing"
            }
        })
        playbackControlsContainer.addView(Button(this).apply {
            text = "Pause"
            setOnClickListener {
                AppContainer.playbackEngine.pause()
                statusText.text = "Playback state: paused"
            }
        })
        playbackControlsContainer.addView(Button(this).apply {
            text = "Stop"
            setOnClickListener {
                AppContainer.playbackEngine.stop()
                statusText.text = "Playback state: stopped"
            }
        })
    }

    private fun clearSection(section: LinearLayout) {
        while (section.childCount > 1) {
            section.removeViewAt(1)
        }
    }

    private fun clearButtons(section: LinearLayout) {
        section.removeAllViews()
    }

    private fun appendBody(section: LinearLayout, text: String) {
        section.addView(TextView(this).apply {
            this.text = text
            textSize = 16f
        })
    }

    private fun space(): View = TextView(this).apply { text = "" }
}
