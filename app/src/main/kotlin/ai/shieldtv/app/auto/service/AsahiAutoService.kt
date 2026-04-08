package ai.shieldtv.app.auto.service

import ai.shieldtv.app.auto.AutoFeature
import ai.shieldtv.app.auto.browse.AutoBrowseRepository
import ai.shieldtv.app.auto.browse.AutoMediaId
import ai.shieldtv.app.auto.model.AutoActionHint
import ai.shieldtv.app.auto.model.AutoBrowseNode
import ai.shieldtv.app.auto.model.AutoPlaybackResult
import ai.shieldtv.app.auto.playback.AutoPlaybackFacade
import ai.shieldtv.app.di.AppContainer
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaLibraryService.MediaLibrarySession.Callback
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.withTimeoutOrNull

class AsahiAutoService : MediaLibraryService() {
    private lateinit var player: ExoPlayer
    private lateinit var browseRepository: AutoBrowseRepository
    private lateinit var playbackFacade: AutoPlaybackFacade
    private var mediaLibrarySession: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        browseRepository = AutoFeature.createBrowseRepository(
            favoritesStore = AppContainer.favoritesStore,
            watchHistoryStore = AppContainer.watchHistoryStore,
            continueWatchingStore = AppContainer.continueWatchingStore,
            searchTitlesUseCase = AppContainer.searchTitlesUseCase
        )
        playbackFacade = AutoFeature.createPlaybackFacade(
            getRealDebridAuthStateUseCase = AppContainer.getRealDebridAuthStateUseCase,
            getTitleDetailsUseCase = AppContainer.getTitleDetailsUseCase,
            findSourcesUseCase = AppContainer.findSourcesUseCase,
            playbackMemoryStore = AppContainer.playbackMemoryStore,
            watchHistoryCoordinator = AppContainer.watchHistoryCoordinator,
            sourcePreferencesStore = AppContainer.sourcePreferencesStore,
            availableProviderIds = { AppContainer.availableProviderIds().toSet() }
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaLibrarySession ?: MediaLibrarySession.Builder(this, player, AutoLibraryCallback(browseRepository, playbackFacade)).build()
            .also { mediaLibrarySession = it }
    }

    override fun onDestroy() {
        mediaLibrarySession?.release()
        mediaLibrarySession = null
        player.release()
        super.onDestroy()
    }
}

