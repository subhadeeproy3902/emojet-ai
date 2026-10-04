package io.github.subhadeeproy3902.emojet

import android.content.Context

object Prefs {
    private fun prefs(c: Context) = c.getSharedPreferences("emojet", Context.MODE_PRIVATE)

    fun enabled(c: Context): Boolean = prefs(c).getBoolean("enabled", true)

    fun setEnabled(c: Context, on: Boolean) = prefs(c).edit().putBoolean("enabled", on).apply()
}
