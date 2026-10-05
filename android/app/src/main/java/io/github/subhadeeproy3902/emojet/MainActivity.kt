package io.github.subhadeeproy3902.emojet

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
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
import android.widget.TextView
import io.github.subhadeeproy3902.emojet.Theme.dp

/** Home: top bar, title, setup steps (until done), try it, preferences, updates, privacy, advanced. */
class MainActivity : Activity() {

    private val main = Handler(Looper.getMainLooper())
    private lateinit var models: ModelManager
    private lateinit var updates: UpdateManager
    private var lastInfo: UpdateManager.Info? = null

    private lateinit var chip: TextView
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var setupCard: LinearLayout
    private lateinit var step1: Step
    private lateinit var step2: Step
    private lateinit var step3: Step
    private lateinit var progress: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var retryBtn: Button
    private lateinit var mobileBtn: Button
    private lateinit var upgradeBtn: Button
    private lateinit var pauseSub: TextView
    private lateinit var pauseAction: TextView
    private lateinit var offAppsBox: LinearLayout
    private lateinit var updateSub: TextView
    private lateinit var installBtn: Button
    private lateinit var advancedBox: LinearLayout
    private lateinit var advancedChevron: TextView
    private lateinit var modelInfo: TextView
    private lateinit var testResult: TextView

    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            main.postDelayed(this, 1_000)
        }
    }

    /** A setup step: badge · title · detail · actions (actions only while it's the current step). */
    private inner class Step(val n: Int, titleText: String) {
        val badge = TextView(this@MainActivity).apply { layoutParams = LinearLayout.LayoutParams(dp(context, 28), dp(context, 28)) }
        val title = Theme.label(this@MainActivity, titleText)
        val detail = Theme.secondaryText(this@MainActivity, "")
        val actions = LinearLayout(this@MainActivity).apply { orientation = LinearLayout.VERTICAL }
        val view = LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(context, 14), 0, dp(context, 14))
            addView(badge)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(context, 14), dp(context, 4), 0, 0)
                addView(this@Step.title)
                addView(detail, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(context, 4) })
                addView(actions)
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        }

        fun set(state: Int, text: String) {
            Theme.stepBadge(this@MainActivity, badge, n, state)
            title.setTextColor(if (state == Theme.STEP_LATER) Theme.INK3 else Theme.INK)
            detail.text = text
            detail.visibility = if (text.isEmpty()) View.GONE else View.VISIBLE
            actions.visibility = if (state == Theme.STEP_NOW) View.VISIBLE else View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Theme.init(this)
        models = ModelManager(this)
        updates = UpdateManager(this)
        @Suppress("DEPRECATION")
        window.statusBarColor = Theme.PAPER
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 20), dp(context, 12), dp(context, 20), dp(context, 40))
        }
        col.addView(topBar())
        title = Theme.displayText(this, "").also { col.addView(it, lp(top = 32)) }
        subtitle = Theme.bodyText(this, "", Theme.INK2).also { col.addView(it, lp(top = 8)) }
        col.addView(buildSetup().also { setupCard = it }, lp(top = 24))
        col.addView(tryCard(), lp(top = 16))
        col.addView(preferencesCard(), lp(top = 16))
        col.addView(updatesCard(), lp(top = 16))
        col.addView(privacyCard(), lp(top = 16))
        col.addView(advancedCard(), lp(top = 16))
        col.addView(Theme.caption(this, "EmoJet ${versionName()} · free and open source").apply { gravity = Gravity.CENTER }, lp(top = 28))

        // Edge-to-edge (Android 15+): a solid paper band behind the status bar, content scrolls below it.
        val scroll = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            clipToPadding = true
            addView(col)
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Theme.PAPER)
            addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
            setOnApplyWindowInsetsListener { v, insets ->
                @Suppress("DEPRECATION")
                v.setPadding(0, insets.systemWindowInsetTop, 0, insets.systemWindowInsetBottom)
                insets
            }
        })
    }

    // ---------------- sections

    private fun topBar() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(ImageView(context).apply {
            setImageResource(R.mipmap.ic_launcher)
            layoutParams = LinearLayout.LayoutParams(dp(context, 36), dp(context, 36))
        })
        addView(Theme.h1(context, "EmoJet").apply { setPadding(dp(context, 10), 0, 0, 0) },
            LinearLayout.LayoutParams(0, -2, 1f))
        chip = Theme.chip(context)
        addView(chip)
    }

    private fun buildSetup() = Theme.card(this).apply {
        setPadding(dp(context, 20), dp(context, 6), dp(context, 20), dp(context, 6))
        step1 = Step(1, "Getting ready")
        progress = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progressTintList = ColorStateList.valueOf(Theme.ACCENT_DEEP)
            progressBackgroundTintList = ColorStateList.valueOf(Theme.LINE)
        }
        step1.actions.addView(progress, LinearLayout.LayoutParams(-1, dp(context, 6)).apply { topMargin = dp(context, 12) })
        progressText = Theme.caption(context, "").also { step1.actions.addView(it, lp(top = 6)) }
        retryBtn = Theme.primaryButton(context, "Try again") { models.startDownload(false); refresh() }
        mobileBtn = Theme.secondaryButton(context, "Use mobile data instead") { models.allowMobileDataNow(); refresh() }
        step1.actions.addView(retryBtn, lp(top = 12))
        step1.actions.addView(mobileBtn, lp(top = 12))
        addView(step1.view)
        addView(Theme.hairline(context))

        step2 = Step(2, "Turn on EmoJet")
        step2.actions.addView(Theme.primaryButton(context, "Open Accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }, lp(top = 14))
        step2.actions.addView(Theme.caption(context, "Find EmoJet under Downloaded apps. Greyed out? Open App info, tap ⋮ in the corner, then Allow restricted settings."), lp(top = 10))
        step2.actions.addView(Theme.secondaryButton(context, "App info") { openAppInfo() }, lp(top = 10))
        addView(step2.view)
        addView(Theme.hairline(context))

        step3 = Step(3, "Keep it running")
        step3.actions.addView(Theme.primaryButton(context, "Allow") { askKeepRunning() }, lp(top = 14))
        if (isXiaomi()) {
            step3.actions.addView(Theme.caption(context, "On Xiaomi, Redmi and POCO also set App info → Battery → No restrictions, and turn on Autostart."), lp(top = 10))
            step3.actions.addView(Theme.secondaryButton(context, "App info") { openAppInfo() }, lp(top = 10))
        }
        addView(step3.view)
    }

    private fun tryCard() = Theme.card(this).apply {
        addView(Theme.h2(context, "Try it"))
        addView(Theme.secondaryText(context, "Type something, pause, then tap an emoji."), lp(top = 4))
        addView(EditText(context).apply {
            hint = "bhai aaj party karte hain"
            typeface = Theme.body
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(Theme.INK)
            setHintTextColor(Theme.INK3)
            minLines = 2
            gravity = Gravity.TOP
            background = Theme.round(context, Theme.PAPER, Theme.R_INPUT, Theme.LINE)
            setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12))
        }, lp(top = 14))
        upgradeBtn = Theme.primaryButton(context, "Get the improved model") { models.startDownload(false); refresh() }.apply { visibility = View.GONE }
        addView(upgradeBtn, lp(top = 14))
    }

    private fun preferencesCard() = Theme.card(this).apply {
        setPadding(dp(context, 20), dp(context, 16), dp(context, 20), dp(context, 8))
        addView(Theme.h2(context, "Preferences"))
        addView(row("Suggestions", "Show emojis while you type", Theme.switch(context, Prefs.enabled(context)) { Prefs.setEnabled(context, it) }), lp(top = 4))
        addView(Theme.hairline(context))
        addView(row("Understand replies", "Use the message you're replying to", Theme.switch(context, Prefs.useReply(context)) { Prefs.setUseReply(context, it) }))
        addView(Theme.hairline(context))
        pauseSub = Theme.secondaryText(context, "")
        pauseAction = action("Pause") {
            if (paused()) Prefs.resume(context) else Prefs.pauseFor(context, 3_600_000)
            refresh()
        }
        addView(row("Pause for an hour", pauseSub, pauseAction))
        addView(Theme.hairline(context))
        addView(row("Strip position", "Back to just above the text box", action("Reset") {
            Prefs.resetStripOffsets(context)
            testResult.text = "Strip position reset."
        }))
        offAppsBox = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL }
        addView(offAppsBox)
        addView(Theme.caption(context, "On the strip: drag ⠿ to move it · ✕ hides it until your next word · hold ⠿ to turn EmoJet off in that app."), lp(top = 8, bottom = 8))
    }

    private fun updatesCard() = Theme.card(this).apply {
        setPadding(dp(context, 20), dp(context, 8), dp(context, 20), dp(context, 8))
        updateSub = Theme.secondaryText(context, "Only checks when you tap")
        addView(row("Updates", updateSub, action("Check") { checkUpdates() }))
        installBtn = Theme.primaryButton(context, "Update now") { installUpdate() }.apply { visibility = View.GONE }
        addView(installBtn, lp(top = 4, bottom = 12))
    }

    private fun privacyCard() = Theme.card(this).apply {
        addView(Theme.h2(context, "Private by design"))
        for (line in listOf(
            "Runs entirely on your phone. Nothing you type is stored or sent anywhere.",
            "Reads only the box you're typing in and the message just above it. Never password fields.",
            "Uses the internet once to get ready, and when you check for updates.",
        )) {
            addView(Theme.secondaryText(context, line).apply {
                setCompoundDrawablesRelativeWithIntrinsicBounds(Theme.dot(context, Theme.ACCENT_DEEP), null, null, null)
                compoundDrawablePadding = dp(context, 12)
            }, lp(top = 12))
        }
    }

    private fun advancedCard() = Theme.card(this).apply {
        setPadding(dp(context, 20), dp(context, 8), dp(context, 20), dp(context, 8))
        advancedChevron = action("Show") {
            val open = advancedBox.visibility != View.VISIBLE
            advancedBox.visibility = if (open) View.VISIBLE else View.GONE
            advancedChevron.text = if (open) "Hide" else "Show"
        }
        addView(row("Advanced", "Model and diagnostics", advancedChevron))
        advancedBox = LinearLayout(context).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        modelInfo = Theme.secondaryText(context, "")
        advancedBox.addView(modelInfo, lp(top = 4))
        testResult = Theme.bodyText(context, "")
        advancedBox.addView(testResult, lp(top = 10))
        advancedBox.addView(Theme.secondaryButton(context, "Run a quick test") { selfTest() }, lp(top = 10))
        advancedBox.addView(Theme.secondaryButton(context, "Import a model file") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*"), PICK_MODEL)
        }, lp(top = 10, bottom = 12))
        addView(advancedBox)
    }

    // ---------------- state

    override fun onResume() {
        super.onResume()
        models.autoStartOnce()           // notifications are asked for in step 3, with context
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
        testResult.text = "Checking the file…"
        Thread {
            val msg = runCatching { models.import(uri) }.getOrElse { "Import failed: ${it.message}" }
            runOnUiThread { testResult.text = msg; refresh() }
        }.start()
    }

    private fun refresh() {
        val state = models.poll()
        val ready1 = state is ModelManager.State.Ready
        val ready2 = accessibilityOn()
        val ready3 = backgroundOk() && notificationsOk()
        val firstOpen = listOf(ready1, ready2, ready3).indexOfFirst { !it }

        fun stateOf(i: Int, done: Boolean) = when {
            done -> Theme.STEP_DONE
            i == firstOpen -> Theme.STEP_NOW
            else -> Theme.STEP_LATER
        }
        step1.set(stateOf(0, ready1), when (state) {
            is ModelManager.State.Ready -> "Ready to use offline"
            is ModelManager.State.Failed -> "Couldn't finish the download. Check your connection."
            is ModelManager.State.Verifying -> "Almost there…"
            is ModelManager.State.Downloading ->
                if (state.waiting == "waiting for Wi-Fi") "Waiting for Wi-Fi. One-time download, then it works offline."
                else "One-time download, then it works offline. You can leave the app."
            is ModelManager.State.Missing -> "One-time download, then it works offline."
        })
        step1.actions.visibility = if (!ready1) View.VISIBLE else View.GONE
        val pct = if (state is ModelManager.State.Downloading && state.total > 0) (state.done * 1000 / state.total).toInt() else 0
        progress.isIndeterminate = state is ModelManager.State.Verifying || state is ModelManager.State.Missing
        progress.progress = pct
        progress.visibility = vis(state !is ModelManager.State.Failed)
        progressText.text = if (state is ModelManager.State.Downloading) "${pct / 10} %  ·  ${state.done / 1_000_000} of ${state.total / 1_000_000} MB" else ""
        progressText.visibility = vis(state is ModelManager.State.Downloading)
        retryBtn.visibility = vis(state is ModelManager.State.Failed)
        mobileBtn.visibility = vis(state is ModelManager.State.Downloading && state.waiting == "waiting for Wi-Fi")
        step2.set(stateOf(1, ready2), if (ready2) "" else "So EmoJet can see the box you're typing in.")
        step3.set(stateOf(2, ready3), if (ready3) "" else "So your phone doesn't pause EmoJet while you use other apps.")

        val allSet = ready1 && ready2 && ready3
        setupCard.visibility = vis(!allSet)
        title.text = if (allSet) "You're all set" else "Let's set you up"
        subtitle.text = if (allSet) "Type in any app, pause, and tap an emoji." else "Three quick steps. About a minute."

        val on = Prefs.enabled(this)
        val (dotColor, label) = when {
            !allSet -> Theme.ACCENT_DEEP to "Setting up"
            !on -> Theme.INK3 to "Off"
            paused() -> Theme.INK3 to "Paused"
            else -> Theme.OK to "Ready"
        }
        chip.text = label
        chip.setCompoundDrawablesRelativeWithIntrinsicBounds(Theme.dot(this, dotColor), null, null, null)

        pauseSub.text = if (paused()) "${(Prefs.pausedUntil(this) - System.currentTimeMillis()) / 60_000 + 1} min left" else "Take a quick break from suggestions"
        pauseAction.text = if (paused()) "Resume" else "Pause"

        val upgrade = if (ready1) models.upgradeAvailable() else null
        upgradeBtn.visibility = vis(upgrade != null)

        val off = Prefs.disabledApps(this).sorted()
        if (offAppsBox.tag != off) {
            offAppsBox.tag = off
            offAppsBox.removeAllViews()
            for (pkg in off) {
                val name = runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)) }.getOrDefault(pkg)
                offAppsBox.addView(Theme.hairline(this))
                offAppsBox.addView(row("Off in $name", "You held the strip's handle there", action("Turn on") { Prefs.enableApp(this, pkg); refresh() }))
            }
        }
        modelInfo.text = models.installed()?.let { "Model: ${it.label}" } ?: "Model: not downloaded yet"
        updates.pollApk()?.let { updateSub.text = it }
    }

    // ---------------- actions

    private fun paused() = System.currentTimeMillis() < Prefs.pausedUntil(this)

    private fun accessibilityOn(): Boolean {
        val service = ComponentName(this, EmojetService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: ""
        return enabled.split(':').any { it.equals(service, ignoreCase = true) }
    }

    private fun notificationsOk() = Build.VERSION.SDK_INT < 33 ||
        checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun backgroundOk() = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)

    private fun isXiaomi() = Build.MANUFACTURER.lowercase() in setOf("xiaomi", "redmi", "poco")

    private fun versionName() = packageManager.getPackageInfo(packageName, 0).versionName

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
        updateSub.text = "Checking…"
        Thread {
            val msg = runCatching {
                val info = updates.check()
                lastInfo = info
                when {
                    info.appNewer -> "Version ${info.versionName} is available"
                    info.modelsNewer -> "An improved model is available"
                    else -> "You're up to date"
                }
            }.getOrElse { "Couldn't check right now" }
            runOnUiThread {
                updateSub.text = msg
                installBtn.visibility = vis(lastInfo?.appNewer == true)
                refresh()
            }
        }.start()
    }

    private fun installUpdate() {
        val info = lastInfo ?: return
        if (!updates.canInstall()) {
            updateSub.text = "Allow EmoJet to install updates, then tap again"
            startActivity(updates.unknownSourcesSettings())
            return
        }
        updates.downloadApk(info)
        updateSub.text = "Downloading the update…"
    }

    private fun selfTest() {
        testResult.text = "Running…"
        Thread {
            val engine = Engine.get(this)
            val ranker = Ranker(this)
            val msg = try {
                if (models.installed() == null) "The model isn't downloaded yet."
                else if (!engine.start()) "The engine didn't start.\n${engine.lastLog()}"
                else {
                    val sentence = "Happy birthday bro!! have an amazing year"
                    val t0 = SystemClock.elapsedRealtime()
                    val emojis = ranker.suggest(engine, sentence, null)
                    val ms = SystemClock.elapsedRealtime() - t0
                    "${emojis.joinToString("  ")}\n$ms ms" +
                        (if (engine.kind == "classifier") " · ${ranker.lastTones.joinToString { it.name }} · ${ranker.lastStyle}" else "")
                }
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
            runOnUiThread { testResult.text = msg; refresh() }
        }.start()
    }

    // ---------------- building blocks

    /** Settings row: title + subtitle on the left, a control on the right. */
    private fun row(titleText: String, sub: Any, trailing: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(context, 64)
        setPadding(0, dp(context, 10), 0, dp(context, 10))
        addView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(Theme.label(context, titleText))
            val s = if (sub is TextView) sub else Theme.secondaryText(context, sub.toString())
            (s.parent as? ViewGroup)?.removeView(s)
            addView(s, lp(top = 3))
        }, LinearLayout.LayoutParams(0, -2, 1f))
        addView(trailing)
    }

    /** Text action on the right of a row ("Pause", "Check"…). */
    private fun action(s: String, onClick: () -> Unit) = TextView(this).apply {
        text = s
        typeface = Theme.semibold
        setTextColor(Theme.INK)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
        background = Theme.ripple(Theme.round(context, Theme.ACCENT_SOFT, 14), Theme.round(context, Theme.ACCENT_SOFT, 14))
        setPadding(dp(context, 14), dp(context, 8), dp(context, 14), dp(context, 8))
        setOnClickListener { onClick() }
    }

    private fun vis(b: Boolean) = if (b) View.VISIBLE else View.GONE

    private fun lp(top: Int = 0, bottom: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply {
        topMargin = dp(this@MainActivity, top)
        bottomMargin = dp(this@MainActivity, bottom)
    }

    companion object {
        private const val PICK_MODEL = 1
    }
}
