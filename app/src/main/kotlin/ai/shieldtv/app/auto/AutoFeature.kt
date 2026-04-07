package ai.shieldtv.app.auto

import ai.shieldtv.app.auto.browse.AutoBrowseRepository
import ai.shieldtv.app.auto.browse.DefaultAutoBrowseRepository
import ai.shieldtv.app.auto.playback.AutoPlaybackFacade
import ai.shieldtv.app.auto.playback.DefaultAutoPlaybackFacade
import ai.shieldtv.app.auto.playback.DefaultAutoShowProgressResolver
import ai.shieldtv.app.auto.playback.DefaultAutoSourceSelector
import ai.shieldtv.app.continuewatching.ContinueWatchingStore
import ai.shieldtv.app.domain.usecase.auth.GetRealDebridAuthStateUseCase
import ai.shieldtv.app.domain.usecase.details.GetTitleDetailsUseCase
import ai.shieldtv.app.domain.usecase.search.SearchTitlesUseCase
import ai.shieldtv.app.domain.usecase.sources.FindSourcesUseCase
import ai.shieldtv.app.favorites.FavoritesStore
import ai.shieldtv.app.history.WatchHistoryCoordinator
import ai.shieldtv.app.history.WatchHistoryStore
import ai.shieldtv.app.playback.PlaybackMemoryStore
import ai.shieldtv.app.settings.SourcePreferencesStore

object AutoFeature {
    fun createBrowseRepository(
        favoritesStore: FavoritesStore,
        watchHistoryStore: WatchHistoryStore,
        continueWatchingStore: ContinueWatchingStore,
        searchTitlesUseCase: SearchTitlesUseCase
    ): AutoBrowseRepository {
        return DefaultAutoBrowseRepository(
            favoritesStore = favoritesStore,
            watchHistoryStore = watchHistoryStore,
            continueWatchingStore = continueWatchingStore,
            searchTitlesUseCase = searchTitlesUseCase
        )
    }

    fun createPlaybackFacade(
        getRealDebridAuthStateUseCase: GetRealDebridAuthStateUseCase,
        getTitleDetailsUseCase: GetTitleDetailsUseCase,
        findSourcesUseCase: FindSourcesUseCase,
        playbackMemoryStore: PlaybackMemoryStore,
        watchHistoryCoordinator: WatchHistoryCoordinator,
        sourcePreferencesStore: SourcePreferencesStore,
        availableProviderIds: () -> Set<String>
    ): AutoPlaybackFacade {
        return DefaultAutoPlaybackFacade(
            getRealDebridAuthStateUseCase = getRealDebridAuthStateUseCase,
            getTitleDetailsUseCase = getTitleDetailsUseCase,
            findSourcesUseCase = findSourcesUseCase,
            sourceSelector = DefaultAutoSourceSelector(),
            showProgressResolver = DefaultAutoShowProgressResolver(),
            playbackMemoryStore = playbackMemoryStore,
            watchedEpisodeKeys = watchHistoryCoordinator::watchedEpisodeKeys,
            sourcePreferencesStore = sourcePreferencesStore,
            availableProviderIds = availableProviderIds
        )
    }
}
