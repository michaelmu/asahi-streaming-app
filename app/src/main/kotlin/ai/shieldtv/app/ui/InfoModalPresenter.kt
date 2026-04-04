package ai.shieldtv.app.ui

import android.view.View

data class InfoModalSpec(
    val title: String,
    val message: String,
    val primaryLabel: String = "OK",
    val onPrimary: (() -> Unit)? = null,
    val secondaryLabel: String? = null,
    val onSecondary: (() -> Unit)? = null,
    val tertiaryLabel: String? = null,
    val onTertiary: (() -> Unit)? = null,
    val dismissOnBack: Boolean = true,
    val defaultAction: ModalDefaultAction = ModalDefaultAction.PRIMARY,
    val customContent: View? = null
)

class InfoModalPresenter(
    private val overlayPopup: OverlayPopup,
    private val modalHost: ModalHost,
    private val dismissModal: () -> Unit
) {
    fun show(spec: InfoModalSpec) {
        dismissModal()
        val view = overlayPopup.build(
            title = spec.title,
            message = spec.message,
            primaryLabel = spec.primaryLabel,
            onPrimary = {
                dismissModal()
                spec.onPrimary?.invoke()
            },
            secondaryLabel = spec.secondaryLabel,
            onSecondary = spec.onSecondary?.let { action ->
                {
                    dismissModal()
                    action()
                }
            },
            tertiaryLabel = spec.tertiaryLabel,
            onTertiary = spec.onTertiary?.let { action ->
                {
                    dismissModal()
                    action()
                }
            },
            dismissOnBack = spec.dismissOnBack,
            defaultAction = spec.defaultAction,
            customContent = spec.customContent
        )
        modalHost.show(view)
    }
}
