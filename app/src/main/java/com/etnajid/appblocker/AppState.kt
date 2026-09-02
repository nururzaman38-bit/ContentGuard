package com.etnajid.appblocker

import android.content.Context

object AppState {
    val features = listOf("Adult site block", "Mixed-app scanning", "Suggestion click-block", "Reels video warning", "Focus schedule", "SafeSearch enforcement", "Private browsing block", "Screen time tracking")
    private const val PREF = "protection"
    fun enabled(c: Context, key: String) = c.getSharedPreferences(PREF, 0).getBoolean(key, false)
    fun set(c: Context, key: String, value: Boolean) { c.getSharedPreferences(PREF, 0).edit().putBoolean(key, value).apply() }
    fun panic(c: Context) = enabled(c, "panic")
    fun count(c: Context) = c.getSharedPreferences("activity_log", 0).getStringSet("events", emptySet())!!.size
    fun streak(c: Context) = if (count(c) == 0) 0 else 1
    fun log(c: Context, event: String, app: String = "system") {
        val p = c.getSharedPreferences("activity_log", 0); val old = p.getStringSet("events", emptySet())!!.toMutableSet()
        old.add("${System.currentTimeMillis()}|$app|$event"); p.edit().putStringSet("events", old).apply()
    }
}
