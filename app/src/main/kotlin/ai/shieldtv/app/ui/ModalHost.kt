package ai.shieldtv.app.ui

import android.view.View
import android.widget.FrameLayout

class ModalHost(
    private val overlayHost: FrameLayout,
    private val onOverlayVisibilityChanged: (Boolean) -> Unit = {}
) {
    private var activeModalView: View? = null

    fun currentView(): View? = activeModalView

    fun show(view: View) {
        dismiss()
        activeModalView = view
        overlayHost.addView(view)
        onOverlayVisibilityChanged(true)
        view.requestFocus()
    }

    fun dismiss() {
        activeModalView?.let { overlayHost.removeView(it) }
        activeModalView = null
        onOverlayVisibilityChanged(false)
    }
}
