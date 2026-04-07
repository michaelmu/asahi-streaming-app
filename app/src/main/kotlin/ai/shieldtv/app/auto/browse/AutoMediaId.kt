package ai.shieldtv.app.auto.browse

import ai.shieldtv.app.auto.model.AutoActionHint
import ai.shieldtv.app.core.model.media.MediaIds
import ai.shieldtv.app.core.model.media.MediaRef
import ai.shieldtv.app.core.model.media.MediaType

sealed interface AutoMediaId {
    val rawValue: String

    data class Collection(private val key: String) : AutoMediaId {
        override val rawValue: String = "collection:$key"
    }

    data class Search(private val query: String? = null) : AutoMediaId {
        override val rawValue: String = if (query.isNullOrBlank()) {
            "search"
        } else {
            "search:${encodeSegment(query)}"
        }
    }

    data class Item(
        val action: AutoActionHint,
        val mediaRef: MediaRef,
        val seasonNumber: Int? = null,
        val episodeNumber: Int? = null
    ) : AutoMediaId {
        override val rawValue: String = buildString {
            append("item:")
            append(action.name.lowercase())
            append(':')
            append(mediaRef.mediaType.name.lowercase())
            append(':')
            append(encodeSegment(mediaIdentity(mediaRef)))
            if (seasonNumber != null) {
                append(":s")
                append(seasonNumber)
            }
            if (episodeNumber != null) {
                append(":e")
                append(episodeNumber)
            }
        }
    }

    companion object {
        private val supportedActions = AutoActionHint.entries.associateBy { it.name.lowercase() }
        private val supportedTypes = MediaType.entries.associateBy { it.name.lowercase() }

        fun rootCollections(): List<Collection> = listOf(
            Collection("continue-watching"),
            Collection("favorites"),
            Collection("recent"),
            Collection("movies"),
            Collection("tv-shows"),
            Collection("search")
        )

        fun parse(rawValue: String): AutoMediaId? {
            return when {
                rawValue == "search" -> Search()
                rawValue.startsWith("search:") -> Search(decodeSegment(rawValue.substringAfter("search:")))
                rawValue.startsWith("collection:") -> Collection(rawValue.substringAfter("collection:"))
                rawValue.startsWith("item:") -> parseItem(rawValue)
                else -> null
            }
        }

        private fun parseItem(rawValue: String): Item? {
            val parts = rawValue.split(':')
            if (parts.size < 4) return null
            val action = supportedActions[parts[1]] ?: return null
            val mediaType = supportedTypes[parts[2]] ?: return null
            val identity = decodeSegment(parts[3])
            val mediaRef = parseMediaRef(mediaType, identity) ?: return null
            val seasonNumber = parts.getOrNull(4)?.removePrefix("s")?.toIntOrNull()
            val episodeNumber = parts.getOrNull(5)?.removePrefix("e")?.toIntOrNull()
            return Item(
                action = action,
                mediaRef = mediaRef,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber
            )
        }

        fun mediaIdentity(mediaRef: MediaRef): String {
            return listOfNotNull(
                mediaRef.ids.tmdbId?.let { "tmdb=$it" },
                mediaRef.ids.imdbId?.let { "imdb=$it" },
                mediaRef.ids.tvdbId?.let { "tvdb=$it" },
                mediaRef.title.takeIf { it.isNotBlank() }?.let { "title=$it" },
                mediaRef.year?.let { "year=$it" }
            ).joinToString("|")
        }

        private fun parseMediaRef(mediaType: MediaType, identity: String): MediaRef? {
            if (identity.isBlank()) return null
            val fields = identity.split('|')
                .mapNotNull { part ->
                    val index = part.indexOf('=')
                    if (index <= 0) null else part.substring(0, index) to part.substring(index + 1)
                }
                .toMap()
            val title = fields["title"] ?: return null
            return MediaRef(
                mediaType = mediaType,
                ids = MediaIds(
                    tmdbId = fields["tmdb"],
                    imdbId = fields["imdb"],
                    tvdbId = fields["tvdb"]
                ),
                title = title,
                year = fields["year"]?.toIntOrNull()
            )
        }

        private fun encodeSegment(value: String): String =
            value.replace("%", "%25").replace(":", "%3A")

        private fun decodeSegment(value: String): String =
            value.replace("%3A", ":").replace("%25", "%")
    }
}
