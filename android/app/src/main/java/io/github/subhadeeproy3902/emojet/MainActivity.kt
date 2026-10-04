package io.github.subhadeeproy3902.emojet

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.View
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

    private val main = Handler(Looper.getMainLooper())
    private lateinit var models: ModelManager
    private lateinit var status: TextView
    private lateinit var modelStatus: TextView
    private lateinit var modelActions: LinearLayout
    private lateinit var cancelButton: Button
    private lateinit var mobileData: Switch
    private lateinit var testResult: TextView
    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            main.postDelayed(this, 1_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        models = ModelManager(this)
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

        // Model
        col.addView(text("Model", 20f, bold = true), margins(top = 24))
        modelStatus = text("", 15f).also { col.addView(it) }
        modelActions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        mobileData = Switch(this).apply { text = "Allow mobile data for the download"; textSize = 15f }
        modelActions.addView(mobileData)
        modelActions.addView(button("Download the Laya model") { models.startDownload(mobileData.isChecked); refresh() })
        modelActions.addView(button("Import a model file (.gguf)") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), PICK_MODEL)
        })
        col.addView(modelActions)
        cancelButton = button("Cancel download") { models.cancel(); refresh() }.also { col.addView(it) }

        // Permissions
        col.addView(text("Setup", 20f, bold = true), margins(top = 24))
        status = text("", 15f).also { col.addView(it) }
        col.addView(Switch(this).apply {
            text = "Suggestions on"
            textSize = 17f
            isChecked = Prefs.enabled(this@MainActivity)
            setOnCheckedChangeListener { _, on -> Prefs.setEnabled(this@MainActivity, on) }
        }, margins(top = 8))
        col.addView(button("1. Turn on EmoJet in Accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }, margins(top = 12))
        col.addView(text("Greyed out? Use step 2 first: App info → ⋮ (top right) → Allow restricted settings.", 13f))
        col.addView(button("2. App info (allow restricted settings)") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }, margins(top = 8))

        // Try it
        col.addView(text("Try it", 20f, bold = true), margins(top = 24))
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
        main.post(ticker)
    }

    override fun onPause() {
        main.removeCallbacks(ticker)
        super.onPause()
    }

    @Deprecated("Activity API; fine for a single picker")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val uri = data?.data
        if (requestCode != PICK_MODEL || resultCode != RESULT_OK || uri == null) return
        modelStatus.text = "Checking the file…"
        Thread {
            val msg = runCatching { models.import(uri) }.getOrElse { "Import failed: ${it.message}" }
            runOnUiThread {
                testResult.text = msg
                refresh()
            }
        }.start()
    }

    private fun refresh() {
        val state = models.poll()
        modelStatus.text = when (state) {
            is ModelManager.State.Ready -> "✅ ${state.model.label} is ready."
            is ModelManager.State.Missing -> "The Laya model (0.25–0.45 GB) is downloaded once, then everything works offline."
            is ModelManager.State.Verifying -> "Checking the downloaded file…"
            is ModelManager.State.Failed -> "⚠️ ${state.reason}"
            is ModelManager.State.Downloading -> {
                val pct = if (state.total > 0) state.done * 100 / state.total else 0
                "Downloading ${state.label}: $pct % (${state.done / 1_000_000} / ${state.total / 1_000_000} MB)" +
                    (state.waiting?.let { " — $it" } ?: "")
            }
        }
        val idle = state is ModelManager.State.Missing || state is ModelManager.State.Failed
        modelActions.visibility = if (idle) View.VISIBLE else View.GONE
        cancelButton.visibility = if (state is ModelManager.State.Downloading) View.VISIBLE else View.GONE

        val service = ComponentName(this, EmojetService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val a11y = enabled.split(':').any { it.equals(service, ignoreCase = true) }
        status.text = (if (a11y) "✅ Accessibility on" else "⚠️ Accessibility off — step 1 below") +
            (if (Engine.get(this).isRunning) "\n✅ Engine running" else "\n• Engine starts when needed")
    }

    private fun selfTest() {
        testResult.text = "Running…"
        Thread {
            val engine = Engine.get(this)
            val ranker = Ranker(this)
            val msg = try {
                if (models.installed() == null) "Download the model first."
                else if (!engine.start()) "Engine didn't start.\n${engine.lastLog()}"
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

    companion object {
        private const val PICK_MODEL = 1
    }
}
