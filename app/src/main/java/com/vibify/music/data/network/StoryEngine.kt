package com.vibify.music.data.network

import android.net.Uri
import com.vibify.music.data.model.StoryBook
import com.vibify.music.data.model.StoryChapter
import com.vibify.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class StoryEngine {

    suspend fun loadShelf(name: String): List<StoryBook> = withContext(Dispatchers.IO) {
        val queries = queriesFor(name)
        val lists = queries.map { query ->
            async { search(query) }
        }.awaitAll()
        val books = LinkedHashMap<String, StoryBook>()
        lists.flatten().forEach { book -> books.putIfAbsent(book.id, book) }
        books.values.take(40)
    }

    suspend fun loadChapters(book: StoryBook): List<StoryChapter> = withContext(Dispatchers.IO) {
        val body = get(URL("https://archive.org/metadata/${book.id}")) ?: return@withContext emptyList()
        val files = JSONObject(body).optJSONArray("files") ?: return@withContext emptyList()
        val mp3s = buildList {
            for (i in 0 until files.length()) {
                val file = files.getJSONObject(i)
                val name = file.optString("name")
                val format = file.optString("format")
                if (name.endsWith(".mp3", ignoreCase = true) && format.contains("MP3", ignoreCase = true)) {
                    add(file)
                }
            }
        }
        val sections = mp3s.filter { file ->
            val name = file.optString("name")
            !name.contains("_64kb", ignoreCase = true) && !name.contains("_128kb", ignoreCase = true)
        }.ifEmpty {
            mp3s.filter { it.optString("name").contains("_64kb", ignoreCase = true) }
        }
        sections
            .sortedBy { it.optString("name") }
            .take(80)
            .map { file ->
                val name = file.optString("name")
                val encoded = URLEncoder.encode(name, "UTF-8").replace("+", "%20")
                StoryChapter(
                    id = name.hashCode().toUInt().toString(),
                    title = chapterTitle(file.optString("title"), name),
                    audioUrl = "https://archive.org/download/${book.id}/$encoded",
                    durationMs = parseLength(file.optString("length"))
                )
            }
    }

    private fun queriesFor(name: String): List<String> {
        val term = name.trim()
        return when {
            term.equals("Hindi", ignoreCase = true) || term.isBlank() -> listOf(
                "collection:librivoxaudio AND (language:Hindi OR language:hin)",
                "mediatype:audio AND (language:Hindi OR language:hin) AND (creator:Premchand OR creator:\"Munshi Premchand\")",
                "mediatype:audio AND (language:Hindi OR language:hin) AND (title:Panchatantra OR title:Panchtantra OR title:Hitopadesh OR title:Jataka OR title:Birbal)"
            )
            term.equals("Premchand", ignoreCase = true) -> listOf(
                "mediatype:audio AND (creator:Premchand OR creator:\"Munshi Premchand\" OR title:Premchand)"
            )
            term.equals("Panchatantra", ignoreCase = true) -> listOf(
                "mediatype:audio AND (title:Panchatantra OR title:Panchtantra OR creator:\"Vishnu Sharma\")"
            )
            term.equals("Classics", ignoreCase = true) -> listOf(
                "collection:librivoxaudio AND (title:Sherlock OR title:\"Alice's Adventures\" OR title:\"Pride and Prejudice\" OR title:\"Treasure Island\" OR title:\"The Jungle Book\" OR title:\"Arabian Nights\")"
            )
            else -> {
                val safe = term.replace("\"", "").take(60)
                listOf(
                    "mediatype:audio AND (language:Hindi OR language:hin) AND (title:\"$safe\" OR creator:\"$safe\")"
                )
            }
        }
    }

    private fun search(query: String): List<StoryBook> {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = URL(
            "https://archive.org/advancedsearch.php?q=$encoded" +
                "&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=creator" +
                "&sort%5B%5D=downloads+desc&rows=20&page=1&output=json"
        )
        val body = get(url) ?: return emptyList()
        val docs = JSONObject(body).optJSONObject("response")?.optJSONArray("docs") ?: return emptyList()
        return buildList {
            for (i in 0 until docs.length()) {
                val item = docs.getJSONObject(i)
                val id = item.optString("identifier")
                val title = text(item, "title")
                val author = text(item, "creator")
                if (id.isBlank() || title.isBlank() || unsafe("$title $author")) continue
                add(
                    StoryBook(
                        id = id,
                        title = title,
                        author = author,
                        artworkUrl = "https://archive.org/services/img/$id"
                    )
                )
            }
        }
    }

    private fun text(item: JSONObject, key: String): String {
        return when (val value = item.opt(key)) {
            is String -> value
            is JSONArray -> (0 until value.length()).joinToString(", ") { value.optString(it) }
            null -> ""
            else -> value.toString()
        }.trim()
    }

    private fun unsafe(value: String): Boolean {
        val text = value.lowercase()
        return blocked.any { text.contains(it) }
    }

    private fun chapterTitle(given: String, fileName: String): String {
        val raw = given.ifBlank {
            fileName.substringAfterLast('/').substringBeforeLast('.')
        }
        return raw.replace('_', ' ').replace(Regex("\\s+"), " ").trim()
    }

    private fun parseLength(raw: String): Long {
        val text = raw.trim()
        if (text.isBlank()) return 0L
        if (text.contains(":")) {
            val parts = text.split(":").mapNotNull { it.toLongOrNull() }
            val seconds = when (parts.size) {
                3 -> parts[0] * 3600 + parts[1] * 60 + parts[2]
                2 -> parts[0] * 60 + parts[1]
                else -> 0L
            }
            return seconds * 1000L
        }
        return ((text.toDoubleOrNull() ?: 0.0) * 1000).toLong()
    }

    private fun get(url: URL): String? {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 20000
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Vibify/1.0 (Android story player)")
        }
        return try {
            if (connection.responseCode !in 200..299) null
            else connection.inputStream.bufferedReader().readText()
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        val blocked = listOf(
            "chudai",
            "antarvasna",
            "erotic",
            "xxx",
            "porn",
            "nude",
            "incest",
            "bhabhi",
            "sex story",
            "sex-story",
            "adult story"
        )
    }
}

fun StoryChapter.toTrack(book: StoryBook): Track {
    return Track(
        id = "story_$id",
        title = title,
        artist = book.author.ifBlank { "Kahani" },
        album = book.title,
        durationMs = durationMs,
        mediaUri = Uri.parse(audioUrl),
        artworkUri = book.artworkUrl
    )
}
