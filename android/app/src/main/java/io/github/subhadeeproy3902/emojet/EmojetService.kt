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
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
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
    private var dismissedText: String? = null
    private var row: LinearLayout? = null
    @Volatile private var generation = 0
    private var lastContentCheck = 0L
    @Volatile private var replyShown: String? = null
    private var tonePopup: View? = null
    private var current: List<String>? = null
    private val glyphs = android.graphics.Paint()

    override fun onServiceConnected() {
        Theme.init(this)
        keepAwake()
        engine = Engine.get(this)
        ranker = Ranker(this)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        worker.execute { runCatching { engine.start() } }
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
                if (node.textSelectionStart >= 0 && node.textSelectionStart != node.textSelectionEnd) {
                    log { "  text selected -> hide while selecting" }
                    hide()
                    return
                }
                schedule(textOf(node))
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // only interesting while the box is empty: a quote box may have appeared (swipe to reply)
                val t = target ?: return
                if (pkg != t.packageName?.toString()) return
                val now = SystemClock.elapsedRealtime()
                if (now - lastContentCheck < 400) return
                lastContentCheck = now
                if (!t.refresh() || textOf(t).isNotBlank()) return
                schedule("")
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
        // Android can stop the service before onServiceConnected ran (reinstall, upgrade, quick toggle):
        // the lateinit fields are not set yet, and touching them used to crash the app.
        if (::wm.isInitialized) hide()
        worker.shutdownNow()
        if (::engine.isInitialized) runCatching { engine.stop() }
        super.onDestroy()
    }

    /** The typed text. Some apps (WhatsApp) report the grey placeholder ("Message") as text: then it's "". */
    private fun textOf(node: AccessibilityNodeInfo): String {
        if (node.isShowingHintText) return ""
        val t = node.text?.toString() ?: return ""
        if (t == node.hintText?.toString()) return ""
        // real text always has the cursor somewhere after a character; a placeholder keeps it at 0
        if (node.textSelectionStart == 0 && node.textSelectionEnd == 0) return ""
        return t
    }

    /** Debounce: ask the model 250 ms after typing pauses. Unchanged text is not asked again (dedup). */
    private fun schedule(text: String) {
        pending?.let(main::removeCallbacks)
        if (text.isBlank()) {
            // empty box: no strip, unless the user picked a message to reply to (then: emojis for that message)
            if (shownFor != "") hide()
            if (Prefs.useReply(this) && target?.packageName?.toString() != packageName) {
                pending = Runnable { suggest("") }.also { main.postDelayed(it, DEBOUNCE_MS) }
            }
            return
        }
        if (text == shownFor && strip?.parent != null) return
        // closed with ✕: stay hidden only for that same message until a new word is added
        dismissedText?.let { d ->
            if (text.startsWith(d.trimEnd()) && words(text) <= words(d)) return
            dismissedText = null
        }
        pending = Runnable { suggest(text) }.also { main.postDelayed(it, DEBOUNCE_MS) }
    }

    private fun suggest(text: String) {
        val gen = ++generation
        val t0 = SystemClock.elapsedRealtime()
        worker.execute {
            if (gen != generation) return@execute
            if (!engine.start()) { log { "engine not started" }; return@execute }
            val tStart = SystemClock.elapsedRealtime()
            val ownScreen = target?.packageName?.toString() == packageName     // EmoJet's own "Try it" box has no conversation
            val found = if (Prefs.useReply(this) && !ownScreen) runCatching {
                target?.let { ReplyFinder.find(rootInActiveWindow, it, text, resources.displayMetrics.heightPixels, resources.displayMetrics.widthPixels) }
            }.getOrNull() else null
            if (gen != generation) return@execute
            val tFound = SystemClock.elapsedRealtime()
            if (text.isBlank()) {
                if (found?.quoted != true) {                                        // empty box, no reply picked: no strip
                    main.post { if (gen == generation && shownFor == "") hide() }
                    return@execute
                }
                if (found.text == replyShown && shownFor == "") return@execute      // same quote, already showing (no model call)
            }
            val emojis = runCatching {
                if (text.isBlank()) ranker.suggest(engine, found!!.text, null)     // reacting to the picked message
                else ranker.suggest(engine, clip(text), found?.text)
            }.onFailure { log { "suggest failed: $it" } }.getOrNull() ?: return@execute
            log { "suggest ok in ${SystemClock.elapsedRealtime() - t0} ms (reply search ${tFound - tStart} ms, model ${SystemClock.elapsedRealtime() - tFound} ms, +${DEBOUNCE_MS} ms pause): ${emojis.joinToString("")} len=${text.length} reply=${found?.text?.length} quoted=${found?.quoted} (${ReplyFinder.lastPick})" }
            main.post {
                if (gen == generation) {
                    shownFor = text
                    replyShown = if (text.isBlank()) found?.text else null
                    show(emojis)
                }
            }
        }
    }

    private fun show(emojis: List<String>) {
        val node = target ?: return
        val pkg = node.packageName?.toString() ?: return
        val bounds = Rect().also { node.getBoundsInScreen(it) }
        val view = strip ?: buildStrip().also { strip = it }
        val emojiRow = row ?: return
        emojiRow.removeAllViews()
        current = emojis
        val tone = Prefs.skinTone(this)
        for (base in emojis) {
            val emoji = toned(base, tone) ?: base
            emojiRow.addView(TextView(this).apply {
                text = emoji
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 25f)
                setPadding(dp(7), dp(4), dp(7), dp(4))
                background = Theme.ripple(null, Theme.round(this@EmojetService, if (Theme.dark) 0x22FFFFFF else 0x142B1C00, 14))
                // dock-style magnification while the finger is on it
                setOnTouchListener { v, e ->
                    when (e.actionMasked) {
                        MotionEvent.ACTION_DOWN -> v.animate().scaleX(1.28f).scaleY(1.28f).translationY(-dp(6).toFloat()).setDuration(110).start()
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> v.animate().scaleX(1f).scaleY(1f).translationY(0f).setDuration(160).start()
                    }
                    false
                }
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    insert(emoji)
                }
                // like a keyboard: hold a people emoji to pick a skin tone
                if (toned(base, 1) != null) setOnLongClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    showTones(it, base)
                    true
                }
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
        // Default: right side, just above the text box. If the user dragged it in this app, use that.
        val (x, dy) = Prefs.stripOffset(this, pkg) ?: (dp(10) to dp(62))
        lp.x = x
        lp.y = (bounds.top - dy).coerceIn(0, resources.displayMetrics.heightPixels - dp(60))
        log { "show strip at y=${lp.y} field=$bounds attached=${view.parent != null}" }
        if (view.parent == null) {
            wm.addView(view, lp)
            view.scaleX = 0.85f; view.scaleY = 0.85f; view.alpha = 0f
            view.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(160).start()
        } else {
            wm.updateViewLayout(view, lp)
        }
    }

    /** Dock-style glass pill: ⠿ grip (drag to move, long-press = off in this app) · emojis · ✕ close. */
    private fun buildStrip(): LinearLayout {
        Theme.init(this)                                   // pick up the phone's current light/dark mode
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(5), dp(4), dp(5))
            background = Theme.dockGlass(this@EmojetService, 22)
            elevation = dp(14).toFloat()
            clipChildren = false
            clipToPadding = false
        }
        val grip = TextView(this).apply {
            text = "⠿"
            setTextColor(Theme.dockControl())
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setPadding(dp(10), dp(4), dp(4), dp(4))
            contentDescription = "Move the emoji strip"
        }
        grip.setOnTouchListener(DragToMove(bar))
        bar.addView(grip)
        row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; clipChildren = false; clipToPadding = false }
        bar.addView(row)
        bar.addView(android.widget.ImageView(this).apply {
            setImageResource(R.drawable.ic_close)
            setColorFilter(Theme.dockControl())
            setPadding(dp(8), dp(8), dp(12), dp(8))
            contentDescription = "Close until you type a new word"
            background = Theme.ripple(null, Theme.round(this@EmojetService, if (Theme.dark) 0x22FFFFFF else 0x142B1C00, 14))
            setOnClickListener {
                dismissedText = target?.let(::textOf).orEmpty()
                hide()
            }
        }, LinearLayout.LayoutParams(dp(36), dp(36)))
        return bar
    }

    /** Drag the strip by its grip; position is remembered for this app. A still long-press switches it off here. */
    private inner class DragToMove(private val bar: View) : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var moved = false
        private val longPress = Runnable { if (!moved) disableHere() }

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            val lp = bar.layoutParams as? WindowManager.LayoutParams ?: return false
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY; startX = lp.x; startY = lp.y; moved = false
                    main.postDelayed(longPress, 650)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!moved && kotlin.math.hypot(dx, dy) > dp(6)) {
                        moved = true
                        main.removeCallbacks(longPress)
                    }
                    if (moved) {
                        lp.x = (startX - dx).toInt().coerceIn(0, resources.displayMetrics.widthPixels - bar.width)
                        lp.y = (startY + dy).toInt().coerceIn(0, resources.displayMetrics.heightPixels - bar.height)
                        wm.updateViewLayout(bar, lp)
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    main.removeCallbacks(longPress)
                    if (moved) {
                        val node = target
                        val pkg = node?.packageName?.toString()
                        if (node != null && pkg != null) {
                            val top = Rect().also { node.getBoundsInScreen(it) }.top
                            Prefs.setStripOffset(this@EmojetService, pkg, lp.x, top - lp.y)
                        }
                    }
                }
            }
            return true
        }
    }

    /** Very long drafts: keep the start and the (more important) end, within what the model reads at once. */
    private fun clip(text: String): String {
        val t = text.trim()
        if (t.length <= MAX_CHARS) return t
        val head = t.take(MAX_CHARS / 4).substringBeforeLast(' ')
        val tail = t.takeLast(MAX_CHARS - head.length - 3).substringAfter(' ')
        return "$head … $tail"
    }

    private fun words(text: String) = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

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

    /** The emoji with a skin tone (1..5), or null if it has no toned form on this phone. Tone 0 = as is. */
    private fun toned(emoji: String, tone: Int): String? {
        if (tone == 0) return emoji
        val first = emoji.offsetByCodePoints(0, 1)
        var rest = emoji.substring(first)
        if (rest.startsWith("\uFE0F")) rest = rest.substring(1)
        val t = emoji.substring(0, first) + SKIN[tone] + rest
        return t.takeIf { glyphs.hasGlyph(it) }
    }

    /** Row of the 6 tones above the held emoji; the pick is inserted and remembered. */
    private fun showTones(anchor: View, base: String) {
        dismissTones()
        Theme.init(this)
        val chosen = Prefs.skinTone(this)
        val pop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(5), dp(6), dp(5))
            background = Theme.dockGlass(this@EmojetService, 20)
            elevation = dp(16).toFloat()
        }
        for (tone in 0..5) {
            val variant = toned(base, tone) ?: continue
            pop.addView(TextView(this).apply {
                text = variant
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 27f)
                setPadding(dp(7), dp(4), dp(7), dp(4))
                background = if (tone == chosen) Theme.round(this@EmojetService, Theme.ACCENT and 0x00FFFFFF or 0x55000000, 14)
                else Theme.ripple(null, Theme.round(this@EmojetService, if (Theme.dark) 0x22FFFFFF else 0x142B1C00, 14))
                setOnClickListener {
                    it.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    Prefs.setSkinTone(this@EmojetService, tone)
                    dismissTones()
                    insert(variant)
                    current?.let(::show)                          // the strip now shows the new tone
                }
            })
        }
        pop.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val at = IntArray(2).also(anchor::getLocationOnScreen)
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            val w = resources.displayMetrics.widthPixels
            x = (at[0] + anchor.width / 2 - pop.measuredWidth / 2).coerceIn(dp(8), maxOf(dp(8), w - pop.measuredWidth - dp(8)))
            y = maxOf(0, at[1] - pop.measuredHeight - dp(10))
        }
        pop.setOnTouchListener { _, e -> if (e.actionMasked == MotionEvent.ACTION_OUTSIDE) dismissTones(); false }
        wm.addView(pop, lp)
        pop.alpha = 0f; pop.translationY = dp(6).toFloat()
        pop.animate().alpha(1f).translationY(0f).setDuration(140).start()
        tonePopup = pop
    }

    private fun dismissTones() {
        tonePopup?.let { if (it.parent != null) wm.removeView(it) }
        tonePopup = null
    }

    private fun hide() {
        dismissTones()
        pending?.let(main::removeCallbacks)
        generation++
        shownFor = null
        strip?.let { if (it.parent != null) runCatching { wm.removeView(it) } }
        strip = null
        row = null
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
        private val SKIN = arrayOf("", "\uD83C\uDFFB", "\uD83C\uDFFC", "\uD83C\uDFFD", "\uD83C\uDFFE", "\uD83C\uDFFF")
        private const val CHANNEL = "emojet_on"
        private const val NOTIFICATION_ID = 1
        private const val DEBOUNCE_MS = 250L
        private const val MAX_CHARS = 900
    }
}
