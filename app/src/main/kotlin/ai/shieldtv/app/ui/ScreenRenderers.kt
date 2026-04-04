package ai.shieldtv.app.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.InputType
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.media3.ui.PlayerView
import ai.shieldtv.app.R
import ai.shieldtv.app.AppState
import ai.shieldtv.app.ContinueWatchingItem
import ai.shieldtv.app.SearchMode
import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.core.model.media.EpisodeSummary
import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType
import ai.shieldtv.app.core.model.media.SearchResult
import ai.shieldtv.app.core.model.playback.PlaybackState
import ai.shieldtv.app.core.model.source.CacheStatus
import ai.shieldtv.app.core.model.source.Quality
import ai.shieldtv.app.core.model.source.SourceResult
import coil.load

private fun updateDescendantTextColors(root: View, primary: Int, secondary: Int) {
    if (root is ViewGroup) {
        for (index in 0 until root.childCount) {
            updateDescendantTextColors(root.getChildAt(index), primary, secondary)
        }
        return
    }
    if (root is TextView) {
        val isEmphasis = root.textSize >= 17f || root.typeface?.isBold == true
        root.setTextColor(if (isEmphasis) primary else secondary)
    }
}

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
        onQuit: () -> Unit,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        host.removeAllViews()
        host.addView(viewFactory.sectionTitle("Library"))
        host.addView(viewFactory.spacer(12))

        val movieButton = focusableButton(
            if (!inSettings && selectedMode == SearchMode.MOVIES) "Movies •" else "Movies",
            onMovies
        )
        host.addView(movieButton)
        host.addView(viewFactory.spacer(10))
        host.addView(
            focusableButton(
                if (!inSettings && selectedMode == SearchMode.SHOWS) "TV Shows •" else "TV Shows",
                onShows
            )
        )
        host.addView(viewFactory.spacer(10))
        host.addView(focusableButton(if (inSettings) "Settings •" else "Settings", onSettings))
        host.addView(viewFactory.spacer(18))
        host.addView(viewFactory.sectionTitle("Session"))
        host.addView(viewFactory.spacer(12))
        host.addView(focusableButton("Quit", onQuit))

        movieButton.post { onFirstFocusTarget(movieButton) }
    }

    private fun focusableButton(text: String, onClick: () -> Unit): View {
        return viewFactory.button(text, onClick)
    }
}

