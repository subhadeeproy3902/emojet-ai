package io.github.subhadeeproy3902.emojet

import android.content.Context
import org.json.JSONObject

/**
 * Builds the Laya request from the whole draft (+ the message being replied to) and turns Laya's
 * mood probabilities into 6 emojis. Same algorithm as suggest() in scripts/test-prompts.py.
 * Will be replaced by Laya-Emoji's direct emoji scores (docs/TRAINING.md).
 */
class Ranker(context: Context) {

    private val question: String
    private val moods = LinkedHashMap<String, List<String>>()

    init {
        val json = JSONObject(context.assets.open("moods.json").bufferedReader().use { it.readText() })
        question = json.getString("question")
        val m = json.getJSONObject("moods")
        for (key in m.keys()) {
            val row = m.getJSONObject(key).getJSONArray("emojis")
            moods[key] = List(row.length()) { row.getString(it) }
        }
    }

    fun request(draft: String, replyingTo: String?): JSONObject {
        val state: Any = if (replyingTo.isNullOrBlank()) draft
        else JSONObject().put("replying_to", replyingTo).put("my_draft", draft)
        val criteria = JSONObject()
        for (key in moods.keys) criteria.put(key, JSONObject.NULL)
        val mood = JSONObject().put("type", "choice").put("instructions", question).put("criteria", criteria)
        return JSONObject().put("state", state).put("questions", JSONObject().put("mood", mood))
    }

    fun suggest(answer: JSONObject, n: Int = 6, topMoods: Int = 3): List<String> {
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
