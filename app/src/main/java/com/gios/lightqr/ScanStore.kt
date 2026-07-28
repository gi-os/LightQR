package com.gios.lightqr

import android.content.Context
import android.util.Patterns
import org.json.JSONArray
import org.json.JSONObject

/** One decoded QR result. */
data class ScannedItem(
    val text: String,
    val time: Long
) {
    val isLink: Boolean
        get() = Patterns.WEB_URL.matcher(text.trim()).matches() ||
                text.trim().startsWith("http://") ||
                text.trim().startsWith("https://")

    /** Normalize bare domains (example.com) into openable URLs. */
    val url: String
        get() {
            val t = text.trim()
            return if (t.startsWith("http://") || t.startsWith("https://")) t else "https://$t"
        }
}

/** Tiny SharedPreferences-backed history. No cloud, no accounts — stays on device. */
class ScanStore(context: Context) {
    private val prefs = context.getSharedPreferences("lightqr_history", Context.MODE_PRIVATE)
    private val key = "items"
    private val max = 200

    fun load(): List<ScannedItem> {
        val raw = prefs.getString(key, "[]") ?: "[]"
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            ScannedItem(o.getString("text"), o.getLong("time"))
        }
    }

    /** Add newest first; skip if identical to the most recent entry (debounces re-scans). */
    fun add(text: String): List<ScannedItem> {
        val current = load().toMutableList()
        if (current.firstOrNull()?.text == text) return current
        current.add(0, ScannedItem(text, System.currentTimeMillis()))
        val trimmed = current.take(max)
        save(trimmed)
        return trimmed
    }

    fun clear() { prefs.edit().remove(key).apply() }

    private fun save(items: List<ScannedItem>) {
        val arr = JSONArray()
        items.forEach { arr.put(JSONObject().put("text", it.text).put("time", it.time)) }
        prefs.edit().putString(key, arr.toString()).apply()
    }
}
