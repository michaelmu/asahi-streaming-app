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

class HomeScreenRenderer(
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    fun render(settingsLabel: String, onMovies: () -> Unit, onShows: () -> Unit, onSettings: () -> Unit) {
        host.addView(viewFactory.title("Home"))
        host.addView(viewFactory.body("Start with a content mode, or open settings/accounts."))
        host.addView(viewFactory.button("Movies", onMovies))
        host.addView(viewFactory.button("TV Shows", onShows))
        host.addView(viewFactory.button(settingsLabel, onSettings))
    }
}

class SearchScreenRenderer(
    private val activity: android.app.Activity,
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    fun render(state: AppState, onSearch: (SearchMode, String) -> Unit, onBack: () -> Unit) {
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
        }
        searchRow.addView(queryInput)
        searchRow.addView(searchButton)

        host.addView(searchRow)
        host.addView(viewFactory.spacer())
        host.addView(viewFactory.button("Back to Home", onBack))
    }
}

class ResultsScreenRenderer(
    private val activity: android.app.Activity,
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    fun render(state: AppState, emptyMessage: String, onResultSelected: (ai.shieldtv.app.core.model.media.SearchResult) -> Unit, onNewSearch: () -> Unit) {
        host.addView(viewFactory.title("Results"))
        host.addView(viewFactory.body("Query: ${state.query}"))

        if (state.searchResults.isEmpty()) {
            host.addView(viewFactory.body(emptyMessage))
        } else {
            state.searchResults.take(20).forEach { result ->
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
    fun render(state: AppState, onBrowseEpisodes: () -> Unit, onFindSources: () -> Unit) {
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
            host.addView(viewFactory.button("Browse Episodes", onBrowseEpisodes))
        } else {
            host.addView(viewFactory.button("Find Sources", onFindSources))
        }
    }
}

class EpisodePickerScreenRenderer(
    private val activity: android.app.Activity,
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    fun render(state: AppState, onSeasonSelected: (Int) -> Unit, onEpisodeSelected: (Int) -> Unit, onFindSources: () -> Unit) {
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

        episodeChoices.forEach { episode ->
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
                setOnClickListener { onEpisodeSelected(episode.episodeNumber) }
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
    fun render(state: AppState, diagnostics: String?, error: String?, onSourceSelected: (SourceResult) -> Unit) {
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
            host.addView(viewFactory.body("Diagnostics: $it"))
        }

        if (state.selectedSources.isEmpty()) {
            host.addView(viewFactory.body("No sources."))
            return
        }

        state.selectedSources.take(12).forEach { source ->
            host.addView(Button(activity).apply {
                text = buildString {
                    append(source.displayName)
                    append("\n")
                    append(source.quality)
                    append(" · ")
                    append(source.cacheStatus)
                    source.sizeLabel?.let {
                        append(" · ")
                        append(it)
                    }
                    append("\nProvider: ")
                    append(source.providerDisplayName)
                }
                gravity = Gravity.START or Gravity.CENTER_VERTICAL
                setOnClickListener { onSourceSelected(source) }
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
        host.addView(viewFactory.title("Player"))
        if (source == null) {
            host.addView(viewFactory.body("No source selected."))
            return
        }

        host.addView(
            viewFactory.body(
                buildString {
                    appendLine("Selected source: ${source.displayName}")
                    appendLine("Provider: ${source.providerDisplayName}")
                    appendLine("Quality: ${source.quality}")
                    appendLine("Cache: ${source.cacheStatus}")
                    if (state.selectedSeasonNumber != null && state.selectedEpisodeNumber != null) {
                        appendLine("Episode target: S${state.selectedSeasonNumber.toString().padStart(2, '0')}E${state.selectedEpisodeNumber.toString().padStart(2, '0')}")
                    }
                    appendLine()
                    append(playbackMessage ?: "Preparing playback…")
                }
            )
        )
        playbackError?.let {
            host.addView(viewFactory.body("Playback error: $it"))
        }
        host.addView(playerView)
        host.addView(viewFactory.spacer())
        host.addView(playbackControls)
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
        buildAuthUrl: (DeviceCodeFlow) -> String,
        onStartLink: () -> Unit,
        onPoll: (DeviceCodeFlow) -> Unit,
        onTogglePlaybackMode: () -> Unit,
        onCopyDebugInfo: () -> Unit
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

        if (!authState.isLinked) {
            host.addView(viewFactory.button("Start Real-Debrid Link", onStartLink))
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

        host.addView(viewFactory.button("Toggle Playback Mode", onTogglePlaybackMode))
        host.addView(viewFactory.button("Copy Debug Info", onCopyDebugInfo))
    }
}
