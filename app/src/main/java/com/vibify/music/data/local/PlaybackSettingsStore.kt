package com.vibify.music.data.local

import android.content.Context
import android.net.Uri
import com.vibify.music.data.model.Track
import org.json.JSONArray
import org.json.JSONObject

data class SavedPlayback(
    val tracks: List<Track>,
    val index: Int,
    val positionMs: Long
)

class PlaybackSettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("vibify_playback", Context.MODE_PRIVATE)

    var spokenSpeed: Float
        get() {
            val saved = prefs.getFloat(KEY_SPEED, 1f)
            return if (saved in SPEEDS) saved else 1f
        }
        set(value) {
            prefs.edit().putFloat(KEY_SPEED, value).apply()
        }

    var skipSeconds: Int
        get() {
            val saved = prefs.getInt(KEY_SKIP, 15)
            return if (saved in SKIPS) saved else 15
        }
        set(value) {
            prefs.edit().putInt(KEY_SKIP, value).apply()
        }

    var resumeOnOpen: Boolean
        get() = prefs.getBoolean(KEY_RESUME, true)
        set(value) {
            prefs.edit().putBoolean(KEY_RESUME, value).apply()
        }

    fun saveSession(tracks: List<Track>, index: Int, positionMs: Long) {
        if (tracks.isEmpty() || index !in tracks.indices) return
        val array = JSONArray()
        tracks.take(40).forEach { track ->
            array.put(JSONObject().apply {
                put("id", track.id)
                put("title", track.title)
                put("artist", track.artist)
                put("album", track.album)
                put("durationMs", track.durationMs)
                put("mediaUri", track.mediaUri.toString())
                put("artworkUri", track.artworkUri ?: "")
                put("isLocal", track.isLocal)
                put("mediaKey", track.mediaKey)
            })
        }
        val root = JSONObject()
            .put("index", index.coerceAtMost(array.length() - 1))
            .put("positionMs", positionMs.coerceAtLeast(0L))
            .put("tracks", array)
        prefs.edit().putString(KEY_SESSION, root.toString()).apply()
    }

    fun loadSession(): SavedPlayback? {
        val raw = prefs.getString(KEY_SESSION, null) ?: return null
        return try {
            val root = JSONObject(raw)
            val array = root.optJSONArray("tracks") ?: return null
            val tracks = buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val uri = obj.optString("mediaUri")
                    if (uri.isBlank()) continue
                    add(
                        Track(
                            id = obj.optString("id"),
                            title = obj.optString("title", "Unknown"),
                            artist = obj.optString("artist", "Unknown Artist"),
                            album = obj.optString("album", ""),
                            durationMs = obj.optLong("durationMs", 0L),
                            mediaUri = Uri.parse(uri),
                            artworkUri = obj.optString("artworkUri").ifBlank { null },
                            isLocal = obj.optBoolean("isLocal", false),
                            mediaKey = obj.optString("mediaKey")
                        )
                    )
                }
            }
            if (tracks.isEmpty()) return null
            val index = root.optInt("index", 0).coerceIn(0, tracks.lastIndex)
            SavedPlayback(tracks, index, root.optLong("positionMs", 0L))
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val KEY_SPEED = "spoken_speed"
        private const val KEY_SKIP = "skip_seconds"
        private const val KEY_RESUME = "resume_on_open"
        private const val KEY_SESSION = "last_session"
        val SPEEDS = listOf(1f, 1.25f, 1.5f, 2f)
        val SKIPS = listOf(10, 15, 30)
    }
}
