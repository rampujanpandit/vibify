package com.vibify.music.data.local

import android.content.Context

class PodcastProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("podcast_progress", Context.MODE_PRIVATE)

    fun get(trackId: String): Long = prefs.getLong(trackId, 0L)

    fun save(trackId: String, positionMs: Long) {
        prefs.edit().putLong(trackId, positionMs).apply()
    }

    fun clear(trackId: String) {
        prefs.edit().remove(trackId).apply()
    }
}
