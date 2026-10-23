package io.github.subhadeeproy3902.emojet

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * In-app updates, never uninstall + reinstall (docs/WORKFLOWS.md §4).
 * Reads the public updates.json from the repo only when the user taps "Check for updates";
 * sends nothing about the user. App updates go through Android's installer (one "Update" tap,
 * same signing key, settings and model kept). Model updates replace the model list, then the
 * normal model download takes over.
 */
class UpdateManager(context: Context) {

    data class Info(
        val appNewer: Boolean, val versionName: String, val notes: String,
        val apkUrl: String?, val apkSha256: String?, val modelsNewer: Boolean,
        /** Build numbers, so a newer build of the same version name is not announced as "1.0.0 is available". */
        val versionCode: Long = 0, val installedVersionCode: Long = 0, val installedVersionName: String = "",
    )

    private val app = context.applicationContext
    private val prefs = app.getSharedPreferences("updates", Context.MODE_PRIVATE)
    private val dm = app.getSystemService(DownloadManager::class.java)
    private val apkFile get() = File(app.getExternalFilesDir(null), "update.apk")

    /** Network call: run on a background thread. */
    fun check(): Info {
        val c = URL(URL_UPDATES).openConnection() as HttpURLConnection
        val json = try {
            c.connectTimeout = 10_000
            c.readTimeout = 15_000
            JSONObject(c.inputStream.bufferedReader().use { it.readText() })
        } finally {
            c.disconnect()
        }
        val a = json.getJSONObject("app")
        val apk = a.optJSONObject("apk")
        val installed = app.packageManager.getPackageInfo(app.packageName, 0).let {
            if (Build.VERSION.SDK_INT >= 28) it.longVersionCode else @Suppress("DEPRECATION") it.versionCode.toLong()
        }
        val remoteModels = json.optJSONObject("models")
        val modelsNewer = remoteModels != null && remoteModels.optInt("version", 0) > ModelManager(app).listVersion
        if (modelsNewer) ModelManager.saveRemoteList(app, remoteModels!!.toString())
        return Info(
            appNewer = a.getLong("versionCode") > installed && apk != null,
            versionName = a.getString("versionName"),
            notes = a.optString("notes"),
            apkUrl = apk?.optString("url"), apkSha256 = apk?.optString("sha256"),
            modelsNewer = modelsNewer,
            versionCode = a.getLong("versionCode"),
            installedVersionCode = installed,
            installedVersionName = app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "",
        )
    }

    fun canInstall(): Boolean = app.packageManager.canRequestPackageInstalls()

    fun unknownSourcesSettings(): Intent =
        Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${app.packageName}"))

    fun downloadApk(info: Info) {
        apkFile.delete()
        val id = dm.enqueue(
            DownloadManager.Request(Uri.parse(info.apkUrl))
                .setTitle("EmoJet ${info.versionName}")
                .setDestinationInExternalFilesDir(app, null, "update.apk")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE),
        )
        prefs.edit().putLong("apk_id", id).putString("apk_sha", info.apkSha256).apply()
    }

    /** null = no APK download; otherwise a short status. Starts the install when the download is verified. */
    fun pollApk(): String? {
        val id = prefs.getLong("apk_id", -1).takeIf { it >= 0 } ?: return null
        dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if (!c.moveToFirst()) return null.also { prefs.edit().remove("apk_id").apply() }
            return when (c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                DownloadManager.STATUS_SUCCESSFUL -> {
                    prefs.edit().remove("apk_id").apply()
                    if (sha256(apkFile) != prefs.getString("apk_sha", "")) "The update file was damaged. Try again."
                    else { install(apkFile); "Confirm the update in the Android dialog." }
                }
                DownloadManager.STATUS_FAILED -> "Update download failed.".also { prefs.edit().remove("apk_id").apply() }
                else -> "Downloading the update…"
            }
        }
    }

    private fun install(file: File) {
        val installer = app.packageManager.packageInstaller
        val sessionId = installer.createSession(PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL))
        installer.openSession(sessionId).use { session ->
            session.openWrite("emojet.apk", 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val pending = PendingIntent.getBroadcast(
                app, sessionId, Intent(app, InstallResultReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pending.intentSender)
        }
    }

    private fun sha256(f: File): String {
        val d = MessageDigest.getInstance("SHA-256")
        f.inputStream().use { input ->
            val buf = ByteArray(1 shl 20)
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                d.update(buf, 0, n)
            }
        }
        return d.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        const val URL_UPDATES = "https://raw.githubusercontent.com/subhadeeproy3902/emojet-ai/main/updates.json"
    }
}

/** Android asks the user to confirm the install; show its dialog. */
class InstallResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -1) == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
            context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
