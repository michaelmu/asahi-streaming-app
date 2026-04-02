package ai.shieldtv.app.ui

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import androidx.media3.ui.PlayerView
import ai.shieldtv.app.AppState
import ai.shieldtv.app.SearchMode
import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.media.EpisodeSummary
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.SourceResult
import coil.load

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
        host.removeAllViews()
        host.addView(viewFactory.sectionTitle("Library"))
        host.addView(viewFactory.spacer(12))

        val movieButton = viewFactory.button(
            if (!inSettings && selectedMode == SearchMode.MOVIES) "Movies •" else "Movies",
            onMovies
        )
        host.addView(movieButton)
        host.addView(viewFactory.spacer(10))
        host.addView(
            viewFactory.button(
                if (!inSettings && selectedMode == SearchMode.SHOWS) "TV Shows •" else "TV Shows",
                onShows
            )
        )
        host.addView(viewFactory.spacer(10))
        host.addView(viewFactory.button(if (inSettings) "Settings •" else "Settings", onSettings))

        movieButton.post { onFirstFocusTarget(movieButton) }
    }
}

class HomeScreenRenderer(
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    fun render(
        state: AppState,
        authLinked: Boolean,
        statusMessage: String,
        onOpenMovies: () -> Unit,
        onOpenShows: () -> Unit,
        onOpenSettings: () -> Unit,
        onResumeSearch: (() -> Unit)?,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        host.addView(viewFactory.heroCard(
            title = "Asahi",
            subtitle = "A cleaner TV-first shell: browse, inspect, choose a source, then let playback take over the room."
        ))
        host.addView(viewFactory.spacer())

        val actionRow = LinearLayout(host.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        val moviesButton = actionButton("Browse Movies", onOpenMovies)
        val showsButton = actionButton("Browse TV Shows", onOpenShows, startMarginDp = 12)
        val settingsButton = actionButton("Settings / Accounts", onOpenSettings, startMarginDp = 12)
        actionRow.addView(moviesButton)
        actionRow.addView(showsButton)
        actionRow.addView(settingsButton)
        host.addView(actionRow)
        host.addView(viewFactory.spacer())

        val statusRow = LinearLayout(host.context).apply { orientation = LinearLayout.HORIZONTAL }
        statusRow.addView(
            viewFactory.statPill(
                "Real-Debrid",
                if (authLinked) "Linked" else "Not linked",
                if (authLinked) StatTone.SUCCESS else StatTone.WARNING
            )
        )
        statusRow.addView(horizontalGap())
        statusRow.addView(viewFactory.statPill("Search Mode", state.searchMode.label, StatTone.ACCENT))
        state.selectedMedia?.title?.takeIf { it.isNotBlank() }?.let {
            statusRow.addView(horizontalGap())
            statusRow.addView(viewFactory.statPill("Selected", it.take(18), StatTone.NORMAL))
        }
        host.addView(HorizontalScrollView(host.context).apply { addView(statusRow) })
        host.addView(viewFactory.spacer())

        host.addView(viewFactory.panel(elevated = true).apply {
            addView(viewFactory.sectionTitle("What’s Ready"))
            addView(viewFactory.spacer(12))
            addView(viewFactory.body(statusMessage))
            addView(viewFactory.spacer(12))
            addView(viewFactory.caption("TMDb metadata, Torrentio source lookup, Real-Debrid auth/resolve, and Media3 playback are all in the loop."))
        })
        host.addView(viewFactory.spacer())

        host.addView(viewFactory.panel(elevated = false).apply {
            addView(viewFactory.sectionTitle("Suggested Next Step"))
            addView(viewFactory.spacer(12))
            addView(viewFactory.body(
                when {
                    !authLinked -> "Link Real-Debrid in Settings if you want the best cached-source path."
                    onResumeSearch != null -> "Jump back into your last search flow or start a new browse pass from Movies / TV Shows."
                    else -> "Start with Movies or TV Shows from the action row above."
                }
            ))
            onResumeSearch?.let {
                addView(viewFactory.spacer(16))
                addView(viewFactory.button("Resume Last Flow", it))
            }
        })

        moviesButton.post { onFirstFocusTarget(moviesButton) }
    }

    private fun actionButton(text: String, onClick: () -> Unit, startMarginDp: Int = 0): View {
        return viewFactory.button(text, onClick).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                if (startMarginDp > 0) it.marginStart = viewFactory.dp(startMarginDp)
            }
        }
    }

    private fun horizontalGap(): View = View(host.context).apply {
        layoutParams = LinearLayout.LayoutParams(viewFactory.dp(10), 1)
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
        onBack: (() -> Unit)?,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        host.addView(viewFactory.heroCard(
            title = state.searchMode.label,
            subtitle = when (state.searchMode) {
                SearchMode.MOVIES -> "Search films, jump into details, then pick the best cached source."
                SearchMode.SHOWS -> "Search series, browse episodes, and move straight into the source picker."
            }
        ))
        host.addView(viewFactory.spacer())

        val searchPanel = viewFactory.panel(elevated = true).apply {
            addView(viewFactory.sectionTitle("Search"))
            addView(viewFactory.spacer(12))
            addView(viewFactory.body("Lean into a Stremio-style flow: query first, results next, source chooser after that."))
            addView(viewFactory.spacer(16))

            val searchRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val queryInput = viewFactory.input(
                hint = when (state.searchMode) {
                    SearchMode.MOVIES -> "Try: Dune, Alien, Blade Runner"
                    SearchMode.SHOWS -> "Try: Severance, Andor, The Last of Us"
                },
                initialValue = state.query
            ).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                inputType = InputType.TYPE_CLASS_TEXT
            }
            val searchButton = viewFactory.button("Search") {
                onSearch(state.searchMode, queryInput.text.toString())
            }.apply {
                layoutParams = LinearLayout.LayoutParams(
                    viewFactory.dp(180),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginStart = viewFactory.dp(16) }
            }

            searchRow.addView(queryInput)
            searchRow.addView(searchButton)
            addView(searchRow)

            searchButton.post { onFirstFocusTarget(searchButton) }
        }

        host.addView(searchPanel)
        onBack?.let {
            host.addView(viewFactory.spacer())
            host.addView(viewFactory.button("Back", it))
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
        host.addView(viewFactory.heroCard(
            title = "Results for \"${state.query}\"",
            subtitle = "${state.searchResults.size} match(es) across ${state.searchMode.label.lowercase()}. Pick a title to drill into details."
        ))
        host.addView(viewFactory.spacer())

        if (state.searchResults.isEmpty()) {
            host.addView(viewFactory.panel(elevated = true).apply {
                addView(viewFactory.title("Nothing useful yet"))
                addView(viewFactory.spacer(10))
                addView(viewFactory.body(emptyMessage))
                addView(viewFactory.spacer(16))
                addView(viewFactory.button("Try another search", onNewSearch))
            })
            return
        }

        val featured = state.searchResults.take(3)
        val featuredRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        featured.forEachIndexed { index, result ->
            val featuredCard = viewFactory.panel(elevated = true).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                    if (index > 0) it.marginStart = viewFactory.dp(14)
                }
                isFocusable = true
                isClickable = true
                setOnClickListener { onResultSelected(result) }
                setOnFocusChangeListener { view, hasFocus ->
                    view.scaleX = if (hasFocus) 1.02f else 1f
                    view.scaleY = if (hasFocus) 1.02f else 1f
                }
            }
            featuredCard.addView(TextView(activity).apply {
                text = result.mediaRef.title
                setTextColor(viewFactory.textPrimaryColor)
                textSize = 22f
                setTypeface(typeface, Typeface.BOLD)
            })
            featuredCard.addView(viewFactory.spacer(8))
            featuredCard.addView(viewFactory.caption(
                buildString {
                    append(result.mediaRef.mediaType.name.lowercase().replaceFirstChar { it.uppercase() })
                    result.mediaRef.year?.let {
                        append(" • ")
                        append(it)
                    }
                }
            ))
            result.subtitle?.takeIf { it.isNotBlank() }?.let {
                featuredCard.addView(viewFactory.spacer(10))
                featuredCard.addView(viewFactory.body(it.take(140)))
            }
            featuredRow.addView(featuredCard)
            if (index == 0) {
                featuredCard.post { onFirstFocusTarget(featuredCard) }
            }
        }
        host.addView(featuredRow)
        host.addView(viewFactory.spacer())

        host.addView(viewFactory.sectionTitle("All Results"))
        host.addView(viewFactory.spacer(12))

        state.searchResults.drop(3).take(17).forEach { result ->
            val card = viewFactory.panel(vertical = false, elevated = false).apply {
                gravity = Gravity.CENTER_VERTICAL
                isFocusable = true
                isClickable = true
                setOnClickListener { onResultSelected(result) }
                setOnFocusChangeListener { view, hasFocus ->
                    view.scaleX = if (hasFocus) 1.02f else 1f
                    view.scaleY = if (hasFocus) 1.02f else 1f
                }
            }

            val posterView = ImageView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(viewFactory.dp(120), viewFactory.dp(180)).apply {
                    marginEnd = viewFactory.dp(20)
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                clipToOutline = true
                result.posterUrl?.takeIf { it.isNotBlank() }?.let { url -> load(url) }
            }

            val textColumn = LinearLayout(activity).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textColumn.addView(TextView(activity).apply {
                text = buildString {
                    append(result.mediaRef.title)
                    result.mediaRef.year?.let { append(" ($it)") }
                }
                setTextColor(viewFactory.textPrimaryColor)
                textSize = 22f
                setTypeface(typeface, Typeface.BOLD)
            })
            textColumn.addView(viewFactory.spacer(6))
            textColumn.addView(viewFactory.caption(result.mediaRef.mediaType.name.lowercase().replaceFirstChar { it.uppercase() }))
            result.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                textColumn.addView(viewFactory.spacer(10))
                textColumn.addView(viewFactory.body(subtitle.take(220)))
            }

            card.addView(posterView)
            card.addView(textColumn)
            host.addView(card)
            host.addView(viewFactory.spacer(14))
        }

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
            host.addView(viewFactory.spacer(10))
            host.addView(viewFactory.body("No details loaded."))
            return
        }

        host.addView(viewFactory.heroCard(
            title = details.mediaRef.title,
            subtitle = details.overview?.take(180)
                ?: "No overview yet."
        ))
        host.addView(viewFactory.spacer())

        val topPanel = LinearLayout(host.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        val posterPanel = viewFactory.panel(elevated = true).apply {
            layoutParams = LinearLayout.LayoutParams(viewFactory.dp(250), LinearLayout.LayoutParams.WRAP_CONTENT)
            addView(TextView(host.context).apply {
                text = details.mediaRef.title.take(1).uppercase()
                gravity = Gravity.CENTER
                minHeight = viewFactory.dp(320)
                setTextColor(viewFactory.textMutedColor)
                textSize = 96f
                setTypeface(typeface, Typeface.BOLD)
            })
        }
        val infoPanel = LinearLayout(host.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = viewFactory.dp(18)
            }
        }

        val statsRow = LinearLayout(host.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        statsRow.addView(viewFactory.statPill("Type", details.mediaRef.mediaType.name))
        details.mediaRef.year?.let {
            statsRow.addView(spacedStat(viewFactory.statPill("Year", it.toString())))
        }
        details.runtimeMinutes?.let {
            statsRow.addView(spacedStat(viewFactory.statPill("Runtime", "${it}m", StatTone.ACCENT)))
        }
        details.seasonCount?.let {
            statsRow.addView(spacedStat(viewFactory.statPill("Seasons", it.toString(), StatTone.SUCCESS)))
        }
        infoPanel.addView(HorizontalScrollView(host.context).apply { addView(statsRow) })
        infoPanel.addView(viewFactory.spacer())
        infoPanel.addView(viewFactory.panel(elevated = true).apply {
            addView(viewFactory.sectionTitle("Overview"))
            addView(viewFactory.spacer(12))
            addView(viewFactory.body(details.overview ?: "No overview yet."))
            if (details.genres.isNotEmpty()) {
                addView(viewFactory.spacer(16))
                addView(viewFactory.caption("Genres: ${details.genres.joinToString()}"))
            }
        })

        topPanel.addView(posterPanel)
        topPanel.addView(infoPanel)
        host.addView(topPanel)
        host.addView(viewFactory.spacer())

        val primaryAction = if (details.mediaRef.mediaType == MediaType.SHOW) {
            viewFactory.button("Browse Episodes", onBrowseEpisodes)
        } else {
            viewFactory.button("Find Sources", onFindSources)
        }
        host.addView(primaryAction)
        primaryAction.post { onFirstFocusTarget(primaryAction) }
    }

    private fun spacedStat(view: View): View {
        return view.apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.marginStart = viewFactory.dp(10) }
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
            host.addView(viewFactory.spacer(10))
            host.addView(viewFactory.body("No show loaded."))
            return
        }

        val selectedSeason = state.selectedSeasonNumber ?: 1
        val selectedEpisode = state.selectedEpisodeNumber ?: 1
        val knownSeasonCount = (details.seasonCount ?: 3).coerceAtMost(12)

        host.addView(viewFactory.heroCard(
            title = details.mediaRef.title,
            subtitle = "Season $selectedSeason · Episode $selectedEpisode — closer to Stremio than the old one-page scroll monster."
        ))
        host.addView(viewFactory.spacer())

        host.addView(viewFactory.sectionTitle("Seasons"))
        host.addView(viewFactory.spacer(12))
        val seasonStrip = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        (1..knownSeasonCount).forEach { season ->
            val chip = viewFactory.chip("Season $season", selected = season == selectedSeason) {
                onSeasonSelected(season)
            }
            seasonStrip.addView(chip)
            if (season < knownSeasonCount) {
                seasonStrip.addView(View(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(viewFactory.dp(10), 1)
                })
            }
        }
        host.addView(HorizontalScrollView(activity).apply { addView(seasonStrip) })
        host.addView(viewFactory.spacer())

        host.addView(viewFactory.sectionTitle("Episodes"))
        host.addView(viewFactory.spacer(12))
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
            val card = viewFactory.panel(elevated = isSelected).apply {
                isFocusable = true
                isClickable = true
                setOnClickListener {
                    onEpisodeSelected(episode.episodeNumber)
                    onEpisodePlay(episode.episodeNumber)
                }
                setOnFocusChangeListener { view, hasFocus ->
                    view.scaleX = if (hasFocus) 1.015f else 1f
                    view.scaleY = if (hasFocus) 1.015f else 1f
                }
            }
            card.addView(TextView(activity).apply {
                text = "Episode ${episode.episodeNumber.toString().padStart(2, '0')}"
                setTextColor(viewFactory.textPrimaryColor)
                textSize = 20f
                setTypeface(typeface, Typeface.BOLD)
            })
            episode.title?.takeIf { it.isNotBlank() }?.let {
                card.addView(viewFactory.spacer(8))
                card.addView(viewFactory.body(it))
            }
            episode.airDate?.let {
                card.addView(viewFactory.spacer(8))
                card.addView(viewFactory.caption(it))
            }
            episode.overview?.takeIf { it.isNotBlank() }?.let {
                card.addView(viewFactory.spacer(10))
                card.addView(viewFactory.body(it.take(180) + if (it.length > 180) "…" else ""))
            }
            host.addView(card)
            host.addView(viewFactory.spacer(12))
            if (isSelected || (selectedEpisode !in episodeNumbers && index == 0)) {
                card.post { onFirstFocusTarget(card) }
            }
        }

        host.addView(viewFactory.button(
            "Find Sources for S${selectedSeason.toString().padStart(2, '0')}E${selectedEpisode.toString().padStart(2, '0')}",
            onFindSources
        ))
    }
}

