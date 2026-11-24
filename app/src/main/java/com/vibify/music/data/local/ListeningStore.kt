package com.vibify.music.data.local

import android.content.Context
import com.vibify.music.data.model.ListeningAction
import com.vibify.music.data.model.ListeningEvent
import com.vibify.music.data.model.TasteProfile
import com.vibify.music.data.model.Track
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.exp

/**
 * Stores listening signals and builds a Spotify-style taste profile
 * using time-decayed weights (recent listens matter more).
 */
class ListeningStore(context: Context) {

    private val prefs = context.getSharedPreferences("vibify_listening", Context.MODE_PRIVATE)

    fun record(track: Track, action: ListeningAction, positionMs: Long = 0L) {
        val events = loadEvents().toMutableList()
        events.add(
            0,
            ListeningEvent(
                trackId = track.id,
                title = track.title,
                artist = track.artist,
                action = action,
                positionMs = positionMs
            )
        )
        // Keep last 500 events
        saveEvents(events.take(500))
    }

    fun recordLike(track: Track) = record(track, ListeningAction.LIKE)

    fun buildTasteProfile(now: Long = System.currentTimeMillis()): TasteProfile {
        val events = loadEvents()
        if (events.isEmpty()) return TasteProfile()

        val artistScores = mutableMapOf<String, Double>()
        val trackScores = mutableMapOf<String, Double>()
        val recentIds = LinkedHashSet<String>()

        for (event in events) {
            val ageDays = ((now - event.timestamp).coerceAtLeast(0L) / 86_400_000.0)
            val decay = exp(-ageDays / 14.0) // ~2 week half-life feel
            val weight = actionWeight(event.action) * decay

            val artistKey = event.artist.trim().lowercase()
            if (artistKey.isNotBlank()) {
                artistScores[artistKey] = (artistScores[artistKey] ?: 0.0) + weight
            }
            trackScores[event.trackId] = (trackScores[event.trackId] ?: 0.0) + weight

            if (recentIds.size < 40 &&
                (event.action == ListeningAction.PLAY ||
                    event.action == ListeningAction.COMPLETE ||
                    event.action == ListeningAction.LIKE)
            ) {
                recentIds.add(event.trackId)
            }
        }

        val topArtists = artistScores.entries
            .sortedByDescending { it.value }
            .take(12)
            .map { it.key }

        return TasteProfile(
            artistScores = artistScores,
            trackScores = trackScores,
            recentTrackIds = recentIds.toList(),
            topArtists = topArtists
        )
    }

    fun playedTrackIds(): Set<String> {
        return loadEvents()
            .filter {
                it.action == ListeningAction.PLAY ||
                    it.action == ListeningAction.COMPLETE ||
                    it.action == ListeningAction.LIKE
            }
            .map { it.trackId }
            .toSet()
    }

    private fun actionWeight(action: ListeningAction): Double = when (action) {
        ListeningAction.LIKE -> 5.0
        ListeningAction.COMPLETE -> 3.0
        ListeningAction.PLAY -> 1.5
        ListeningAction.QUEUE -> 1.0
        ListeningAction.SKIP -> -3.0
    }

    private fun loadEvents(): List<ListeningEvent> {
        val json = prefs.getString("events_json", null) ?: return emptyList()
        return try {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val actionName = obj.optString("action")
                    val action = runCatching { ListeningAction.valueOf(actionName) }.getOrNull()
                        ?: continue
                    add(
                        ListeningEvent(
                            trackId = obj.optString("trackId"),
                            title = obj.optString("title"),
                            artist = obj.optString("artist"),
                            action = action,
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            positionMs = obj.optLong("positionMs", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveEvents(events: List<ListeningEvent>) {
        val array = JSONArray()
        events.forEach { event ->
            array.put(
                JSONObject().apply {
                    put("trackId", event.trackId)
                    put("title", event.title)
                    put("artist", event.artist)
                    put("action", event.action.name)
                    put("timestamp", event.timestamp)
                    put("positionMs", event.positionMs)
                }
            )
        }
        prefs.edit().putString("events_json", array.toString()).apply()
    }
}
