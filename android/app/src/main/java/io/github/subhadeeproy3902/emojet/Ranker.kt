package io.github.subhadeeproy3902.emojet

import android.content.Context
import org.json.JSONObject
import kotlin.math.exp
import kotlin.math.ln

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
    private val isTopic: BooleanArray
    private val tones: List<String>
    private val toneRows: List<List<Int>>
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
        isTopic = BooleanArray(cat.length()) {
            val e = cat.getJSONObject(it)
            e.getString("group") in TOPIC_GROUPS && e.getString("e") !in VIBE
        }
        val index = emojis.withIndex().associate { (i, e) -> e to i }
        val t = JSONObject(assets.open("tones.json").bufferedReader().use { it.readText() })
        val tj = t.getJSONObject("tones")
        tones = tj.keys().asSequence().toList()
        toneRows = tones.map { name ->
            val row = tj.getJSONObject(name).getJSONArray("emojis")
            (0 until row.length()).mapNotNull { index[row.getString(it)] }
        }
        styles = t.getJSONObject("styles").getJSONObject("options").keys().asSequence().toList()
    }

    fun suggest(engine: Engine, draft: String, replyingTo: String?, n: Int = 6): List<String> =
        if (engine.kind == "classifier") classifier(engine, draft, replyingTo, n)
        else moodsToEmojis(engine.decide(request(draft, replyingTo)), n)

    // ---- Emojet-v1 ----

    /** Must match fmt() in the training notebook exactly. */
    fun format(draft: String, replyingTo: String?): String =
        (if (replyingTo.isNullOrBlank()) "" else "replying to: ${replyingTo.trim()}\n") + "my message: ${draft.trim()}"

    /**
     * Same algorithm as scripts/rank_sim.py ("pooled") in the emojet repo:
     * reply blending by draft confidence, tone boost, topic guarantee with a bonus.
     */
    private fun classifier(engine: Engine, draft: String, replyingTo: String?, n: Int): List<String> {
        val ne = emojis.size
        val nt = tones.size
        var s = engine.classify(format(draft, null))
        if (s.size < ne + nt + styles.size) return emptyList()
        if (!replyingTo.isNullOrBlank()) {
            // a confident draft ("Laptop") keeps its own emojis; a vague one ("yes!!") follows the reply
            val m1 = (0 until ne).maxOf { s[it] }
            val conf = 1.0 / (0 until ne).sumOf { exp((s[it] - m1).toDouble()) }
            val w = (0.25 + conf * 1.5).coerceIn(0.25, 0.85).toFloat()
            val s2 = engine.classify(format(draft, replyingTo))
            s = FloatArray(s.size) { w * s[it] + (1 - w) * s2[it] }
        }
        val m = (0 until ne).maxOf { s[it] }
        val z = ln((0 until ne).sumOf { exp((s[it] - m).toDouble()) }).toFloat()
        val lp = FloatArray(ne) { s[it] - m - z }
        val adj = lp.copyOf()
        val toneP = FloatArray(nt) { sigmoid(s[ne + it]) }
        for (t in 0 until nt) {
            if (toneP[t] >= TONE_MIN && tones[t] !in SKIP_TONE_BOOST) for (i in toneRows[t]) adj[i] += TONE_BONUS * toneP[t]
        }
        val best = lp.max()
        (0 until ne).filter { isTopic[it] && lp[it] >= best - TOPIC_WINDOW }
            .sortedByDescending { lp[it] }.take(MAX_TOPIC)
            .forEach { adj[it] += TOPIC_BONUS }
        lastTones = (0 until nt).map { Tone(tones[it], toneP[it]) }.sortedByDescending { it.p }.take(3)
        lastStyle = styles[(0 until styles.size).maxBy { s[ne + nt + it] }]
        return (0 until ne).sortedByDescending { adj[it] }.take(n).map { emojis[it] }
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

    companion object {
        private val TOPIC_GROUPS = setOf("Animals & Nature", "Food & Drink", "Travel & Places", "Activities", "Objects", "Flags")
        private val VIBE = setOf("🎉", "🎊", "✨", "🔥", "💥", "⭐", "🌟", "💫", "⚡", "🌈", "🎈", "🎆", "🎇", "🪩")
        private val SKIP_TONE_BOOST = setOf("neutral", "friendly")
        private const val TOPIC_WINDOW = 4.5f
        private const val TOPIC_BONUS = 1.6f
        private const val MAX_TOPIC = 3
        private const val TONE_MIN = 0.5f
        private const val TONE_BONUS = 0.8f
    }
}
