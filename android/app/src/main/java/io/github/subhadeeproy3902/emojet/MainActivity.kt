package io.github.subhadeeproy3902.emojet

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView

/** Setup + status screen. Plain views on purpose: keeps the APK small. */
class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var testResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(28))
        }
        col.addView(ImageView(this).apply {
            setImageResource(R.mipmap.ic_launcher)
            layoutParams = LinearLayout.LayoutParams(dp(72), dp(72))
        })
        col.addView(text("EmoJet", 28f, bold = true))
        col.addView(text("Emoji suggestions from your whole message, computed on this phone by Laya. Nothing you type leaves the phone.", 15f))

        status = text("", 15f).also { col.addView(it, margins(top = 20)) }

        col.addView(Switch(this).apply {
            text = "Suggestions on"
            textSize = 17f
            isChecked = Prefs.enabled(this@MainActivity)
            setOnCheckedChangeListener { _, on -> Prefs.setEnabled(this@MainActivity, on) }
        }, margins(top = 16))

        col.addView(button("1. Turn on EmoJet in Accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }, margins(top = 16))
        col.addView(text("Greyed out? Use step 2 first: App info → ⋮ (top right) → Allow restricted settings.", 13f))
        col.addView(button("2. App info (allow restricted settings)") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }, margins(top = 8))

        col.addView(text("Try it", 20f, bold = true), margins(top = 28))
        col.addView(EditText(this).apply {
            hint = "Type a message here, pause, then tap an emoji"
            minLines = 2
        })
        testResult = text("", 14f)
        col.addView(testResult)
        col.addView(button("Run engine self-test") { selfTest() }, margins(top = 8))

        setContentView(ScrollView(this).apply { addView(col) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val engine = Engine.get(this)
        val service = ComponentName(this, EmojetService::class.java).flattenToString()
        val enabledServices = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val a11y = enabledServices.split(':').any { it.equals(service, ignoreCase = true) }
        status.text = buildString {
            append(if (engine.isModelPresent) "✅ Model ready (${engine.modelFile.length() / 1_000_000} MB)\n"
                   else "⚠️ Model missing. In this test build, copy it to:\n${engine.modelFile.path}\n")
            append(if (a11y) "✅ Accessibility on\n" else "⚠️ Accessibility off — step 1 below\n")
            append(if (engine.isRunning) "✅ Engine running" else "• Engine starts when needed")
        }
    }

    private fun selfTest() {
        testResult.text = "Running…"
        Thread {
            val engine = Engine.get(this)
            val ranker = Ranker(this)
            val msg = try {
                if (!engine.start()) "Engine didn't start.\n${engine.lastLog()}"
                else {
                    val t0 = SystemClock.elapsedRealtime()
                    val emojis = ranker.suggest(engine.decide(ranker.request("Happy birthday bro!! have an amazing year", null)))
                    "\"Happy birthday bro!! have an amazing year\" → ${emojis.joinToString(" ")}  (${SystemClock.elapsedRealtime() - t0} ms)"
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
            runOnUiThread {
                testResult.text = msg
                refresh()
            }
        }.start()
    }

    private fun text(s: String, sp: Float, bold: Boolean = false) = TextView(this).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun margins(top: Int) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
    ).apply { topMargin = dp(top) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
}
