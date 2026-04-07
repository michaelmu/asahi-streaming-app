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
import androidx.core.view.children
import androidx.core.view.setMargins
import android.view.inputmethod.EditorInfo
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.media3.ui.PlayerView
import ai.shieldtv.app.R
import ai.shieldtv.app.AppState
import ai.shieldtv.app.ContinueWatchingItem
import ai.shieldtv.app.SearchMode
import ai.shieldtv.app.core.model.auth.DeviceCodeFlow
import ai.shieldtv.app.core.model.auth.RealDebridAuthState
import ai.shieldtv.app.favorites.toFavoriteStableKey
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
        inHome: Boolean,
        inSearch: Boolean,
        selectedMode: SearchMode,
        inSettings: Boolean,
        onHome: () -> Unit,
        onMovies: () -> Unit,
        onShows: () -> Unit,
        onSettings: () -> Unit,
        onQuit: () -> Unit,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        host.removeAllViews()
        host.addView(viewFactory.sectionTitle("Navigate"))
        host.addView(viewFactory.spacer(12))

        val homeButton = focusableButton("Home", R.drawable.ic_nav_home, onHome, selected = inHome)
        host.addView(homeButton)
        host.addView(viewFactory.spacer(10))
        host.addView(focusableButton("Movies", R.drawable.ic_nav_movie, onMovies, selected = inSearch && !inSettings && selectedMode == SearchMode.MOVIES))
        host.addView(viewFactory.spacer(10))
        host.addView(focusableButton("TV Shows", R.drawable.ic_nav_tv, onShows, selected = inSearch && !inSettings && selectedMode == SearchMode.SHOWS))
        host.addView(viewFactory.spacer(10))
        host.addView(focusableButton("Settings", R.drawable.ic_nav_settings, onSettings, selected = inSettings))
        host.addView(viewFactory.spacer(18))
        host.addView(viewFactory.sectionTitle("Session"))
        host.addView(viewFactory.spacer(12))
        host.addView(focusableButton("Quit", R.drawable.ic_nav_history, onQuit))

        homeButton.post { onFirstFocusTarget(homeButton) }
    }

    private fun focusableButton(text: String, iconResId: Int, onClick: () -> Unit, selected: Boolean = false): View {
        return viewFactory.button(text, onClick, selected = selected, iconResId = iconResId)
    }
}

