package ai.shieldtv.app.ui

import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TextView

class ScreenViewFactory(
    private val context: Context
) {
    fun title(text: String): View {
        return TextView(context).apply {
            this.text = text
            textSize = 24f
        }
    }

    fun body(text: String): View {
        return TextView(context).apply {
            this.text = text
            textSize = 16f
        }
    }

    fun button(text: String, onClick: () -> Unit): View {
        return Button(context).apply {
            this.text = text
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            setPadding(24, 20, 24, 20)
            setOnClickListener { onClick() }
            setOnFocusChangeListener { _, hasFocus ->
                alpha = if (hasFocus) 1.0f else 0.88f
                setTextColor(if (hasFocus) Color.WHITE else Color.parseColor("#F2F2F2"))
            }
        }
    }

    fun spacer(): View = TextView(context).apply { text = "" }
}
