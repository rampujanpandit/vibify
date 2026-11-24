package com.vibify.music.data.model

import android.net.Uri

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String = "",
    val durationMs: Long = 0L,
    val mediaUri: Uri,
    val artworkUri: String? = null,
    val isLocal: Boolean = false,
    val isLiked: Boolean = false,
    /** Saavn encrypted media key. Used at play time to fetch the full song instead of the 30s preview. */
    val mediaKey: String = ""
) {
    val isSpokenWord: Boolean
        get() = id.startsWith("podcast_") || id.startsWith("story_")

    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val remainingSeconds = totalSeconds % 60
            return "%d:%02d".format(minutes, remainingSeconds)
        }
}

data class Category(
    val id: String,
    val name: String,
    val backgroundColorHex: Long,
    val imageUrl: String? = null
)

data class HomeSection(
    val id: String,
    val title: String,
    val tracks: List<Track>
)

data class HomeFeed(
    val quickAccess: List<Track> = emptyList(),
    val trending: List<Track> = emptyList(),
    val madeForYou: List<Track> = emptyList(),
    val localSongs: List<Track> = emptyList(),
    val chartSections: List<HomeSection> = emptyList(),
    val autoPlaylists: List<AutoPlaylist> = emptyList()
)