class SourcesScreenRenderer(
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
        val title = if (mediaRef != null && state.selectedSeasonNumber != null && state.selectedEpisodeNumber != null) {
            "${mediaRef.title} S${state.selectedSeasonNumber.toString().padStart(2, '0')}E${state.selectedEpisodeNumber.toString().padStart(2, '0')}"
        } else {
            mediaRef?.title ?: "Sources"
        }

        host.addView(viewFactory.heroCard(
            title = title,
            subtitle = "Choose the clearest cached stream first — Kodi/Fenlight brain, cleaner native TV shell."
        ))
        host.addView(viewFactory.spacer())

        val summaryPanel = viewFactory.panel(elevated = true).apply {
            addView(viewFactory.sectionTitle("Source Summary"))
            addView(viewFactory.spacer(10))
            if (!error.isNullOrBlank()) {
                addView(viewFactory.body("Lookup issue: $error"))
                addView(viewFactory.spacer(10))
            }
            addView(viewFactory.caption(diagnostics ?: "No provider diagnostics yet."))
        }
        host.addView(summaryPanel)
        host.addView(viewFactory.spacer())

        if (state.selectedSources.isEmpty()) {
            host.addView(viewFactory.panel(elevated = false).apply {
                addView(viewFactory.title("No sources surfaced"))
                addView(viewFactory.spacer(8))
                addView(viewFactory.body("Either the providers came back empty, or the filter/ranking path stripped everything out."))
            })
            return
        }

        val topSources = state.selectedSources.take(3)
        if (topSources.isNotEmpty()) {
            host.addView(viewFactory.sectionTitle("Top Picks"))
            host.addView(viewFactory.spacer(12))
            val picksRow = LinearLayout(host.context).apply { orientation = LinearLayout.HORIZONTAL }
            topSources.forEachIndexed { index, source ->
                val pickCard = viewFactory.panel(elevated = true).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                        if (index > 0) it.marginStart = viewFactory.dp(12)
                    }
                    isFocusable = true
                    isClickable = true
                    setOnClickListener { onSourceSelected(source) }
                    setOnFocusChangeListener { view, hasFocus ->
                        view.scaleX = if (hasFocus) 1.02f else 1f
                        view.scaleY = if (hasFocus) 1.02f else 1f
                    }
                }
                pickCard.addView(TextView(host.context).apply {
                    text = source.displayName.lineSequence().firstOrNull()?.take(42) ?: source.displayName.take(42)
                    setTextColor(viewFactory.textPrimaryColor)
                    textSize = 20f
                    setTypeface(typeface, Typeface.BOLD)
                })
                pickCard.addView(viewFactory.spacer(8))
                pickCard.addView(viewFactory.caption("${source.quality} • ${source.cacheStatus}"))
                source.sizeLabel?.let {
                    pickCard.addView(viewFactory.spacer(8))
                    pickCard.addView(viewFactory.body(it))
                }
                picksRow.addView(pickCard)
                if (index == 0) {
                    pickCard.post { onFirstFocusTarget(pickCard) }
                }
            }
            host.addView(picksRow)
            host.addView(viewFactory.spacer())
            host.addView(viewFactory.sectionTitle("All Sources"))
            host.addView(viewFactory.spacer(12))
        }

        state.selectedSources.drop(3).take(13).forEach { source ->
            val card = viewFactory.panel(elevated = source.cacheStatus == CacheStatus.CACHED).apply {
                isFocusable = true
                isClickable = true
                setOnClickListener { onSourceSelected(source) }
                setOnFocusChangeListener { view, hasFocus ->
                    view.scaleX = if (hasFocus) 1.015f else 1f
                    view.scaleY = if (hasFocus) 1.015f else 1f
                }
            }

            card.addView(TextView(host.context).apply {
                text = source.displayName.lineSequence().firstOrNull()?.take(72) ?: source.displayName.take(72)
                setTextColor(viewFactory.textPrimaryColor)
                textSize = 21f
                setTypeface(typeface, Typeface.BOLD)
            })
            card.addView(viewFactory.spacer(8))

            val pillRow = LinearLayout(host.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START
            }
            pillRow.addView(viewFactory.statPill("Quality", source.quality.name, StatTone.ACCENT))
            pillRow.addView(statSpacer())
            pillRow.addView(
                viewFactory.statPill(
                    "Cache",
                    source.cacheStatus.name.lowercase(),
                    when (source.cacheStatus) {
                        CacheStatus.CACHED -> StatTone.SUCCESS
                        CacheStatus.UNCACHED -> StatTone.WARNING
                        CacheStatus.UNCHECKED,
                        CacheStatus.DIRECT -> StatTone.NORMAL
                    }
                )
            )
            source.sizeLabel?.let {
                pillRow.addView(statSpacer())
                pillRow.addView(viewFactory.statPill("Size", it, StatTone.NORMAL))
            }
            card.addView(HorizontalScrollView(host.context).apply { addView(pillRow) })
            card.addView(viewFactory.spacer(12))
            card.addView(viewFactory.caption("${source.providerDisplayName} • ${source.debridService.name.replace('_', ' ')}"))
            val detailText = buildString {
                source.sourceSite?.takeIf { it.isNotBlank() }?.let {
                    append(it)
                }
                source.rawMetadata["releaseTitle"]?.takeIf { it.isNotBlank() }?.let {
                    if (isNotBlank()) append(" • ")
                    append(it)
                }
            }
            detailText.takeIf { it.isNotBlank() }?.let {
                card.addView(viewFactory.spacer(8))
                card.addView(viewFactory.body(it.take(160)))
            }
            host.addView(card)
            host.addView(viewFactory.spacer(12))
        }
    }

    private fun statSpacer(): View = View(host.context).apply {
        layoutParams = LinearLayout.LayoutParams(viewFactory.dp(10), 1)
    }
}

