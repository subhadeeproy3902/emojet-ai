package io.github.subhadeeproy3902.emojet

import android.content.Context

object Prefs {
    private fun prefs(c: Context) = c.getSharedPreferences("emojet", Context.MODE_PRIVATE)

    fun enabled(c: Context): Boolean = prefs(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, on: Boolean) = prefs(c).edit().putBoolean("enabled", on).apply()

    /** On, not paused, and not switched off for this app. */
    fun activeFor(c: Context, pkg: String?): Boolean =
        enabled(c) && System.currentTimeMillis() >= pausedUntil(c) && (pkg == null || pkg !in disabledApps(c))

    fun pausedUntil(c: Context): Long = prefs(c).getLong("paused_until", 0)
    fun pauseFor(c: Context, millis: Long) = prefs(c).edit().putLong("paused_until", System.currentTimeMillis() + millis).apply()
    fun resume(c: Context) = prefs(c).edit().remove("paused_until").apply()

    fun useReply(c: Context): Boolean = prefs(c).getBoolean("use_reply", true)
    fun setUseReply(c: Context, on: Boolean) = prefs(c).edit().putBoolean("use_reply", on).apply()

    fun disabledApps(c: Context): Set<String> = prefs(c).getStringSet("disabled_apps", emptySet()) ?: emptySet()
    fun disableApp(c: Context, pkg: String) = prefs(c).edit().putStringSet("disabled_apps", disabledApps(c) + pkg).apply()
    fun enableApp(c: Context, pkg: String) = prefs(c).edit().putStringSet("disabled_apps", disabledApps(c) - pkg).apply()
}
