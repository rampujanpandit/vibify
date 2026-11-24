package com.vibify.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vibify.music.R
import com.vibify.music.data.model.AutoPlaylist
import com.vibify.music.data.model.Track
import com.vibify.music.player.PlayerState
import com.vibify.music.ui.components.TrackItem
import com.vibify.music.ui.theme.SpotifyBlack
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.playlistHeroSize
import com.vibify.music.ui.theme.songCountLabel
import com.vibify.music.ui.theme.vibifyListContentPadding

@Composable
fun PlaylistDetailScreen(
    playlist: AutoPlaylist,
    playerState: PlayerState,
    likedTrackIds: Set<String>,
    visibleCount: Int,
    isLoadingMore: Boolean,
    onBack: () -> Unit,
    onPlayAll: () -> Unit,
    onTrackClick: (Track, Int) -> Unit,
    onLikeClick: (Track) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleTracks = remember(playlist.tracks, visibleCount) {
        playlist.tracks.take(visibleCount)
    }
    val cover = playlist.tracks.firstOrNull()?.artworkUri
    val accent = Color(playlist.accentColor)

    val heroSize = playlistHeroSize()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        contentPadding = vibifyListContentPadding()
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = 0.9f), accent.copy(alpha = 0.35f), SpotifyBlack)
                        )
                    )
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = SpotifyWhite
                        )
                    }
                }
                AsyncImage(
                    model = cover,
                    contentDescription = playlist.title,
                    modifier = Modifier
                        .size(heroSize)
                        .shadow(16.dp, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.app_logo)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = playlist.title,
                    color = SpotifyWhite,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = prettyLine(playlist.subtitle),
                    color = SpotifyWhite.copy(alpha = 0.78f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = songCountLabel(playlist.tracks.size),
                    color = SpotifyWhite.copy(alpha = 0.62f),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(SpotifyGreen)
                        .clickable(onClick = onPlayAll)
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = SpotifyBlack,
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "Play",
                        color = SpotifyBlack,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        itemsIndexed(visibleTracks, key = { _, track -> track.id }) { index, track ->
            if (index == visibleTracks.lastIndex && visibleCount < playlist.tracks.size) {
                LaunchedEffect(visibleCount, playlist.tracks.size) {
                    onLoadMore()
                }
            }
            val isCurrent = playerState.currentTrack?.id == track.id
            TrackItem(
                track = track,
                isPlaying = isCurrent && playerState.isPlaying,
                isCurrentTrack = isCurrent,
                isLiked = likedTrackIds.contains(track.id),
                onTrackClick = { onTrackClick(track, index) },
                onLikeClick = { onLikeClick(track) }
            )
        }

        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Loading more…", color = SpotifySubtext, fontSize = 13.sp)
                }
            }
        }
    }
}

private val smallWords = setOf("a", "an", "and", "for", "from", "of", "on", "the", "to")

private fun prettyLine(text: String): String {
    return text.split(" ").mapIndexed { index, word ->
        val bare = word.trim(',').lowercase()
        if (index > 0 && bare in smallWords) word.lowercase()
        else word.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
    }.joinToString(" ")
}
