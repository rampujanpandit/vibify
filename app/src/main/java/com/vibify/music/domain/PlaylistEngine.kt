package com.vibify.music.domain

import com.vibify.music.data.model.AutoPlaylist
import com.vibify.music.data.model.TasteProfile
import com.vibify.music.data.model.Track
import com.vibify.music.data.network.FullMusicEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Spotify-style playlist generators using:
 * - Taste profile (collaborative-lite / behavioral)
 * - Content signals (artist / title affinity)
 * - Chart pools (algotorial-lite)
 */
class PlaylistEngine(
    private val musicEngine: FullMusicEngine = FullMusicEngine()
) {

    data class PlaylistBundle(
        val madeForYou: AutoPlaylist,
        val discover: AutoPlaylist,
        val dailyMixes: List<AutoPlaylist>,
        val becauseYouLiked: AutoPlaylist?
    )

    suspend fun generate(
        taste: TasteProfile,
        likedTracks: List<Track>,
        trending: List<Track>,
        localSongs: List<Track>,
        chartPools: List<List<Track>>,
        playedIds: Set<String>
    ): PlaylistBundle = withContext(Dispatchers.IO) {
        val catalog = (likedTracks + trending + localSongs + chartPools.flatten())
            .distinctBy { it.id }

        val madeForYouTracks = buildMadeForYou(
            taste = taste,
            likedTracks = likedTracks,
            catalog = catalog,
            trending = trending
        )

        val discoverTracks = buildDiscover(
            taste = taste,
            catalog = catalog,
            trending = trending,
            chartPools = chartPools,
            playedIds = playedIds,
            likedIds = likedTracks.map { it.id }.toSet()
        )

        val dailyMixes = buildDailyMixes(
            taste = taste,
            likedTracks = likedTracks,
            catalog = catalog,
            localSongs = localSongs,
            trending = trending
        )

        // Enrich with online search for top artists when catalog is thin
        val enrichedMade = enrichWithArtistSearch(madeForYouTracks, taste.topArtists.take(3))
        val enrichedDiscover = enrichDiscover(discoverTracks, taste.topArtists.take(2), playedIds)

        val because = buildBecauseYouLiked(likedTracks, catalog, trending)

        PlaylistBundle(
            madeForYou = AutoPlaylist(
                id = "auto_made_for_you",
                title = "Made for You",
                subtitle = if (taste.topArtists.isNotEmpty()) {
                    "Based on ${taste.topArtists.first().replaceFirstChar { it.uppercase() }}"
                } else {
                    "Picks from your taste + charts"
                },
                tracks = diversify(enrichedMade).take(40),
                accentColor = 0xFF1DB954
            ),
            discover = AutoPlaylist(
                id = "auto_discover",
                title = "Discover for You",
                subtitle = "New songs you haven't played yet",
                tracks = diversify(enrichedDiscover).take(40),
                accentColor = 0xFF535353
            ),
            dailyMixes = dailyMixes,
            becauseYouLiked = because
        )
    }

    private fun buildMadeForYou(
        taste: TasteProfile,
        likedTracks: List<Track>,
        catalog: List<Track>,
        trending: List<Track>
    ): List<Track> {
        if (likedTracks.isEmpty() && taste.topArtists.isEmpty()) {
            return trending.shuffled().take(30)
        }

        val scored = catalog.map { track ->
            val artistScore = taste.artistAffinity(track.artist) * 3.0
            val trackScore = taste.trackScores[track.id] ?: 0.0
            val likedBoost = if (likedTracks.any { it.id == track.id }) 2.0 else 0.0
            val novelty = if (taste.recentTrackIds.contains(track.id)) -1.5 else 0.8
            track to (artistScore + trackScore + likedBoost + novelty)
        }

        return scored
            .sortedByDescending { it.second }
            .map { it.first }
            .distinctBy { it.id }
    }

    private fun buildDiscover(
        taste: TasteProfile,
        catalog: List<Track>,
        trending: List<Track>,
        chartPools: List<List<Track>>,
        playedIds: Set<String>,
        likedIds: Set<String>
    ): List<Track> {
        val pool = (trending + chartPools.flatten() + catalog).distinctBy { it.id }
        val fresh = pool.filter { it.id !in playedIds && it.id !in likedIds }

        val familiar = pool.filter { track ->
            taste.artistAffinity(track.artist) > 0.5 && track.id !in likedIds
        }.take(8)

        val discovery = fresh
            .sortedByDescending { taste.artistAffinity(it.artist) }
            .ifEmpty { trending }

        // ~20% familiar + 80% new (Spotify Discover Weekly trust trick)
        val mix = mutableListOf<Track>()
        mix.addAll(familiar.take((discovery.size * 0.2).toInt().coerceAtLeast(2)))
        mix.addAll(discovery)
        return mix.distinctBy { it.id }
    }

    private fun buildDailyMixes(
        taste: TasteProfile,
        likedTracks: List<Track>,
        catalog: List<Track>,
        localSongs: List<Track>,
        trending: List<Track>
    ): List<AutoPlaylist> {
        val artists = taste.topArtists.ifEmpty {
            likedTracks.map { it.artist.trim().lowercase() }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .map { it.key }
        }

        val mixes = mutableListOf<AutoPlaylist>()
        val colors = listOf(0xFF1E3264L, 0xFF8D67ABL, 0xFFE13300L)

        artists.take(2).forEachIndexed { index, artistKey ->
            val tracks = catalog.filter {
                it.artist.lowercase().contains(artistKey) || artistKey.contains(it.artist.lowercase())
            }.ifEmpty {
                trending.filter {
                    it.artist.lowercase().contains(artistKey)
                }
            }
            if (tracks.isNotEmpty()) {
                mixes.add(
                    AutoPlaylist(
                        id = "daily_mix_${index + 1}",
                        title = "Daily Mix ${index + 1}",
                        subtitle = artistKey.replaceFirstChar { it.uppercase() } + " and more",
                        tracks = diversify(tracks).take(30),
                        accentColor = colors.getOrElse(index) { 0xFF1DB954 }
                    )
                )
            }
        }

        if (localSongs.isNotEmpty()) {
            mixes.add(
                AutoPlaylist(
                    id = "daily_mix_local",
                    title = "Daily Mix ${mixes.size + 1}",
                    subtitle = "From your phone",
                    tracks = diversify(localSongs).take(30),
                    accentColor = colors.getOrElse(2) { 0xFFE13300 }
                )
            )
        }

        if (mixes.isEmpty() && trending.isNotEmpty()) {
            mixes.add(
                AutoPlaylist(
                    id = "daily_mix_1",
                    title = "Daily Mix 1",
                    subtitle = "Fresh chart energy",
                    tracks = diversify(trending).take(30),
                    accentColor = 0xFF1E3264
                )
            )
        }

        return mixes.take(3)
    }

    private fun buildBecauseYouLiked(
        likedTracks: List<Track>,
        catalog: List<Track>,
        trending: List<Track>
    ): AutoPlaylist? {
        val seed = likedTracks.firstOrNull() ?: return null
        val seedArtist = seed.artist.split(",", "&", " feat", " Ft")
            .firstOrNull()
            ?.trim()
            .orEmpty()
        if (seedArtist.isBlank()) return null

        val related = (catalog + trending)
            .filter {
                it.id != seed.id &&
                    it.artist.contains(seedArtist, ignoreCase = true)
            }
            .distinctBy { it.id }
            .take(30)

        if (related.isEmpty()) return null

        return AutoPlaylist(
            id = "because_${seed.id}",
            title = "Because you liked ${seed.title.take(28)}",
            subtitle = "More like $seedArtist",
            tracks = diversify(related),
            accentColor = 0xFF148A08
        )
    }

    private suspend fun enrichWithArtistSearch(
        base: List<Track>,
        artists: List<String>
    ): List<Track> = coroutineScope {
        if (artists.isEmpty()) return@coroutineScope base
        val extras = artists.map { artist ->
            async {
                runCatching {
                    musicEngine.searchFullSongs(artist).take(8)
                }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten()
        (base + extras).distinctBy { it.id }
    }

    private suspend fun enrichDiscover(
        base: List<Track>,
        artists: List<String>,
        playedIds: Set<String>
    ): List<Track> = coroutineScope {
        if (artists.isEmpty()) return@coroutineScope base
        val queries = artists.map { "$it songs" } + listOf("trending hindi songs")
        val extras = queries.take(3).map { q ->
            async {
                runCatching { musicEngine.searchFullSongs(q).take(12) }.getOrDefault(emptyList())
            }
        }.awaitAll().flatten()
            .filter { it.id !in playedIds }

        (base + extras).distinctBy { it.id }
    }

    /** Avoid 3+ same-artist in a row (Spotify-like flow). */
    private fun diversify(tracks: List<Track>): List<Track> {
        if (tracks.size <= 2) return tracks
        val remaining = tracks.toMutableList()
        val result = mutableListOf<Track>()
        var lastArtist: String? = null
        var streak = 0

        while (remaining.isNotEmpty()) {
            val idx = remaining.indexOfFirst { track ->
                val artist = track.artist.lowercase()
                if (lastArtist == null || artist != lastArtist) true
                else streak < 2
            }.let { if (it == -1) 0 else it }

            val next = remaining.removeAt(idx)
            val artist = next.artist.lowercase()
            if (artist == lastArtist) streak++ else {
                lastArtist = artist
                streak = 1
            }
            result.add(next)
        }
        return result
    }
}
