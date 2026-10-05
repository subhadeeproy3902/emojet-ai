package io.github.subhadeeproy3902.emojet

import android.content.Context
import org.json.JSONObject
import kotlin.math.exp

/**
 * Turns the whole draft (+ the message being replied to) into 6 emojis.
 * - "decision" model (Laya): asks the mood question, blends the top moods' emoji rows
 *   (same algorithm as suggest() in scripts/test-prompts.py).
 * - "classifier" model (Emojet-v1): one pass scores all 1,923 emojis + 46 tones + 4 styles;
 *   label order = shared/emoji-catalog.json then shared/tones.json (same as the training notebook).
 */
class Ranker(context: Context) {

    data class Tone(val name: String, val p: Float)

    private val question: String
    private val moods = LinkedHashMap<String, List<String>>()
    private val emojis: List<String>
    private val tones: List<String>
    private val styles: List<String>

    @Volatile var lastTones: List<Tone> = emptyList()
        private set
    @Volatile var lastStyle: String? = null
        private set

    init {
        val assets = context.assets
        val m = JSONObject(assets.open("moods.json").bufferedReader().use { it.readText() })
        question = m.getString("question")
        val mm = m.getJSONObject("moods")
        for (key in mm.keys()) {
            val row = mm.getJSONObject(key).getJSONArray("emojis")
            moods[key] = List(row.length()) { row.getString(it) }
        }
        val cat = JSONObject(assets.open("emoji-catalog.json").bufferedReader().use { it.readText() }).getJSONArray("emojis")
        emojis = List(cat.length()) { cat.getJSONObject(it).getString("e") }
        val t = JSONObject(assets.open("tones.json").bufferedReader().use { it.readText() })
        tones = t.getJSONObject("tones").keys().asSequence().toList()
        styles = t.getJSONObject("styles").getJSONObject("options").keys().asSequence().toList()
    }

    fun suggest(engine: Engine, draft: String, replyingTo: String?, n: Int = 6): List<String> =
        if (engine.kind == "classifier") classifier(engine, draft, replyingTo, n)
        else moodsToEmojis(engine.decide(request(draft, replyingTo)), n)

    // ---- Emojet-v1 ----

    /** Must match fmt() in the training notebook exactly. */
    fun format(draft: String, replyingTo: String?): String =
        (if (replyingTo.isNullOrBlank()) "" else "replying to: ${replyingTo.trim()}\n") + "my message: ${draft.trim()}"

    private fun classifier(engine: Engine, draft: String, replyingTo: String?, n: Int): List<String> {
        val s = engine.classify(format(draft, replyingTo))
        val ne = emojis.size
        val nt = tones.size
        if (s.size < ne + nt + styles.size) return emptyList()
        lastTones = (0 until nt).map { Tone(tones[it], sigmoid(s[ne + it])) }.sortedByDescending { it.p }.take(3)
        lastStyle = styles[(0 until styles.size).maxBy { s[ne + nt + it] }]
        return (0 until ne).sortedByDescending { s[it] }.take(n).map { emojis[it] }
    }

    private fun sigmoid(x: Float) = (1.0 / (1.0 + exp(-x.toDouble()))).toFloat()

    // ---- Laya (mood question) ----

    fun request(draft: String, replyingTo: String?): JSONObject {
        val state: Any = if (replyingTo.isNullOrBlank()) draft
        else JSONObject().put("replying_to", replyingTo).put("my_draft", draft)
        val criteria = JSONObject()
        for (key in moods.keys) criteria.put(key, JSONObject.NULL)
        val mood = JSONObject().put("type", "choice").put("instructions", question).put("criteria", criteria)
        return JSONObject().put("state", state).put("questions", JSONObject().put("mood", mood))
    }

    fun moodsToEmojis(answer: JSONObject, n: Int = 6, topMoods: Int = 3): List<String> {
        val probs = answer.getJSONObject("answers").getJSONObject("mood").getJSONObject("probabilities")
        val ranked = probs.keys().asSequence()
            .map { it to probs.getDouble(it) }
            .sortedByDescending { it.second }
            .take(topMoods)
        val scores = LinkedHashMap<String, Double>()
        for ((mood, p) in ranked) {
            moods[mood]?.forEachIndexed { rank, emoji ->
                scores[emoji] = (scores[emoji] ?: 0.0) + p / (1 + rank * 0.35)
            }
        }
        return scores.entries.sortedByDescending { it.value }.take(n).map { it.key }
    }
}
