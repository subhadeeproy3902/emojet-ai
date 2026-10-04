package io.github.subhadeeproy3902.emojet

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import java.util.UUID

/**
 * Runs Laya on the phone: llama-server (shipped as libllama_server.so) as a child process on a
 * random 127.0.0.1 port, protected by a random API key so other apps on the phone can't use it.
 * One instance per app process, loaded once and kept warm.
 */
class Engine private constructor(private val app: Context) {

    private var process: Process? = null
    private var port = 0
    private val apiKey = UUID.randomUUID().toString()

    private val models = ModelManager(app)
    val isRunning: Boolean @Synchronized get() = process?.isAlive == true

    /** Starts the engine if needed and waits until it answers. Call from a background thread. */
    @Synchronized
    fun start(): Boolean {
        if (process?.isAlive == true) return true
        val model = models.installed() ?: return false
        kind = model.kind
        port = ServerSocket(0).use { it.localPort }
        val bin = File(app.applicationInfo.nativeLibraryDir, "libllama_server.so")
        process = ProcessBuilder(
            bin.path, "-m", models.fileOf(model).path,
            "--host", "127.0.0.1", "--port", port.toString(), "--api-key", apiKey,
            "-t", fastCores().toString(),
            "-c", "512", "-b", "512", "-ub", "512", "-np", "1", "--cache-ram", "0", "--no-webui",
            *(if (model.kind == "classifier") arrayOf("--embedding", "--pooling", "rank") else emptyArray()),
        ).redirectErrorStream(true).redirectOutput(File(app.cacheDir, "engine.log")).start()

        val deadline = System.currentTimeMillis() + 30_000
        while (System.currentTimeMillis() < deadline) {
            if (process?.isAlive != true) return false
            if (healthy()) return true
            Thread.sleep(200)
        }
        return false
    }

    @Synchronized
    fun stop() {
        process?.destroy()
        process = null
    }

    /** "decision" (Laya, mood question) or "classifier" (Laya-Emoji); valid after start(). */
    @Volatile var kind: String = "decision"
        private set

    fun decide(request: JSONObject): JSONObject = JSONObject(http("POST", "/v1/systemone", request.toString()))

    /** Laya-Emoji: raw scores for every output (emojis, tones, styles, dimensions), no normalisation. */
    fun classify(text: String): FloatArray {
        val body = JSONObject().put("content", text).put("embd_normalize", -1).toString()
        var v: Any = org.json.JSONArray(http("POST", "/embedding", body)).getJSONObject(0).get("embedding")
        while (v is org.json.JSONArray && v.length() > 0 && v.get(0) is org.json.JSONArray) v = v.getJSONArray(0)
        val arr = v as org.json.JSONArray
        return FloatArray(arr.length()) { arr.getDouble(it).toFloat() }
    }

    fun lastLog(): String = File(app.cacheDir, "engine.log").takeIf { it.isFile }?.readLines()?.takeLast(5)?.joinToString("\n") ?: ""

    private fun healthy(): Boolean = try {
        http("GET", "/health", null).contains("ok")
    } catch (e: Exception) {
        false
    }

    private fun http(method: String, path: String, body: String?): String {
        val c = URL("http://127.0.0.1:$port$path").openConnection() as HttpURLConnection
        try {
            c.requestMethod = method
            c.connectTimeout = 2_000
            c.readTimeout = 20_000
            c.setRequestProperty("Authorization", "Bearer $apiKey")
            if (body != null) {
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.outputStream.use { it.write(body.toByteArray()) }
            }
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }

    /** Number of fast ("big") cores: Phase 1 showed slow cores only slow Laya down. */
    private fun fastCores(): Int {
        val freqs = (0 until Runtime.getRuntime().availableProcessors()).mapNotNull { cpu ->
            runCatching {
                File("/sys/devices/system/cpu/cpu$cpu/cpufreq/cpuinfo_max_freq").readText().trim().toLong()
            }.getOrNull()
        }
        if (freqs.isEmpty()) return 4
        val top = freqs.max()
        return freqs.count { it >= top * 0.75 }.coerceIn(2, 6)
    }

    companion object {
        @Volatile
        private var instance: Engine? = null

        fun get(context: Context): Engine =
            instance ?: synchronized(this) { instance ?: Engine(context.applicationContext).also { instance = it } }
    }
}
