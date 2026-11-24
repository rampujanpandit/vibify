package com.vibify.music.player

import com.vibify.music.data.model.Track

enum class RepeatMode {
    OFF,   // Do not repeat after queue ends
    ALL,   // Repeat entire playlist/queue
    ONE    // Repeat current single track continuously
}

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffle: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isLoading: Boolean = false,
    val queue: List<Track> = emptyList(),
    val currentQueueIndex: Int = -1,
    val playbackSpeed: Float = 1f
) {
    val isRepeat: Boolean
        get() = repeatMode != RepeatMode.OFF

    val progress: Float
        get() = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f
}
