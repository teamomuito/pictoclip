package app.pictoclip

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Clip(val id: Long, val text: String, val time: Long)

/** Tiny persistent clip list (newest first), backed by SharedPreferences. */
class ClipStore(context: Context) {
    private val prefs = context.getSharedPreferences("pictoclip", Context.MODE_PRIVATE)
    private val clips = mutableListOf<Clip>()

    init {
        runCatching {
            val arr = JSONArray(prefs.getString(KEY, "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                clips += Clip(o.getLong("id"), o.getString("text"), o.getLong("time"))
            }
        }
    }

    fun all(): List<Clip> = clips.toList()

    /** Saves [text] at the top. An identical older clip is replaced instead of duplicated. */
    fun add(text: String): Clip? {
        if (text.isBlank()) return null
        clips.removeAll { it.text == text }
        val now = System.currentTimeMillis()
        val clip = Clip(now, text, now)
        clips.add(0, clip)
        while (clips.size > MAX) clips.removeAt(clips.lastIndex)
        save()
        return clip
    }

    fun remove(id: Long) {
        if (clips.removeAll { it.id == id }) save()
    }

    fun clear() {
        clips.clear()
        save()
    }

    private fun save() {
        val arr = JSONArray()
        clips.forEach {
            arr.put(JSONObject().put("id", it.id).put("text", it.text).put("time", it.time))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        const val MAX = 200
        private const val KEY = "clips"
    }
}
