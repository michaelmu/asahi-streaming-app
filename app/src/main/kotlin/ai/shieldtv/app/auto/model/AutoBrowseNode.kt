package ai.shieldtv.app.auto.model

import ai.shieldtv.app.core.model.media.MediaRef

data class AutoBrowseNode(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val browsable: Boolean,
    val playable: Boolean,
    val mediaRef: MediaRef? = null,
    val artworkUrl: String? = null,
    val actionHint: AutoActionHint? = null
)
