package io.github.subhadeeproy3902.emojet

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

/**
 * The Laya model file: one-time download (Android's DownloadManager: resumable, notification,
 * Wi-Fi only unless allowed), sha256 check, fallback to the next source, or import from a file.
 * Sources and checksums come from shared/models.json.
 */
class ModelManager(context: Context) {

    /** kind: "decision" = Laya answering the mood question; "classifier" = Emojet-v1 scoring every emoji at once. */
    data class Model(val id: String, val file: String, val size: Long, val sha256: String, val label: String, val urls: List<String>, val kind: String)

    sealed class State {
        object Missing : State()
        data class Downloading(val label: String, val done: Long, val total: Long, val waiting: String?) : State()
        object Verifying : State()
        data class Failed(val reason: String) : State()
        data class Ready(val model: Model) : State()
    }

    private val app = context.applicationContext
    private val dir: File get() = app.getExternalFilesDir(null)!!
    private val dm = app.getSystemService(DownloadManager::class.java)
    private val prefs = app.getSharedPreferences("models", Context.MODE_PRIVATE)

    /** Bundled shared/models.json, or a newer list saved by "Check for updates". */
    private val listJson: JSONObject = run {
        val bundled = JSONObject(app.assets.open("models.json").bufferedReader().use { it.readText() })
        val remote = File(app.filesDir, REMOTE_LIST).takeIf { it.isFile }?.let { runCatching { JSONObject(it.readText()) }.getOrNull() }
        if (remote != null && remote.optInt("version") > bundled.optInt("version")) remote else bundled
    }
    val listVersion: Int get() = listJson.optInt("version")

    val models: List<Model> = listJson.getJSONArray("models").let { arr ->
        List(arr.length()) { i ->
            val m = arr.getJSONObject(i)
            val urls = m.getJSONArray("urls")
            Model(m.getString("id"), m.getString("file"), m.getLong("size"), m.getString("sha256"),
                m.getString("label"), List(urls.length()) { urls.getString(it) }, m.optString("kind", "decision"))
        }
    }

    /** The best model file that is fully present (size check; sha256 was checked when it arrived). */
    fun installed(): Model? = models.firstOrNull { File(dir, it.file).let { f -> f.isFile && f.length() == it.size } }

    fun fileOf(m: Model) = File(dir, m.file)

    /** A better model (earlier in the list) than the installed one, e.g. after "Check for updates". */
    fun upgradeAvailable(): Model? {
        val current = installed() ?: return null
        return models.firstOrNull { it.urls.isNotEmpty() }?.takeIf { models.indexOf(it) < models.indexOf(current) }
    }

    fun startDownload(allowMobileData: Boolean) {
        prefs.edit().putBoolean("mobile", allowMobileData).remove("error").apply()
        val first = models.indexOfFirst { it.urls.isNotEmpty() }
        if (first < 0) prefs.edit().putString("error", "No download source configured. Import the model file instead.").apply()
        else enqueue(first, 0)
    }

    /** First launch: start the download by itself (Wi-Fi only) unless it already ran or a model is present. */
    fun autoStartOnce(): Boolean {
        if (prefs.getBoolean("auto_started", false) || installed() != null || prefs.contains("id")) return false
        prefs.edit().putBoolean("auto_started", true).apply()
        startDownload(allowMobileData = false)
        return true
    }

    /** "Use mobile data now": restart the waiting download with mobile data allowed. */
    fun allowMobileDataNow() {
        cancel()
        startDownload(allowMobileData = true)
    }

    /** Next model that can be downloaded after index [from], or -1. Unpublished models have no URLs. */
    private fun nextDownloadable(from: Int) = (from + 1 until models.size).firstOrNull { models[it].urls.isNotEmpty() } ?: -1

    private fun enqueue(model: Int, url: Int) {
        val m = models[model]
        File(dir, m.file + ".part").delete()
        val request = DownloadManager.Request(Uri.parse(m.urls[url]))
            .setTitle("EmoJet: ${m.label}")
            .setDescription("One-time download of the on-device Laya model")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(app, null, m.file + ".part")
            .setAllowedOverMetered(prefs.getBoolean("mobile", false))
            .setAllowedOverRoaming(false)
        prefs.edit().putLong("id", dm.enqueue(request)).putInt("model", model).putInt("url", url).apply()
    }

