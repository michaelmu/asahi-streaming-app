package ai.shieldtv.app

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.media.TitleDetails
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
    private lateinit var resultsContainer: LinearLayout
    private lateinit var detailsContainer: LinearLayout
    private lateinit var sourcesContainer: LinearLayout
    private lateinit var playbackContainer: LinearLayout
    private lateinit var playbackControlsContainer: LinearLayout
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContentView())
        attachPlayerView()
        runInitialSearch()
    }

    override fun onDestroy() {
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

        resultsContainer = verticalSection("Search Results")
        detailsContainer = verticalSection("Details")
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
        root.addView(space())
        root.addView(searchRow)
        root.addView(space())
        root.addView(loadingView)
        root.addView(statusText)
        root.addView(space())

        val scrollContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(resultsContainer)
            addView(space())
            addView(detailsContainer)
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

    private fun runInitialSearch() {
        runSearch(queryInput.text.toString())
    }

    private fun runSearch(rawQuery: String) {
        val query = rawQuery.trim().ifEmpty { "Dune" }
        queryInput.setText(query)
        clearSection(resultsContainer)
        clearSection(detailsContainer)
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
        clearSection(detailsContainer)
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

            setLoading(false, "Loaded details for ${details.mediaRef.title}.")
            renderDetails(details)
            loadSourcesFor(details.mediaRef)
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
                appendLine()
                append(details.overview ?: "No overview yet.")
            }
        )
    }

    private fun loadSourcesFor(mediaRef: MediaRef) {
        clearSection(sourcesContainer)
        clearSection(playbackContainer)
        clearButtons(playbackControlsContainer)
        playerView.visibility = View.GONE
        setLoading(true, "Finding sources for ${mediaRef.title}…")

        lifecycleScope.launch {
            val state = sourcesViewModel.load(SourceSearchRequest(mediaRef = mediaRef))
            setLoading(false, state.error ?: "Found ${state.sources.size} source(s) for ${mediaRef.title}.")
            renderSources(state.sources, state.error)
        }
    }

    private fun renderSources(sources: List<SourceResult>, error: String?) {
        if (error != null) {
            appendBody(sourcesContainer, "Source lookup failed: $error")
            return
        }
        if (sources.isEmpty()) {
            appendBody(sourcesContainer, "No sources.")
            return
        }

        sources.take(12).forEach { source ->
            val button = Button(this).apply {
                text = buildString {
                    append(source.displayName)
                    append(" · ")
                    append(source.quality)
                    append(" · ")
                    append(source.providerDisplayName)
                    append(" · ")
                    append(source.cacheStatus)
                    source.sizeLabel?.let {
                        append(" · ")
                        append(it)
                    }
                }
                setOnClickListener { preparePlayback(source) }
            }
            sourcesContainer.addView(button)
        }
    }

    private fun preparePlayback(source: SourceResult) {
        clearSection(playbackContainer)
        clearButtons(playbackControlsContainer)
        playerView.visibility = View.GONE
        setLoading(true, "Resolving ${source.displayName}…")

        lifecycleScope.launch {
            val state = playerViewModel.prepare(source)
            setLoading(false, state.error ?: if (state.prepared) "Playback item prepared." else "Playback not prepared.")
            appendBody(
                playbackContainer,
                buildString {
                    appendLine("Selected source: ${source.displayName}")
                    appendLine("Provider: ${source.providerDisplayName}")
                    appendLine("Quality: ${source.quality}")
                    appendLine("Cache: ${source.cacheStatus}")
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