class HomeScreenRenderer(
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    private val featuredMovies = listOf(
        SearchResult(
            mediaRef = MediaRef(MediaType.MOVIE, MediaIds(tmdbId = "438631", imdbId = null, tvdbId = null), "Dune: Part Two", year = 2024),
            subtitle = "Big sci-fi spectacle and a very safe default demo title.",
            posterUrl = "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/xOMo8BRK7PfcJv9JCnx7s5hj0PX.jpg",
            badges = listOf("Featured", "Movie")
        ),
        SearchResult(
            mediaRef = MediaRef(MediaType.MOVIE, MediaIds(tmdbId = "157336", imdbId = null, tvdbId = null), "Interstellar", year = 2014),
            subtitle = "Reliable showcase title with strong artwork and broad source availability.",
            posterUrl = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/rAiYTfKGqDCRIIqo664sY9XZIvQ.jpg",
            badges = listOf("Sci-Fi")
        ),
        SearchResult(
            mediaRef = MediaRef(MediaType.MOVIE, MediaIds(tmdbId = "603692", imdbId = null, tvdbId = null), "John Wick: Chapter 4", year = 2023),
            subtitle = "Action-heavy quick pick for testing details and source rows.",
            posterUrl = "https://image.tmdb.org/t/p/w500/vZloFAK7NmvMGKE7VkF5UHaz0I.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/h8gHn0OzBoaefsYseUByqsmEDMY.jpg",
            badges = listOf("Action")
        )
    )

    private val featuredShows = listOf(
        SearchResult(
            mediaRef = MediaRef(MediaType.SHOW, MediaIds(tmdbId = "95396", imdbId = null, tvdbId = null), "Severance", year = 2022),
            subtitle = "Great modern TV test case with strong episodic flow.",
            posterUrl = "https://image.tmdb.org/t/p/w500/7lQzaWm0M0aX9P4o0P6m0Ht4dQJ.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/kU98MbVVgi72wzceyrEbClZmMFe.jpg",
            badges = listOf("Featured", "TV")
        ),
        SearchResult(
            mediaRef = MediaRef(MediaType.SHOW, MediaIds(tmdbId = "83867", imdbId = null, tvdbId = null), "Andor", year = 2022),
            subtitle = "Good fit for source + episode navigation validation.",
            posterUrl = "https://image.tmdb.org/t/p/w500/59SVNwLfoMnZPPB6ukW6dlPxAdI.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/zGoZB4CboMzY1z4G3nU6BWnMDB2.jpg",
            badges = listOf("Sci-Fi")
        ),
        SearchResult(
            mediaRef = MediaRef(MediaType.SHOW, MediaIds(tmdbId = "100088", imdbId = null, tvdbId = null), "The Last of Us", year = 2023),
            subtitle = "Solid browse/demo pick with recognizable artwork.",
            posterUrl = "https://image.tmdb.org/t/p/w500/uKvVjHNqB5VmOrdxqAt2F7J78ED.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
            badges = listOf("Drama")
        )
    )

    fun render(
        state: AppState,
        authLinked: Boolean,
        statusMessage: String,
        onBrowseMovies: () -> Unit,
        onMovieFavorites: () -> Unit,
        onMovieHistory: () -> Unit,
        onBrowseShows: () -> Unit,
        onShowFavorites: () -> Unit,
        onShowHistory: () -> Unit,
        onOpenSettings: () -> Unit,
        onResumeSearch: (() -> Unit)?,
        onQuickPick: (SearchResult) -> Unit,
        onRecentQuery: (String) -> Unit,
        onContinueWatching: (ContinueWatchingItem) -> Unit,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        val libraryPicks = if (state.searchMode == SearchMode.SHOWS) featuredShows else featuredMovies
        val featured = dynamicPicks(state, libraryPicks)

        val firstMovieShortcut = actionButton("Browse Movies", onBrowseMovies)
        host.addView(viewFactory.sectionTitle("Movies"))
        host.addView(viewFactory.spacer(10))
        host.addView(shortcutRow(
            listOf(
                firstMovieShortcut,
                actionButton("Favorites", onMovieFavorites, 12),
                actionButton("Watch History", onMovieHistory, 12)
            )
        ))
        host.addView(viewFactory.spacer(14))

        host.addView(viewFactory.sectionTitle("TV Shows"))
        host.addView(viewFactory.spacer(10))
        host.addView(shortcutRow(
            listOf(
                actionButton("Browse TV Shows", onBrowseShows),
                actionButton("Favorites", onShowFavorites, 12),
                actionButton("Watch History", onShowHistory, 12)
            )
        ))
        host.addView(viewFactory.spacer(14))

        val actionRow = LinearLayout(host.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        val settingsButton = actionButton("Settings / Accounts", onOpenSettings)
        actionRow.addView(settingsButton)
        host.addView(actionRow)
        host.addView(viewFactory.spacer(14))

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
        host.addView(viewFactory.spacer(14))

        if (state.continueWatching.isNotEmpty()) {
            host.addView(viewFactory.sectionTitle("Continue Watching"))
            host.addView(viewFactory.spacer(12))
            host.addView(buildContinueWatchingRow(state.continueWatching, onContinueWatching, onFirstFocusTarget))
            host.addView(viewFactory.spacer(14))
        }

        host.addView(viewFactory.sectionTitle("Quick Picks"))
        host.addView(viewFactory.spacer(10))
        host.addView(buildQuickPickRow(featured, onQuickPick, onFirstFocusTarget))
        host.addView(viewFactory.spacer(14))

        val lowerRow = LinearLayout(host.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        val leftPanel = viewFactory.panel(elevated = true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(viewFactory.sectionTitle("What’s Ready"))
            addView(viewFactory.spacer(12))
            addView(viewFactory.body(statusMessage))
            addView(viewFactory.spacer(12))
            addView(viewFactory.caption("TMDb metadata, Torrentio source lookup, Real-Debrid auth/resolve, and Media3 playback are all in the loop."))
            onResumeSearch?.let {
                addView(viewFactory.spacer(16))
                addView(actionButton("Resume Last Flow", it))
            }
        }
        val rightPanel = viewFactory.panel(elevated = false).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = viewFactory.dp(14)
            }
            addView(viewFactory.sectionTitle("Recent Searches"))
            addView(viewFactory.spacer(12))
            if (state.recentQueries.isEmpty()) {
                addView(viewFactory.body("Nothing recent yet. Search for a title and it’ll show up here."))
            } else {
                state.recentQueries.take(6).forEachIndexed { index, query ->
                    val button = actionButton(query, onClick = { onRecentQuery(query) })
                    addView(button)
                    if (index < state.recentQueries.take(6).lastIndex) addView(viewFactory.spacer(10))
                }
            }
        }
        lowerRow.addView(leftPanel)
        lowerRow.addView(rightPanel)
        host.addView(lowerRow)

        firstMovieShortcut.post { onFirstFocusTarget(firstMovieShortcut) }
    }

    private fun dynamicPicks(state: AppState, fallback: List<SearchResult>): List<SearchResult> {
        val fromResults = state.searchResults.take(3)
        if (fromResults.isNotEmpty()) return fromResults
        val selected = state.selectedMedia?.let { media ->
            SearchResult(
                mediaRef = media,
                subtitle = "Pick up where you left off.",
                posterUrl = state.selectedDetails?.posterUrl,
                backdropUrl = state.selectedDetails?.backdropUrl,
                badges = listOf("Resume")
            )
        }
        return listOfNotNull(selected).plus(fallback).distinctBy { it.mediaRef.title }.take(3)
    }

    private fun buildContinueWatchingRow(
        items: List<ContinueWatchingItem>,
        onContinueWatching: (ContinueWatchingItem) -> Unit,
        onFirstFocusTarget: (View) -> Unit
    ): View {
        return LinearLayout(host.context).apply {
            orientation = LinearLayout.HORIZONTAL
            items.forEachIndexed { index, item ->
                val card = focusableCard(onClick = { onContinueWatching(item) }).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                        if (index > 0) it.marginStart = viewFactory.dp(12)
                    }
                }
                card.addView(ImageView(host.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        viewFactory.dp(120)
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    item.artworkUrl?.takeIf { it.isNotBlank() }?.let { load(it) }
                })
                card.addView(viewFactory.spacer(10))
                card.addView(TextView(host.context).apply {
                    text = item.mediaTitle
                    setTextColor(viewFactory.textPrimaryColor)
                    textSize = 17f
                    setTypeface(typeface, Typeface.BOLD)
                })
                card.addView(viewFactory.spacer(6))
                card.addView(viewFactory.caption("${item.subtitle} • Resumable"))
                card.addView(viewFactory.spacer(8))
                card.addView(viewFactory.progressBar(item.progressPercent))
                card.addView(viewFactory.spacer(8))
                card.addView(viewFactory.caption("Resume from ${formatProgress(item.progressPercent)}"))
                addView(card)
                if (index == 0) card.post { onFirstFocusTarget(card) }
            }
        }
    }

    private fun formatProgress(progressPercent: Int): String {
        return when {
            progressPercent <= 0 -> "start"
            progressPercent >= 95 -> "near end"
            else -> "$progressPercent%"
        }
    }

    private fun buildQuickPickRow(
        items: List<SearchResult>,
        onQuickPick: (SearchResult) -> Unit,
        onFirstFocusTarget: (View) -> Unit
    ): View {
        return LinearLayout(host.context).apply {
            orientation = LinearLayout.HORIZONTAL
            items.forEachIndexed { index, item ->
                val card = focusableCard(onClick = { onQuickPick(item) }).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                        if (index > 0) it.marginStart = viewFactory.dp(12)
                    }
                }
                card.addView(ImageView(host.context).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        viewFactory.dp(132)
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    (item.backdropUrl ?: item.posterUrl)?.takeIf { it.isNotBlank() }?.let { load(it) }
                })
                card.addView(viewFactory.spacer(10))
                card.addView(TextView(host.context).apply {
                    text = item.mediaRef.title
                    setTextColor(viewFactory.textPrimaryColor)
                    textSize = 18f
                    setTypeface(typeface, Typeface.BOLD)
                })
                card.addView(viewFactory.spacer(6))
                card.addView(viewFactory.caption(item.badges.joinToString(" • ")))
                addView(card)
                if (index == 0) card.post { onFirstFocusTarget(card) }
            }
        }
    }

    private fun shortcutRow(shortcuts: List<View>): View {
        return HorizontalScrollView(host.context).apply {
            addView(LinearLayout(host.context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.START
                shortcuts.forEach { addView(it) }
            })
        }
    }

    private fun actionButton(text: String, onClick: () -> Unit, startMarginDp: Int = 0): View {
        return viewFactory.button(text, onClick).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                if (startMarginDp > 0) it.marginStart = viewFactory.dp(startMarginDp)
            }
        }
    }

    private fun focusableCard(onClick: () -> Unit): LinearLayout {
        return viewFactory.panel(elevated = true).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            alpha = 0.97f
            setOnClickListener { onClick() }
            setOnFocusChangeListener { view, hasFocus ->
                view.scaleX = if (hasFocus) 1.05f else 1f
                view.scaleY = if (hasFocus) 1.05f else 1f
                view.alpha = if (hasFocus) 1f else 0.97f
            }
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    onClick()
                    true
                } else {
                    false
                }
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
        onResultSelected: (SearchResult) -> Unit,
        onResultLongPress: (SearchResult) -> Unit,
        onNewSearch: () -> Unit,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        val isFavoritesBrowse = state.favoritesBrowseMode != null
        val isHistoryBrowse = state.historyBrowseMode != null
        host.addView(viewFactory.title(
            when {
                isFavoritesBrowse -> "${state.searchMode.label} Favorites"
                isHistoryBrowse -> "${state.searchMode.label} Watch History"
                else -> "Results for \"${state.query}\""
            }
        ))
        host.addView(viewFactory.spacer(6))
        host.addView(viewFactory.caption(
            when {
                isFavoritesBrowse -> "${state.searchResults.size} favorite(s) in ${state.searchMode.label.lowercase()}, newest first."
                isHistoryBrowse -> "${state.searchResults.size} watched item(s) in ${state.searchMode.label.lowercase()}, newest first."
                else -> "${state.searchResults.size} match(es) in ${state.searchMode.label.lowercase()}."
            }
        ))
        host.addView(viewFactory.spacer(16))

        if (state.searchResults.isEmpty()) {
            host.addView(viewFactory.panel(elevated = true).apply {
                addView(viewFactory.title(
                    when {
                        isFavoritesBrowse -> "No favorites yet"
                        isHistoryBrowse -> "No watch history yet"
                        else -> "Nothing useful yet"
                    }
                ))
                addView(viewFactory.spacer(10))
                addView(viewFactory.body(
                    when {
                        isFavoritesBrowse -> "Favorite something from search results and it’ll show up here."
                        isHistoryBrowse -> "Start playback on something and it’ll show up here."
                        else -> emptyMessage
                    }
                ))
                addView(viewFactory.spacer(16))
                addView(viewFactory.button(if (isFavoritesBrowse || isHistoryBrowse) "Go to Search" else "Try another search", onNewSearch))
            })
            return
        }

        state.searchResults.take(20).forEachIndexed { index, result ->
            val card = focusableMediaCard(
                onClick = { onResultSelected(result) },
                onLongPress = { onResultLongPress(result) },
                elevated = index < 4
            ).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val posterView = ImageView(activity).apply {
                layoutParams = LinearLayout.LayoutParams(viewFactory.dp(104), viewFactory.dp(156)).apply {
                    marginEnd = viewFactory.dp(18)
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
                textSize = 21f
                setTypeface(typeface, Typeface.BOLD)
            })
            textColumn.addView(viewFactory.spacer(4))
            textColumn.addView(viewFactory.caption(
                buildString {
                    append(result.mediaRef.mediaType.name.lowercase().replaceFirstChar { it.uppercase() })
                    if (result.badges.isNotEmpty()) {
                        append(" • ")
                        append(result.badges.take(2).joinToString())
                    }
                }
            ))
            result.subtitle?.takeIf { it.isNotBlank() }?.let { subtitle ->
                textColumn.addView(viewFactory.spacer(8))
                textColumn.addView(viewFactory.body(subtitle.take(140)))
            }

            card.addView(posterView)
            card.addView(textColumn)
            host.addView(card)
            host.addView(viewFactory.spacer(12))
            if (index == 0) card.post { onFirstFocusTarget(card) }
        }

        host.addView(viewFactory.button("New Search", onNewSearch))
    }

    private fun focusableMediaCard(onClick: () -> Unit, onLongPress: () -> Unit, elevated: Boolean = true): LinearLayout {
        return viewFactory.panel(elevated = elevated).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            isLongClickable = true
            alpha = 0.97f
            setOnClickListener { onClick() }
            setOnLongClickListener {
                onLongPress()
                true
            }
            setOnFocusChangeListener { view, hasFocus ->
                view.scaleX = if (hasFocus) 1.02f else 1f
                view.scaleY = if (hasFocus) 1.02f else 1f
                view.alpha = if (hasFocus) 1f else 0.97f
                background = ContextCompat.getDrawable(
                    context,
                    if (hasFocus) R.drawable.asahi_button_bg else if (elevated) R.drawable.asahi_panel_elevated_bg else R.drawable.asahi_panel_bg
                )
                updateDescendantTextColors(
                    root = view,
                    primary = if (hasFocus) viewFactory.textPrimaryColor else viewFactory.textPrimaryColor,
                    secondary = if (hasFocus) viewFactory.textPrimaryColor else viewFactory.textSecondaryColor
                )
            }
            setOnKeyListener { _, keyCode, event ->
                when {
                    event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) -> {
                        onClick()
                        true
                    }
                    event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_MENU -> {
                        onLongPress()
                        true
                    }
                    else -> false
                }
            }
        }
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

        host.addView(viewFactory.artworkHero(
            title = details.mediaRef.title,
            subtitle = details.overview?.take(180) ?: "No overview yet.",
            imageUrl = details.backdropUrl ?: details.posterUrl,
            imageHeightDp = 320
        ))
        host.addView(viewFactory.spacer())

        val topPanel = LinearLayout(host.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        val posterPanel = viewFactory.panel(elevated = true).apply {
            layoutParams = LinearLayout.LayoutParams(viewFactory.dp(250), LinearLayout.LayoutParams.WRAP_CONTENT)
            val poster = ImageView(host.context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    viewFactory.dp(360)
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                details.posterUrl?.takeIf { it.isNotBlank() }?.let { load(it) }
            }
            addView(poster)
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

        val primaryAction = viewFactory.button(
            if (details.mediaRef.mediaType == MediaType.SHOW) "Browse Episodes" else "Find Sources",
            if (details.mediaRef.mediaType == MediaType.SHOW) onBrowseEpisodes else onFindSources
        )
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
        watchedEpisodeKeys: Set<String>,
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

        host.addView(viewFactory.artworkHero(
            title = details.mediaRef.title,
            subtitle = "Season $selectedSeason · Episode $selectedEpisode — closer to Stremio than the old one-page scroll monster.",
            imageUrl = details.backdropUrl ?: details.posterUrl,
            imageHeightDp = 280
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
            val isWatched = watchedEpisodeKeys.contains(
                ai.shieldtv.app.history.episodeWatchKey(
                    showIds = details.mediaRef.ids,
                    showTitle = details.mediaRef.title,
                    seasonNumber = selectedSeason,
                    episodeNumber = episode.episodeNumber
                )
            )
            val card = viewFactory.panel(vertical = false, elevated = isSelected).apply {
                gravity = Gravity.CENTER_VERTICAL
                setPadding(viewFactory.dp(16), viewFactory.dp(12), viewFactory.dp(16), viewFactory.dp(12))
                isFocusable = true
                isFocusableInTouchMode = true
                isClickable = true
                alpha = if (isSelected) 1f else 0.97f
                setOnClickListener {
                    onEpisodeSelected(episode.episodeNumber)
                    onEpisodePlay(episode.episodeNumber)
                }
                setOnFocusChangeListener { view, hasFocus ->
                    view.scaleX = if (hasFocus) 1.02f else 1f
                    view.scaleY = if (hasFocus) 1.02f else 1f
                    view.alpha = if (hasFocus || isSelected) 1f else 0.97f
                    background = ContextCompat.getDrawable(
                        context,
                        when {
                            hasFocus -> R.drawable.asahi_button_bg
                            isSelected -> R.drawable.asahi_panel_elevated_bg
                            else -> R.drawable.asahi_panel_bg
                        }
                    )
                    updateDescendantTextColors(
                        root = view,
                        primary = viewFactory.textPrimaryColor,
                        secondary = if (hasFocus || isSelected) viewFactory.textPrimaryColor else viewFactory.textSecondaryColor
                    )
                }
            }
            card.addView(TextView(activity).apply {
                text = episode.episodeNumber.toString().padStart(2, '0')
                setTextColor(if (isSelected) viewFactory.accentAltColor else viewFactory.textPrimaryColor)
                textSize = 18f
                setTypeface(typeface, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(viewFactory.dp(42), LinearLayout.LayoutParams.WRAP_CONTENT)
            })
            card.addView(TextView(activity).apply {
                text = episode.title?.takeIf { it.isNotBlank() } ?: "Episode ${episode.episodeNumber.toString().padStart(2, '0')}"
                setTextColor(viewFactory.textPrimaryColor)
                textSize = 17f
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            card.addView(TextView(activity).apply {
                text = buildString {
                    append("S${selectedSeason.toString().padStart(2, '0')}E${episode.episodeNumber.toString().padStart(2, '0')}")
                    if (isWatched) {
                        append(" • Watched")
                    }
                    episode.airDate?.takeIf { it.isNotBlank() }?.let {
                        append(" • ")
                        append(it)
                    }
                }
                setTextColor(if (isWatched) viewFactory.accentAltColor else viewFactory.textSecondaryColor)
                textSize = 13f
                maxLines = 1
            })
            host.addView(card)
            host.addView(viewFactory.spacer(8))
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

        host.addView(viewFactory.title(title))
        host.addView(viewFactory.spacer(6))
        host.addView(viewFactory.caption("Choose the clearest cached stream first."))
        host.addView(viewFactory.spacer(16))

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

        val grouped = state.selectedSources.groupBy { SourcePresentation.groupTitle(it) }

        grouped.forEach { (groupTitle, items) ->
            host.addView(viewFactory.sectionTitle(groupTitle))
            host.addView(viewFactory.spacer(12))
            items.take(10).forEachIndexed { index, source ->
                val card = focusableSourceCard(onClick = { onSourceSelected(source) }, elevated = source.cacheStatus == CacheStatus.CACHED).apply {
                    setPadding(viewFactory.dp(18), viewFactory.dp(14), viewFactory.dp(18), viewFactory.dp(14))
                }
                val row = LinearLayout(host.context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                }
                row.addView(TextView(host.context).apply {
                    text = source.displayName.lineSequence().firstOrNull()?.replace('\n', ' ')?.take(58) ?: source.displayName.take(58)
                    setTextColor(viewFactory.textPrimaryColor)
                    textSize = 18f
                    setTypeface(typeface, Typeface.BOLD)
                    maxLines = 1
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(TextView(host.context).apply {
                    text = SourcePresentation.detailLabel(source)
                    setTextColor(
                        when (source.cacheStatus) {
                            CacheStatus.CACHED -> viewFactory.accentAltColor
                            CacheStatus.DIRECT -> viewFactory.accentColor
                            CacheStatus.UNCACHED, CacheStatus.UNCHECKED -> viewFactory.warningColor
                        }
                    )
                    textSize = 13f
                    setTypeface(typeface, Typeface.BOLD)
                    maxLines = 1
                })
                card.addView(row)
                source.rawMetadata["flags"]?.takeIf { it.isNotBlank() }?.let { flags ->
                    card.addView(viewFactory.spacer(6))
                    card.addView(viewFactory.caption(flags.replace(",", " • ")))
                }
                host.addView(card)
                host.addView(viewFactory.spacer(8))
                if (groupTitle == "Cached Picks" && index == 0) card.post { onFirstFocusTarget(card) }
            }
            host.addView(viewFactory.spacer(8))
        }
    }

    private fun focusableSourceCard(onClick: () -> Unit, elevated: Boolean = true): LinearLayout {
        return viewFactory.panel(elevated = elevated).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            alpha = 0.97f
            setOnClickListener { onClick() }
            setOnFocusChangeListener { view, hasFocus ->
                view.scaleX = if (hasFocus) 1.02f else 1f
                view.scaleY = if (hasFocus) 1.02f else 1f
                view.alpha = if (hasFocus) 1f else 0.97f
                background = ContextCompat.getDrawable(
                    context,
                    if (hasFocus) R.drawable.asahi_button_bg else if (elevated) R.drawable.asahi_panel_elevated_bg else R.drawable.asahi_panel_bg
                )
                updateDescendantTextColors(
                    root = view,
                    primary = viewFactory.textPrimaryColor,
                    secondary = if (hasFocus) viewFactory.textPrimaryColor else viewFactory.textSecondaryColor
                )
            }
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    onClick()
                    true
                } else {
                    false
                }
            }
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
        playerView: PlayerView,
        playbackState: PlaybackState
    ) {
        val source = state.selectedSource
        if (source == null) {
            host.addView(viewFactory.title("Player"))
            host.addView(viewFactory.spacer(10))
            host.addView(viewFactory.body("No source selected."))
            return
        }

        val playerFrame = FrameLayout(host.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        playerView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        playerView.setBackgroundColor(Color.BLACK)
        playerView.setShutterBackgroundColor(Color.BLACK)
        playerView.useController = true
        playerView.controllerShowTimeoutMs = 3500
        playerFrame.addView(playerView)

        val playbackStateLabel = playbackState.playerStateLabel
        val playbackPositionMs = playbackState.positionMs
        val playbackDurationMs = playbackState.durationMs

        if (!playbackError.isNullOrBlank()) {
            val errorOverlay = viewFactory.panel(elevated = true).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                ).also {
                    it.marginStart = viewFactory.dp(24)
                    it.marginEnd = viewFactory.dp(24)
                    it.bottomMargin = viewFactory.dp(24)
                }
                alpha = 0.94f
                addView(viewFactory.sectionTitle("Playback Error"))
                addView(viewFactory.spacer(10))
                addView(TextView(host.context).apply {
                    text = playbackError
                    setTextColor(viewFactory.errorColor)
                    textSize = 16f
                })
            }
            playerFrame.addView(errorOverlay)
        } else {
            val shouldShowOverlay = playbackState.errorMessage != null
            if (shouldShowOverlay) {
                val subtleOverlay = LinearLayout(host.context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP or Gravity.START
                    )
                    setPadding(viewFactory.dp(20))
                    addView(TextView(host.context).apply {
                        text = source.mediaRef.title
                        setTextColor(viewFactory.textPrimaryColor)
                        textSize = 20f
                        setTypeface(typeface, Typeface.BOLD)
                    })
                    addView(viewFactory.caption(buildString {
                        append(stateLabel(playbackStateLabel))
                        append(" • ")
                        append(formatTime(playbackPositionMs))
                        append(" / ")
                        append(formatTime(playbackDurationMs))
                        source.seasonNumber?.let { season ->
                            val episode = source.episodeNumber ?: 1
                            append(" • S${season.toString().padStart(2, '0')}E${episode.toString().padStart(2, '0')}")
                        }
                    }))
                    playbackMessage?.lineSequence()?.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
                        addView(viewFactory.spacer(6))
                        addView(viewFactory.caption(it))
                    }
                }
                playerFrame.addView(subtleOverlay)
            }
        }

        host.addView(playerFrame)
    }

    private fun stateLabel(label: String): String = when (label.lowercase()) {
        "playing" -> "Playing"
        "paused" -> "Paused"
        "buffering" -> "Buffering"
        "ended" -> "Ended"
        "idle" -> "Idle"
        else -> label.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun formatTime(valueMs: Long): String {
        if (valueMs <= 0L) return "00:00"
        val totalSeconds = valueMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
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
        updateSummary: String?,
        providerSummary: String?,
        sourcePreferencesSummary: String?,
        movieMaxSizeLabel: String,
        tvMaxSizeLabel: String,
        providerSelectionLabel: String,
        buildAuthUrl: (DeviceCodeFlow) -> String,
        onStartLink: () -> Unit,
        onResetAuth: () -> Unit,
        onCopyDebugInfo: () -> Unit,
        onCheckForUpdates: () -> Unit,
        onOpenLatestUpdate: (() -> Unit)?,
        onConfigureMovieMaxSize: () -> Unit,
        onConfigureTvMaxSize: () -> Unit,
        onToggleProviders: () -> Unit,
        onResetSourcePreferences: () -> Unit,
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

        val updatePanel = viewFactory.panel(elevated = false).apply {
            addView(viewFactory.sectionTitle("Updates"))
            addView(viewFactory.spacer(10))
            addView(viewFactory.body(updateSummary ?: "No update check run yet."))
            addView(viewFactory.spacer(10))
            addView(viewFactory.caption("If install fails, the most likely causes are signature mismatch or a non-incrementing version code on the downloaded APK."))
        }
        host.addView(updatePanel)
        host.addView(viewFactory.spacer())

        val providersPanel = viewFactory.panel(elevated = false).apply {
            addView(viewFactory.sectionTitle("Providers"))
            addView(viewFactory.spacer(10))
            addView(viewFactory.body(providerSummary ?: "No provider diagnostics yet."))
            addView(viewFactory.spacer(10))
            addView(viewFactory.caption(sourcePreferencesSummary ?: "No source preference overrides yet."))
        }
        host.addView(providersPanel)
        host.addView(viewFactory.spacer())

        val primaryButton = viewFactory.button(
            "Start Real-Debrid Link",
            onStartLink
        )
        host.addView(primaryButton)
        primaryButton.post { onFirstFocusTarget(primaryButton) }
        host.addView(viewFactory.spacer(10))

        if (authState.isLinked) {
            host.addView(viewFactory.button("Reset Real-Debrid Auth", onResetAuth))
            host.addView(viewFactory.spacer(10))
        }
        host.addView(viewFactory.button("Check for Updates", onCheckForUpdates))
        host.addView(viewFactory.spacer(10))
        onOpenLatestUpdate?.let { openLatest ->
            host.addView(viewFactory.button("Open Latest APK", openLatest))
            host.addView(viewFactory.spacer(10))
        }
        host.addView(viewFactory.button("Movie Max Size ($movieMaxSizeLabel)", onConfigureMovieMaxSize))
        host.addView(viewFactory.spacer(10))
        host.addView(viewFactory.button("TV Max Size ($tvMaxSizeLabel)", onConfigureTvMaxSize))
        host.addView(viewFactory.spacer(10))
        host.addView(viewFactory.button("Choose Providers ($providerSelectionLabel)", onToggleProviders))
        host.addView(viewFactory.spacer(10))
        host.addView(viewFactory.button("Reset Source Preferences", onResetSourcePreferences))
        host.addView(viewFactory.spacer(10))
        host.addView(viewFactory.button("Copy Debug Info", onCopyDebugInfo))
        host.addView(viewFactory.spacer(10))
        host.addView(viewFactory.button("Back to Browse", onBackToHome))
    }
}
