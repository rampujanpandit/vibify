package com.vibify.music.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.vibify.music.data.local.ListeningStore
import com.vibify.music.data.local.LocalAudioScanner
import com.vibify.music.data.model.AutoPlaylist
import com.vibify.music.data.model.Category
import com.vibify.music.data.model.HomeFeed
import com.vibify.music.data.model.HomeSection
import com.vibify.music.data.model.ListeningAction
import com.vibify.music.data.model.Track
import com.vibify.music.data.network.FullMusicEngine
import com.vibify.music.data.network.SearchEngine
import com.vibify.music.domain.PlaylistEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class MusicRepository(private val context: Context) {

    private val localAudioScanner = LocalAudioScanner(context)
    private val searchEngine = SearchEngine()
    private val fullMusicEngine = FullMusicEngine()
    private val listeningStore = ListeningStore(context)
    private val playlistEngine = PlaylistEngine(fullMusicEngine)
    private val prefs: SharedPreferences = context.getSharedPreferences("vibify_prefs", Context.MODE_PRIVATE)

    private val _allTracks = MutableStateFlow<List<Track>>(emptyList())
    val allTracks: StateFlow<List<Track>> = _allTracks.asStateFlow()

    private val _homeFeed = MutableStateFlow(HomeFeed())
    val homeFeed: StateFlow<HomeFeed> = _homeFeed.asStateFlow()

    private val _likedTracks = MutableStateFlow<List<Track>>(loadLikedTracks())
    val likedTracks: StateFlow<List<Track>> = _likedTracks.asStateFlow()

    private val _likedTrackIds = MutableStateFlow(_likedTracks.value.map { it.id }.toSet())
    val likedTrackIds: StateFlow<Set<String>> = _likedTrackIds.asStateFlow()

    init {
        restoreHomeCache()
        if (_homeFeed.value.trending.isEmpty() && _likedTracks.value.isNotEmpty()) {
            _homeFeed.value = _homeFeed.value.copy(
                quickAccess = _likedTracks.value.take(6),
                madeForYou = _likedTracks.value
            )
        }
    }

    private val fallbackCategories = listOf(
        Category("cat_1", "Bollywood Hits", 0xFF27856A),
        Category("cat_2", "Pop", 0xFFE13300),
        Category("cat_3", "Hip-Hop", 0xFFBA5D07),
        Category("cat_4", "Lo-Fi Beats", 0xFF8D67AB),
        Category("cat_5", "Punjabi Vibes", 0xFFE8115B),
        Category("cat_6", "Chill & Relax", 0xFF148A08),
        Category("cat_7", "Rock & Metal", 0xFFE91429),
        Category("cat_8", "Electronic / EDM", 0xFF0D73EC)
    )

    /** Kept for older call sites; prefer [browseCategories]. */
    val categories: List<Category>
        get() = browseCategories()

    private var searchPlaylistsCache: List<AutoPlaylist> = emptyList()

    private val categoryColors = listOf(
        0xFF27856AL, 0xFFE13300L, 0xFFBA5D07L, 0xFF8D67ABL,
        0xFFE8115BL, 0xFF148A08L, 0xFFE91429L, 0xFF0D73ECL,
        0xFF1DB954L, 0xFF509BF5L, 0xFFAF2896L, 0xFFFF4632L
    )

    fun recordListening(track: Track, action: ListeningAction, positionMs: Long = 0L) {
        listeningStore.record(track, action, positionMs)
    }

    fun getAutoPlaylist(id: String): AutoPlaylist? {
        return _homeFeed.value.autoPlaylists.firstOrNull { it.id == id }
            ?: searchPlaylistsCache.firstOrNull { it.id == id }
    }

    /** Live browse tiles from charts + top artists (fallback if feed empty). */
    fun browseCategories(): List<Category> {
        val feed = _homeFeed.value
        val built = mutableListOf<Category>()

        feed.chartSections.forEachIndexed { index, section ->
            val title = section.title.trim()
            if (title.isNotBlank()) {
                built.add(
                    Category(
                        id = "chart_$index",
                        name = title,
                        backgroundColorHex = categoryColors[index % categoryColors.size],
                        imageUrl = section.tracks.firstOrNull()?.artworkUri
                    )
                )
            }
        }

        topArtistNames().take(6).forEachIndexed { index, artist ->
            if (built.none { it.name.equals(artist, ignoreCase = true) }) {
                built.add(
                    Category(
                        id = "artist_cat_$index",
                        name = artist,
                        backgroundColorHex = categoryColors[(index + 3) % categoryColors.size],
                        imageUrl = feed.trending.firstOrNull {
                            primaryArtist(it.artist).equals(artist, ignoreCase = true)
                        }?.artworkUri
                    )
                )
            }
        }

        if (feed.trending.isNotEmpty() && built.none { it.name.contains("Trending", true) }) {
            built.add(
                0,
                Category(
                    id = "trending_now",
                    name = "Trending Now",
                    backgroundColorHex = 0xFF1DB954,
                    imageUrl = feed.trending.firstOrNull()?.artworkUri
                )
            )
        }

        return built.take(8).ifEmpty { fallbackCategories }
    }

    /** Popular chips from recent searches + taste + trending artists. No hardcoded names. */
    fun popularSearchTags(): List<String> {
        val tags = linkedSetOf<String>()
        tags.addAll(loadRecentSearches())
        tags.addAll(listeningStore.buildTasteProfile().topArtists.take(6))
        tags.addAll(topArtistNames().take(8))
        return tags.filter { it.length in 2..40 }.take(10)
    }

    fun rememberSearchQuery(query: String) {
        val q = query.trim()
        if (q.length < 2) return
        val updated = (listOf(q) + loadRecentSearches().filterNot { it.equals(q, true) })
            .take(12)
        prefs.edit().putString("recent_searches_json", JSONArray(updated).toString()).apply()
    }

    /**
     * Builds Spotify-style singer / search mix bundles from results.
     * e.g. "This is Arijit Singh", "Arijit Singh Radio", "{query} Mix"
     */
    fun buildSearchPlaylists(query: String, results: List<Track>): List<AutoPlaylist> {
        if (results.isEmpty()) {
            searchPlaylistsCache = emptyList()
            return emptyList()
        }

        val q = query.trim()
        val accents = listOf(0xFF1DB954L, 0xFF509BF5L, 0xFFAF2896L, 0xFFE13300L, 0xFF8D67ABL)
        val playlists = mutableListOf<AutoPlaylist>()

        val grouped = results
            .groupBy { primaryArtist(it.artist) }
            .mapValues { (_, tracks) -> tracks.distinctBy { it.id } }
            .filter { it.key.isNotBlank() && it.value.isNotEmpty() }

        val matchedArtists = grouped.entries
            .filter { (artist, tracks) ->
                tracks.size >= 2 && artistMatchesQuery(artist, q)
            }
            .sortedByDescending { it.value.size }
            .take(3)

        // If query looks like a singer but exact match failed, use the most common artist
        val artistEntries = if (matchedArtists.isNotEmpty()) {
            matchedArtists
        } else {
            grouped.entries
                .sortedByDescending { it.value.size }
                .filter { it.value.size >= 3 }
                .take(1)
        }

        artistEntries.forEachIndexed { index, (artist, tracks) ->
            playlists.add(
                AutoPlaylist(
                    id = "search_artist_${artist.lowercase().hashCode()}",
                    title = "This is $artist",
                    subtitle = "${tracks.size} songs · Artist mix",
                    tracks = tracks,
                    accentColor = accents[index % accents.size]
                )
            )
            if (tracks.size >= 4) {
                playlists.add(
                    AutoPlaylist(
                        id = "search_radio_${artist.lowercase().hashCode()}",
                        title = "$artist Radio",
                        subtitle = "Based on $artist",
                        tracks = tracks.shuffled().take(tracks.size.coerceAtMost(30)),
                        accentColor = accents[(index + 2) % accents.size]
                    )
                )
            }
        }

        if (results.size >= 3) {
            playlists.add(
                AutoPlaylist(
                    id = "search_mix_${q.lowercase().hashCode()}",
                    title = "$q Mix",
                    subtitle = "Playlist from your search",
                    tracks = results.distinctBy { it.id }.take(40),
                    accentColor = 0xFF0D73EC
                )
            )
        }

        searchPlaylistsCache = playlists.distinctBy { it.id }
        return searchPlaylistsCache
    }

    fun clearSearchPlaylists() {
        searchPlaylistsCache = emptyList()
    }

    private fun topArtistNames(): List<String> {
        val feed = _homeFeed.value
        return (feed.trending + feed.madeForYou + _likedTracks.value)
            .map { primaryArtist(it.artist) }
            .filter { it.isNotBlank() && !it.equals("Unknown Artist", true) }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }
    }

    private fun loadRecentSearches(): List<String> {
        val raw = prefs.getString("recent_searches_json", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i).trim()
                    if (s.isNotBlank()) add(s)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun primaryArtist(raw: String): String {
        return raw.split(",", "&", " feat.", " feat ", " ft.", " ft ", " x ", "/")
            .firstOrNull()
            ?.trim()
            .orEmpty()
    }

    private fun artistMatchesQuery(artist: String, query: String): Boolean {
        val a = artist.lowercase().trim()
        val q = query.lowercase().trim()
        if (a.isBlank() || q.isBlank()) return false
        if (a.contains(q) || q.contains(a)) return true
        val aTokens = a.split(Regex("\\s+")).filter { it.length > 2 }
        val qTokens = q.split(Regex("\\s+")).filter { it.length > 2 }
        if (qTokens.isEmpty()) return false
        val hits = qTokens.count { qt -> aTokens.any { at -> at.startsWith(qt) || qt.startsWith(at) } }
        return hits >= (qTokens.size + 1) / 2
    }

    /**
     * Loads live charts + local songs, then builds Spotify-style auto playlists
     * from the user's taste profile (likes / plays / skips).
     */
    suspend fun loadHomeFeed() = coroutineScope {
        val liked = _likedTracks.value
        if (_homeFeed.value.trending.isEmpty() && liked.isNotEmpty()) {
            _homeFeed.value = _homeFeed.value.copy(
                quickAccess = liked.take(6),
                madeForYou = liked
            )
        }

        val localDeferred = async { localAudioScanner.scanLocalTracks() }
        val chartsDeferred = async {
            try {
                fullMusicEngine.fetchHomeChartSections(maxCharts = 2)
            } catch (_: Exception) {
                emptyList()
            }
        }

        val localTracks = localDeferred.await()
        publishHome(
            localTracks = localTracks,
            chartSections = _homeFeed.value.chartSections,
            trending = _homeFeed.value.trending,
            autoPlaylists = _homeFeed.value.autoPlaylists,
            madeForYou = _homeFeed.value.madeForYou.ifEmpty { liked }
        )

        val chartSections = chartsDeferred.await()
        val trending = chartSections.firstOrNull()?.tracks
            ?: _homeFeed.value.trending
        val secondaryCharts = if (chartSections.size > 1) {
            chartSections.drop(1)
        } else {
            _homeFeed.value.chartSections
        }

        if (chartSections.isNotEmpty()) {
            publishHome(
                localTracks = localTracks,
                chartSections = secondaryCharts,
                trending = trending,
                autoPlaylists = _homeFeed.value.autoPlaylists,
                madeForYou = _homeFeed.value.madeForYou.ifEmpty { liked.ifEmpty { trending } }
            )
        }

        val taste = listeningStore.buildTasteProfile()
        val playedIds = listeningStore.playedTrackIds()
        val bundle = try {
            playlistEngine.generate(
                taste = taste,
                likedTracks = liked,
                trending = trending,
                localSongs = localTracks,
                chartPools = secondaryCharts.map { it.tracks },
                playedIds = playedIds
            )
        } catch (_: Exception) {
            null
        }

        val autoPlaylists = buildList {
            bundle?.let {
                add(it.madeForYou)
                add(it.discover)
                addAll(it.dailyMixes)
                it.becauseYouLiked?.let { because -> add(because) }
            }
        }.filter { it.tracks.isNotEmpty() }.ifEmpty { _homeFeed.value.autoPlaylists }

        val madeForYouTracks = bundle?.madeForYou?.tracks
            ?: liked.ifEmpty { trending }

        publishHome(
            localTracks = localTracks,
            chartSections = secondaryCharts,
            trending = trending,
            autoPlaylists = autoPlaylists,
            madeForYou = madeForYouTracks
        )
        saveHomeCache(_homeFeed.value)
    }

    private fun publishHome(
        localTracks: List<Track>,
        chartSections: List<HomeSection>,
        trending: List<Track>,
        autoPlaylists: List<AutoPlaylist>,
        madeForYou: List<Track>
    ) {
        val liked = _likedTracks.value
        val quickAccess = buildList {
            addAll(liked.take(2))
            addAll(localTracks.take(3))
            addAll(madeForYou.take(4))
            addAll(trending.take(6))
        }.distinctBy { it.id }.take(6)

        _allTracks.value = (trending + localTracks + liked + madeForYou).distinctBy { it.id }
        _homeFeed.value = HomeFeed(
            quickAccess = quickAccess.ifEmpty { trending.take(6) },
            trending = trending,
            madeForYou = madeForYou,
            localSongs = localTracks,
            chartSections = chartSections,
            autoPlaylists = autoPlaylists
        )
    }

    suspend fun refreshTracks() {
        val localTracks = localAudioScanner.scanLocalTracks()
        val online = _homeFeed.value.trending
        _allTracks.value = (online + localTracks + _likedTracks.value).distinctBy { it.id }
        _homeFeed.value = _homeFeed.value.copy(localSongs = localTracks)
    }

    suspend fun getSearchSuggestions(query: String): List<String> {
        return searchEngine.getSuggestions(query)
    }

    suspend fun searchSongs(query: String): List<Track> {
        if (query.isBlank()) return _allTracks.value

        val localMatches = _allTracks.value.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
        }

        val fullMatches = fullMusicEngine.searchFullSongs(query)
        val onlineMatches = fullMatches

        val existingIds = localMatches.map { it.id }.toSet()
        return localMatches + onlineMatches.filter { !existingIds.contains(it.id) }
    }

    suspend fun withFullStreams(tracks: List<Track>): List<Track> {
        return fullMusicEngine.attachFullStreams(tracks)
    }

    fun toggleLike(track: Track) {
        val current = _likedTracks.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.id == track.id }
        if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        } else {
            current.add(0, track.copy(isLiked = true))
            listeningStore.recordLike(track)
        }
        _likedTracks.value = current
        _likedTrackIds.value = current.map { it.id }.toSet()
        saveLikedTracks(current)

        val feed = _homeFeed.value
        _homeFeed.value = feed.copy(
            madeForYou = if (current.isNotEmpty()) {
                (current + feed.madeForYou).distinctBy { it.id }
            } else {
                feed.trending.take(12)
            },
            quickAccess = buildList {
                addAll(current.take(2))
                addAll(feed.localSongs.take(3))
                addAll(feed.trending.take(6))
            }.distinctBy { it.id }.take(6)
        )
    }

    fun toggleLike(trackId: String) {
        val fromLiked = _likedTracks.value.firstOrNull { it.id == trackId }
        val fromAll = _allTracks.value.firstOrNull { it.id == trackId }
        val fromFeed = listOf(
            _homeFeed.value.trending,
            _homeFeed.value.madeForYou,
            _homeFeed.value.localSongs,
            _homeFeed.value.quickAccess
        ).flatten().firstOrNull { it.id == trackId }

        val track = fromLiked ?: fromAll ?: fromFeed
        if (track != null) {
            toggleLike(track)
        } else {
            val current = _likedTracks.value.filterNot { it.id == trackId }
            _likedTracks.value = current
            _likedTrackIds.value = current.map { it.id }.toSet()
            saveLikedTracks(current)
        }
    }

    fun isTrackLiked(trackId: String): Boolean {
        return _likedTrackIds.value.contains(trackId)
    }

    fun getLikedTracks(): List<Track> = _likedTracks.value

    fun clearLikedTracks() {
        _likedTracks.value = emptyList()
        _likedTrackIds.value = emptySet()
        saveLikedTracks(emptyList())
        val feed = _homeFeed.value
        _homeFeed.value = feed.copy(
            madeForYou = feed.trending.take(12),
            quickAccess = buildList {
                addAll(feed.localSongs.take(3))
                addAll(feed.trending.take(6))
            }.distinctBy { it.id }.take(6)
        )
    }

    private fun loadLikedTracks(): List<Track> {
        val json = prefs.getString("liked_tracks_json", null)
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val uri = obj.optString("mediaUri")
                    if (uri.isBlank()) continue
                    add(
                        Track(
                            id = obj.optString("id"),
                            title = obj.optString("title", "Unknown"),
                            artist = obj.optString("artist", "Unknown Artist"),
                            album = obj.optString("album", ""),
                            durationMs = obj.optLong("durationMs", 0L),
                            mediaUri = Uri.parse(uri),
                            artworkUri = obj.optString("artworkUri").ifBlank { null },
                            isLocal = obj.optBoolean("isLocal", false),
                            isLiked = true,
                            mediaKey = obj.optString("mediaKey")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveLikedTracks(tracks: List<Track>) {
        val array = JSONArray()
        tracks.forEach { track ->
            array.put(
                JSONObject().apply {
                    put("id", track.id)
                    put("title", track.title)
                    put("artist", track.artist)
                    put("album", track.album)
                    put("durationMs", track.durationMs)
                    put("mediaUri", track.mediaUri.toString())
                    put("artworkUri", track.artworkUri ?: "")
                    put("isLocal", track.isLocal)
                    put("mediaKey", track.mediaKey)
                }
            )
        }
        prefs.edit()
            .putString("liked_tracks_json", array.toString())
            .putString("liked_track_ids", tracks.joinToString(",") { it.id })
            .apply()
    }

    private fun restoreHomeCache() {
        val raw = prefs.getString("home_feed_cache", null) ?: return
        try {
            val root = JSONObject(raw)
            val trending = decodeTracks(root.optJSONArray("trending")).take(24)
            val quick = decodeTracks(root.optJSONArray("quickAccess")).take(6)
            val local = decodeTracks(root.optJSONArray("localSongs")).take(20)
            val made = decodeTracks(root.optJSONArray("madeForYou")).take(20)
            val charts = root.optJSONArray("chartSections")?.let { array ->
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val tracks = decodeTracks(obj.optJSONArray("tracks")).take(16)
                        if (tracks.isEmpty()) continue
                        add(
                            HomeSection(
                                id = obj.optString("id", "chart_$i"),
                                title = obj.optString("title", "Chart"),
                                tracks = tracks
                            )
                        )
                    }
                }
            }.orEmpty()
            val playlists = root.optJSONArray("autoPlaylists")?.let { array ->
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val tracks = decodeTracks(obj.optJSONArray("tracks")).take(16)
                        if (tracks.isEmpty()) continue
                        add(
                            AutoPlaylist(
                                id = obj.optString("id", "cached_$i"),
                                title = obj.optString("title", "Playlist"),
                                subtitle = obj.optString("subtitle", ""),
                                tracks = tracks,
                                accentColor = obj.optLong("accentColor", 0xFF1DB954)
                            )
                        )
                    }
                }
            }.orEmpty()
            if (trending.isEmpty() && quick.isEmpty() && local.isEmpty()) return
            _homeFeed.value = HomeFeed(
                quickAccess = quick.ifEmpty { trending.take(6) },
                trending = trending,
                madeForYou = made,
                localSongs = local,
                chartSections = charts,
                autoPlaylists = playlists
            )
        } catch (_: Exception) {
            // Ignore a bad cache and load live data.
        }
    }

    private fun saveHomeCache(feed: HomeFeed) {
        if (feed.trending.isEmpty() && feed.quickAccess.isEmpty()) return
        val root = JSONObject()
        root.put("quickAccess", encodeTracks(feed.quickAccess.take(6)))
        root.put("trending", encodeTracks(feed.trending.take(20)))
        root.put("localSongs", encodeTracks(feed.localSongs.take(12)))
        root.put("madeForYou", encodeTracks(feed.madeForYou.take(12)))
        root.put("chartSections", JSONArray().apply {
            feed.chartSections.take(2).forEach { section ->
                put(JSONObject().apply {
                    put("id", section.id)
                    put("title", section.title)
                    put("tracks", encodeTracks(section.tracks.take(12)))
                })
            }
        })
        root.put("autoPlaylists", JSONArray().apply {
            feed.autoPlaylists.take(4).forEach { playlist ->
                put(JSONObject().apply {
                    put("id", playlist.id)
                    put("title", playlist.title)
                    put("subtitle", playlist.subtitle)
                    put("accentColor", playlist.accentColor)
                    put("tracks", encodeTracks(playlist.tracks.take(12)))
                })
            }
        })
        prefs.edit().putString("home_feed_cache", root.toString()).apply()
    }

    private fun encodeTracks(tracks: List<Track>): JSONArray {
        val array = JSONArray()
        tracks.forEach { track ->
            array.put(JSONObject().apply {
                put("id", track.id)
                put("title", track.title)
                put("artist", track.artist)
                put("album", track.album)
                put("durationMs", track.durationMs)
                put("mediaUri", track.mediaUri.toString())
                put("artworkUri", track.artworkUri ?: "")
                put("isLocal", track.isLocal)
                put("mediaKey", track.mediaKey)
            })
        }
        return array
    }

    private fun decodeTracks(array: JSONArray?): List<Track> {
        if (array == null) return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val uri = obj.optString("mediaUri")
                if (uri.isBlank()) continue
                add(
                    Track(
                        id = obj.optString("id"),
                        title = obj.optString("title", "Unknown"),
                        artist = obj.optString("artist", "Unknown Artist"),
                        album = obj.optString("album", ""),
                        durationMs = obj.optLong("durationMs", 0L),
                        mediaUri = Uri.parse(uri),
                        artworkUri = obj.optString("artworkUri").ifBlank { null },
                        isLocal = obj.optBoolean("isLocal", false),
                        mediaKey = obj.optString("mediaKey")
                    )
                )
            }
        }
    }
}
