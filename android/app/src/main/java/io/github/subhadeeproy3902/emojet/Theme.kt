package io.github.subhadeeproy3902.emojet

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.TextView

/** EmoJet's look: glossy sun-yellow, warm cream, bold ink. Plain drawables, no UI library (keeps the APK small). */
object Theme {
    const val SUN = 0xFFFFD12E.toInt()          // main yellow
    const val SUN_DEEP = 0xFFFFB300.toInt()     // gradient bottom
    const val SUN_LIGHT = 0xFFFFF0A0.toInt()    // gloss highlight
    const val SUN_EDGE = 0xFFE0A000.toInt()     // outline
    const val ORANGE = 0xFFFF8A00.toInt()
    const val CREAM = 0xFFFFF8E1.toInt()
    const val CREAM_LIGHT = 0xFFFFFDF5.toInt()
    const val INK = 0xFF1B1A17.toInt()
    const val INK_SOFT = 0xFF6B6250.toInt()
    const val CARD = 0xFFFFFFFF.toInt()
    const val OK = 0xFF23A455.toInt()
    const val WARN = 0xFFE8590C.toInt()

    val BOLD: Typeface = Typeface.create("sans-serif-black", Typeface.NORMAL)
    val MEDIUM: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)

    fun dp(c: Context, v: Number) = (v.toFloat() * c.resources.displayMetrics.density).toInt()

    /** Glossy yellow: vertical gradient + a soft white highlight on the top half + thin darker edge. */
    fun gloss(c: Context, radiusDp: Number, edge: Boolean = true): Drawable {
        val r = dp(c, radiusDp).toFloat()
        val base = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xFFFFE45C.toInt(), SUN, SUN_DEEP)).apply {
            cornerRadius = r
            if (edge) setStroke(dp(c, 1), SUN_EDGE)
        }
        val shine = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x99FFFFFF.toInt(), 0x00FFFFFF)).apply {
            cornerRadius = r
        }
        return LayerDrawable(arrayOf(base, shine)).apply {
            val inset = dp(c, 2)
            setLayerInset(1, inset, inset, inset, 0)
            setLayerGravity(1, Gravity.TOP or Gravity.FILL_HORIZONTAL)
            setLayerHeight(1, dp(c, 18))
        }
    }

    fun round(c: Context, color: Int, radiusDp: Number, strokeColor: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(c, radiusDp).toFloat()
            strokeColor?.let { setStroke(dp(c, 1), it) }
        }

    fun background(): Drawable = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(CREAM, CREAM_LIGHT))

    fun ripple(content: Drawable, mask: Drawable? = null) = RippleDrawable(ColorStateList.valueOf(0x33000000), content, mask)

    fun card(v: View) = v.apply {
        background = round(context, CARD, 24, 0x14000000)
        elevation = dp(context, 3).toFloat()
        val p = dp(context, 18)
        setPadding(p, p, p, p)
    }

    fun primary(c: Context, label: String, onClick: () -> Unit) = Button(c).apply {
        text = label
        isAllCaps = false
        setTextColor(INK)
        typeface = BOLD
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        background = ripple(gloss(c, 18))
        stateListAnimator = null
        elevation = dp(c, 2).toFloat()
        val h = dp(c, 14)
        setPadding(h, dp(c, 12), h, dp(c, 12))
        setOnClickListener { onClick() }
    }

    fun secondary(c: Context, label: String, onClick: () -> Unit) = Button(c).apply {
        text = label
        isAllCaps = false
        setTextColor(INK)
        typeface = MEDIUM
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        background = ripple(round(c, CREAM, 18, SUN_EDGE))
        stateListAnimator = null
        val h = dp(c, 14)
        setPadding(h, dp(c, 10), h, dp(c, 10))
        setOnClickListener { onClick() }
    }

    fun text(c: Context, s: CharSequence, sp: Float, color: Int = INK, face: Typeface? = null) = TextView(c).apply {
        text = s
        setTextColor(color)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        face?.let { typeface = it }
        setLineSpacing(0f, 1.12f)
    }

    /** Small rounded label, e.g. "Ready ✓". */
    fun pill(c: Context, s: String, bg: Int, fg: Int) = text(c, s, 13f, fg, MEDIUM).apply {
        background = round(c, bg, 14)
        setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4))
    }

    fun tintSwitch(s: android.widget.Switch) {
        s.thumbTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(SUN_DEEP, Color.WHITE),
        )
        s.trackTintList = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(0xFFFFE08A.toInt(), 0xFFD9D2C3.toInt()),
        )
    }
}
