package ai.shieldtv.app.ui

import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import androidx.media3.ui.PlayerView
import ai.shieldtv.app.AppState
import ai.shieldtv.app.SearchMode
import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.media.EpisodeSummary
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.SourceResult

class NavigationRailRenderer(
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    fun render(
        selectedMode: SearchMode,
        inSettings: Boolean,
        onMovies: () -> Unit,
        onShows: () -> Unit,
        onSettings: () -> Unit,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        host.addView(viewFactory.body("Browse"))
        host.addView(Button(host.context).apply {
            text = if (!inSettings && selectedMode == SearchMode.MOVIES) "• Movies" else "Movies"
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setOnClickListener { onMovies() }
            post { onFirstFocusTarget(this) }
        })
        host.addView(viewFactory.button(
            if (!inSettings && selectedMode == SearchMode.SHOWS) "• TV Shows" else "TV Shows",
            onShows
        ))
        host.addView(viewFactory.button(if (inSettings) "• Settings" else "Settings", onSettings))
    }
}

class SearchScreenRenderer(
    private val activity: android.app.Activity,
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    fun render(
        state: AppState,
        onSearch: (SearchMode, String) -> Unit,
        onBack: () -> Unit,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        host.addView(viewFactory.title("Search ${state.searchMode.label}"))
        host.addView(viewFactory.body("Enter a title to search ${state.searchMode.label.lowercase()}."))

        val searchRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val queryInput = EditText(activity).apply {
            setText(state.query)
            hint = when (state.searchMode) {
                SearchMode.MOVIES -> "Search movies"
                SearchMode.SHOWS -> "Search TV shows"
            }
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val searchButton = Button(activity).apply {
            text = "Search"
            setOnClickListener { onSearch(state.searchMode, queryInput.text.toString()) }
            post { onFirstFocusTarget(this) }
        }
        searchRow.addView(queryInput)
        searchRow.addView(searchButton)

        host.addView(searchRow)
        if (onBack !== {}) {
            host.addView(viewFactory.spacer())
            host.addView(viewFactory.button("Back", onBack))
        }
    }
}

class ResultsScreenRenderer(
    private val activity: android.app.Activity,
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    fun render(
        state: AppState,
        emptyMessage: String,
        onResultSelected: (ai.shieldtv.app.core.model.media.SearchResult) -> Unit,
        onNewSearch: () -> Unit,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        host.addView(viewFactory.title("Results"))
        host.addView(viewFactory.body("Query: ${state.query}"))

        if (state.searchResults.isEmpty()) {
            host.addView(viewFactory.body(emptyMessage))
        } else {
            state.searchResults.take(20).forEachIndexed { index, result ->
                host.addView(Button(activity).apply {
                    text = buildString {
                        append(result.mediaRef.title)
                        result.mediaRef.year?.let { append(" ($it)") }
                        result.subtitle?.takeIf { it.isNotBlank() }?.let {
                            append(" — ")
                            append(it)
                        }
                    }
                    gravity = Gravity.START or Gravity.CENTER_VERTICAL
                    setOnClickListener { onResultSelected(result) }
                    if (index == 0) {
                        post { onFirstFocusTarget(this) }
                    }
                })
            }
        }

        host.addView(viewFactory.spacer())
        host.addView(viewFactory.button("New Search", onNewSearch))
    }
}

class DetailsScreenRenderer(
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    fun render(
        state: AppState,
        onBrowseEpisodes: () -> Unit,
        onFindSources: () -> Unit,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        val details = state.selectedDetails
        if (details == null) {
            host.addView(viewFactory.title("Details"))
            host.addView(viewFactory.body("No details loaded."))
            return
        }

        host.addView(viewFactory.title(details.mediaRef.title))
        host.addView(
            viewFactory.body(
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
            host.addView(Button(host.context).apply {
                text = "Browse Episodes"
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setOnClickListener { onBrowseEpisodes() }
                post { onFirstFocusTarget(this) }
            })
        } else {
            host.addView(Button(host.context).apply {
                text = "Find Sources"
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setOnClickListener { onFindSources() }
                post { onFirstFocusTarget(this) }
            })
        }
    }
}

class EpisodePickerScreenRenderer(
    private val activity: android.app.Activity,
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    fun render(
        state: AppState,
        onSeasonSelected: (Int) -> Unit,
        onEpisodeSelected: (Int) -> Unit,
        onFindSources: () -> Unit,
        onEpisodePlay: (Int) -> Unit,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        val details = state.selectedDetails
        if (details == null) {
            host.addView(viewFactory.title("Episodes"))
            host.addView(viewFactory.body("No show loaded."))
            return
        }

        host.addView(viewFactory.title("${details.mediaRef.title} Episodes"))
        host.addView(viewFactory.body("Pick a season and episode, then load sources."))

        val knownSeasonCount = (details.seasonCount ?: 3).coerceAtMost(12)
        val selectedSeason = state.selectedSeasonNumber ?: 1
        val selectedEpisode = state.selectedEpisodeNumber ?: 1

        val seasonStrip = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        (1..knownSeasonCount).forEach { season ->
            seasonStrip.addView(Button(activity).apply {
                text = if (season == selectedSeason) "• S${season.toString().padStart(2, '0')}" else "S${season.toString().padStart(2, '0')}"
                setOnClickListener { onSeasonSelected(season) }
            })
        }
        host.addView(HorizontalScrollView(activity).apply { addView(seasonStrip) })

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
        val episodeNumbers = episodeChoices.map { it.episodeNumber }

        episodeChoices.forEachIndexed { index, episode ->
            val isSelected = selectedEpisode == episode.episodeNumber
            host.addView(Button(activity).apply {
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
                    onEpisodeSelected(episode.episodeNumber)
                    onEpisodePlay(episode.episodeNumber)
                }
                if (isSelected || (selectedEpisode !in episodeNumbers && index == 0)) {
                    post { onFirstFocusTarget(this) }
                }
            })
        }

        host.addView(viewFactory.spacer())
        host.addView(
            viewFactory.button(
                "Find Sources for S${selectedSeason.toString().padStart(2, '0')}E${selectedEpisode.toString().padStart(2, '0')}",
                onFindSources
            )
        )
    }
}

class SourcesScreenRenderer(
    private val activity: android.app.Activity,
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    fun render(
        state: AppState,
        diagnostics: String?,
        error: String?,
        onSourceSelected: (SourceResult) -> Unit,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        val mediaRef = state.selectedMedia
        host.addView(viewFactory.title("Sources"))
        if (mediaRef != null) {
            val label = if (state.selectedSeasonNumber != null && state.selectedEpisodeNumber != null) {
                "${mediaRef.title} S${state.selectedSeasonNumber.toString().padStart(2, '0')}E${state.selectedEpisodeNumber.toString().padStart(2, '0')}"
            } else {
                mediaRef.title
            }
            host.addView(viewFactory.body(label))
        }

        error?.let {
            host.addView(viewFactory.body("Source lookup failed: $it"))
        }
        diagnostics?.let {
            host.addView(viewFactory.body("Source summary: $it"))
        }

        if (state.selectedSources.isEmpty()) {
            host.addView(viewFactory.body("No sources."))
            return
        }

        state.selectedSources.take(10).forEachIndexed { index, source ->
            host.addView(Button(activity).apply {
                text = buildString {
                    append(source.displayName.lineSequence().firstOrNull()?.take(52) ?: source.displayName.take(52))
                    append("\n")
                    append(source.quality)
                    append(" • ")
                    append(source.cacheStatus)
                    source.sizeLabel?.let {
                        append(" • ")
                        append(it)
                    }
                    append("\n")
                    append(source.providerDisplayName)
                    source.debridService.name.takeIf { it.isNotBlank() }?.let {
                        append(" • ")
                        append(it.removePrefix("REAL_").replace('_', ' '))
                    }
                }
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setPadding(24, 18, 24, 18)
                setOnClickListener { onSourceSelected(source) }
                if (index == 0) {
                    post { onFirstFocusTarget(this) }
                }
            })
        }
    }
}

class PlayerScreenRenderer(
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    fun render(state: AppState, playbackMessage: String?, playbackError: String?, playerView: PlayerView, playbackControls: View) {
        val source = state.selectedSource
        if (source == null) {
            host.addView(viewFactory.title("Player"))
            host.addView(viewFactory.body("No source selected."))
            return
        }

        playbackError?.takeIf { it.isNotBlank() }?.let {
            host.addView(viewFactory.body("Playback error: $it"))
        }
        playerView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        host.addView(playerView)
    }
}

class SettingsScreenRenderer(
    private val activity: android.app.Activity,
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    fun render(
        authState: RealDebridAuthState,
        activeDeviceFlow: DeviceCodeFlow?,
        playbackModeLabel: String,
        updateSummary: String?,
        buildAuthUrl: (DeviceCodeFlow) -> String,
        onStartLink: () -> Unit,
        onPoll: (DeviceCodeFlow) -> Unit,
        onTogglePlaybackMode: () -> Unit,
        onCopyDebugInfo: () -> Unit,
        onCheckForUpdates: () -> Unit,
        onOpenLatestUpdate: (() -> Unit)?,
        onBackToHome: () -> Unit,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        host.addView(viewFactory.title("Settings / Accounts"))
        host.addView(
            viewFactory.body(
                if (authState.isLinked) {
                    "Real-Debrid linked${authState.username?.let { " as $it" } ?: ""}."
                } else {
                    "Real-Debrid not linked. Debrid-backed playback will fail until you link it."
                }
            )
        )
        host.addView(viewFactory.body("Playback mode: $playbackModeLabel"))
        updateSummary?.let {
            host.addView(viewFactory.body(it))
        }

        if (!authState.isLinked) {
            host.addView(Button(activity).apply {
                text = "Start Real-Debrid Link"
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setOnClickListener { onStartLink() }
                post { onFirstFocusTarget(this) }
            })
        } else {
            host.addView(Button(activity).apply {
                text = "Toggle Playback Mode"
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setOnClickListener { onTogglePlaybackMode() }
                post { onFirstFocusTarget(this) }
            })
        }

        activeDeviceFlow?.let { flow ->
            host.addView(
                viewFactory.body(
                    buildString {
                        appendLine("Open: ${flow.verificationUrl}")
                        appendLine("Code: ${flow.userCode}")
                        append("Polling starts automatically for up to 2 minutes after you begin linking.")
                    }
                )
            )
            host.addView(viewFactory.button("Open Real-Debrid Link Page") {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(buildAuthUrl(flow))))
            })
            host.addView(viewFactory.button("Poll Link Status") { onPoll(flow) })
        }

        authState.lastError?.takeIf { it.isNotBlank() }?.let {
            host.addView(viewFactory.body("Auth error: $it"))
        }

        if (!authState.isLinked) {
            host.addView(viewFactory.button("Toggle Playback Mode", onTogglePlaybackMode))
        }
        host.addView(viewFactory.button("Check for Updates", onCheckForUpdates))
        onOpenLatestUpdate?.let { openLatest ->
            host.addView(viewFactory.button("Open Latest APK", openLatest))
        }
        host.addView(viewFactory.button("Copy Debug Info", onCopyDebugInfo))
        host.addView(viewFactory.button("Back to Browse", onBackToHome))
    }
}
