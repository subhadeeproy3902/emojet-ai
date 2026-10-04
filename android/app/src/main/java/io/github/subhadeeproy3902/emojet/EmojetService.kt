package io.github.subhadeeproy3902.emojet

import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.concurrent.Executors

/**
 * Reads the focused text field in any app (never password fields), asks Laya about the whole
 * message after a typing pause, and shows a strip of emojis just above the field.
 * Tapping inserts at the cursor; the strip stays so several emojis can be chained, and refreshes
 * when the words change. Nothing typed is stored.
 */
class EmojetService : AccessibilityService() {

    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private lateinit var engine: Engine
    private lateinit var ranker: Ranker
    private lateinit var wm: WindowManager

    private var strip: LinearLayout? = null
    private var target: AccessibilityNodeInfo? = null
    private var pending: Runnable? = null
    private var shownFor: String? = null
    @Volatile private var generation = 0

    override fun onServiceConnected() {
        keepAwake()
        engine = Engine.get(this)
        ranker = Ranker(this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        worker.execute { engine.start() }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString()
        log { "event ${AccessibilityEvent.eventTypeToString(event.eventType)} pkg=$pkg class=${event.className} source=${event.source != null}" }
        if (pkg == packageName && event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (!Prefs.activeFor(this, if (pkg == keyboardPackage()) target?.packageName?.toString() else pkg)) {
            log { "inactive for $pkg" }
            hide()
            return
        }
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> {
                // Some phones (HyperOS) hand over events without their text box: ask the screen which box has the cursor.
                val node = event.source ?: rootInActiveWindow?.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: run {
                    log { "  no source and no focused input" }
                    return
                }
                log { "  node editable=${node.isEditable} password=${node.isPassword} class=${node.className} len=${node.text?.length}" }
                if (!node.isEditable || node.isPassword) {
                    if (event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) hide()
                    return
                }
                target = node
                schedule(textOf(node))
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (pkg != null && pkg != target?.packageName?.toString() && pkg != keyboardPackage()) {
                    log { "  window change from $pkg (target ${target?.packageName}) -> hide" }
                    hide()
                }
            }
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        hide()
        worker.shutdownNow()
        engine.stop()
        super.onDestroy()
    }

    private fun textOf(node: AccessibilityNodeInfo): String =
        if (node.isShowingHintText) "" else node.text?.toString() ?: ""

    /** Debounce: ask Laya ~350 ms after typing pauses. Unchanged text is not asked again (dedup). */
    private fun schedule(text: String) {
        pending?.let(main::removeCallbacks)
        if (text.isBlank()) {
            hide()
            return
        }
        if (text == shownFor && strip?.parent != null) return
        pending = Runnable { suggest(text) }.also { main.postDelayed(it, DEBOUNCE_MS) }
    }

    private fun suggest(text: String) {
        val gen = ++generation
        worker.execute {
            if (gen != generation) return@execute
            if (!engine.start()) { log { "engine not started" }; return@execute }
            val reply = if (Prefs.useReply(this)) runCatching {
                target?.let { ReplyFinder.find(rootInActiveWindow, it, text, resources.displayMetrics.heightPixels) }
            }.getOrNull() else null
            if (gen != generation) return@execute
            val emojis = runCatching {
                ranker.suggest(engine, text.takeLast(MAX_CHARS), reply)
            }.onFailure { log { "suggest failed: $it" } }.getOrNull() ?: return@execute
            log { "suggest ok: ${emojis.size} emojis, reply=${reply?.length}" }
            main.post {
                if (gen == generation) {
                    shownFor = text
                    show(emojis)
                }
            }
        }
    }

    private fun show(emojis: List<String>) {
        val node = target ?: return
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        val view = strip ?: buildStrip().also { strip = it }
        view.removeAllViews()
        for (emoji in emojis) {
            view.addView(TextView(this).apply {
                text = emoji
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 26f)
                setPadding(dp(9), dp(4), dp(9), dp(4))
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    insert(emoji)
                }
                setOnLongClickListener { disableHere(); true }
            })
        }
        val lp = (view.layoutParams as? WindowManager.LayoutParams) ?: WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.END }
        lp.x = dp(8)
        lp.y = (bounds.top - dp(56)).coerceAtLeast(0)
        log { "show strip at y=${lp.y} field=$bounds attached=${view.parent != null}" }
        if (view.parent == null) wm.addView(view, lp) else wm.updateViewLayout(view, lp)
    }

    private fun buildStrip() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        isLongClickable = true
        setOnLongClickListener { disableHere(); true }
        setPadding(dp(6), dp(2), dp(6), dp(2))
        background = GradientDrawable().apply {
            cornerRadius = dp(24).toFloat()
            setColor(0xEE1C1B22.toInt())
        }
        elevation = dp(6).toFloat()
    }

    /** Insert at the cursor (or replace the selection), then put the cursor after the emoji. */
    private fun insert(emoji: String) {
        val node = target ?: return
        node.refresh()
        val text = textOf(node)
        var start = node.textSelectionStart
        var end = node.textSelectionEnd
        if (start < 0 || end < 0 || start > text.length || end > text.length) {
            start = text.length
            end = text.length
        }
        val from = minOf(start, end)
        val newText = text.substring(0, from) + emoji + text.substring(maxOf(start, end))
        val setText = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, newText)
        }
        if (node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, setText)) {
            val cursor = from + emoji.length
            node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, cursor)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, cursor)
            })
            // Keep this row up so more emojis can be chained; new words bring a new row.
            shownFor = newText
        }
    }

    /** Long-press on the strip: switch EmoJet off in this app (back on from the EmoJet app). */
    private fun disableHere() {
        val pkg = target?.packageName?.toString() ?: return
        Prefs.disableApp(this, pkg)
        hide()
        val name = runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)) }.getOrDefault(pkg)
        Toast.makeText(this, "EmoJet is off in $name. Turn it back on in the EmoJet app.", Toast.LENGTH_LONG).show()
    }

    private fun hide() {
        pending?.let(main::removeCallbacks)
        generation++
        shownFor = null
        strip?.let { if (it.parent != null) wm.removeView(it) }
    }

    private inline fun log(msg: () -> String) {
        if (Log.isLoggable(TAG, Log.DEBUG) || BuildConfig.DEBUG) Log.d(TAG, msg())
    }

    /**
     * Run as a foreground service while EmoJet is on. Without this, HyperOS (and other aggressive
     * battery managers) freeze the app in the background: typing events pile up and arrive late,
     * without their text box, and the Laya engine is frozen too.
     */
    private fun keepAwake() {
        val nm = getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(NotificationChannel(CHANNEL, "EmoJet is on", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Shown while EmoJet suggests emojis, so the phone doesn't freeze it."
                setShowBadge(false)
            })
        }
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val n = Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle("EmoJet is on")
            .setContentText("Emoji suggestions are ready. Tap for settings.")
            .setContentIntent(open)
            .setOngoing(true)
            .build()
        runCatching {
            if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            else startForeground(NOTIFICATION_ID, n)
        }.onFailure { log { "foreground start failed: $it" } }
    }

    private fun keyboardPackage(): String? =
        Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)?.substringBefore('/')

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    companion object {
        private const val TAG = "EmoJet"
        private const val CHANNEL = "emojet_on"
        private const val NOTIFICATION_ID = 1
        private const val DEBOUNCE_MS = 350L
        private const val MAX_CHARS = 280
    }
}
