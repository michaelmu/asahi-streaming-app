package ai.shieldtv.app.auto.service

import ai.shieldtv.app.auto.AutoFeature
import ai.shieldtv.app.auto.browse.AutoBrowseRepository
import ai.shieldtv.app.auto.browse.AutoMediaId
import ai.shieldtv.app.auto.model.AutoBrowseNode
import ai.shieldtv.app.di.AppContainer
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaLibraryService.MediaLibrarySession.Callback
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.exoplayer.ExoPlayer
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

class AsahiAutoService : MediaLibraryService() {
    private lateinit var player: ExoPlayer
    private lateinit var browseRepository: AutoBrowseRepository
    private var mediaLibrarySession: MediaLibrarySession? = null

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        browseRepository = AutoFeature.createBrowseRepository(
            favoritesStore = AppContainer.favoritesStore,
            watchHistoryStore = AppContainer.watchHistoryStore,
            continueWatchingStore = AppContainer.continueWatchingStore
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaLibrarySession {
        return mediaLibrarySession ?: MediaLibrarySession.Builder(this, player, AutoLibraryCallback(browseRepository)).build()
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
    private val browseRepository: AutoBrowseRepository
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
        val nodes = when (parentId) {
            ROOT_ID -> runBlockingResult { browseRepository.root() }
            AutoMediaId.Collection("continue-watching").rawValue -> runBlockingResult { browseRepository.continueWatching() }
            AutoMediaId.Collection("favorites").rawValue -> runBlockingResult { browseRepository.favorites(mediaType = null) }
            AutoMediaId.Collection("recent").rawValue -> runBlockingResult { browseRepository.recent(mediaType = null) }
            AutoMediaId.Collection("movies").rawValue,
            AutoMediaId.Collection("tv-shows").rawValue,
            AutoMediaId.Search().rawValue -> emptyList()
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
                subtitle = "Auto collection",
                isBrowsable = true,
                isPlayable = false
            )
            is AutoMediaId.Search -> mediaItem(
                id = parsed.rawValue,
                title = "Search",
                subtitle = parsed.rawValue.substringAfter(':', "Search"),
                isBrowsable = false,
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

    override fun onSetMediaItems(
        mediaSession: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: MutableList<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
        return Futures.immediateFuture(
            MediaSession.MediaItemsWithStartPosition(mediaItems, startIndex, startPositionMs)
        )
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
    }
}