private class AutoLibraryCallback(
    private val browseRepository: AutoBrowseRepository,
    private val playbackFacade: AutoPlaybackFacade
) : Callback {

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val rootItem = mediaItem(
            id = ROOT_ID,
            title = "Asahi",
            subtitle = "Playback-first Android Auto surface",
            isBrowsable = true,
            isPlayable = false
        )
        return Futures.immediateFuture(LibraryResult.ofItem(rootItem, params))
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val nodes = when {
            parentId == ROOT_ID -> runBlockingResult { browseRepository.root() }
            parentId.startsWith(SEARCH_PREFIX) -> {
                val query = parentId.removePrefix(SEARCH_PREFIX)
                runBlockingResult { browseRepository.search(query, mediaType = null) }
            }
            parentId.startsWith(COLLECTION_PREFIX) -> {
                val collectionId = parentId.removePrefix(COLLECTION_PREFIX)
                runBlockingResult { browseRepository.children(collectionId) }
            }
            else -> emptyList()
        }
        val pagedItems = nodes
            .drop((page * pageSize).coerceAtLeast(0))
            .take(pageSize.coerceAtLeast(0))
            .map(::mediaItem)
        return Futures.immediateFuture(
            LibraryResult.ofItemList(ImmutableList.copyOf(pagedItems), params)
        )
    }

    override fun onGetItem(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val parsed = AutoMediaId.parse(mediaId)
        val item = when (parsed) {
            is AutoMediaId.Collection -> mediaItem(
                id = parsed.rawValue,
                title = parsed.rawValue.substringAfter("collection:").replace('-', ' ').replaceFirstChar { it.uppercase() },
                subtitle = if (parsed.rawValue.contains("message:")) "Status" else "Auto collection",
                isBrowsable = !parsed.rawValue.contains("message:"),
                isPlayable = false
            )
            is AutoMediaId.Search -> mediaItem(
                id = parsed.rawValue,
                title = "Search",
                subtitle = parsed.rawValue.substringAfter(':', "Find movies and shows"),
                isBrowsable = parsed.rawValue != AutoMediaId.Search().rawValue,
                isPlayable = false
            )
            is AutoMediaId.Item -> mediaItem(
                id = parsed.rawValue,
                title = parsed.mediaRef.title,
                subtitle = parsed.mediaRef.year?.toString(),
                isBrowsable = false,
                isPlayable = true
            )
            null -> mediaItem(
                id = mediaId,
                title = "Unknown item",
                subtitle = "Unsupported Auto media id",
                isBrowsable = false,
                isPlayable = false
            )
        }
        return Futures.immediateFuture(LibraryResult.ofItem(item, null))
    }

    override fun onSearch(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        return Futures.immediateFuture(LibraryResult.ofVoid(params))
    }

    override fun onGetSearchResult(
        session: MediaLibrarySession,
        browser: MediaSession.ControllerInfo,
        query: String,
        page: Int,
        pageSize: Int,
        params: MediaLibraryService.LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val nodes = runBlockingResult { browseRepository.search(query, mediaType = null) }
        val pagedItems = nodes
            .drop((page * pageSize).coerceAtLeast(0))
            .take(pageSize.coerceAtLeast(0))
            .map(::mediaItem)
        return Futures.immediateFuture(
            LibraryResult.ofItemList(ImmutableList.copyOf(pagedItems), params)
        )
    }

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        val requestedItem = mediaItems.getOrNull(startIndex.coerceAtLeast(0)) ?: mediaItems.firstOrNull()
        val requestedMediaId = requestedItem?.mediaId
        val resolved = requestedMediaId?.let { mediaId ->
            when (val parsed = AutoMediaId.parse(mediaId)) {
                is AutoMediaId.Item -> runBlockingResult {
                    withTimeoutOrNull(8_000) {
                        when (parsed.action) {
                            AutoActionHint.PLAY_MOVIE -> playbackFacade.playMovie(parsed.mediaRef)
                            AutoActionHint.RESUME -> playbackFacade.resume(parsed.mediaRef)
                            AutoActionHint.PLAY_SHOW_DEFAULT -> playbackFacade.playShowDefault(parsed.mediaRef)
                            AutoActionHint.PLAY_EPISODE -> {
                                val seasonNumber = parsed.seasonNumber
                                val episodeNumber = parsed.episodeNumber
                                if (seasonNumber != null && episodeNumber != null) {
                                    playbackFacade.playEpisode(parsed.mediaRef, seasonNumber, episodeNumber)
                                } else {
                                    AutoPlaybackResult.Failed("Episode target was incomplete.")
                                }
                            }
                            else -> AutoPlaybackResult.Failed("Unsupported Auto action.")
                        }
                    } ?: AutoPlaybackResult.Failed("Auto playback timed out while loading sources.")
                }
                else -> AutoPlaybackResult.Failed("Unsupported Auto media selection.")
            }
        } ?: AutoPlaybackResult.Failed("No Auto media item was provided.")

        return when (resolved) {
            is AutoPlaybackResult.Ready -> {
                val playableItem = MediaItem.Builder()
                    .setMediaId(requestedMediaId ?: resolved.source.id)
                    .setUri(resolved.source.url)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(resolved.source.mediaRef.title)
                            .setSubtitle(resolved.source.displayName)
                            .setIsPlayable(true)
                            .build()
                    )
                    .build()
                Futures.immediateFuture(
                    MediaSession.MediaItemsWithStartPosition(listOf(playableItem), 0, startPositionMs)
                )
            }
            is AutoPlaybackResult.Blocked -> Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    listOf(buildStatusItem(requestedMediaId, requestedItem, resolved.userMessage)),
                    0,
                    0L
                )
            )
            is AutoPlaybackResult.Failed -> Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    listOf(buildStatusItem(requestedMediaId, requestedItem, resolved.userMessage)),
                    0,
                    0L
                )
            )
        }
    }

    private fun buildStatusItem(
        requestedMediaId: String?,
        requestedItem: MediaItem?,
        message: String
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId(requestedMediaId ?: "auto-status")
            .setUri("https://invalid.local/auto-status")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(requestedItem?.mediaMetadata?.title ?: "Asahi")
                    .setSubtitle(message)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
    }

    override fun onCustomCommand(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        customCommand: SessionCommand,
        args: Bundle
    ): ListenableFuture<SessionResult> {
        return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
    }

    private fun mediaItem(node: AutoBrowseNode): MediaItem {
        return mediaItem(
            id = node.id,
            title = node.title,
            subtitle = node.subtitle,
            isBrowsable = node.browsable,
            isPlayable = node.playable,
            artworkUri = node.artworkUrl
        )
    }

    private fun mediaItem(
        id: String,
        title: String,
        subtitle: String?,
        isBrowsable: Boolean,
        isPlayable: Boolean,
        artworkUri: String? = null
    ): MediaItem {
        return MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setIsBrowsable(isBrowsable)
                    .setIsPlayable(isPlayable)
                    .apply {
                        artworkUri?.let { setArtworkUri(android.net.Uri.parse(it)) }
                    }
                    .build()
            )
            .build()
    }

    private fun <T> runBlockingResult(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }

    companion object {
        private const val ROOT_ID = "asahi-auto-root"
        private const val COLLECTION_PREFIX = "collection:"
        private const val SEARCH_PREFIX = "search:"
    }
}
