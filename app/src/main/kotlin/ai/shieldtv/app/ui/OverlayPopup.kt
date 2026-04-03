package ai.shieldtv.app.ui

import android.content.Context
import android.graphics.Typeface
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

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
        onSecondary: (() -> Unit)? = null
    ): View {
        return FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(viewFactory.backgroundColor and 0x66FFFFFF)

            val card = viewFactory.panel(elevated = true).apply {
                layoutParams = FrameLayout.LayoutParams(
                    viewFactory.dp(760),
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER
                )
                setPadding(viewFactory.dp(28), viewFactory.dp(28), viewFactory.dp(28), viewFactory.dp(28))
                addView(TextView(context).apply {
                    text = title
                    setTextColor(viewFactory.textPrimaryColor)
                    textSize = 28f
                    setTypeface(typeface, Typeface.BOLD)
                })
                addView(viewFactory.spacer(12))
                addView(TextView(context).apply {
                    text = message
                    setTextColor(viewFactory.textSecondaryColor)
                    textSize = 17f
                    lineSpacingExtra = viewFactory.dp(3).toFloat()
                })
                addView(viewFactory.spacer(20))
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                }
                val primary = viewFactory.button(primaryLabel, onPrimary).apply {
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                row.addView(primary)
                if (secondaryLabel != null && onSecondary != null) {
                    row.addView(viewFactory.button(secondaryLabel, onSecondary).apply {
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).also {
                            it.marginStart = viewFactory.dp(12)
                        }
                    })
                }
                addView(row)
                primary.post { primary.requestFocus() }
            }

            addView(card)
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK && onSecondary != null) {
                    onSecondary()
                    true
                } else {
                    false
                }
            }
            isFocusable = true
            isFocusableInTouchMode = true
        }
    }
}
