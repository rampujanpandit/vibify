package com.vibify.music.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.vibify.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalAudioScanner(private val context: Context) {

    suspend fun scanLocalTracks(): List<Track> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<Track>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.ALBUM_ID
        )

        // Only music tracks longer than 10 seconds to filter out notification tones
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 10000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        try {
            val cursor = context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: "Unknown Title"
                    val artist = it.getString(artistCol) ?: "Unknown Artist"
                    val album = it.getString(albumCol) ?: "Unknown Album"
                    val duration = it.getLong(durationCol)
                    val albumId = it.getLong(albumIdCol)

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )
                    val albumArtUri = try {
                        ContentUris.withAppendedId(
                            Uri.parse("content://media/external/audio/albumart"),
                            albumId
                        ).toString()
                    } catch (e: Exception) {
                        null
                    }

                    tracks.add(
                        Track(
                            id = "local_$id",
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = album,
                            durationMs = duration,
                            mediaUri = contentUri,
                            artworkUri = albumArtUri,
                            isLocal = true
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tracks
    }
}