class HomeScreenRenderer(
    private val host: LinearLayout,
    private val viewFactory: ScreenViewFactory
) {
    private val featuredMovies = listOf(
        SearchResult(
            mediaRef = MediaRef(MediaType.MOVIE, MediaIds(tmdbId = "438631", imdbId = null, tvdbId = null), "Dune: Part Two", year = 2024),
            subtitle = "Big sci-fi spectacle with a proper big-screen silhouette.",
            posterUrl = "https://image.tmdb.org/t/p/w500/1pdfLvkbY9ohJlCjQH2CZjjYVvJ.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/xOMo8BRK7PfcJv9JCnx7s5hj0PX.jpg",
            badges = listOf("Featured", "Epic")
        ),
        SearchResult(
            mediaRef = MediaRef(MediaType.MOVIE, MediaIds(tmdbId = "157336", imdbId = null, tvdbId = null), "Interstellar", year = 2014),
            subtitle = "Quiet start, huge payoff, and still one of the better couch-night picks.",
            posterUrl = "https://image.tmdb.org/t/p/w500/gEU2QniE6E77NI6lCU6MxlNBvIx.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/rAiYTfKGqDCRIIqo664sY9XZIvQ.jpg",
            badges = listOf("Sci-Fi")
        ),
        SearchResult(
            mediaRef = MediaRef(MediaType.MOVIE, MediaIds(tmdbId = "603692", imdbId = null, tvdbId = null), "John Wick: Chapter 4", year = 2023),
            subtitle = "Pure momentum when you want something fast and loud.",
            posterUrl = "https://image.tmdb.org/t/p/w500/vZloFAK7NmvMGKE7VkF5UHaz0I.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/h8gHn0OzBoaefsYseUByqsmEDMY.jpg",
            badges = listOf("Action")
        )
    )

    private val featuredShows = listOf(
        SearchResult(
            mediaRef = MediaRef(MediaType.SHOW, MediaIds(tmdbId = "95396", imdbId = null, tvdbId = null), "Severance", year = 2022),
            subtitle = "Sharp, strange, and exactly the kind of show that earns a hero slot.",
            posterUrl = "https://image.tmdb.org/t/p/w500/7lQzaWm0M0aX9P4o0P6m0Ht4dQJ.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/kU98MbVVgi72wzceyrEbClZmMFe.jpg",
            badges = listOf("Featured", "TV")
        ),
        SearchResult(
            mediaRef = MediaRef(MediaType.SHOW, MediaIds(tmdbId = "83867", imdbId = null, tvdbId = null), "Andor", year = 2022),
            subtitle = "Slow-burn sci-fi with enough weight to carry a full evening.",
            posterUrl = "https://image.tmdb.org/t/p/w500/59SVNwLfoMnZPPB6ukW6dlPxAdI.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/zGoZB4CboMzY1z4G3nU6BWnMDB2.jpg",
            badges = listOf("Sci-Fi")
        ),
        SearchResult(
            mediaRef = MediaRef(MediaType.SHOW, MediaIds(tmdbId = "100088", imdbId = null, tvdbId = null), "The Last of Us", year = 2023),
            subtitle = "Big emotional stakes, good art, easy sell from the sofa.",
            posterUrl = "https://image.tmdb.org/t/p/w500/uKvVjHNqB5VmOrdxqAt2F7J78ED.jpg",
            backdropUrl = "https://image.tmdb.org/t/p/w780/56v2KjBlU4XaOv9rVYEQypROD7P.jpg",
            badges = listOf("Drama")
        )
    )

    fun render(
        state: AppState,
        movieFavorites: List<SearchResult>,
        showFavorites: List<SearchResult>,
        movieHistory: List<SearchResult>,
        showHistory: List<SearchResult>,
        onBrowseMovies: () -> Unit,
        onMovieFavorites: () -> Unit,
        onMovieHistory: () -> Unit,
        onBrowseShows: () -> Unit,
        onShowFavorites: () -> Unit,
        onShowHistory: () -> Unit,
        onQuickPick: (SearchResult) -> Unit,
        onRecentQuery: (String) -> Unit,
        onContinueWatching: (ContinueWatchingItem) -> Unit,
        onFavoriteSelected: (SearchResult) -> Unit,
        onHistorySelected: (SearchResult) -> Unit,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        host.removeAllViews()
        host.clearFocus()

        val libraryPicks = if (state.searchMode == SearchMode.SHOWS) featuredShows else featuredMovies
        val featured = dynamicPicks(state, libraryPicks)

        val heroPick = featured.firstOrNull()
        val shelfPicks = featured.drop(1)

        if (state.continueWatching.isNotEmpty()) {
            host.addView(viewFactory.sectionTitle("Continue Watching"))
            host.addView(viewFactory.spacer(12))
            host.addView(buildContinueWatchingRow(state.continueWatching, onContinueWatching, onFirstFocusTarget))
            host.addView(viewFactory.spacer(18))
        } else if (heroPick != null) {
            host.addView(buildFeaturedHero(heroPick, onQuickPick, onFirstFocusTarget))
            host.addView(viewFactory.spacer(18))
        }

        if (shelfPicks.isNotEmpty()) {
            host.addView(viewFactory.sectionTitle("Quick Picks"))
            host.addView(viewFactory.spacer(10))
            host.addView(buildQuickPickRow(shelfPicks, onQuickPick, onFirstFocusTarget))
            host.addView(viewFactory.spacer(18))
        }

        val favoritesShelfItems = (movieFavorites + showFavorites).take(4)
        host.addView(viewFactory.sectionTitle("Your Picks"))
        host.addView(viewFactory.spacer(10))
        if (favoritesShelfItems.isNotEmpty()) {
            host.addView(buildQuickPickRow(favoritesShelfItems, onFavoriteSelected, onFirstFocusTarget))
        } else {
            host.addView(
                viewFactory.emptyStatePanel(
                    title = "No favorites yet",
                    body = "Save a movie or show from search results and it’ll appear here.",
                    nextStep = "Use the menu on a search result to add it quickly."
                )
            )
        }
        host.addView(viewFactory.spacer(10))
        host.addView(buildHomeActionRow(
            primaryLabel = "Open Movie Favorites",
            onPrimary = onMovieFavorites,
            primaryIcon = R.drawable.ic_nav_favorite,
            secondaryLabel = "Open TV Favorites",
            onSecondary = onShowFavorites,
            secondaryIcon = R.drawable.ic_nav_favorite
        ))
        host.addView(viewFactory.spacer(14))

        val historyShelfItems = (showHistory + movieHistory).take(4)
        host.addView(viewFactory.sectionTitle("Recently Watched"))
        host.addView(viewFactory.spacer(10))
        if (historyShelfItems.isNotEmpty()) {
            host.addView(buildQuickPickRow(historyShelfItems, onHistorySelected, onFirstFocusTarget))
        } else {
            host.addView(
                viewFactory.emptyStatePanel(
                    title = "No watch history yet",
                    body = "Start a movie or episode and it’ll show up here for quick return.",
                    nextStep = "Playback automatically turns this into a resume shelf."
                )
            )
        }
        host.addView(viewFactory.spacer(10))
        host.addView(buildHomeActionRow(
            primaryLabel = "Open Movie Watch History",
            onPrimary = onMovieHistory,
            primaryIcon = R.drawable.ic_nav_history,
            secondaryLabel = "Open TV Watch History",
            onSecondary = onShowHistory,
            secondaryIcon = R.drawable.ic_nav_history
        ))
        host.addView(viewFactory.spacer(14))

        val lowerRow = LinearLayout(host.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        val browsePanel = viewFactory.panel(elevated = true).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            addView(viewFactory.sectionTitle("Browse"))
            addView(viewFactory.spacer(8))
            addView(viewFactory.caption("Jump straight into movies or TV."))
            addView(viewFactory.spacer(12))
            addView(actionButton("Browse Movies", onBrowseMovies, R.drawable.ic_nav_movie))
            addView(viewFactory.spacer(10))
            addView(actionButton("Browse TV Shows", onBrowseShows, R.drawable.ic_nav_tv))
        }
        val homeRecentQueries = (state.recentMovieQueries + state.recentShowQueries)
            .distinctBy { it.lowercase() }
            .take(4)
        val recentPanel = viewFactory.panel(elevated = false).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = viewFactory.dp(16)
            }
            addView(viewFactory.sectionTitle("Recent Searches"))
            addView(viewFactory.spacer(8))
            if (homeRecentQueries.isEmpty()) {
                addView(viewFactory.caption("Recent searches show up here after your first browse."))
            } else {
                homeRecentQueries.forEachIndexed { index, query ->
                    val button = actionButton(query, onClick = { onRecentQuery(query) }, iconResId = R.drawable.ic_nav_search)
                    addView(button)
                    if (index < homeRecentQueries.lastIndex) addView(viewFactory.spacer(10))
                }
            }
        }
        lowerRow.addView(browsePanel)
        lowerRow.addView(recentPanel)
        host.addView(lowerRow)

        host.post {
            host.findFocus()?.let(onFirstFocusTarget)
        }
    }

    private fun buildFeaturedHero(
        item: SearchResult,
        onQuickPick: (SearchResult) -> Unit,
        onFirstFocusTarget: (View) -> Unit
    ): View {
        return viewFactory.artworkHero(
            title = item.mediaRef.title,
            subtitle = item.subtitle ?: "A solid featured pick for a couch-first browse.",
            imageUrl = item.backdropUrl?.takeIf { it.isNotBlank() } ?: item.posterUrl,
            imageHeightDp = 300
        ).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            background = ContextCompat.getDrawable(context, R.drawable.asahi_poster_card_bg)
            foreground = null
            alpha = 0.99f
            setOnClickListener { onQuickPick(item) }
            setOnFocusChangeListener { view, hasFocus ->
                view.scaleX = if (hasFocus) 1.02f else 1f
                view.scaleY = if (hasFocus) 1.02f else 1f
                view.alpha = if (hasFocus) 1f else 0.99f
                view.translationZ = if (hasFocus) viewFactory.dp(20).toFloat() else 0f
            }
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    onQuickPick(item)
                    true
                } else {
                    false
                }
            }
            post { onFirstFocusTarget(this) }
        }
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
                card.minimumHeight = viewFactory.dp(132)
                card.addView(homeShelfArtwork(
                    artworkUrl = item.artworkUrl,
                    heightDp = 132,
                    fallbackIconResId = R.drawable.ic_nav_play,
                    fallbackLabel = "Continue Watching"
                ))
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
                card.minimumHeight = viewFactory.dp(140)
                card.addView(homeShelfArtwork(
                    artworkUrl = item.backdropUrl?.takeIf { it.isNotBlank() } ?: item.posterUrl,
                    heightDp = 140,
                    fallbackIconResId = if (item.mediaRef.mediaType == MediaType.SHOW) R.drawable.ic_nav_tv else R.drawable.ic_nav_movie,
                    fallbackLabel = item.mediaRef.mediaType.name.lowercase().replaceFirstChar { it.uppercase() }
                ))
                addView(card)
                if (index == 0) card.post { onFirstFocusTarget(card) }
            }
        }
    }

    private fun buildHomeActionRow(
        primaryLabel: String,
        onPrimary: () -> Unit,
        primaryIcon: Int,
        secondaryLabel: String,
        onSecondary: () -> Unit,
        secondaryIcon: Int
    ): View {
        return LinearLayout(host.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            addView(actionButton(primaryLabel, onPrimary, primaryIcon).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            addView(actionButton(secondaryLabel, onSecondary, secondaryIcon).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginStart = viewFactory.dp(12)
                }
            })
        }
    }

    private fun actionButton(text: String, onClick: () -> Unit, iconResId: Int? = null, startMarginDp: Int = 0): View {
        return viewFactory.button(text, onClick, iconResId = iconResId).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                if (startMarginDp > 0) it.marginStart = viewFactory.dp(startMarginDp)
            }
        }
    }

    private fun focusableCard(onClick: () -> Unit): LinearLayout {
        return LinearLayout(host.context).apply {
            orientation = LinearLayout.VERTICAL
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            alpha = 0.985f
            foreground = null
            setOnClickListener { onClick() }
            setOnFocusChangeListener { view, hasFocus ->
                view.scaleX = if (hasFocus) 1.03f else 1f
                view.scaleY = if (hasFocus) 1.03f else 1f
                view.alpha = if (hasFocus) 1f else 0.985f
                view.translationZ = if (hasFocus) viewFactory.dp(18).toFloat() else 0f
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

    private fun homeShelfArtwork(
        artworkUrl: String?,
        heightDp: Int,
        fallbackIconResId: Int,
        fallbackLabel: String
    ): View {
        return FrameLayout(host.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                viewFactory.dp(heightDp)
            )

            val resolvedArtwork = artworkUrl?.takeIf { it.isNotBlank() }
            addView(ImageView(host.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                resolvedArtwork?.let { load(it) }
                    ?: setImageDrawable(ContextCompat.getDrawable(context, fallbackIconResId))
                imageAlpha = if (resolvedArtwork == null) 144 else 255
                setBackgroundColor(Color.argb(18, 255, 255, 255))
            })

            addView(View(host.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.argb(18, 7, 10, 15))
            })

            addView(TextView(host.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                ).apply {
                    setMargins(viewFactory.dp(10))
                }
                text = fallbackLabel
                setTextColor(viewFactory.textPrimaryColor)
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                visibility = if (resolvedArtwork == null) View.VISIBLE else View.GONE
            })
        }
    }

    private fun homeShelfMetadata(item: SearchResult): String {
        val tokens = mutableListOf<String>()
        item.mediaRef.year?.let { tokens += it.toString() }
        tokens += item.mediaRef.mediaType.name.lowercase().replaceFirstChar { it.uppercase() }
        tokens += item.badges.filterNot {
            it.equals("Favorite", ignoreCase = true) || it.equals("Watched", ignoreCase = true)
        }.take(1)
        if (item.badges.any { it.equals("Favorite", ignoreCase = true) }) tokens += "Favorite"
        if (item.badges.any { it.equals("Watched", ignoreCase = true) }) tokens += "Watched"
        return tokens.distinct().joinToString(" • ")
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
        onOpenFavorites: () -> Unit,
        onOpenHistory: () -> Unit,
        onBack: (() -> Unit)?,
        onFirstFocusTarget: (View) -> Unit = {}
    ) {
        val recentQueries = when (state.searchMode) {
            SearchMode.MOVIES -> state.recentMovieQueries
            SearchMode.SHOWS -> state.recentShowQueries
        }

        val searchPanel = viewFactory.panel(elevated = true).apply {
            addView(viewFactory.pageTitle(state.searchMode.label))
            addView(viewFactory.spacer(12))

            val actionRow = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            val favoritesButton = viewFactory.button("Favorites", onClick = onOpenFavorites, iconResId = R.drawable.ic_nav_favorite).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val historyButton = viewFactory.button("Watch History", onClick = onOpenHistory, iconResId = R.drawable.ic_nav_history).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginStart = viewFactory.dp(16)
                }
            }
            actionRow.addView(favoritesButton)
            actionRow.addView(historyButton)
            addView(actionRow)
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
                initialValue = state.query.takeIf { state.favoritesBrowseMode == null && state.historyBrowseMode == null } ?: ""
            ).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                inputType = InputType.TYPE_CLASS_TEXT
            }
            val submitSearch = {
                onSearch(state.searchMode, queryInput.text.toString())
            }
            queryInput.setOnEditorActionListener { _, actionId, event ->
                val isSubmitAction = actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE
                val isEnterKey = event?.action == KeyEvent.ACTION_DOWN &&
                    (event.keyCode == KeyEvent.KEYCODE_ENTER || event.keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)
                if (isSubmitAction || isEnterKey) {
                    submitSearch()
                    true
                } else {
                    false
                }
            }
            val searchButton = viewFactory.button("Search", onClick = {
                submitSearch()
            }, iconResId = R.drawable.ic_nav_search).apply {
                id = View.generateViewId()
                layoutParams = LinearLayout.LayoutParams(
                    viewFactory.dp(180),
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginStart = viewFactory.dp(16) }
            }

            searchRow.addView(queryInput)
            searchRow.addView(searchButton)
            addView(searchRow)
            if (recentQueries.isNotEmpty()) {
                addView(viewFactory.spacer(12))
                addView(viewFactory.sectionTitle("Recent Searches"))
                addView(viewFactory.spacer(8))
                recentQueries.take(3).forEachIndexed { index, query ->
                    val recentButton = viewFactory.button(query, onClick = { onSearch(state.searchMode, query) }, iconResId = R.drawable.ic_nav_search)
                    addView(recentButton)
                    if (index < recentQueries.take(3).lastIndex) addView(viewFactory.spacer(10))
                }
            }
            addView(viewFactory.spacer(10))
            addView(viewFactory.caption("Down moves into recent searches first when present, then into results controls, with favorites and history one step above."))

            favoritesButton.nextFocusDownId = queryInput.id
            historyButton.nextFocusDownId = searchButton.id
            queryInput.nextFocusUpId = favoritesButton.id
            queryInput.nextFocusRightId = searchButton.id
            searchButton.nextFocusUpId = historyButton.id
            searchButton.nextFocusLeftId = queryInput.id

            favoritesButton.post {
                onFirstFocusTarget(favoritesButton)
                favoritesButton.requestFocus()
            }
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
    private val gridColumnCount = 4

    fun render(
        state: AppState,
        emptyMessage: String,
        favoriteKeys: Set<String>,
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
        host.addView(viewFactory.spacer(18))

        if (state.searchResults.isEmpty()) {
            host.addView(
                viewFactory.emptyStatePanel(
                    title = when {
                        isFavoritesBrowse -> "No favorites yet"
                        isHistoryBrowse -> "No watch history yet"
                        else -> "No matches yet"
                    },
                    body = when {
                        isFavoritesBrowse -> "Favorite something from search results and it’ll show up here. Use Search to find a title, then open the menu to save it."
                        isHistoryBrowse -> "Start playback on something and it’ll show up here. Once you play a title, this view becomes your quick return path."
                        else -> emptyMessage.ifBlank { "Try a broader title, a simpler query, or switch between Movies and TV Shows." }
                    },
                    nextStep = when {
                        isFavoritesBrowse -> "Next step: go to Search, open a result, then add it to favorites."
                        isHistoryBrowse -> "Next step: search for something, start playback, then come back here."
                        else -> "Next step: try another search with fewer words or a cleaner title."
                    },
                    actionLabel = if (isFavoritesBrowse || isHistoryBrowse) "Go to Search" else "Try another search",
                    actionIconResId = R.drawable.ic_nav_search,
                    onAction = onNewSearch
                )
            )
            return
        }

        val results = state.searchResults.take(20)
        val grid = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
        }

        results.chunked(gridColumnCount).forEachIndexed { rowIndex, rowItems ->
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.TOP
            }
            rowItems.forEachIndexed { columnIndex, result ->
                val absoluteIndex = rowIndex * gridColumnCount + columnIndex
                val card = focusablePosterCard(
                    result = result,
                    isFavorite = favoriteKeys.contains(result.toFavoriteStableKey()),
                    onClick = { onResultSelected(result) },
                    onLongPress = { onResultLongPress(result) },
                    elevated = absoluteIndex < gridColumnCount
                ).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                        if (columnIndex > 0) it.marginStart = viewFactory.dp(16)
                    }
                }
                row.addView(card)
                if (absoluteIndex == 0) {
                    card.post { onFirstFocusTarget(card) }
                }
            }
            repeat((gridColumnCount - rowItems.size).coerceAtLeast(0)) { spacerIndex ->
                row.addView(View(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(0, 1, 1f).also {
                        if (rowItems.isNotEmpty() || spacerIndex > 0) it.marginStart = viewFactory.dp(16)
                    }
                    isFocusable = false
                    isClickable = false
                    alpha = 0f
                })
            }
            grid.addView(row)
            if (rowIndex < results.chunked(gridColumnCount).lastIndex) {
                grid.addView(viewFactory.spacer(16))
            }
        }

        host.addView(grid)
        host.addView(viewFactory.spacer(18))
        host.addView(viewFactory.button("New Search", onNewSearch, iconResId = R.drawable.ic_nav_search))
    }

    private fun focusablePosterCard(
        result: SearchResult,
        isFavorite: Boolean,
        onClick: () -> Unit,
        onLongPress: () -> Unit,
        elevated: Boolean = true
    ): LinearLayout {
        return LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            minimumHeight = viewFactory.dp(300)
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            isLongClickable = true
            alpha = 0.985f
            foreground = null
            elevation = viewFactory.dp(if (elevated) 8 else 4).toFloat()

            val artworkUrl = result.posterUrl?.takeIf { it.isNotBlank() }
                ?: result.backdropUrl?.takeIf { it.isNotBlank() }

            val posterFrame = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    viewFactory.dp(300)
                )
            }
            val posterView = ImageView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                artworkUrl?.let { url -> load(url) }
                    ?: setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_nav_movie))
                imageAlpha = if (artworkUrl == null) 144 else 255
                setBackgroundColor(Color.argb(18, 255, 255, 255))
            }
            posterFrame.addView(posterView)

            val posterScrim = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(Color.argb(if (isFavorite) 14 else 18, 7, 10, 15))
            }
            posterFrame.addView(posterScrim)

            val fallbackLabel = TextView(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.BOTTOM
                ).apply {
                    setMargins(viewFactory.dp(10))
                }
                text = result.mediaRef.mediaType.name.lowercase().replaceFirstChar { it.uppercase() }
                setTextColor(viewFactory.textPrimaryColor)
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                visibility = if (artworkUrl == null) View.VISIBLE else View.GONE
            }
            posterFrame.addView(fallbackLabel)
            addView(posterFrame)

            setOnClickListener { onClick() }
            setOnLongClickListener {
                onLongPress()
                true
            }
            setOnFocusChangeListener { view, hasFocus ->
                view.scaleX = if (hasFocus) 1.025f else 1f
                view.scaleY = if (hasFocus) 1.025f else 1f
                view.alpha = if (hasFocus) 1f else 0.985f
                view.translationZ = if (hasFocus) viewFactory.dp(20).toFloat() else 0f
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
        isFavorite: Boolean,
        onToggleFavorite: () -> Unit,
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

        val primaryActionLabel = if (details.mediaRef.mediaType == MediaType.SHOW) "Browse Episodes" else "Find Sources"
        val primaryActionHandler = if (details.mediaRef.mediaType == MediaType.SHOW) onBrowseEpisodes else onFindSources
        val metadataLine = buildList {
            add(details.mediaRef.mediaType.name.lowercase().replaceFirstChar { it.uppercase() })
            details.mediaRef.year?.let { add(it.toString()) }
            details.runtimeMinutes?.let { add("${it}m") }
            details.seasonCount?.let { add("${it} season" + if (it == 1) "" else "s") }
        }.joinToString(" • ")
        val overviewText = details.overview?.trim().orEmpty().ifBlank { "No overview yet." }
        val overviewPreview = if (overviewText.length > 280) overviewText.take(277).trimEnd() + "..." else overviewText

        host.addView(viewFactory.artworkHero(
            title = details.mediaRef.title,
            subtitle = listOfNotNull(
                metadataLine.takeIf { it.isNotBlank() },
                details.genres.take(3).takeIf { it.isNotEmpty() }?.joinToString(" • ")
            ).joinToString("\n"),
            imageUrl = details.backdropUrl?.takeIf { it.isNotBlank() } ?: details.posterUrl,
            imageHeightDp = 320
        ))
        host.addView(viewFactory.spacer(18))

        val topPanel = LinearLayout(host.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
        }
        val posterPanel = FrameLayout(host.context).apply {
            layoutParams = LinearLayout.LayoutParams(viewFactory.dp(270), viewFactory.dp(405))
            val poster = ImageView(host.context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                details.posterUrl?.takeIf { it.isNotBlank() }?.let { load(it) }
                    ?: details.backdropUrl?.takeIf { it.isNotBlank() }?.let { load(it) }
                    ?: setImageDrawable(ContextCompat.getDrawable(context, if (details.mediaRef.mediaType == MediaType.SHOW) R.drawable.ic_nav_tv else R.drawable.ic_nav_movie))
                imageAlpha = if (details.posterUrl.isNullOrBlank() && details.backdropUrl.isNullOrBlank()) 144 else 255
                setBackgroundColor(Color.argb(18, 255, 255, 255))
            }
            addView(poster)
        }
        val infoPanel = LinearLayout(host.context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = viewFactory.dp(20)
            }
        }

        infoPanel.addView(viewFactory.sectionTitle("Up Next"))
        infoPanel.addView(viewFactory.spacer(8))
        infoPanel.addView(viewFactory.title(primaryActionLabel))
        infoPanel.addView(viewFactory.spacer(8))
        infoPanel.addView(viewFactory.caption(
            if (details.mediaRef.mediaType == MediaType.SHOW) {
                "Open the episode picker and jump straight into a season."
            } else {
                "Open the ranked source list and pick a stream without digging through utility clutter."
            }
        ))
        infoPanel.addView(viewFactory.spacer(18))

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
        infoPanel.addView(viewFactory.spacer(18))
        infoPanel.addView(viewFactory.panel(elevated = true).apply {
            addView(viewFactory.sectionTitle("Overview"))
            addView(viewFactory.spacer(10))
            addView(viewFactory.body(overviewPreview))
            if (details.genres.isNotEmpty()) {
                addView(viewFactory.spacer(14))
                addView(viewFactory.caption("Genres: ${details.genres.joinToString()}"))
            }
        })

        topPanel.addView(posterPanel)
        topPanel.addView(infoPanel)
        host.addView(topPanel)
        host.addView(viewFactory.spacer(18))

        val actionRow = LinearLayout(host.context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.START
        }
        val primaryAction = viewFactory.button(
            primaryActionLabel,
            primaryActionHandler,
            iconResId = if (details.mediaRef.mediaType == MediaType.SHOW) R.drawable.ic_nav_tv else R.drawable.ic_nav_play
        ).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val favoriteAction = viewFactory.button(
            if (isFavorite) "Remove Favorite" else "Add Favorite",
            onToggleFavorite,
            iconResId = R.drawable.ic_nav_favorite
        ).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = viewFactory.dp(12)
            }
        }
        actionRow.addView(primaryAction)
        actionRow.addView(favoriteAction)
        host.addView(actionRow)
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

        val availableSeasonNumbers = buildList {
            val reportedSeasonCount = details.seasonCount ?: 0
            if (reportedSeasonCount > 0) {
                addAll(1..reportedSeasonCount)
            }
            addAll(details.episodesBySeason.keys)
        }.distinct().sortedDescending()
        val fallbackSeasonCount = details.seasonCount ?: 3
        val knownSeasonCount = availableSeasonNumbers.maxOrNull() ?: fallbackSeasonCount
        val defaultSeason = availableSeasonNumbers.firstOrNull() ?: knownSeasonCount.coerceAtLeast(1)
        val selectedSeason = state.selectedSeasonNumber ?: defaultSeason
        val selectedEpisode = state.selectedEpisodeNumber ?: 1

        host.addView(viewFactory.artworkHero(
            title = details.mediaRef.title,
            subtitle = "Season $selectedSeason · Episode $selectedEpisode — quick episode picking without the long scroll.",
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
        (knownSeasonCount downTo 1).forEach { season ->
            val chip = viewFactory.chip("Season $season", selected = season == selectedSeason) {
                onSeasonSelected(season)
            }
            seasonStrip.addView(chip)
            if (season < knownSeasonCount) {
                seasonStrip.addView(View(activity).apply {
                    layoutParams = LinearLayout.LayoutParams(viewFactory.dp(10), 1)
                })
            }
            if (season == selectedSeason) {
                chip.post { onFirstFocusTarget(chip) }
            }
        }
        host.addView(HorizontalScrollView(activity).apply {
            addView(seasonStrip)
            post {
                val selectedIndex = (knownSeasonCount - selectedSeason).coerceAtLeast(0) * 2
                val selectedSeasonView = seasonStrip.getChildAt(selectedIndex)
                if (selectedSeasonView != null) {
                    smoothScrollTo((selectedSeasonView.left - viewFactory.dp(24)).coerceAtLeast(0), 0)
                }
            }
        })
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
                    view.scaleX = if (hasFocus) 1.025f else 1f
                    view.scaleY = if (hasFocus) 1.025f else 1f
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
            if (selectedSeason !in availableSeasonNumbers && (isSelected || (selectedEpisode !in episodeNumbers && index == 0))) {
                card.post { onFirstFocusTarget(card) }
            }
        }

        host.addView(viewFactory.button(
            "Find Sources for S${selectedSeason.toString().padStart(2, '0')}E${selectedEpisode.toString().padStart(2, '0')}",
            onFindSources,
            iconResId = R.drawable.ic_nav_play
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
        host.addView(viewFactory.caption("Start with a ready or direct stream, then drop to other options if needed."))
        host.addView(viewFactory.spacer(16))

        val sourceCount = state.selectedSources.size
        val summaryPanel = viewFactory.panel(elevated = true).apply {
            addView(viewFactory.sectionTitle("Best Path"))
            addView(viewFactory.spacer(10))
            addView(viewFactory.body(
                when {
                    sourceCount == 0 -> "Nothing surfaced yet."
                    state.selectedSources.firstOrNull()?.cacheStatus == CacheStatus.CACHED -> "A ready-to-play cached option is already at the top of the list."
                    state.selectedSources.firstOrNull()?.cacheStatus == CacheStatus.DIRECT -> "A direct link is leading this list, with other options below it."
                    else -> "No ready stream surfaced first, so you may need to inspect the fallback options."
                }
            ))
            if (!error.isNullOrBlank()) {
                addView(viewFactory.spacer(10))
                addView(viewFactory.caption("Lookup issue: $error"))
            }
        }
        host.addView(summaryPanel)
        diagnostics?.takeIf { it.isNotBlank() }?.let {
            host.addView(viewFactory.spacer(12))
            host.addView(viewFactory.panel(elevated = false).apply {
                addView(viewFactory.sectionTitle("Provider Notes"))
                addView(viewFactory.spacer(8))
                addView(viewFactory.caption(it))
            })
        }
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
        val selectedSourceId = state.selectedSource?.id
        val selectedSourceUrl = state.selectedSource?.url
        var restoredFocused = false

        grouped.forEach { (groupTitle, items) ->
            host.addView(viewFactory.sectionTitle(groupTitle))
            host.addView(viewFactory.spacer(12))
            items.take(10).forEachIndexed { index, source ->
                val isTopPick = source.cacheStatus == CacheStatus.CACHED && index == 0 && groupTitle == "Ready to Play"
                val card = focusableSourceCard(onClick = { onSourceSelected(source) }, elevated = source.cacheStatus == CacheStatus.CACHED).apply {
                    setPadding(viewFactory.dp(18), viewFactory.dp(14), viewFactory.dp(18), viewFactory.dp(14))
                }
                if (isTopPick) {
                    card.addView(viewFactory.sectionTitle("Top pick"))
                    card.addView(viewFactory.spacer(8))
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
                SourcePresentation.supportLabel(source).takeIf { it.isNotBlank() }?.let { support ->
                    card.addView(viewFactory.spacer(6))
                    card.addView(viewFactory.caption(support))
                }
                host.addView(card)
                host.addView(viewFactory.spacer(8))
                if (!restoredFocused && ((selectedSourceId != null && source.id == selectedSourceId) || (selectedSourceUrl != null && source.url == selectedSourceUrl))) {
                    restoredFocused = true
                    card.post { onFirstFocusTarget(card) }
                } else if (!restoredFocused && selectedSourceId == null && selectedSourceUrl == null && groupTitle == "Ready to Play" && index == 0) {
                    restoredFocused = true
                    card.post { onFirstFocusTarget(card) }
                }
            }
            host.addView(viewFactory.spacer(8))
        }
    }

    private fun focusableSourceCard(onClick: () -> Unit, elevated: Boolean = true): LinearLayout {
        return viewFactory.panel(elevated = elevated).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            isClickable = true
            alpha = 0.985f
            foreground = null
            setOnClickListener { onClick() }
            setOnFocusChangeListener { view, hasFocus ->
                view.scaleX = if (hasFocus) 1.025f else 1f
                view.scaleY = if (hasFocus) 1.025f else 1f
                view.alpha = if (hasFocus) 1f else 0.985f
                view.translationZ = if (hasFocus) viewFactory.dp(18).toFloat() else 0f
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

        (playerView.parent as? ViewGroup)?.removeView(playerView)
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
            val shouldShowStatusStrip = playbackState.isBuffering ||
                playbackStateLabel.equals("paused", ignoreCase = true) ||
                (playbackPositionMs in 1..15_000L)
            if (shouldShowStatusStrip) {
                val subtleOverlay = viewFactory.panel(elevated = true).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        FrameLayout.LayoutParams.WRAP_CONTENT,
                        Gravity.TOP or Gravity.START
                    ).also {
                        it.marginStart = viewFactory.dp(24)
                        it.topMargin = viewFactory.dp(24)
                    }
                    alpha = 0.92f
                    minimumWidth = viewFactory.dp(300)
                    addView(viewFactory.sectionTitle("Now Playing"))
                    addView(viewFactory.spacer(8))
                    addView(TextView(host.context).apply {
                        text = source.mediaRef.title
                        setTextColor(viewFactory.textPrimaryColor)
                        textSize = 20f
                        setTypeface(typeface, Typeface.BOLD)
                        maxLines = 2
                    })
                    addView(viewFactory.spacer(6))
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
            title = "Settings",
            subtitle = "Account status, updates, and source controls — still sharp, just less cold and toolish."
        ))
        host.addView(viewFactory.spacer())

        val accountPanel = viewFactory.panel(elevated = true).apply {
            addView(viewFactory.sectionTitle("Real-Debrid"))
            addView(viewFactory.spacer(8))
            addView(viewFactory.title(if (authState.isLinked) "Connected" else "Not Connected"))
            addView(viewFactory.spacer(8))
            addView(viewFactory.caption(
                if (authState.isLinked) {
                    buildString {
                        append("Linked")
                        authState.username?.takeIf { it.isNotBlank() }?.let { append(" as $it") }
                        append(". Cached playback is available.")
                    }
                } else {
                    "Link your account to unlock cached playback and better source resolution."
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
                addView(viewFactory.spacer(14))
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
                addView(viewFactory.button("Open Link Page", onClick = {
                    activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(buildAuthUrl(flow))))
                }, iconResId = R.drawable.ic_nav_link))
            }
        }
        host.addView(accountPanel)
        host.addView(viewFactory.spacer())

        val updatePanel = viewFactory.panel(elevated = false).apply {
            addView(viewFactory.sectionTitle("Updates"))
            addView(viewFactory.spacer(8))
            addView(viewFactory.title(updateSummary ?: "No update check yet"))
            addView(viewFactory.spacer(8))
            addView(viewFactory.caption("If install fails, it is usually a signature mismatch or a version code issue."))
        }
        host.addView(updatePanel)
        host.addView(viewFactory.spacer())

        val providersPanel = viewFactory.panel(elevated = false).apply {
            addView(viewFactory.sectionTitle("Sources"))
            addView(viewFactory.spacer(8))
            addView(viewFactory.title(providerSelectionLabel))
            addView(viewFactory.spacer(8))
            addView(viewFactory.caption(providerSummary ?: "No provider notes yet."))
            sourcePreferencesSummary?.takeIf { it.isNotBlank() }?.let {
                addView(viewFactory.spacer(10))
                addView(viewFactory.caption(it))
            }
            addView(viewFactory.spacer(12))
            addView(viewFactory.caption("Movie max: $movieMaxSizeLabel • TV max: $tvMaxSizeLabel"))
        }
        host.addView(providersPanel)
        host.addView(viewFactory.spacer())

        host.addView(viewFactory.sectionTitle("Account"))
        host.addView(viewFactory.spacer(10))
        val primaryButton = viewFactory.button(
            if (authState.isLinked) "Refresh Real-Debrid Link" else "Start Real-Debrid Link",
            onStartLink,
            iconResId = R.drawable.ic_nav_link
        )
        host.addView(primaryButton)
        primaryButton.post { onFirstFocusTarget(primaryButton) }
        host.addView(viewFactory.spacer(10))

        if (authState.isLinked) {
            host.addView(viewFactory.button("Reset Real-Debrid Auth", onResetAuth, iconResId = R.drawable.ic_nav_link))
            host.addView(viewFactory.spacer(16))
        }

        host.addView(viewFactory.sectionTitle("Maintenance"))
        host.addView(viewFactory.spacer(10))
        host.addView(viewFactory.button("Check for Updates", onCheckForUpdates, iconResId = R.drawable.ic_nav_download))
        host.addView(viewFactory.spacer(10))
        onOpenLatestUpdate?.let { openLatest ->
            host.addView(viewFactory.button("Open Latest APK", openLatest, iconResId = R.drawable.ic_nav_download))
            host.addView(viewFactory.spacer(10))
        }
        host.addView(viewFactory.button("Back to Browse", onBackToHome, iconResId = R.drawable.ic_nav_home))
        host.addView(viewFactory.spacer(16))

        host.addView(viewFactory.sectionTitle("Source Controls"))
        host.addView(viewFactory.spacer(10))
        host.addView(viewFactory.button("Movie Max Size ($movieMaxSizeLabel)", onConfigureMovieMaxSize, iconResId = R.drawable.ic_nav_movie))
        host.addView(viewFactory.spacer(10))
        host.addView(viewFactory.button("TV Max Size ($tvMaxSizeLabel)", onConfigureTvMaxSize, iconResId = R.drawable.ic_nav_tv))
        host.addView(viewFactory.spacer(10))
        host.addView(viewFactory.button("Choose Providers ($providerSelectionLabel)", onToggleProviders, iconResId = R.drawable.ic_nav_settings))
        host.addView(viewFactory.spacer(10))
        host.addView(viewFactory.button("Reset Source Preferences", onResetSourcePreferences, iconResId = R.drawable.ic_nav_settings))
        host.addView(viewFactory.spacer(16))

        host.addView(viewFactory.sectionTitle("Advanced"))
        host.addView(viewFactory.spacer(10))
        host.addView(viewFactory.button("Copy Debug Info", onCopyDebugInfo, iconResId = R.drawable.ic_nav_history))
    }
}
