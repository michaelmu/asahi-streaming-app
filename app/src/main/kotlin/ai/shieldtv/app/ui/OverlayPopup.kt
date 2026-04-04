package ai.shieldtv.app.ui

import android.content.Context
import android.graphics.Typeface
import android.view.FocusFinder
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
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
        val root = FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(viewFactory.backgroundColor and 0x66FFFFFF)
            isFocusable = true
            isFocusableInTouchMode = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        val card = viewFactory.panel(elevated = true).apply {
            layoutParams = FrameLayout.LayoutParams(
                viewFactory.dp(760),
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            setPadding(viewFactory.dp(28), viewFactory.dp(28), viewFactory.dp(28), viewFactory.dp(28))
            isFocusable = true
            isFocusableInTouchMode = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        val titleView = TextView(context).apply {
            text = title
            setTextColor(viewFactory.textPrimaryColor)
            textSize = 28f
            setTypeface(typeface, Typeface.BOLD)
        }
        card.addView(titleView)

        card.addView(viewFactory.spacer(12))

        val messageView = TextView(context).apply {
            text = message
            setTextColor(viewFactory.textSecondaryColor)
            textSize = 17f
            setLineSpacing(viewFactory.dp(3).toFloat(), 1f)
        }
        card.addView(messageView)

        customContent?.let {
            card.addView(viewFactory.spacer(18))
            card.addView(it)
        }

        card.addView(viewFactory.spacer(20))

        val buttonRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            isFocusable = false
            isFocusableInTouchMode = false
        }

        val focusableButtons = mutableListOf<View>()

        val primary = viewFactory.button(primaryLabel, onPrimary).apply {
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        buttonRow.addView(primary)
        focusableButtons += primary

        var secondaryView: View? = null
        if (secondaryLabel != null && onSecondary != null) {
            val secondary = viewFactory.button(secondaryLabel, onSecondary).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginStart = viewFactory.dp(12)
                }
            }
            buttonRow.addView(secondary)
            secondaryView = secondary
            focusableButtons += secondary
        }

        var tertiaryView: View? = null
        if (tertiaryLabel != null && onTertiary != null) {
            val tertiary = viewFactory.button(tertiaryLabel, onTertiary).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginStart = viewFactory.dp(12)
                }
            }
            buttonRow.addView(tertiary)
            tertiaryView = tertiary
            focusableButtons += tertiary
        }

        trapHorizontalFocus(focusableButtons)

        card.addView(buttonRow)
        val defaultFocusView = when (defaultAction) {
            ModalDefaultAction.PRIMARY -> primary
            ModalDefaultAction.SECONDARY -> secondaryView ?: primary
            ModalDefaultAction.TERTIARY -> tertiaryView ?: secondaryView ?: primary
        }

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

        root.post {
            defaultFocusView.requestFocus()
            root.requestFocus()
            defaultFocusView.requestFocus()
        }

        root.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus && !card.hasFocus() && focusableButtons.none { it.hasFocus() }) {
                defaultFocusView.requestFocus()
            }
        }

        card.setOnKeyListener { _, keyCode, event ->
            if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false
            val currentFocus = focusableButtons.firstOrNull { it.hasFocus() } ?: return@setOnKeyListener false
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT,
                KeyEvent.KEYCODE_DPAD_RIGHT,
                KeyEvent.KEYCODE_DPAD_UP,
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    val next = FocusFinder.getInstance().findNextFocus(card, currentFocus, keyDirectionFor(keyCode))
                    if (next == null || !isDescendantOf(card, next)) {
                        currentFocus.requestFocus()
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }

        return root
    }

    private fun trapHorizontalFocus(buttons: List<View>) {
        buttons.forEachIndexed { index, button ->
            button.nextFocusLeftId = buttons.getOrNull((index - 1).coerceAtLeast(0))?.id ?: button.id
            button.nextFocusRightId = buttons.getOrNull((index + 1).coerceAtMost(buttons.lastIndex))?.id ?: button.id
            button.nextFocusUpId = button.id
            button.nextFocusDownId = button.id
        }
    }

    private fun keyDirectionFor(keyCode: Int): Int = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_LEFT -> View.FOCUS_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> View.FOCUS_RIGHT
        KeyEvent.KEYCODE_DPAD_UP -> View.FOCUS_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> View.FOCUS_DOWN
        else -> View.FOCUS_FORWARD
    }

    private fun isDescendantOf(parent: View, child: View): Boolean {
        var current: View? = child
        while (current != null) {
            if (current == parent) return true
            current = (current.parent as? View)
        }
        return false
    }
}
