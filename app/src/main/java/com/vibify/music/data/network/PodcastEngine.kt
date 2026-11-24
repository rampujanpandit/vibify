package com.vibify.music.data.network

import android.net.Uri
import com.vibify.music.data.model.PodcastEpisode
import com.vibify.music.data.model.PodcastShow
import com.vibify.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class PodcastEngine {

    suspend fun searchShows(query: String): List<PodcastShow> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        val url = URL(
            "https://itunes.apple.com/search?term=$encoded&media=podcast&entity=podcast&limit=25&country=in"
        )
        val body = get(url) ?: return@withContext emptyList()
        val results = JSONObject(body).optJSONArray("results") ?: return@withContext emptyList()
        buildList {
            for (i in 0 until results.length()) {
                val item = results.getJSONObject(i)
                val feed = item.optString("feedUrl")
                if (feed.isBlank()) continue
                val art = item.optString("artworkUrl600")
                    .ifBlank { item.optString("artworkUrl100") }
                    .replace("100x100bb", "600x600bb")
                add(
                    PodcastShow(
                        id = item.optLong("collectionId").toString(),
                        title = item.optString("collectionName", "Podcast"),
                        author = item.optString("artistName", ""),
                        artworkUrl = art.ifBlank { null },
                        feedUrl = feed,
                        genre = item.optString("primaryGenreName", "")
                    )
                )
            }
        }
    }

    suspend fun loadEpisodes(show: PodcastShow): List<PodcastEpisode> = withContext(Dispatchers.IO) {
        val connection = open(URL(show.feedUrl))
        try {
            if (connection.responseCode !in 200..299) return@withContext emptyList()
            parseRss(connection.inputStream.bufferedReader().readText(), show)
        } catch (_: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private fun parseRss(xml: String, show: PodcastShow): List<PodcastEpisode> {
        val parser = XmlPullParserFactory.newInstance().newPullParser()
        parser.setInput(xml.reader())
        val episodes = mutableListOf<PodcastEpisode>()
        var inItem = false
        var title = ""
        var description = ""
        var audioUrl = ""
        var image = show.artworkUrl
        var guid = ""
        var published = ""
        var durationMs = 0L

        fun flush() {
            if (title.isNotBlank() && audioUrl.isNotBlank()) {
                val id = (guid.ifBlank { audioUrl }).hashCode().toUInt().toString()
                episodes.add(
                    PodcastEpisode(
                        id = id,
                        title = clean(title),
                        description = clean(description).take(280),
                        audioUrl = audioUrl,
                        artworkUrl = image,
                        publishedLabel = published.take(22),
                        durationMs = durationMs
                    )
                )
            }
        }

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT && episodes.size < 40) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name.substringAfter(':')
                    if (name == "item") {
                        inItem = true
                        title = ""
                        description = ""
                        audioUrl = ""
                        image = show.artworkUrl
                        guid = ""
                        published = ""
                        durationMs = 0L
                    } else if (inItem) {
                        when (name) {
                            "title" -> title = parser.nextText()
                            "description", "summary" -> if (description.isBlank()) description = parser.nextText()
                            "guid" -> guid = parser.nextText()
                            "pubDate" -> published = parser.nextText()
                            "duration" -> durationMs = parseDuration(parser.nextText())
                            "enclosure", "content" -> {
                                val url = parser.getAttributeValue(null, "url").orEmpty()
                                val type = parser.getAttributeValue(null, "type").orEmpty()
                                if (audioUrl.isBlank() && isAudioLink(url, type, name == "enclosure")) {
                                    audioUrl = url
                                }
                            }
                            "image" -> {
                                val href = parser.getAttributeValue(null, "href")
                                if (!href.isNullOrBlank()) image = href
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.substringAfter(':') == "item") {
                        flush()
                        inItem = false
                    }
                }
            }
            event = parser.next()
        }
        return episodes
    }

    private fun isAudioLink(url: String, type: String, allowBlankType: Boolean): Boolean {
        if (url.isBlank() || type.startsWith("image") || type.startsWith("video")) return false
        if (type.startsWith("audio")) return true
        val lower = url.lowercase()
        if (lower.contains(".mp3") || lower.contains(".m4a") || lower.contains(".aac") || lower.contains(".ogg")) {
            return true
        }
        return allowBlankType && type.isBlank()
    }

    private fun parseDuration(raw: String): Long {
        val text = raw.trim()
        if (text.all { it.isDigit() }) return text.toLongOrNull()?.times(1000L) ?: 0L
        val parts = text.split(":").mapNotNull { it.toLongOrNull() }
        val seconds = when (parts.size) {
            3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
            2 -> parts[0] * 60 + parts[1]
            1 -> parts[0]
            else -> 0L
        }
        return seconds * 1000L
    }

    private fun clean(value: String): String {
        return value
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun get(url: URL): String? {
        val connection = open(url)
        return try {
            if (connection.responseCode !in 200..299) null
            else connection.inputStream.bufferedReader().readText()
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: URL): HttpURLConnection {
        return (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 15000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Vibify/1.0 (Android podcast player)")
        }
    }
}

fun PodcastEpisode.toTrack(show: PodcastShow): Track {
    return Track(
        id = "podcast_$id",
        title = title,
        artist = show.author.ifBlank { show.title },
        album = show.title,
        durationMs = durationMs,
        mediaUri = Uri.parse(audioUrl),
        artworkUri = artworkUrl ?: show.artworkUrl
    )
}