    /** Current state; call every second or so while a screen shows progress.
     *  blocking = verify a finished download on this thread (used by the download-done receiver). */
    fun poll(blocking: Boolean = false): State {
        installed()?.let { if (!prefs.contains("id")) return State.Ready(it) }
        prefs.getString("error", null)?.let { return State.Failed(it) }
        if (System.currentTimeMillis() - prefs.getLong("verifying_at", 0) < 60_000) return State.Verifying
        val id = prefs.getLong("id", -1)
        if (id < 0) return State.Missing
        val modelIdx = prefs.getInt("model", 0)
        val urlIdx = prefs.getInt("url", 0)
        val m = models[modelIdx]
        dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if (!c.moveToFirst()) {
                prefs.edit().remove("id").apply()
                return State.Missing
            }
            val status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val done = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)).takeIf { it > 0 } ?: m.size
            val reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
            return when (status) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    if (blocking) verify(m) else verifyInBackground(m)
                    if (blocking) installed()?.let { State.Ready(it) } ?: State.Failed(prefs.getString("error", null) ?: "Check failed.")
                    else State.Verifying
                }
                DownloadManager.STATUS_FAILED -> {
                    // Try the next URL, then the next model (e.g. our file not published yet -> official file).
                    when {
                        urlIdx + 1 < m.urls.size -> enqueue(modelIdx, urlIdx + 1)
                        nextDownloadable(modelIdx) >= 0 -> enqueue(nextDownloadable(modelIdx), 0)
                        else -> prefs.edit().remove("id").putString("error", "Download failed (code $reason). Check the connection and try again.").apply()
                    }
                    poll()
                }
                DownloadManager.STATUS_PAUSED -> State.Downloading(m.label, done, total, when (reason) {
                    DownloadManager.PAUSED_QUEUED_FOR_WIFI -> "waiting for Wi-Fi"
                    DownloadManager.PAUSED_WAITING_FOR_NETWORK -> "waiting for a connection"
                    else -> "paused"
                })
                else -> State.Downloading(m.label, done, total, null)
            }
        }
    }

    fun cancel() {
        prefs.getLong("id", -1).takeIf { it >= 0 }?.let { dm.remove(it) }
        prefs.edit().remove("id").remove("error").remove("verifying_at").apply()
    }

    private fun verifyInBackground(m: Model) {
        Thread { verify(m) }.start()
    }

    /** Checksum the finished download and move it into place. One at a time per process (screen + receiver). */
    private fun verify(m: Model) = synchronized(VERIFY_LOCK) {
        if (!prefs.contains("id")) return@synchronized            // someone else finished it
        prefs.edit().putLong("verifying_at", System.currentTimeMillis()).apply()
        val part = File(dir, m.file + ".part")
        val ok = part.isFile && part.length() == m.size && sha256(part.inputStream()) == m.sha256
        val editor = prefs.edit().remove("verifying_at").remove("id")
        if (ok) {
            part.renameTo(fileOf(m))
        } else {
            part.delete()
            editor.putString("error", "The downloaded file was damaged (checksum mismatch). Try again.")
        }
        editor.apply()
    }

    /** Import a .gguf picked by the user; accepted only if it is byte-identical to a known model. */
    fun import(uri: Uri): String {
        val tmp = File(dir, "import.part")
        val digest = MessageDigest.getInstance("SHA-256")
        app.contentResolver.openInputStream(uri)?.use { input ->
            tmp.outputStream().use { out ->
                val buf = ByteArray(1 shl 20)
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    digest.update(buf, 0, n)
                    out.write(buf, 0, n)
                }
            }
        } ?: return "Couldn't open that file."
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        val m = models.firstOrNull { it.sha256 == hash }
        if (m == null) {
            tmp.delete()
            return "That file isn't a known Laya model (checksum doesn't match)."
        }
        tmp.renameTo(fileOf(m))
        return "Imported ${m.label}."
    }

    private fun sha256(input: java.io.InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        input.use {
            val buf = ByteArray(1 shl 20)
            while (true) {
                val n = it.read(buf)
                if (n < 0) break
                digest.update(buf, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val REMOTE_LIST = "models-remote.json"
        private val VERIFY_LOCK = Any()

        fun saveRemoteList(context: Context, json: String) =
            File(context.applicationContext.filesDir, REMOTE_LIST).writeText(json)
    }
}
