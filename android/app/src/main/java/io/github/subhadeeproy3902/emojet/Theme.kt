package io.github.subhadeeproy3902.emojet

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.ClipDrawable
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
 * EmoJet design system, taken from the splash: midnight-indigo night, soft violet glow, glass
 * cards, the emoji's glossy yellow for the one primary action, and the neon trail gradient only
 * for progress. Type: Syne (headings) + DM Sans (text). Spacing on an 8 dp grid.
 */
object Theme {
    // colour tokens (sampled from assets/brand/splash.png)
    const val NIGHT = 0xFF040429.toInt()        // background
    const val SURFACE = 0xFF0F0E3F.toInt()      // cards
    const val SURFACE2 = 0xFF17165A.toInt()     // inputs, chips
    const val LINE = 0xFF24236A.toInt()         // hairlines
    const val INK = 0xFFF5F3FF.toInt()          // primary text
    const val INK2 = 0xFFB8B5E0.toInt()         // secondary text
    const val INK3 = 0xFF7C79AE.toInt()         // hints, captions
    const val ACCENT = 0xFFFABE3C.toInt()       // the emoji's yellow
    const val ON_ACCENT = 0xFF1A1033.toInt()    // text on yellow
    const val MAGENTA = 0xFFD32ACF.toInt()
    const val VIOLET = 0xFF6A2CF0.toInt()
    const val OK = 0xFF34D399.toInt()

    const val R_CARD = 22
    const val R_INPUT = 16
    const val R_PILL = 26

    lateinit var display: Typeface; private set
    lateinit var heading: Typeface; private set
    lateinit var body: Typeface; private set
    lateinit var medium: Typeface; private set
    lateinit var semibold: Typeface; private set

    fun init(c: Context) {
        if (::display.isInitialized) return
        // With the system font as fallback (Android 10+), so symbols the font lacks (✕, ⋮, →) still render.
        fun font(file: String, axes: String): Typeface = runCatching {
            if (android.os.Build.VERSION.SDK_INT >= 29) {
                val f = android.graphics.fonts.Font.Builder(c.assets, "fonts/$file").setFontVariationSettings(axes).build()
                Typeface.CustomFallbackBuilder(android.graphics.fonts.FontFamily.Builder(f).build())
                    .setSystemFallback("sans-serif").build()
            } else {
                Typeface.Builder(c.assets, "fonts/$file").setFontVariationSettings(axes).build()
            }
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

    /** Night with a soft violet/magenta glow behind the top of the page, like the light behind the splash emoji. */
    fun background(c: Context): Drawable {
        val glow = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            colors = intArrayOf(0x553A1FC8, 0x22D32ACF, 0x00040429)
            gradientRadius = dp(c, 420).toFloat()
            setGradientCenter(0.85f, 0.0f)
        }
        return LayerDrawable(arrayOf(GradientDrawable().apply { setColor(NIGHT) }, glow))
    }

    /** The emoji's glossy yellow: gradient, gentle top highlight, slightly deeper edge. */
    fun gloss(c: Context, radiusDp: Number): Drawable {
        val r = dp(c, radiusDp).toFloat()
        val base = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFFFFD877.toInt(), ACCENT, 0xFFF0A21E.toInt())).apply {
            cornerRadius = r
            setStroke(dp(c, 1), 0xFFD98B12.toInt())
        }
        // full-size highlight that fades out by the middle: follows the pill's curve exactly
        val shine = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x55FFFFFF, 0x00FFFFFF, 0x00FFFFFF)).apply { cornerRadius = r }
        return LayerDrawable(arrayOf(base, shine)).apply {
            val i = dp(c, 1)
            setLayerInset(1, i, i, i, i)
        }
    }

    /** Progress bar in the splash's neon trail colours. */
    fun trailProgress(c: Context): Drawable {
        val track = round(c, SURFACE2, 4)
        val fill = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(ACCENT, MAGENTA, VIOLET)).apply {
            cornerRadius = dp(c, 4).toFloat()
        }
        return LayerDrawable(arrayOf(track, ClipDrawable(fill, Gravity.START, ClipDrawable.HORIZONTAL))).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }
    }

    fun ripple(content: Drawable?, mask: Drawable? = null) = RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), content, mask)

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
        setTextColor(ON_ACCENT)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        background = RippleDrawable(ColorStateList.valueOf(0x331A1033), gloss(c, R_PILL), round(c, ACCENT, R_PILL))
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
        background = ripple(round(c, SURFACE2, R_PILL, LINE), round(c, SURFACE2, R_PILL))
        stateListAnimator = null
        minHeight = dp(c, 48)
        setPadding(dp(c, 18), 0, dp(c, 18), 0)
        setOnClickListener { onClick() }
    }

    fun switch(c: Context, on: Boolean, onChange: (Boolean) -> Unit) = Switch(c).apply {
        isChecked = on
        thumbTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(ACCENT, INK3))
        trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(0xFF7A5A1A.toInt(), LINE))
        setOnCheckedChangeListener { _, v -> onChange(v) }
    }

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

    /** Step badge: done = soft-white ✓, current = glossy yellow number, later = outlined number. */
    fun stepBadge(c: Context, view: TextView, n: Int, state: Int) {
        view.gravity = Gravity.CENTER
        view.typeface = semibold
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
        when (state) {
            STEP_DONE -> {
                view.text = "✓"; view.setTextColor(NIGHT)
                view.background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(INK) }
            }
            STEP_NOW -> {
                view.text = "$n"; view.setTextColor(ON_ACCENT)
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

    /** macOS-dock style glass for the emoji strip: smoky dark, soft light edge, top sheen. Neutral over any app. */
    fun dockGlass(c: Context, radiusDp: Number): Drawable {
        val r = dp(c, radiusDp).toFloat()
        val body = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xEB3A3A44.toInt(), 0xEB24242C.toInt(), 0xEB18181E.toInt())).apply {
            cornerRadius = r
            setStroke(dp(c, 1), 0x33FFFFFF)
        }
        val sheen = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x24FFFFFF, 0x00FFFFFF, 0x00FFFFFF)).apply { cornerRadius = r }
        return LayerDrawable(arrayOf(body, sheen)).apply {
            val i = dp(c, 1)
            setLayerInset(1, i, i, i, i)
        }
    }
}
