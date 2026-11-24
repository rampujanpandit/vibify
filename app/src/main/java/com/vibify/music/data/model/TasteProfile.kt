package com.vibify.music.data.model

enum class ListeningAction {
    PLAY,
    COMPLETE,
    LIKE,
    SKIP,
    QUEUE
}

data class ListeningEvent(
    val trackId: String,
    val title: String,
    val artist: String,
    val action: ListeningAction,
    val timestamp: Long = System.currentTimeMillis(),
    val positionMs: Long = 0L
)

data class TasteProfile(
    val artistScores: Map<String, Double> = emptyMap(),
    val trackScores: Map<String, Double> = emptyMap(),
    val recentTrackIds: List<String> = emptyList(),
    val topArtists: List<String> = emptyList()
) {
    fun artistAffinity(artist: String): Double {
        if (artist.isBlank()) return 0.0
        val key = artist.trim().lowercase()
        // Exact or partial match (multi-artist credits)
        return artistScores.entries
            .filter { key.contains(it.key) || it.key.contains(key) || key == it.key }
            .maxOfOrNull { it.value }
            ?: 0.0
    }
}

data class AutoPlaylist(
    val id: String,
    val title: String,
    val subtitle: String,
    val tracks: List<Track>,
    val accentColor: Long = 0xFF1DB954
)
