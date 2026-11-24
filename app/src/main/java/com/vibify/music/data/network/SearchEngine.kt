package com.vibify.music.data.network

import android.net.Uri
import com.vibify.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SearchEngine {

    /**
     * Fetches YouTube-style instant keyword suggestions as the user types letters
     */
    suspend fun getSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()

        val suggestions = mutableListOf<String>()
        var connection: HttpURLConnection? = null

        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = URL("https://suggestqueries.google.com/complete/search?client=firefox&ds=yt&q=$encodedQuery")

            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 3000
                readTimeout = 3000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonArray = JSONArray(response)
                if (jsonArray.length() > 1) {
                    val items = jsonArray.getJSONArray(1)
                    for (i in 0 until items.length().coerceAtMost(8)) {
                        suggestions.add(items.getString(i))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }

        suggestions
    }

    /**
     * Searches online music database for full tracks, artist, album, audio stream and high-res cover
     */
    suspend fun searchOnlineMusic(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.trim().isBlank()) return@withContext emptyList()

        val tracks = mutableListOf<Track>()
        var connection: HttpURLConnection? = null

        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = URL("https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=30")

            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "GET"
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val jsonObject = JSONObject(response)
                val results = jsonObject.optJSONArray("results") ?: JSONArray()

                for (i in 0 until results.length()) {
                    val item = results.getJSONObject(i)
                    val previewUrl = item.optString("previewUrl")

                    // Only include tracks that have a valid playable audio stream preview
                    if (previewUrl.isNotBlank()) {
                        val trackId = item.optLong("trackId", System.currentTimeMillis() + i)
                        val trackName = item.optString("trackName", "Unknown Song")
                        val artistName = item.optString("artistName", "Unknown Artist")
                        val collectionName = item.optString("collectionName", "")
                        val duration = item.optLong("trackTimeMillis", 30000L)
                        val rawArtwork = item.optString("artworkUrl100")
                        val highResArtwork = rawArtwork.replace("100x100bb", "600x600bb")

                        tracks.add(
                            Track(
                                id = "online_$trackId",
                                title = trackName,
                                artist = artistName,
                                album = collectionName,
                                durationMs = duration,
                                mediaUri = Uri.parse(previewUrl),
                                artworkUri = highResArtwork.ifBlank { null },
                                isLocal = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }

        tracks
    }
}
