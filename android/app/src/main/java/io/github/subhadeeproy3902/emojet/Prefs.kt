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

    /** Skin tone for people emojis: 0 = default yellow, 1..5 = light .. dark (kept until the user picks another). */
    fun skinTone(c: Context): Int = prefs(c).getInt("skin_tone", 0)
    fun setSkinTone(c: Context, tone: Int) = prefs(c).edit().putInt("skin_tone", tone).apply()

    /** Restrict the one-time model download to Wi-Fi. Off by default: mobile data works too, and being
     *  told to "wait for Wi-Fi" when you have 5G is just an app getting in the way. */
    fun wifiOnly(c: Context): Boolean = prefs(c).getBoolean("wifi_only", false)
    fun setWifiOnly(c: Context, on: Boolean) = prefs(c).edit().putBoolean("wifi_only", on).apply()

    /** First run finished (OnboardingActivity). */
    fun onboarded(c: Context): Boolean = prefs(c).getBoolean("onboarded", false)
    fun setOnboarded(c: Context, v: Boolean) = prefs(c).edit().putBoolean("onboarded", v).apply()

    /** How often the notification permission was asked. Android stops showing the dialog after two refusals,
     *  so the third time we send the user to the system settings page instead of a dialog that never appears. */
    fun notifAsks(c: Context): Int = prefs(c).getInt("notif_asks", 0)
    fun countNotifAsk(c: Context) = prefs(c).edit().putInt("notif_asks", notifAsks(c) + 1).apply()

    fun disabledApps(c: Context): Set<String> = prefs(c).getStringSet("disabled_apps", emptySet()) ?: emptySet()
    fun disableApp(c: Context, pkg: String) = prefs(c).edit().putStringSet("disabled_apps", disabledApps(c) + pkg).apply()
    fun enableApp(c: Context, pkg: String) = prefs(c).edit().putStringSet("disabled_apps", disabledApps(c) - pkg).apply()

    /** Where the user dragged the strip in this app: x = distance from the right edge, dy = height above the text box (px). */
    fun stripOffset(c: Context, pkg: String): Pair<Int, Int>? =
        prefs(c).getString("strip_$pkg", null)?.split(',')?.let { (x, dy) -> x.toInt() to dy.toInt() }
    fun setStripOffset(c: Context, pkg: String, x: Int, dy: Int) = prefs(c).edit().putString("strip_$pkg", "$x,$dy").apply()
    fun resetStripOffsets(c: Context) = prefs(c).edit().apply {
        prefs(c).all.keys.filter { it.startsWith("strip_") }.forEach { remove(it) }
    }.apply()
}
