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
    private val TIME = Regex("""^\s*\d{1,2}[:.]\d{2}(\s?[AaPp]\.?[Mm]\.?)?\s*$""")
    private val SKIP_CLASSES = setOf("android.widget.Button", "android.widget.ImageButton", "android.widget.EditText")

    fun find(root: AccessibilityNodeInfo?, input: AccessibilityNodeInfo, draft: String, screenHeight: Int): String? {
        root ?: return null
        val inputBounds = Rect().also { input.getBoundsInScreen(it) }
        val minTop = (screenHeight * 0.12).toInt()                 // below the title bar
        val maxGap = (screenHeight * 0.55).toInt()                 // ignore text far above the input
        var best: String? = null
        var bestBottom = Int.MIN_VALUE
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
            // closest to the input wins; on the same row, the longer text (the bubble, not its label)
            if (b.bottom > bestBottom || (b.bottom == bestBottom && text.length > (best?.length ?: 0))) {
                best = text
                bestBottom = b.bottom
            }
        }
        return best?.take(300)
    }
}
