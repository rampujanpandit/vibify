package com.vibify.music.data.network

import android.net.Uri
import android.util.Base64
import android.util.Log
import com.vibify.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import com.vibify.music.data.model.HomeSection
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class FullMusicEngine {

    private val desKey = "38346591".toByteArray(Charsets.UTF_8)
    private val keySpec = SecretKeySpec(desKey, "DES")

    /**
     * Decrypts the CDN audio stream link to get full-length (160kbps/320kbps) audio
     */
    fun decryptMediaUrl(encryptedUrl: String): String? {
        return try {
            val cipher = Cipher.getInstance("DES/ECB/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec)
            val decodedBytes = Base64.decode(encryptedUrl.trim(), Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            val decryptedUrl = String(decryptedBytes, Charsets.UTF_8).trim()
                .replace("http://", "https://")
            // Signed links break if the filename is rewritten. Unsigned previews are
            // mapped off the 30-second preview host onto the full 96kbps file.
            if (isSignedStream(decryptedUrl)) decryptedUrl else promotePreviewToFull(decryptedUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Live home charts: India Superhits + a few more editorial playlists.
     */
    suspend fun fetchHomeChartSections(maxCharts: Int = 2): List<HomeSection> = withContext(Dispatchers.IO) {
        val charts = fetchCharts().take(maxCharts)
        coroutineScope {
            charts.map { chart ->
                async {
                    val songs = fetchPlaylistSongs(chart.id, limit = 20)
                    if (songs.isEmpty()) null
                    else HomeSection(id = chart.id, title = chart.title, tracks = songs)
                }
            }.awaitAll().filterNotNull()
        }
    }

    private data class ChartInfo(val id: String, val title: String)

    private fun fetchCharts(): List<ChartInfo> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://www.jiosaavn.com/api.php?__call=content.getCharts&api_version=4&_format=json&_marker=0")
            connection = openConnection(url)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return emptyList()

            val response = connection.inputStream.bufferedReader().readText()
            val array = JSONArray(response)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val id = item.optString("id")
                    val title = cleanHtmlEntities(item.optString("title", "Chart"))
                    if (id.isNotBlank()) add(ChartInfo(id, title))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    private fun fetchPlaylistSongs(listId: String, limit: Int = 25): List<Track> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(
                "https://www.jiosaavn.com/api.php?__call=playlist.getDetails&listid=$listId&api_version=4&_format=json&_marker=0"
            )
            connection = openConnection(url)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return emptyList()

            val response = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(response)
            val list = json.optJSONArray("list") ?: return emptyList()

            buildList {
                for (i in 0 until minOf(list.length(), limit)) {
                    parseV4Song(list.getJSONObject(i))?.let { add(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseV4Song(item: JSONObject): Track? {
        val moreInfo = item.optJSONObject("more_info") ?: JSONObject()
        val encryptedUrl = moreInfo.optString("encrypted_media_url")
        val streamUrl = if (encryptedUrl.isNotBlank()) decryptMediaUrl(encryptedUrl) else null
        if (streamUrl.isNullOrBlank()) return null

        val id = item.optString("id").ifBlank { return null }
        val title = cleanHtmlEntities(item.optString("title", "Unknown Title"))

        val artistFromMap = moreInfo.optJSONObject("artistMap")
            ?.optJSONArray("primary_artists")
            ?.let { artists ->
                buildList {
                    for (i in 0 until artists.length()) {
                        val name = artists.optJSONObject(i)?.optString("name").orEmpty()
                        if (name.isNotBlank()) add(cleanHtmlEntities(name))
                    }
                }.joinToString(", ")
            }
            .orEmpty()

        val artist = artistFromMap.ifBlank {
            cleanHtmlEntities(item.optString("subtitle")).ifBlank { "Unknown Artist" }
        }

        val album = cleanHtmlEntities(moreInfo.optString("album", ""))
        val durationSec = moreInfo.optString("duration").toLongOrNull()
            ?: moreInfo.optLong("duration", 0L)
        val image = item.optString("image", "")
            .replace("http://", "https://")
            .replace("150x150", "500x500")

        return Track(
            id = "chart_$id",
            title = title,
            artist = artist,
            album = album,
            durationMs = durationSec * 1000L,
            mediaUri = Uri.parse(streamUrl),
            artworkUri = image.ifBlank { null },
            isLocal = false,
            mediaKey = encryptedUrl
        )
    }

    private fun openConnection(url: URL): HttpURLConnection {
        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 7000
            readTimeout = 8000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            useCaches = false
            setRequestProperty("Cache-Control", "no-cache, no-store")
        }
    }

    /**
     * Searches full-length songs with no 30-second preview limit.
     * Fetches multiple pages for broader results, especially for artist/singer name searches.
     */
    suspend fun searchFullSongs(query: String): List<Track> = withContext(Dispatchers.IO) {
        if (query.trim().isBlank()) return@withContext emptyList()

        val tracks = mutableListOf<Track>()
        val seenIds = mutableSetOf<String>()

        // Fetch up to 2 pages (40 results each = max 80 songs) for broader coverage
        for (page in 1..2) {
            val pageTracks = fetchPage(query, page)
            for (track in pageTracks) {
                if (seenIds.add(track.id)) {
                    tracks.add(track)
                }
            }
            // If first page returned fewer than 20 results, no point fetching page 2
            if (page == 1 && pageTracks.size < 20) break
        }

        tracks
    }

    /**
     * Fetches a single page of search results from JioSaavn API
     */
    private fun fetchPage(query: String, page: Int): List<Track> {
        val tracks = mutableListOf<Track>()
        var connection: HttpURLConnection? = null

        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val urlString = "https://www.jiosaavn.com/api.php?__call=search.getResults&_format=json&_marker=0&cc=in&p=$page&n=40&q=$encodedQuery"
            val url = URL(urlString)

            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                requestMethod = "GET"
                setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                // Disable HTTP caching to always get fresh results
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache, no-store")
                setRequestProperty("Pragma", "no-cache")
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val results = json.optJSONArray("results")

                if (results != null) {
                    for (i in 0 until results.length()) {
                        val item = results.getJSONObject(i)
                        val encryptedUrl = item.optString("encrypted_media_url")
                        val mediaPreviewUrl = item.optString("media_preview_url")

                        val streamUrl = if (encryptedUrl.isNotBlank()) {
                            decryptMediaUrl(encryptedUrl) ?: upgradePreviewUrl(mediaPreviewUrl)
                        } else {
                            upgradePreviewUrl(mediaPreviewUrl)
                        }

                        if (!streamUrl.isNullOrBlank()) {
                            val secureUrl = streamUrl
                                .replace("http://", "https://")
                                .trim()

                            val id = item.optString("id", "saavn_${page}_$i")
                            val rawTitle = item.optString("song", item.optString("title", "Unknown Title"))
                            val title = cleanHtmlEntities(rawTitle)

                            val rawArtist = item.optString("primary_artists").ifBlank {
                                item.optString("singers").ifBlank {
                                    item.optString("music", "Unknown Artist")
                                }
                            }
                            val artist = cleanHtmlEntities(rawArtist)

                            val rawAlbum = item.optString("album", "")
                            val album = cleanHtmlEntities(rawAlbum)

                            val durationSec = item.optString("duration").toLongOrNull() ?: item.optLong("duration", 180L)
                            val rawImage = item.optString("image", "")
                            val highResImage = rawImage
                                .replace("http://", "https://")
                                .replace("150x150", "500x500")

                            tracks.add(
                                Track(
                                    id = "full_$id",
                                    title = title,
                                    artist = if (artist.isBlank()) "Unknown Artist" else artist,
                                    album = album,
                                    durationMs = durationSec * 1000L,
                                    mediaUri = Uri.parse(secureUrl),
                                    artworkUri = highResImage.ifBlank { null },
                                    isLocal = false,
                                    mediaKey = encryptedUrl
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            connection?.disconnect()
        }

        return tracks
    }

    private fun cleanHtmlEntities(str: String): String {
        return str
            .replace("&quot;", "\"")
            .replace("&amp;", "&")
            .replace("&#039;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    private fun upgradePreviewUrl(previewUrl: String): String? {
        if (previewUrl.isBlank()) return null
        val https = previewUrl.replace("http://", "https://").trim()
        return if (isSignedStream(https)) https else promotePreviewToFull(https)
    }

    private fun isSignedStream(url: String): Boolean {
        val query = url.substringAfter('?', "")
        if (query.isBlank()) return false
        return query.contains("Signature", ignoreCase = true) ||
            query.contains("Expires", ignoreCase = true) ||
            query.contains("token", ignoreCase = true) ||
            query.contains("Policy", ignoreCase = true)
    }

    /** Preview filenames map to the full 96kbps file. Keep the original host. */
    private fun promotePreviewToFull(url: String): String {
        return url
            .replace("_96_p.mp4", "_96.mp4")
            .replace("_96_p.m4a", "_96.m4a")
            .replace("_p.mp4", ".mp4")
            .replace("_p.m4a", ".m4a")
    }

    /**
     * Preview CDN links stop at 30 seconds. Ask Saavn for a fresh full-length stream
     * for each online track before playback.
     */
    suspend fun attachFullStreams(tracks: List<Track>): List<Track> = withContext(Dispatchers.IO) {
        if (tracks.isEmpty()) return@withContext tracks
        val gate = Semaphore(4)
        coroutineScope {
            tracks.map { track ->
                async {
                    gate.withPermit { resolveFullTrack(track) }
                }
            }.awaitAll()
        }
    }

    private fun resolveFullTrack(track: Track): Track {
        if (track.isLocal) return track
        val key = track.mediaKey.ifBlank { encryptedKeyForTrack(track.id) }
        val authUrl = key?.let { requestAuthUrl(it) }
        if (!authUrl.isNullOrBlank()) {
            Log.d("FullMusicEngine", "Full stream ready for ${track.title}")
            return track.copy(mediaUri = Uri.parse(authUrl), mediaKey = key ?: track.mediaKey)
        }
        Log.w("FullMusicEngine", "Full stream missing for ${track.title}, using saved link")
        val upgraded = promotePreviewToFull(track.mediaUri.toString())
        return if (upgraded == track.mediaUri.toString()) track else track.copy(mediaUri = Uri.parse(upgraded))
    }

    private fun encryptedKeyForTrack(trackId: String): String? {
        val pid = when {
            trackId.startsWith("full_") -> trackId.removePrefix("full_")
            trackId.startsWith("chart_") -> trackId.removePrefix("chart_")
            else -> return null
        }
        if (pid.isBlank()) return null
        var connection: HttpURLConnection? = null
        return try {
            val encodedId = URLEncoder.encode(pid, "UTF-8")
            val url = URL(
                "https://www.jiosaavn.com/api.php?__call=song.getDetails&pids=$encodedId&api_version=4&_format=json&_marker=0"
            )
            connection = openConnection(url)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val json = JSONObject(stripJsonPrefix(connection.inputStream.bufferedReader().readText()))
            val song = json.optJSONArray("songs")?.optJSONObject(0) ?: json
            val more = song.optJSONObject("more_info")
            val key = more?.optString("encrypted_media_url").orEmpty()
                .ifBlank { song.optString("encrypted_media_url") }
            key.ifBlank { null }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun requestAuthUrl(encryptedMediaUrl: String): String? {
        val bitrates = listOf("128", "320", "64")
        for (bitrate in bitrates) {
            val auth = requestAuthUrl(encryptedMediaUrl, bitrate)
            if (!auth.isNullOrBlank()) return auth
        }
        return null
    }

    private fun requestAuthUrl(encryptedMediaUrl: String, bitrate: String): String? {
        var connection: HttpURLConnection? = null
        return try {
            val encoded = URLEncoder.encode(encryptedMediaUrl.trim(), "UTF-8")
            val url = URL(
                "https://www.jiosaavn.com/api.php?__call=song.generateAuthToken" +
                    "&url=$encoded&bitrate=$bitrate&api_version=4&_format=json&ctx=web6dot0&_marker=0"
            )
            connection = openConnection(url)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val body = stripJsonPrefix(connection.inputStream.bufferedReader().readText())
            val json = JSONObject(body)
            val auth = json.optString("auth_url").ifBlank { json.optString("url") }
            auth.replace("http://", "https://").trim().takeIf { it.startsWith("https://") }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun stripJsonPrefix(raw: String): String {
        val trimmed = raw.trim()
        val start = trimmed.indexOf('{')
        val arrayStart = trimmed.indexOf('[')
        val cut = when {
            start < 0 -> arrayStart
            arrayStart < 0 -> start
            else -> minOf(start, arrayStart)
        }
        return if (cut <= 0) trimmed else trimmed.substring(cut)
    }
}