class PlayerScreenRenderer(
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    fun render(
        state: AppState,
        playbackMessage: String?,
        playbackError: String?,
        playerView: PlayerView
    ) {
        val source = state.selectedSource
        if (source == null) {
            host.addView(viewFactory.title("Player"))
            host.addView(viewFactory.spacer(10))
            host.addView(viewFactory.body("No source selected."))
            return
        }

        val overlay = LinearLayout(host.context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(viewFactory.dp(24))
            background?.alpha = 0
        }
        overlay.addView(TextView(host.context).apply {
            text = source.mediaRef.title
            setTextColor(viewFactory.textPrimaryColor)
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
        })
        overlay.addView(viewFactory.caption(source.displayName.take(90)))
        playbackError?.takeIf { it.isNotBlank() }?.let {
            overlay.addView(viewFactory.spacer(10))
            overlay.addView(TextView(host.context).apply {
                text = it
                setTextColor(viewFactory.errorColor)
                textSize = 15f
            })
        }
        playbackMessage?.takeIf { it.isNotBlank() }?.let {
            overlay.addView(viewFactory.spacer(10))
            overlay.addView(TextView(host.context).apply {
                text = it.lineSequence().take(3).joinToString("\n")
                setTextColor(viewFactory.textSecondaryColor)
                textSize = 14f
            })
        }

        playerView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        host.addView(playerView)
        host.addView(overlay)
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
        onResetAuth: () -> Unit,
        onTogglePlaybackMode: () -> Unit,
        onCopyDebugInfo: () -> Unit,
        onCheckForUpdates: () -> Unit,
        onOpenLatestUpdate: (() -> Unit)?,
        onBackToHome: () -> Unit,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        host.addView(viewFactory.heroCard(
            title = "Settings / Accounts",
            subtitle = "Keep debrid auth and debug controls out of the main browsing flow — finally."
        ))
        host.addView(viewFactory.spacer())

        val accountPanel = viewFactory.panel(elevated = true).apply {
            addView(viewFactory.sectionTitle("Real-Debrid"))
            addView(viewFactory.spacer(10))
            addView(viewFactory.body(
                if (authState.isLinked) {
                    "Linked${authState.username?.let { " as $it" } ?: ""}. Cached and resolve-backed playback is ready."
                } else {
                    "Not linked yet. Debrid-backed playback and cached-source preference will stay limited until you connect it."
                }
            ))
            authState.lastError?.takeIf { it.isNotBlank() }?.let {
                addView(viewFactory.spacer(10))
                addView(TextView(activity).apply {
                    text = "Auth error: $it"
                    setTextColor(viewFactory.errorColor)
                    textSize = 15f
                })
            }
            activeDeviceFlow?.let { flow ->
                addView(viewFactory.spacer(16))
                addView(viewFactory.caption("Open ${flow.verificationUrl}"))
                addView(viewFactory.spacer(6))
                addView(TextView(activity).apply {
                    text = flow.userCode
                    setTextColor(viewFactory.warningColor)
                    textSize = 24f
                    setTypeface(typeface, Typeface.BOLD)
                    letterSpacing = 0.14f
                })
                addView(viewFactory.spacer(12))
                addView(viewFactory.button("Open Real-Debrid Link Page") {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(buildAuthUrl(flow))))
                })
            }
        }
        host.addView(accountPanel)
        host.addView(viewFactory.spacer())

        val playbackPanel = viewFactory.panel(elevated = false).apply {
            addView(viewFactory.sectionTitle("Playback"))
            addView(viewFactory.spacer(10))
            addView(viewFactory.body("Current render mode: $playbackModeLabel"))
        }
        host.addView(playbackPanel)
        host.addView(viewFactory.spacer())

        val updatePanel = viewFactory.panel(elevated = false).apply {
            addView(viewFactory.sectionTitle("Updates"))
            addView(viewFactory.spacer(10))
            addView(viewFactory.body(updateSummary ?: "No update check run yet."))
        }
        host.addView(updatePanel)
        host.addView(viewFactory.spacer())

        val primaryButton = if (!authState.isLinked) {
            viewFactory.button("Start Real-Debrid Link", onStartLink)
        } else {
            viewFactory.button("Toggle Playback Mode", onTogglePlaybackMode)
        }
        host.addView(primaryButton)
        primaryButton.post { onFirstFocusTarget(primaryButton) }
        host.addView(viewFactory.spacer(10))

        if (authState.isLinked) {
            host.addView(viewFactory.button("Reset Real-Debrid Auth", onResetAuth))
            host.addView(viewFactory.spacer(10))
        } else {
            host.addView(viewFactory.button("Toggle Playback Mode", onTogglePlaybackMode))
            host.addView(viewFactory.spacer(10))
        }
        host.addView(viewFactory.button("Check for Updates", onCheckForUpdates))
        host.addView(viewFactory.spacer(10))
        onOpenLatestUpdate?.let { openLatest ->
            host.addView(viewFactory.button("Open Latest APK", openLatest))
            host.addView(viewFactory.spacer(10))
        }
        host.addView(viewFactory.button("Copy Debug Info", onCopyDebugInfo))
        host.addView(viewFactory.spacer(10))
        host.addView(viewFactory.button("Back to Browse", onBackToHome))
    }
}
