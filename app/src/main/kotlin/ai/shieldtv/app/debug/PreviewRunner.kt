package ai.shieldtv.app.debug

import ai.shieldtv.app.ui.DebugAppPreviewBuilder

object PreviewRunner {
    @JvmStatic
    fun main(args: Array<String>) {
        println(DebugAppPreviewBuilder().build())
    }
}
