package io.github.subhadeeproy3902.emojet

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Finds "the message being replied to" in any app, without app-specific code: the nearest
 * readable text just above the field being typed in. When the user swipes to reply, chat apps
 * show the quoted message right above the input, so that's what is found; otherwise it's the
 * latest message on screen. Used only for the current suggestion, never stored.
 */
object ReplyFinder {
    /** [quoted] = the user picked this message to reply to (a quote box above the input), not just the latest message. */
    data class Found(val text: String, val quoted: Boolean)
    private val QUOTE_ID = Regex("quote|reply|replied", RegexOption.IGNORE_CASE)

    private val TIME = Regex("""^\s*\d{1,2}[:.]\d{2}(\s?[AaPp]\.?[Mm]\.?)?\s*$""")
    private val SKIP_CLASSES = setOf("android.widget.Button", "android.widget.ImageButton", "android.widget.EditText")

    /** What was picked, without its words (for the debug log): view id, class, centered, length. */
    @Volatile var lastPick: String = ""
        private set

    fun find(root: AccessibilityNodeInfo?, input: AccessibilityNodeInfo, draft: String, screenHeight: Int, screenWidth: Int): Found? {
        root ?: return null
        val inputBounds = Rect().also { input.getBoundsInScreen(it) }
        val minTop = (screenHeight * 0.12).toInt()                 // below the title bar
        val maxGap = (screenHeight * 0.55).toInt()                 // ignore text far above the input
        var best: String? = null
        var bestBottom = Int.MIN_VALUE
        var bestQuoted = false
        lastPick = "none"
        val queue = ArrayDeque<AccessibilityNodeInfo>().apply { add(root) }
        var visited = 0
        while (queue.isNotEmpty() && visited < 600) {
            val n = queue.removeFirst()
            visited++
            for (i in 0 until n.childCount) n.getChild(i)?.let(queue::add)
            if (n.isEditable || n.isPassword || !n.isVisibleToUser) continue
            if (n.className?.toString() in SKIP_CLASSES) continue
            val text = n.text?.toString()?.trim().orEmpty()
            if (text.length < 2 || text == draft || TIME.matches(text)) continue
            val b = Rect().also { n.getBoundsInScreen(it) }
            if (b.height() <= 0 || b.top < minTop) continue
            if (b.bottom > inputBounds.top + 4 || inputBounds.top - b.bottom > maxGap) continue
            // date chips ("Today"), "unread messages", system notes: short and centered; message bubbles sit left/right
            val centered = kotlin.math.abs(b.centerX() - screenWidth / 2) < screenWidth * 0.08 && b.width() < screenWidth * 0.6
            if (centered && text.length < 24) continue
            // closest to the input wins; on the same row, the longer text (the bubble, not its label)
            if (b.bottom > bestBottom || (b.bottom == bestBottom && text.length > (best?.length ?: 0))) {
                best = text
                bestBottom = b.bottom
                bestQuoted = n.viewIdResourceName?.let(QUOTE_ID::containsMatchIn) == true
                lastPick = "id=${n.viewIdResourceName} class=${n.className} len=${text.length} centered=$centered left=${b.left} right=${b.right}"
            }
        }
        return best?.let { Found(it.take(300), bestQuoted) }
    }
}
