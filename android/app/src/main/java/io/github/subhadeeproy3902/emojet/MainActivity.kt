package io.github.subhadeeproy3902.emojet

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import io.github.subhadeeproy3902.emojet.Theme.dp

/** Home screen: hero, setup checklist, controls, updates, privacy. Plain views + Theme (small APK). */
class MainActivity : Activity() {

    private val main = Handler(Looper.getMainLooper())
    private lateinit var models: ModelManager
    private lateinit var updates: UpdateManager
    private var lastInfo: UpdateManager.Info? = null

    private lateinit var heroStatus: TextView
    private lateinit var stepModel: Step
    private lateinit var stepA11y: Step
    private lateinit var stepAwake: Step
    private lateinit var progress: ProgressBar
    private lateinit var mobileNow: Button
    private lateinit var cancelDl: Button
    private lateinit var downloadBtn: Button
    private lateinit var importBtn: Button
    private lateinit var upgradeBtn: Button
    private lateinit var pauseBtn: Button
    private lateinit var disabledBox: LinearLayout
    private lateinit var updateStatus: TextView
    private lateinit var installBtn: Button
    private lateinit var testResult: TextView

    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            main.postDelayed(this, 1_000)
        }
    }

    /** One row of the setup checklist: badge (number or ✓) + title + detail + its own actions. */
    private inner class Step(private val n: Int, title: String) {
        val badge = Theme.text(this@MainActivity, "$n", 15f, Theme.INK, Theme.BOLD).apply {
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(dp(context, 30), dp(context, 30))
            background = Theme.gloss(context, 15)
        }
        val title = Theme.text(this@MainActivity, title, 17f, Theme.INK, Theme.BOLD)
        val detail = Theme.text(this@MainActivity, "", 14f, Theme.INK_SOFT)
        val actions = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        val view = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(context, 10), 0, dp(context, 10))
            addView(badge)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(context, 12), 0, 0, 0)
                addView(this@Step.title)
                addView(detail)
                addView(actions)
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

        fun set(done: Boolean, text: String) {
            badge.text = if (done) "✓" else "$n"
            badge.setTextColor(if (done) 0xFFFFFFFF.toInt() else Theme.INK)
            badge.background = if (done) GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Theme.OK) }
                               else Theme.gloss(this@MainActivity, 15)
            detail.text = text
            actions.visibility = if (done) View.GONE else View.VISIBLE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        models = ModelManager(this)
        updates = UpdateManager(this)
        @Suppress("DEPRECATION")
        window.statusBarColor = Theme.CREAM
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(this@MainActivity, 16), dp(this@MainActivity, 8), dp(this@MainActivity, 16), dp(this@MainActivity, 32))
        }
        col.addView(hero())
        col.addView(setupCard(), gap())
        col.addView(controlsCard(), gap())
        col.addView(updatesCard(), gap())
        col.addView(privacyCard(), gap())
        col.addView(Theme.text(this, "EmoJet ${packageManager.getPackageInfo(packageName, 0).versionName} · powered by Laya · free & open source",
            12f, Theme.INK_SOFT).apply { gravity = Gravity.CENTER; setPadding(0, dp(context, 18), 0, 0) })

        setContentView(ScrollView(this).apply {
            background = Theme.background()
            addView(col)
            setOnApplyWindowInsetsListener { v, insets ->         // edge-to-edge (Android 15+): keep clear of the bars
                @Suppress("DEPRECATION")
                v.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
                insets
            }
        })
    }

    // ---------- sections

    private fun hero() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        background = Theme.gloss(this@MainActivity, 32)
        elevation = dp(context, 6).toFloat()
        setPadding(dp(context, 20), dp(context, 22), dp(context, 20), dp(context, 22))
        addView(ImageView(context).apply {
            setImageResource(R.mipmap.ic_launcher)
            layoutParams = LinearLayout.LayoutParams(dp(context, 88), dp(context, 88))
            elevation = dp(context, 4).toFloat()
        })
        addView(Theme.text(context, "EmoJet", 36f, Theme.INK, Theme.BOLD).apply { setPadding(0, dp(context, 6), 0, 0) })
        addView(Theme.text(context, "The right emoji for your whole message.\nOn your phone. Free. Private.", 15f, Theme.INK).apply {
            gravity = Gravity.CENTER
        })
        heroStatus = Theme.pill(context, "", 0xFFFFFFFF.toInt(), Theme.INK)
        addView(heroStatus, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = dp(context, 12)
        })
    }

    private fun setupCard() = Theme.card(LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(Theme.text(context, "Set up", 22f, Theme.INK, Theme.BOLD))

        stepModel = Step(1, "Laya model")
        progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progressTintList = ColorStateList.valueOf(Theme.SUN_DEEP)
            progressBackgroundTintList = ColorStateList.valueOf(0xFFF1E7C8.toInt())
        }
        stepModel.actions.addView(progress, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(context, 14)).apply { topMargin = dp(context, 6) })
        downloadBtn = Theme.primary(context, "Download the model") { models.startDownload(false); refresh() }
        mobileNow = Theme.secondary(context, "Use mobile data now") { models.allowMobileDataNow(); refresh() }
        cancelDl = Theme.secondary(context, "Cancel download") { models.cancel(); refresh() }
        importBtn = Theme.secondary(context, "Import a model file (.gguf)") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), PICK_MODEL)
        }
        for (b in listOf(downloadBtn, mobileNow, cancelDl, importBtn)) stepModel.actions.addView(b, actionLp())
        addView(stepModel.view)
        upgradeBtn = Theme.primary(context, "Get the newer model") { models.startDownload(false); refresh() }
        addView(upgradeBtn, actionLp())

        stepA11y = Step(2, "Turn on EmoJet")
        stepA11y.actions.addView(Theme.primary(context, "Open Accessibility → EmoJet") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }, actionLp())
        stepA11y.actions.addView(Theme.text(context, "Greyed out? App info → ⋮ (top right) → Allow restricted settings, then try again.", 13f, Theme.INK_SOFT))
        stepA11y.actions.addView(Theme.secondary(context, "App info") { openAppInfo() }, actionLp())
        addView(stepA11y.view)

        stepAwake = Step(3, "Keep it running")
        stepAwake.actions.addView(Theme.primary(context, "Allow in the background") { askKeepRunning() }, actionLp())
        if (isXiaomi()) {
            stepAwake.actions.addView(Theme.text(context, "Xiaomi / Redmi / POCO: App info → Battery → No restrictions, and turn Autostart on.", 13f, Theme.INK_SOFT))
            stepAwake.actions.addView(Theme.secondary(context, "App info") { openAppInfo() }, actionLp())
        }
        addView(stepAwake.view)

        val tryStep = Step(4, "Try it")
        tryStep.detail.text = "Type a message, pause, tap an emoji. Works in WhatsApp, Instagram, any app."
        tryStep.actions.addView(EditText(context).apply {
            hint = "e.g. bhai aaj party karte hain"
            minLines = 2
            setTextColor(Theme.INK)
            setHintTextColor(0x886B6250.toInt())
            background = Theme.round(context, Theme.CREAM, 16, Theme.SUN_EDGE)
            setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10))
        }, actionLp())
        testResult = Theme.text(context, "", 14f, Theme.INK)
        tryStep.actions.addView(testResult)
        tryStep.actions.addView(Theme.secondary(context, "Run engine self-test") { selfTest() }, actionLp())
        addView(tryStep.view)
    })

    private fun controlsCard() = Theme.card(LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(Theme.text(context, "Controls", 22f, Theme.INK, Theme.BOLD))
        addView(switch("Suggestions on", Prefs.enabled(context)) { Prefs.setEnabled(context, it) })
        addView(switch("Understand the message I'm replying to", Prefs.useReply(context)) { Prefs.setUseReply(context, it) })
        pauseBtn = Theme.secondary(context, "") {
            if (System.currentTimeMillis() < Prefs.pausedUntil(context)) Prefs.resume(context) else Prefs.pauseFor(context, 3_600_000)
            refresh()
        }
        addView(pauseBtn, actionLp())
        addView(Theme.secondary(context, "Reset the strip position") {
            Prefs.resetStripOffsets(context)
            testResult.text = "Strip position reset in every app."
        }, actionLp())
        addView(Theme.text(context, "On the strip: drag ⠿ to move it (remembered per app) · ✕ hides it until your next word · long-press ⠿ turns EmoJet off in that app. Add the EmoJet tile to Quick Settings for one-tap on/off.",
            13f, Theme.INK_SOFT).apply { setPadding(0, dp(context, 8), 0, 0) })
        disabledBox = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        addView(disabledBox)
    })

    private fun updatesCard() = Theme.card(LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(Theme.text(context, "Updates", 22f, Theme.INK, Theme.BOLD))
        updateStatus = Theme.text(context, "Checks happen only when you tap the button.", 14f, Theme.INK_SOFT)
        addView(updateStatus)
        addView(Theme.secondary(context, "Check for updates") { checkUpdates() }, actionLp())
        installBtn = Theme.primary(context, "Download and install the update") { installUpdate() }.apply { visibility = View.GONE }
        addView(installBtn, actionLp())
    })

    private fun privacyCard() = Theme.card(LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        addView(Theme.text(context, "Privacy", 22f, Theme.INK, Theme.BOLD))
        addView(Theme.text(context,
            "🔒 EmoJet reads the box you're typing in and the message just above it — never password fields.\n" +
                "📱 Laya runs on this phone. Nothing you type is stored or sent anywhere.\n" +
                "🌐 Internet is used only for the one-time model download and when you tap \"Check for updates\".",
            14f, Theme.INK))
    })

    // ---------- state

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= 33 && !notificationsOk()) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2)
        }
        models.autoStartOnce()
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
        stepModel.detail.text = "Checking the file…"
        Thread {
            val msg = runCatching { models.import(uri) }.getOrElse { "Import failed: ${it.message}" }
            runOnUiThread { testResult.text = msg; refresh() }
        }.start()
    }

    private fun refresh() {
        val state = models.poll()
        val modelReady = state is ModelManager.State.Ready
        stepModel.set(modelReady, when (state) {
            is ModelManager.State.Ready -> state.model.label
            is ModelManager.State.Missing -> "Downloaded once (0.25–0.45 GB), then everything works offline."
            is ModelManager.State.Verifying -> "Checking the downloaded file…"
            is ModelManager.State.Failed -> "⚠️ ${state.reason}"
            is ModelManager.State.Downloading -> {
                val pct = if (state.total > 0) state.done * 100 / state.total else 0
                "Downloading ${state.label}: $pct % (${state.done / 1_000_000} / ${state.total / 1_000_000} MB)" +
                    (state.waiting?.let { " — $it" } ?: "") + "\nKeeps going in the background."
            }
        })
        val downloading = state is ModelManager.State.Downloading
        progress.visibility = vis(downloading || state is ModelManager.State.Verifying)
        progress.isIndeterminate = state is ModelManager.State.Verifying
        if (state is ModelManager.State.Downloading && state.total > 0) progress.progress = (state.done * 1000 / state.total).toInt()
        val idle = state is ModelManager.State.Missing || state is ModelManager.State.Failed
        downloadBtn.visibility = vis(idle)
        importBtn.visibility = vis(idle)
        cancelDl.visibility = vis(downloading)
        mobileNow.visibility = vis(state is ModelManager.State.Downloading && state.waiting == "waiting for Wi-Fi")
        val upgrade = if (modelReady) models.upgradeAvailable() else null
        upgradeBtn.visibility = vis(upgrade != null)
        upgrade?.let { upgradeBtn.text = "Get the newer model: ${it.label}" }

        val a11y = accessibilityOn()
        stepA11y.set(a11y, if (a11y) "On — EmoJet suggests in every app." else "Lets EmoJet see the box you're typing in.")
        val awake = backgroundOk() && notificationsOk()
        stepAwake.set(awake, if (awake) "Allowed in the background." else "Without this, the phone may freeze EmoJet while you're in other apps.")

        val paused = System.currentTimeMillis() < Prefs.pausedUntil(this)
        pauseBtn.text = if (paused) "Resume now (${(Prefs.pausedUntil(this) - System.currentTimeMillis()) / 60_000 + 1} min left)" else "Pause for 1 hour"

        val ready = modelReady && a11y && awake && Prefs.enabled(this) && !paused
        heroStatus.text = when {
            ready -> "✅  Ready — just type"
            paused -> "⏸  Paused"
            !Prefs.enabled(this) -> "Suggestions are off"
            else -> "Setup needed — see below"
        }

        val off = Prefs.disabledApps(this).sorted()
        if (disabledBox.childCount != off.size + (if (off.isEmpty()) 0 else 1)) {
            disabledBox.removeAllViews()
            if (off.isNotEmpty()) disabledBox.addView(Theme.text(this, "Switched off in:", 14f, Theme.INK, Theme.MEDIUM).apply { setPadding(0, dp(context, 10), 0, 0) })
            for (pkg in off) {
                val name = runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)) }.getOrDefault(pkg)
                disabledBox.addView(Theme.secondary(this, "Turn back on in $name") { Prefs.enableApp(this, pkg); refresh() }, actionLp())
            }
        }
        updates.pollApk()?.let { updateStatus.text = it }
    }

    // ---------- actions

    private fun accessibilityOn(): Boolean {
        val service = ComponentName(this, EmojetService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return enabled.split(':').any { it.equals(service, ignoreCase = true) }
    }

    private fun notificationsOk() = Build.VERSION.SDK_INT < 33 ||
        checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun backgroundOk() = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    private fun isXiaomi() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

    private fun openAppInfo() = startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))

    private fun askKeepRunning() {
        if (!notificationsOk() && Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2)
            return
        }
        if (!backgroundOk()) {
            @Suppress("BatteryLife")
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName")))
            return
        }
        openAppInfo()
    }

    private fun checkUpdates() {
        updateStatus.text = "Checking…"
        Thread {
            val msg = runCatching {
                val info = updates.check()
                lastInfo = info
                buildString {
                    append(if (info.appNewer) "EmoJet ${info.versionName} is available. ${info.notes}" else "✅ The app is up to date.")
                    if (info.modelsNewer) append("\nA newer model is available — see Set up.")
                }
            }.getOrElse { "Couldn't check: ${it.message}" }
            runOnUiThread {
                updateStatus.text = msg
                installBtn.visibility = vis(lastInfo?.appNewer == true)
                refresh()
            }
        }.start()
    }

    private fun installUpdate() {
        val info = lastInfo ?: return
        if (!updates.canInstall()) {
            updateStatus.text = "Allow EmoJet to install updates (one time), then tap again."
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
                    "\"$sentence\"\n→ ${emojis.joinToString("  ")}   ($ms ms)" +
                        (if (engine.kind == "classifier") "\ntone: ${ranker.lastTones.joinToString { "${it.name} ${"%.2f".format(it.p)}" }} · style: ${ranker.lastStyle}" else "")
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
            runOnUiThread { testResult.text = msg; refresh() }
        }.start()
    }

    // ---------- helpers

    private fun switch(label: String, on: Boolean, onChange: (Boolean) -> Unit) = Switch(this).apply {
        text = label
        setTextColor(Theme.INK)
        typeface = Theme.MEDIUM
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
        isChecked = on
        Theme.tintSwitch(this)
        setPadding(0, dp(context, 10), 0, dp(context, 10))
        setOnCheckedChangeListener { _, v -> onChange(v) }
    }

    private fun vis(b: Boolean) = if (b) View.VISIBLE else View.GONE

    private fun gap() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { topMargin = dp(this@MainActivity, 14) }

    private fun actionLp() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        .apply { topMargin = dp(this@MainActivity, 8) }

    companion object {
        private const val PICK_MODEL = 1
    }
}
