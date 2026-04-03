package ai.shieldtv.app.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

enum class ModalDefaultAction {
    PRIMARY,
    SECONDARY,
    TERTIARY
}

class OverlayPopup(
    private val context: Context,
    private val viewFactory: ScreenViewFactory
) {
    fun build(
        title: String,
        message: String,
        primaryLabel: String,
        onPrimary: () -> Unit,
        secondaryLabel: String? = null,
        onSecondary: (() -> Unit)? = null,
        tertiaryLabel: String? = null,
        onTertiary: (() -> Unit)? = null,
        dismissOnBack: Boolean = true,
        defaultAction: ModalDefaultAction = ModalDefaultAction.PRIMARY,
        customContent: View? = null
    ): View {
        val root = FrameLayout(context)
        root.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        root.setBackgroundColor(viewFactory.backgroundColor and 0x66FFFFFF)

        val card = viewFactory.panel(elevated = true)
        card.layoutParams = FrameLayout.LayoutParams(
            viewFactory.dp(760),
            FrameLayout.LayoutParams.WRAP_CONTENT,
            Gravity.CENTER
        )
        card.setPadding(viewFactory.dp(28), viewFactory.dp(28), viewFactory.dp(28), viewFactory.dp(28))

        val titleView = TextView(context)
        titleView.text = title
        titleView.setTextColor(viewFactory.textPrimaryColor)
        titleView.textSize = 28f
        titleView.setTypeface(titleView.typeface, Typeface.BOLD)
        card.addView(titleView)

        card.addView(viewFactory.spacer(12))

        val messageView = TextView(context)
        messageView.text = message
        messageView.setTextColor(viewFactory.textSecondaryColor)
        messageView.textSize = 17f
        messageView.setLineSpacing(viewFactory.dp(3).toFloat(), 1f)
        card.addView(messageView)

        customContent?.let {
            card.addView(viewFactory.spacer(18))
            card.addView(it)
        }

        card.addView(viewFactory.spacer(20))

        val buttonRow = LinearLayout(context)
        buttonRow.orientation = LinearLayout.HORIZONTAL

        val primary = viewFactory.button(primaryLabel, onPrimary)
        primary.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        buttonRow.addView(primary)

        var secondaryView: View? = null
        if (secondaryLabel != null && onSecondary != null) {
            val secondary = viewFactory.button(secondaryLabel, onSecondary)
            secondary.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = viewFactory.dp(12)
            }
            buttonRow.addView(secondary)
            secondaryView = secondary
        }

        var tertiaryView: View? = null
        if (tertiaryLabel != null && onTertiary != null) {
            val tertiary = viewFactory.button(tertiaryLabel, onTertiary)
            tertiary.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                it.marginStart = viewFactory.dp(12)
            }
            buttonRow.addView(tertiary)
            tertiaryView = tertiary
        }

        card.addView(buttonRow)
        val defaultFocusView = when (defaultAction) {
            ModalDefaultAction.PRIMARY -> primary
            ModalDefaultAction.SECONDARY -> secondaryView ?: primary
            ModalDefaultAction.TERTIARY -> tertiaryView ?: secondaryView ?: primary
        }
        defaultFocusView.post { defaultFocusView.requestFocus() }

        root.addView(card)
        root.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                when {
                    dismissOnBack && onSecondary != null -> {
                        onSecondary()
                        true
                    }
                    dismissOnBack -> true
                    else -> true
                }
            } else {
                false
            }
        }
        root.isFocusable = true
        root.isFocusableInTouchMode = true
        return root
    }
}
