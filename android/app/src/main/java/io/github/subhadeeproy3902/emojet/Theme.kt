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
    // colour tokens: yellow brand, warm neutrals; light/dark follow the phone (refresh() on every screen/strip build)
    var dark = false; private set
    var NIGHT = 0; private set          // page background
    var SURFACE = 0; private set        // cards
    var SURFACE2 = 0; private set       // inputs, chips
    var LINE = 0; private set           // hairlines
    var INK = 0; private set            // primary text
    var INK2 = 0; private set           // secondary text
    var INK3 = 0; private set           // hints, captions
    var OK = 0; private set
    var GLOW = 0; private set           // warm glow at the bottom of the page (from the splash)
    const val ACCENT = 0xFFFFC52E.toInt()       // the brand yellow
    const val ON_ACCENT = 0xFF2B1C00.toInt()
    const val ORANGE = 0xFFFF8A1F.toInt()

    fun refresh(c: Context) {
        dark = (c.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        if (dark) {
            NIGHT = 0xFF120F0A.toInt(); SURFACE = 0xFF1C1812.toInt(); SURFACE2 = 0xFF262017.toInt(); LINE = 0xFF342C1F.toInt()
            INK = 0xFFFFF7E8.toInt(); INK2 = 0xFFCFC2A5.toInt(); INK3 = 0xFF8C806A.toInt(); OK = 0xFF3DDC97.toInt(); GLOW = 0x4DFF8A1F
        } else {
            NIGHT = 0xFFFFF8E8.toInt(); SURFACE = 0xFFFFFFFF.toInt(); SURFACE2 = 0xFFFFF3D9.toInt(); LINE = 0xFFF1E4C6.toInt()
            INK = 0xFF1E1608.toInt(); INK2 = 0xFF6F604A.toInt(); INK3 = 0xFFA9997B.toInt(); OK = 0xFF1FA463.toInt(); GLOW = 0x33FF9A3C
        }
    }

    const val R_CARD = 22
    const val R_INPUT = 16
    const val R_PILL = 26

    lateinit var display: Typeface; private set
    lateinit var heading: Typeface; private set
    lateinit var body: Typeface; private set
    lateinit var medium: Typeface; private set
    lateinit var semibold: Typeface; private set

    fun init(c: Context) {
        refresh(c)
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

    /** Page background with the splash's warm glow rising from the bottom. */
    fun background(c: Context): Drawable {
        val glow = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            colors = intArrayOf(GLOW, GLOW and 0x00FFFFFF or 0x10000000, NIGHT and 0x00FFFFFF)
            gradientRadius = dp(c, 460).toFloat()
            setGradientCenter(0.5f, 1.05f)
        }
        return LayerDrawable(arrayOf(GradientDrawable().apply { setColor(NIGHT) }, glow))
    }

    /** The emoji's glossy yellow: gradient, gentle top highlight, slightly deeper edge. */
    fun gloss(c: Context, radiusDp: Number): Drawable {
        val r = dp(c, radiusDp).toFloat()
        val base = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFFFFDC6E.toInt(), ACCENT, 0xFFF4A51C.toInt())).apply {
            cornerRadius = r
            setStroke(dp(c, 1), 0xFFD98E0F.toInt())
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
        val fill = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(ACCENT, ORANGE)).apply {
            cornerRadius = dp(c, 4).toFloat()
        }
        return LayerDrawable(arrayOf(track, ClipDrawable(fill, Gravity.START, ClipDrawable.HORIZONTAL))).apply {
            setId(0, android.R.id.background)
            setId(1, android.R.id.progress)
        }
    }

    fun ripple(content: Drawable?, mask: Drawable? = null) =
        RippleDrawable(ColorStateList.valueOf(if (dark) 0x33FFFFFF else 0x1F2B1C00), content, mask)

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
        trackTintList = ColorStateList(arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(if (dark) 0xFF7A5A1A.toInt() else 0xFFFFE39A.toInt(), LINE))
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

    /** macOS-dock glass for the emoji strip, following the phone's mode, with a warm yellow touch at the bottom. */
    fun dockGlass(c: Context, radiusDp: Number): Drawable {
        val r = dp(c, radiusDp).toFloat()
        val colors = if (dark) intArrayOf(0xEB3A3A40.toInt(), 0xEB27252A.toInt(), 0xEB4A3814.toInt())
                     else intArrayOf(0xF2FFFFFF.toInt(), 0xEEF7F6F4.toInt(), 0xEEFFE6A0.toInt())
        val body = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors).apply {
            cornerRadius = r
            setStroke(dp(c, 1), if (dark) 0x33FFFFFF else 0x1A2B1C00)
        }
        val sheen = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
            intArrayOf(if (dark) 0x24FFFFFF else 0x80FFFFFF.toInt(), 0x00FFFFFF, 0x00FFFFFF)).apply { cornerRadius = r }
        return LayerDrawable(arrayOf(body, sheen)).apply {
            val i = dp(c, 1)
            setLayerInset(1, i, i, i, i)
        }
    }

    /** Colour of the strip's handle and close icon. */
    fun dockControl(): Int = if (dark) 0x8CFFFFFF.toInt() else 0x732B1C00
}
