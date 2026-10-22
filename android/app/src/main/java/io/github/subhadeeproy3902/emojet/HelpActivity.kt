package io.github.subhadeeproy3902.emojet

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.subhadeeproy3902.emojet.Theme.dp

/** Help: what EmoJet does, how to use the strip, and what to do when it stops suggesting. */
class HelpActivity : Activity() {

    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.init(this)
        @Suppress("DEPRECATION")
        window.statusBarColor = Theme.NIGHT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Theme.NIGHT

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 40))
        }
        col.addView(topBar())
        col.addView(Theme.displayText(this, "Help"), lp(top = 24))
        col.addView(section("How it works", listOf(
            "Type in any app (WhatsApp, Messages, Instagram…). When you pause, a row of emojis appears just above the text box.",
            "It reads the whole sentence, not just the last word, and if you're replying to a message, that message too.",
            "Everything runs on your phone. Nothing you type is stored or sent anywhere.",
        )), lp(top = 20))
        col.addView(section("Using the strip", listOf(
            "Tap an emoji to put it where your cursor is.",
            "Hold a people emoji (👋 🙏 👍) to pick a skin tone. EmoJet remembers it.",
            "Swipe to reply to a message with an empty box: you get emojis to react to that message.",
            "Drag ⠿ to move the strip if it covers something.",
            "Tap ✕ to hide it for this message. It comes back with your next word.",
            "Hold ⠿ to turn EmoJet off in that one app (turn it back on from the home screen).",
        )), lp(top = 16))
        col.addView(troubleshooting(), lp(top = 16))
        col.addView(section("Why Accessibility?", listOf(
            "Android only lets an app see what you type in other apps through Accessibility. Keyboards and screen readers use the same permission.",
            "EmoJet reads only the box you're typing in and the message just above it. Never password fields.",
        )), lp(top = 16))

        val scroll = ScrollView(this).apply { isVerticalScrollBarEnabled = false; addView(col) }
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = Theme.background(context)
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            setOnApplyWindowInsetsListener { v, insets ->
                @Suppress("DEPRECATION")
                v.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
                insets
            }
        }
        setContentView(FrameLayout(this).apply { addView(page) })
        if (Build.VERSION.SDK_INT >= 30) {
            val light = android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or android.view.WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            window.insetsController?.setSystemBarsAppearance(if (Theme.dark) 0 else light, light)
        }
    }

    override fun onResume() {
        super.onResume()
        // live, so coming back from Settings shows the fix worked
        val a11y = accessibilityOn()
        val battery = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        status.text = "Accessibility: ${if (a11y) "on ✓" else "OFF"}\nBattery: ${if (battery) "no restrictions ✓" else "restricted"}"
    }

    private fun topBar() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(TextView(context).apply {
            text = "←"
            typeface = Theme.semibold
            setTextColor(Theme.INK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
            background = Theme.ripple(Theme.round(context, Theme.SURFACE2, 18, Theme.LINE), Theme.round(context, Theme.SURFACE2, 18))
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(context, 36), dp(context, 36)))
        addView(Theme.h1(context, "EmoJet").apply { setPadding(dp(context, 10), 0, 0, 0) }, LinearLayout.LayoutParams(0, -2, 1f))
    }

    private fun section(heading: String, lines: List<String>) = Theme.card(this).apply {
        addView(Theme.h2(context, heading))
        for (line in lines) bullet(this, line)
    }

    private fun bullet(parent: LinearLayout, line: String) {
        parent.addView(Theme.secondaryText(this, line).apply {
            setCompoundDrawablesRelativeWithIntrinsicBounds(Theme.dot(context, Theme.ACCENT), null, null, null)
            compoundDrawablePadding = dp(context, 12)
        }, lp(top = 12))
    }

    private fun troubleshooting() = Theme.card(this).apply {
        addView(Theme.h2(context, "It stopped working"))
        addView(Theme.secondaryText(context, "Usually the phone paused EmoJet in the background after a few days. Check these:"), lp(top = 8))
        status = Theme.bodyText(context, "").also { addView(it, lp(top = 12)) }
        bullet(this, "Accessibility off? Open Accessibility and switch EmoJet on again.")
        bullet(this, "Battery restricted? App info → Battery → No restrictions.")
        if (Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")) {
            bullet(this, "Xiaomi, Redmi, POCO: App info → Autostart → on. Without it the phone stops EmoJet after a while.")
            bullet(this, "Also lock EmoJet in Recents: open Recents, hold EmoJet's card, tap the lock.")
        }
        bullet(this, "Still nothing? Open EmoJet once. That restarts it.")
        addView(Theme.primaryButton(context, "Open Accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }, lp(top = 16))
        addView(Theme.secondaryButton(context, "App info") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }, lp(top = 10, bottom = 4))
    }

    private fun accessibilityOn(): Boolean {
        val service = ComponentName(this, EmojetService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return enabled.split(':').any { it.equals(service, ignoreCase = true) }
    }

    private fun lp(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply {
        topMargin = dp(this@HelpActivity, top)
        bottomMargin = dp(this@HelpActivity, bottom)
    }
}
