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

/** Setup, status, updates and privacy screen. Plain views on purpose: keeps the APK small. */
class MainActivity : Activity() {

    private val main = Handler(Looper.getMainLooper())
    private lateinit var models: ModelManager
    private lateinit var updates: UpdateManager
    private lateinit var modelStatus: TextView
    private lateinit var modelActions: LinearLayout
    private lateinit var upgradeButton: Button
    private lateinit var cancelButton: Button
    private lateinit var mobileNowButton: Button
    private lateinit var mobileData: Switch
    private lateinit var status: TextView
    private lateinit var pauseButton: Button
    private lateinit var disabledBox: LinearLayout
    private lateinit var updateStatus: TextView
    private lateinit var installButton: Button
    private lateinit var testResult: TextView
    private var lastInfo: UpdateManager.Info? = null
    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            main.postDelayed(this, 1_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        models = ModelManager(this)
        updates = UpdateManager(this)
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

        // ---- Model
        col.addView(header("Model"))
        modelStatus = text("", 15f).also { col.addView(it) }
        modelActions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        mobileData = Switch(this).apply { text = "Allow mobile data for the download"; textSize = 15f }
        modelActions.addView(mobileData)
        modelActions.addView(button("Download the Laya model") { models.startDownload(mobileData.isChecked); refresh() })
        modelActions.addView(button("Import a model file (.gguf)") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), PICK_MODEL)
        })
        col.addView(modelActions)
        upgradeButton = button("Download the newer model") { models.startDownload(mobileData.isChecked); refresh() }.also { col.addView(it) }
        mobileNowButton = button("Use mobile data now") { models.allowMobileDataNow(); refresh() }.also { col.addView(it) }
        cancelButton = button("Cancel download") { models.cancel(); refresh() }.also { col.addView(it) }

        // ---- Setup
        col.addView(header("Setup"))
        status = text("", 15f).also { col.addView(it) }
        col.addView(switch("Suggestions on", Prefs.enabled(this)) { Prefs.setEnabled(this, it) }, margins(top = 8))
        col.addView(switch("Use the message I'm replying to (any app)", Prefs.useReply(this)) { Prefs.setUseReply(this, it) })
        pauseButton = button("") {
            if (System.currentTimeMillis() < Prefs.pausedUntil(this)) Prefs.resume(this) else Prefs.pauseFor(this, 3_600_000)
            refresh()
        }.also { col.addView(it) }
        col.addView(button("1. Turn on EmoJet in Accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }, margins(top = 12))
        col.addView(text("Greyed out? Use step 2 first: App info → ⋮ (top right) → Allow restricted settings. On Xiaomi also allow \"Display pop-up windows while running in the background\" and set Battery to \"No restrictions\".", 13f))
        col.addView(button("2. App info (restricted settings, battery)") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }, margins(top = 8))
        col.addView(text("Tip: long-press the emoji strip to switch EmoJet off in that app. Add the EmoJet tile to Quick Settings for one-tap on/off.", 13f))
        disabledBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(disabledBox)

        // ---- Updates
        col.addView(header("Updates"))
        updateStatus = text("Version ${packageManager.getPackageInfo(packageName, 0).versionName}. Checking only happens when you tap the button.", 15f)
        col.addView(updateStatus)
        col.addView(button("Check for updates") { checkUpdates() })
        installButton = button("Download and install the update") { installUpdate() }.apply { visibility = View.GONE }
        col.addView(installButton)

        // ---- Privacy
        col.addView(header("Privacy"))
        col.addView(text(
            "• EmoJet reads the text field you are typing in and the message just above it (to understand replies). Never password fields.\n" +
                "• Laya runs on this phone. Nothing you type or read is stored or sent anywhere.\n" +
                "• The internet is used only to download the model once and when you tap \"Check for updates\".", 14f))

        // ---- Try it
        col.addView(header("Try it"))
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
        models.autoStartOnce()           // first launch: the model downloads by itself (Wi-Fi only)
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
                    (state.waiting?.let { " — $it" } ?: "") +
                    "\nIt continues in the background; you can leave the app."
            }
        }
        val idle = state is ModelManager.State.Missing || state is ModelManager.State.Failed
        modelActions.visibility = if (idle) View.VISIBLE else View.GONE
        cancelButton.visibility = if (state is ModelManager.State.Downloading) View.VISIBLE else View.GONE
        mobileNowButton.visibility =
            if (state is ModelManager.State.Downloading && state.waiting == "waiting for Wi-Fi") View.VISIBLE else View.GONE
        val upgrade = if (state is ModelManager.State.Ready) models.upgradeAvailable() else null
        upgradeButton.visibility = if (upgrade != null) View.VISIBLE else View.GONE
        upgrade?.let { upgradeButton.text = "Download the newer model: ${it.label}" }

        val service = ComponentName(this, EmojetService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        val a11y = enabled.split(':').any { it.equals(service, ignoreCase = true) }
        val paused = System.currentTimeMillis() < Prefs.pausedUntil(this)
        status.text = (if (a11y) "✅ Accessibility on" else "⚠️ Accessibility off — step 1 below") +
            (if (Engine.get(this).isRunning) "\n✅ Engine running" else "\n• Engine starts when needed") +
            (if (paused) "\n⏸ Paused for ${(Prefs.pausedUntil(this) - System.currentTimeMillis()) / 60_000 + 1} min" else "")
        pauseButton.text = if (paused) "Resume now" else "Pause for 1 hour"

        val off = Prefs.disabledApps(this).sorted()
        if (disabledBox.childCount != off.size + (if (off.isEmpty()) 0 else 1)) {
            disabledBox.removeAllViews()
            if (off.isNotEmpty()) disabledBox.addView(text("Switched off in:", 14f))
            for (pkg in off) {
                val name = runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)) }.getOrDefault(pkg)
                disabledBox.addView(button("Turn back on in $name") { Prefs.enableApp(this, pkg); refresh() })
            }
        }
        updates.pollApk()?.let { updateStatus.text = it }
    }

    private fun checkUpdates() {
        updateStatus.text = "Checking…"
        Thread {
            val msg = runCatching {
                val info = updates.check()
                lastInfo = info
                buildString {
                    append(if (info.appNewer) "EmoJet ${info.versionName} is available. ${info.notes}" else "The app is up to date.")
                    if (info.modelsNewer) append("\nA newer model is available — see Model above.")
                }
            }.getOrElse { "Couldn't check: ${it.message}" }
            runOnUiThread {
                updateStatus.text = msg
                installButton.visibility = if (lastInfo?.appNewer == true) View.VISIBLE else View.GONE
                refresh()
            }
        }.start()
    }

    private fun installUpdate() {
        val info = lastInfo ?: return
        if (!updates.canInstall()) {
            updateStatus.text = "Allow EmoJet to install updates (one time), then tap the button again."
            startActivity(updates.unknownSourcesSettings())
            return
        }
        updates.downloadApk(info)
        updateStatus.text = "Downloading the update…"
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
                    val sentence = "Happy birthday bro!! have an amazing year"
                    val t0 = SystemClock.elapsedRealtime()
                    val emojis = ranker.suggest(engine, sentence, null)
                    val ms = SystemClock.elapsedRealtime() - t0
                    "\"$sentence\" → ${emojis.joinToString(" ")}  ($ms ms)" +
                        (if (engine.kind == "classifier") "\ntone: ${ranker.lastTones.joinToString { "${it.name} ${"%.2f".format(it.p)}" }} · style: ${ranker.lastStyle}" else "")
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

    private fun header(s: String) = text(s, 20f, bold = true).apply { setPadding(0, dp(24), 0, dp(4)) }

    private fun text(s: String, sp: Float, bold: Boolean = false) = TextView(this).apply {
        text = s
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun switch(label: String, on: Boolean, onChange: (Boolean) -> Unit) = Switch(this).apply {
        text = label
        textSize = 16f
        isChecked = on
        setOnCheckedChangeListener { _, v -> onChange(v) }
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
