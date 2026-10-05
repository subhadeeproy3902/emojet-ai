package io.github.subhadeeproy3902.emojet

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

/**
 * EmoJet design system. One accent (glossy yellow) used only for the primary action, the current
 * step and the emoji strip; everything else is warm paper, white surfaces, ink and hairlines.
 * Type: Syne (headings) + DM Sans (everything else). Spacing on an 8 dp grid.
 */
object Theme {
    // colour tokens
    const val PAPER = 0xFFFAF7F0.toInt()
    const val SURFACE = 0xFFFFFFFF.toInt()
    const val INK = 0xFF17150F.toInt()
    const val INK2 = 0xFF6F685A.toInt()
    const val INK3 = 0xFFA39C8C.toInt()
    const val LINE = 0xFFECE5D6.toInt()
    const val ACCENT = 0xFFFFD23F.toInt()
    const val ACCENT_DEEP = 0xFFF5B800.toInt()
    const val ACCENT_EDGE = 0xFFD9A300.toInt()
    const val ACCENT_SOFT = 0xFFFFF4CC.toInt()
    const val OK = 0xFF1F9D55.toInt()

    // radii (dp)
    const val R_CARD = 20
    const val R_INPUT = 16
    const val R_PILL = 26

    lateinit var display: Typeface; private set     // Syne 700
    lateinit var heading: Typeface; private set     // Syne 600
    lateinit var body: Typeface; private set        // DM Sans 400
    lateinit var medium: Typeface; private set      // DM Sans 500
    lateinit var semibold: Typeface; private set    // DM Sans 600

    fun init(c: Context) {
        if (::display.isInitialized) return
        fun font(file: String, axes: String) = runCatching {
            Typeface.Builder(c.assets, "fonts/$file").setFontVariationSettings(axes).build()
        }.getOrDefault(Typeface.DEFAULT)
        display = font("Syne.ttf", "'wght' 700")
        heading = font("Syne.ttf", "'wght' 600")
        body = font("DMSans.ttf", "'wght' 400, 'opsz' 14")
        medium = font("DMSans.ttf", "'wght' 500, 'opsz' 14")
        semibold = font("DMSans.ttf", "'wght' 600, 'opsz' 14")
    }

    fun dp(c: Context, v: Number) = (v.toFloat() * c.resources.displayMetrics.density).toInt()

    // ---- surfaces

    fun round(c: Context, fill: Int, radiusDp: Number, stroke: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(c, radiusDp).toFloat()
        stroke?.let { setStroke(dp(c, 1), it) }
    }

    /** The one glossy element: soft yellow gradient, a gentle top highlight, a thin darker edge. */
    fun gloss(c: Context, radiusDp: Number): Drawable {
        val r = dp(c, radiusDp).toFloat()
        val base = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFFFFE27A.toInt(), ACCENT, 0xFFF7C21C.toInt())).apply {
            cornerRadius = r
            setStroke(dp(c, 1), ACCENT_EDGE)
        }
        val shine = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x66FFFFFF, 0x00FFFFFF)).apply { cornerRadius = r }
        return LayerDrawable(arrayOf(base, shine)).apply {
            val i = dp(c, 1)
            setLayerInset(1, i, i, i, 0)
            setLayerGravity(1, Gravity.TOP or Gravity.FILL_HORIZONTAL)
            setLayerHeight(1, dp(c, 20))
        }
    }

    fun ripple(content: Drawable?, mask: Drawable? = null) = RippleDrawable(ColorStateList.valueOf(0x1F17150F), content, mask)

    fun card(c: Context) = LinearLayout(c).apply {
        orientation = LinearLayout.VERTICAL
        background = round(c, SURFACE, R_CARD, LINE)
        val p = dp(c, 20)
        setPadding(p, p, p, p)
    }

    // ---- type scale

    private fun tv(c: Context, s: CharSequence, sp: Float, face: Typeface, color: Int, spacing: Float = 0f) = TextView(c).apply {
        text = s
        typeface = face
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        letterSpacing = spacing
        includeFontPadding = false
        setLineSpacing(dp(c, 2).toFloat(), 1.1f)
    }

    fun displayText(c: Context, s: CharSequence) = tv(c, s, 32f, display, INK, -0.02f)
    fun h1(c: Context, s: CharSequence) = tv(c, s, 22f, display, INK, -0.01f)
    fun h2(c: Context, s: CharSequence) = tv(c, s, 17f, heading, INK)
    fun bodyText(c: Context, s: CharSequence, color: Int = INK) = tv(c, s, 15.5f, body, color)
    fun secondaryText(c: Context, s: CharSequence) = tv(c, s, 14f, body, INK2)
    fun label(c: Context, s: CharSequence, color: Int = INK) = tv(c, s, 15f, medium, color)
    fun caption(c: Context, s: CharSequence) = tv(c, s, 12.5f, body, INK3)

    // ---- controls

    fun primaryButton(c: Context, s: String, onClick: () -> Unit) = Button(c).apply {
        text = s
        isAllCaps = false
        typeface = semibold
        setTextColor(INK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        background = ripple(gloss(c, R_PILL), round(c, ACCENT, R_PILL))
        stateListAnimator = null
        minHeight = dp(c, 52)
        setPadding(dp(c, 20), 0, dp(c, 20), 0)
        setOnClickListener { onClick() }
    }

    fun secondaryButton(c: Context, s: String, onClick: () -> Unit) = Button(c).apply {
        text = s
        isAllCaps = false
        typeface = medium
        setTextColor(INK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        background = ripple(round(c, SURFACE, R_PILL, LINE), round(c, SURFACE, R_PILL))
        stateListAnimator = null
        minHeight = dp(c, 48)
        setPadding(dp(c, 18), 0, dp(c, 18), 0)
        setOnClickListener { onClick() }
    }

    fun switch(c: Context, on: Boolean, onChange: (Boolean) -> Unit) = Switch(c).apply {
        isChecked = on
        thumbTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(INK, SURFACE))
        trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(ACCENT, LINE))
        setOnCheckedChangeListener { _, v -> onChange(v) }
    }

    /** Status chip: coloured dot + label on a white pill. */
    fun chip(c: Context): TextView = tv(c, "", 13.5f, medium, INK).apply {
        background = round(c, SURFACE, 16, LINE)
        setPadding(dp(c, 12), dp(c, 6), dp(c, 12), dp(c, 6))
        compoundDrawablePadding = dp(c, 8)
    }

    fun dot(c: Context, color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
        setSize(dp(c, 8), dp(c, 8))
    }

    /** Step badge: done = ink ✓, current = glossy number, later = outlined number. */
    fun stepBadge(c: Context, view: TextView, n: Int, state: Int) {
        view.gravity = Gravity.CENTER
        view.typeface = semibold
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        when (state) {
            STEP_DONE -> {
                view.text = "✓"; view.setTextColor(SURFACE)
                view.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(INK) }
            }
            STEP_NOW -> {
                view.text = "$n"; view.setTextColor(INK)
                view.background = gloss(c, 14)
            }
            else -> {
                view.text = "$n"; view.setTextColor(INK3)
                view.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(SURFACE); setStroke(dp(c, 1), LINE) }
            }
        }
    }

    const val STEP_DONE = 0
    const val STEP_NOW = 1
    const val STEP_LATER = 2

    fun hairline(c: Context) = View(c).apply {
        setBackgroundColor(LINE)
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 1))
    }
}
