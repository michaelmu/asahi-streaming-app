package ai.shieldtv.app.auto

import ai.shieldtv.app.auto.browse.AutoBrowseRepository
import ai.shieldtv.app.auto.browse.DefaultAutoBrowseRepository
import ai.shieldtv.app.continuewatching.ContinueWatchingStore
import ai.shieldtv.app.favorites.FavoritesStore
import ai.shieldtv.app.history.WatchHistoryStore

object AutoFeature {
    fun createBrowseRepository(
        favoritesStore: FavoritesStore,
        watchHistoryStore: WatchHistoryStore,
        continueWatchingStore: ContinueWatchingStore
    ): AutoBrowseRepository {
        return DefaultAutoBrowseRepository(
            favoritesStore = favoritesStore,
            watchHistoryStore = watchHistoryStore,
            continueWatchingStore = continueWatchingStore
        )
    }
}
