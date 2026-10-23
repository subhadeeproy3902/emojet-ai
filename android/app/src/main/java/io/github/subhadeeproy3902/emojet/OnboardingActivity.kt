package io.github.subhadeeproy3902.emojet

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.subhadeeproy3902.emojet.Theme.dp

/**
 * First run: what EmoJet does, what it costs in space, what it reads and what it never reads.
 * Shown once (Prefs.onboarded); the three setup steps themselves stay on the home screen.
 */
class OnboardingActivity : Activity() {

    private lateinit var pager: LinearLayout
    private lateinit var dots: LinearLayout
    private lateinit var next: TextView
    private lateinit var back: TextView
    private var page = 0
    private val pages by lazy { listOf(::welcome, ::howItWorks, ::privacy, ::whatItNeeds) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.init(this)
        @Suppress("DEPRECATION")
        window.statusBarColor = Theme.NIGHT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Theme.NIGHT

        pager = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 24), dp(context, 24), dp(context, 24), dp(context, 8))
        }
        dots = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        back = navAction("Back") { if (page > 0) show(page - 1) }
        next = navAction("Next") { if (page < pages.size - 1) show(page + 1) else finishOnboarding() }

        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(context, 24), dp(context, 8), dp(context, 24), dp(context, 20))
            addView(back)
            addView(dots, LinearLayout.LayoutParams(0, -2, 1f))
            addView(next)
        }
        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(pager) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Theme.background(context)
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            addView(bar)
            setOnApplyWindowInsetsListener { v, insets ->
                @Suppress("DEPRECATION")
                v.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
                insets
            }
        }
        setContentView(FrameLayout(this).apply { addView(root) })
        if (Build.VERSION.SDK_INT >= 30) {
            val light = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (Theme.dark) 0 else light, light)
        }
        show(0)
    }

    /** Back button goes a page back, like the on-screen one, instead of leaving half-explained. */
    @Deprecated("Activity API")
    override fun onBackPressed() {
        if (page > 0) show(page - 1) else super.onBackPressed()
    }

    private fun finishOnboarding() {
        Prefs.setOnboarded(this, true)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun show(i: Int) {
        page = i
        pager.removeAllViews()
        pager.addView(pages[i]())
        pager.alpha = 0f
        pager.animate().alpha(1f).setDuration(180).start()
        back.visibility = if (i == 0) View.INVISIBLE else View.VISIBLE
        next.text = if (i == pages.size - 1) "Set up EmoJet" else "Next"
        dots.removeAllViews()
        for (k in pages.indices) {
            dots.addView(View(this).apply {
                background = Theme.round(context, if (k == i) Theme.ACCENT else Theme.LINE, 3)
                layoutParams = LinearLayout.LayoutParams(dp(context, if (k == i) 18 else 6), dp(context, 6))
                    .apply { marginStart = dp(context, 4); marginEnd = dp(context, 4) }
            })
        }
    }

    // ---------------- pages

    private fun welcome() = column {
        addView(ImageView(context).apply { setImageResource(R.mipmap.ic_launcher) },
            LinearLayout.LayoutParams(dp(context, 84), dp(context, 84)).apply { topMargin = dp(context, 24) })
        addView(Theme.displayText(context, "EmoJet"), lp(top = 20))
        addView(Theme.bodyText(context, "The right emoji, every time.", Theme.INK2), lp(top = 8))
        addView(stripMock(listOf("😂", "🔥", "🥹", "💯", "🙌", "❤️")), lp(top = 28))
        addView(Theme.secondaryText(context,
            "EmoJet reads what you are typing — in WhatsApp, Instagram, Messages, any app — and suggests the emojis " +
                "that actually fit the sentence, in English, Hindi and Bengali, including Hinglish and Banglish."), lp(top = 24))
    }

    private fun howItWorks() = column {
        addView(Theme.h1(context, "How it works"), lp(top = 16))
        addView(stripMock(listOf("😴", "💤", "🛌", "🥱", "😪", "🌙")), lp(top = 20))
        addView(Theme.caption(context, "“ghum pacche khub” → the row above appears over your keyboard").apply {
            gravity = Gravity.CENTER
        }, lp(top = 10))
        for ((n, title, detail) in listOf(
            Triple("1", "Type as usual", "Write your message in any app. EmoJet watches only the box you are typing in."),
            Triple("2", "Pause for a moment", "A row of six emojis appears just above the text box, about a quarter of a second after you stop."),
            Triple("3", "Tap one", "It goes in at the cursor. Hold a person emoji to choose a skin tone — it is remembered."),
        )) addView(step(n, title, detail), lp(top = 18))
    }

    private fun privacy() = column {
        addView(Theme.h1(context, "Stays on your phone"), lp(top = 16))
        addView(Theme.secondaryText(context,
            "The whole model runs on your device. There is no account, no server and no cost."), lp(top = 8))
        for ((good, line) in listOf(
            true to "Nothing you type is stored, uploaded or shared. It never leaves the phone.",
            true to "EmoJet reads the text box you are typing in, and the message you are replying to.",
            false to "It never reads password fields.",
            false to "It never reads other apps' content, your photos, contacts or files.",
            true to "The internet is used once to download the model, and when you tap “Check for updates”.",
        )) addView(bullet(line, good), lp(top = 14))
    }

    private fun whatItNeeds() = column {
        addView(Theme.h1(context, "What it needs"), lp(top = 16))
        val models = ModelManager(this@OnboardingActivity)
        val mb = (models.downloadSizeBytes() / 1_000_000).toInt()
        addView(sizeCard(mb), lp(top = 16))
        for ((title, detail) in listOf(
            "Accessibility" to "The only way Android lets an app read the box you are typing in. EmoJet uses nothing else from it.",
            "Notifications" to "One silent notification while EmoJet is on, which is what keeps Android from stopping it.",
            "Battery: no restrictions" to "Otherwise the phone pauses EmoJet after a few days and suggestions quietly stop.",
        )) addView(step("•", title, detail), lp(top = 16))
        addView(Theme.caption(context,
            "You choose each one on the next screen, and you can turn EmoJet off at any time."), lp(top = 18))
    }

    // ---------------- building blocks

    private fun sizeCard(modelMb: Int) = Theme.card(this).apply {
        addView(Theme.h2(context, "Download size"))
        val apkMb = (packageManager.getApplicationInfo(packageName, 0).sourceDir
            .let { java.io.File(it).length() } / 1_000_000).toInt().coerceAtLeast(1)
        addView(row("This app", "$apkMb MB — already installed"), lp(top = 12))
        addView(Theme.hairline(context))
        addView(row("The model, once", "$modelMb MB — Wi-Fi or mobile data, then it works offline"), lp(top = 4))
        addView(Theme.hairline(context))
        addView(row("Total on your phone", "about ${apkMb + modelMb} MB"), lp(top = 4))
    }

    private fun row(title: String, detail: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(0, dp(context, 8), 0, dp(context, 8))
        addView(Theme.label(context, title))
        addView(Theme.secondaryText(context, detail), lp(top = 2))
    }

    /** A small mock of the real suggestion strip, so the first screen shows what will appear. */
    private fun stripMock(emojis: List<String>) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER
        background = Theme.round(context, Theme.SURFACE, 18, Theme.LINE)
        setPadding(dp(context, 10), dp(context, 8), dp(context, 10), dp(context, 8))
        layoutParams = LinearLayout.LayoutParams(-2, -2).apply { gravity = Gravity.CENTER_HORIZONTAL }
        addView(TextView(context).apply {
            text = "⠿"
            setTextColor(Theme.INK3)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setPadding(0, 0, dp(context, 6), 0)
        })
        for (e in emojis) addView(TextView(context).apply {
            text = e
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 23f)
            setPadding(dp(context, 6), 0, dp(context, 6), 0)
        })
    }

    private fun step(n: String, title: String, detail: String) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(TextView(context).apply {
            text = n
            typeface = Theme.semibold
            gravity = Gravity.CENTER
            setTextColor(Theme.ON_ACCENT)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            background = Theme.round(context, Theme.ACCENT, 14)
            layoutParams = LinearLayout.LayoutParams(dp(context, 26), dp(context, 26))
        })
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 14), 0, 0, 0)
            addView(Theme.label(context, title))
            addView(Theme.secondaryText(context, detail), lp(top = 3))
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
    }

    private fun bullet(line: String, good: Boolean) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(TextView(context).apply {
            text = if (good) "✓" else "✕"
            typeface = Theme.semibold
            setTextColor(if (good) Theme.OK else Theme.INK3)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            layoutParams = LinearLayout.LayoutParams(dp(context, 22), -2)
        })
        addView(Theme.secondaryText(context, line), LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun navAction(s: String, onClick: () -> Unit) = TextView(this).apply {
        text = s
        typeface = Theme.semibold
        setTextColor(Theme.INK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        background = Theme.ripple(Theme.round(context, Theme.SURFACE2, 16, Theme.LINE), Theme.round(context, Theme.SURFACE2, 16))
        setPadding(dp(context, 18), dp(context, 10), dp(context, 18), dp(context, 10))
        setOnClickListener { onClick() }
    }

    private fun column(body: LinearLayout.() -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        body()
    }

    private fun lp(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply {
        topMargin = dp(this@OnboardingActivity, top)
        bottomMargin = dp(this@OnboardingActivity, bottom)
    }
}
