package ai.shieldtv.app.ui

import android.content.Context
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import coil.load
import ai.shieldtv.app.R

class ScreenViewFactory(
    private val context: Context
) {
    val backgroundColor: Int get() = color(R.color.asahi_bg)
    val panelColor: Int get() = color(R.color.asahi_panel)
    val panelElevatedColor: Int get() = color(R.color.asahi_panel_elevated)
    val accentColor: Int get() = color(R.color.asahi_accent)
    val accentAltColor: Int get() = color(R.color.asahi_accent_alt)
    val warningColor: Int get() = color(R.color.asahi_warning)
    val errorColor: Int get() = color(R.color.asahi_error)
    val textPrimaryColor: Int get() = color(R.color.asahi_text_primary)
    val textSecondaryColor: Int get() = color(R.color.asahi_text_secondary)
    val textMutedColor: Int get() = color(R.color.asahi_text_muted)

    fun pageTitle(text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(textPrimaryColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 32f)
        setTypeface(typeface, Typeface.BOLD)
    }

    fun railTitle(text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(textPrimaryColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        setTypeface(typeface, Typeface.BOLD)
    }

    fun sectionTitle(text: String): TextView = TextView(context).apply {
        this.text = text.uppercase()
        setTextColor(textSecondaryColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f)
        setTypeface(typeface, Typeface.BOLD)
        letterSpacing = 0.18f
    }

    fun title(text: String): View = TextView(context).apply {
        this.text = text
        setTextColor(textPrimaryColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        setTypeface(typeface, Typeface.BOLD)
    }

    fun body(text: String): View = TextView(context).apply {
        this.text = text
        setTextColor(textSecondaryColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        setLineSpacing(dp(4).toFloat(), 1.1f)
    }

    fun caption(text: String): View = TextView(context).apply {
        this.text = text
        setTextColor(textMutedColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
    }

    fun heroCard(title: String, subtitle: String): LinearLayout {
        return panel(vertical = true, elevated = true).apply {
            setPadding(dp(24), dp(22), dp(24), dp(22))
            addView(sectionTitle("Now Building"))
            addView(spacer(10))
            addView(pageTitle(title))
            addView(spacer(10))
            addView(body(subtitle))
        }
    }

    fun artworkHero(
        title: String,
        subtitle: String,
        imageUrl: String? = null,
        imageHeightDp: Int = 260
    ): FrameLayout {
        return FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(imageHeightDp)
            )
            background = ContextCompat.getDrawable(context, R.drawable.asahi_panel_elevated_bg)

            val image = ImageView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                scaleType = ImageView.ScaleType.CENTER_CROP
                alpha = 0.42f
                imageUrl?.takeIf { it.isNotBlank() }?.let { load(it) }
            }

            val scrim = View(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(color(R.color.asahi_scrim))
            }

            val content = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.BOTTOM
                )
                setPadding(dp(24), dp(24), dp(24), dp(24))
                addView(sectionTitle("Featured"))
                addView(spacer(10))
                addView(pageTitle(title))
                addView(spacer(10))
                addView(body(subtitle))
            }

            addView(image)
            addView(scrim)
            addView(content)
        }
    }

    fun panel(vertical: Boolean = true, elevated: Boolean = false): LinearLayout {
        return LinearLayout(context).apply {
            orientation = if (vertical) LinearLayout.VERTICAL else LinearLayout.HORIZONTAL
            background = ContextCompat.getDrawable(
                context,
                if (elevated) R.drawable.asahi_panel_elevated_bg else R.drawable.asahi_panel_bg
            )
            setPadding(dp(20), dp(20), dp(20), dp(20))
        }
    }

    fun button(text: String, onClick: () -> Unit): View {
        return Button(context).apply {
            this.text = text
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(context, R.drawable.asahi_button_bg)
            setTextColor(textPrimaryColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            isAllCaps = false
            minHeight = dp(56)
            setPadding(dp(20), dp(16), dp(20), dp(16))
            elevation = 0f
            stateListAnimator = null
            isFocusable = true
            isFocusableInTouchMode = true
            alpha = 0.96f
            setOnClickListener { onClick() }
            setOnFocusChangeListener { view, hasFocus ->
                view.scaleX = if (hasFocus) 1.03f else 1f
                view.scaleY = if (hasFocus) 1.03f else 1f
                view.alpha = if (hasFocus) 1f else 0.96f
            }
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    performClick()
                    true
                } else {
                    false
                }
            }
        }
    }

    fun chip(text: String, selected: Boolean = false, onClick: () -> Unit): Button {
        return Button(context).apply {
            this.text = text
            isAllCaps = false
            gravity = Gravity.CENTER
            background = ContextCompat.getDrawable(context, R.drawable.asahi_chip_bg)
            setTextColor(if (selected) textPrimaryColor else textSecondaryColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            minHeight = dp(42)
            minimumHeight = dp(42)
            setPadding(dp(18), dp(10), dp(18), dp(10))
            isSelected = selected
            elevation = 0f
            stateListAnimator = null
            isFocusable = true
            isFocusableInTouchMode = true
            setOnClickListener { onClick() }
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)) {
                    performClick()
                    true
                } else {
                    false
                }
            }
        }
    }

    fun input(hint: String, initialValue: String = ""): EditText {
        return EditText(context).apply {
            setText(initialValue)
            this.hint = hint
            setHintTextColor(textMutedColor)
            setTextColor(textPrimaryColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            background = ContextCompat.getDrawable(context, R.drawable.asahi_input_bg)
            setPadding(dp(20), dp(18), dp(20), dp(18))
            isSingleLine = true
        }
    }

    fun statPill(label: String, value: String, tone: StatTone = StatTone.NORMAL): LinearLayout {
        val valueColor = when (tone) {
            StatTone.NORMAL -> textPrimaryColor
            StatTone.ACCENT -> accentColor
            StatTone.SUCCESS -> accentAltColor
            StatTone.WARNING -> warningColor
            StatTone.ERROR -> errorColor
        }
        return panel(vertical = true, elevated = false).apply {
            minimumWidth = dp(120)
            addView(sectionTitle(label))
            addView(spacer(8))
            addView(TextView(context).apply {
                text = value
                setTextColor(valueColor)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                setTypeface(typeface, Typeface.BOLD)
            })
        }
    }

    fun progressBar(progressPercent: Int): ProgressBar {
        return ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = progressPercent.coerceIn(0, 100)
            progressDrawable = ContextCompat.getDrawable(context, R.drawable.asahi_progress_fill)
            background = ContextCompat.getDrawable(context, R.drawable.asahi_progress_bg)
            minimumHeight = dp(8)
            minHeight = dp(8)
        }
    }

    fun spacer(heightDp: Int = 18): View = View(context).apply {
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(heightDp)
        )
    }

    fun dp(value: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        value.toFloat(),
        context.resources.displayMetrics
    ).toInt()

    private fun color(resId: Int): Int = ContextCompat.getColor(context, resId)
}

enum class StatTone {
    NORMAL,
    ACCENT,
    SUCCESS,
    WARNING,
    ERROR
}
