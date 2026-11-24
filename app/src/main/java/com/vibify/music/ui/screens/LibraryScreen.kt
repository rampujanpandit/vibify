package com.vibify.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibify.music.data.model.Track
import com.vibify.music.player.PlayerState
import com.vibify.music.ui.components.TrackItem
import com.vibify.music.ui.theme.SpotifyActiveCard
import com.vibify.music.ui.theme.SpotifyBlack
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.VibifyDimens
import com.vibify.music.ui.theme.songCountLabel
import com.vibify.music.ui.theme.vibifyListContentPadding

@Composable
fun LibraryScreen(
    likedTracks: List<Track>,
    localTracks: List<Track>,
    playerState: PlayerState,
    likedTrackIds: Set<String>,
    visibleCount: Int,
    isLoadingMore: Boolean,
    onTrackClick: (Track) -> Unit,
    onLikeClick: (Track) -> Unit,
    onScanLocalSongs: () -> Unit,
    onFilterChanged: () -> Unit,
    onLoadMore: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("Liked") }

    val displayedTracks = remember(selectedFilter, likedTracks, localTracks) {
        when (selectedFilter) {
            "Local" -> localTracks
            "All" -> (likedTracks + localTracks).distinctBy { it.id }
            else -> likedTracks
        }
    }

    val visibleTracks = displayedTracks.take(visibleCount)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        contentPadding = vibifyListContentPadding()
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VibifyDimens.screenPadding, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Your Library",
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )

                IconButton(onClick = onScanLocalSongs) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan Device Songs",
                        tint = SpotifyWhite
                    )
                }
            }
        }

        // Collection cards
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VibifyDimens.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LibraryCard(
                    title = "Liked Songs",
                    subtitle = songCountLabel(likedTracks.size),
                    gradient = listOf(Color(0xFF450AF5), Color(0xFFC4EFD9)),
                    icon = {
                        Icon(Icons.Filled.Favorite, null, tint = SpotifyWhite, modifier = Modifier.size(26.dp))
                    },
                    onClick = {
                        selectedFilter = "Liked"
                        onFilterChanged()
                    },
                    modifier = Modifier.weight(1f)
                )
                LibraryCard(
                    title = "On Device",
                    subtitle = songCountLabel(localTracks.size),
                    gradient = listOf(Color(0xFF1DB954), Color(0xFF191414)),
                    icon = {
                        Icon(Icons.Rounded.PhoneAndroid, null, tint = SpotifyWhite, modifier = Modifier.size(26.dp))
                    },
                    onClick = {
                        selectedFilter = "Local"
                        onFilterChanged()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VibifyDimens.screenPadding)
                    .clip(RoundedCornerShape(VibifyDimens.cardRadius))
                    .background(SpotifyActiveCard)
                    .clickable(onClick = onScanLocalSongs)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Folder, null, tint = SpotifyGreen, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Scan MP3 from phone", color = SpotifyWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Find local audio files on this device", color = SpotifySubtext, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VibifyDimens.screenPadding),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Liked", "Local", "All").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) SpotifyGreen else SpotifyActiveCard)
                            .clickable {
                                selectedFilter = filter
                                onFilterChanged()
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = filter,
                            color = if (isSelected) SpotifyBlack else SpotifyWhite,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Showing ${visibleTracks.size} of ${displayedTracks.size}",
                color = SpotifySubtext,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = VibifyDimens.screenPadding, vertical = 6.dp)
            )
        }

        if (displayedTracks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillParentMaxHeight(0.45f)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = when (selectedFilter) {
                            "Local" -> "No local songs yet"
                            "All" -> "Library is empty"
                            else -> "No liked songs yet"
                        },
                        color = SpotifyWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (selectedFilter) {
                            "Local" -> "Tap Scan MP3 to find music on your phone."
                            else -> "Like songs from Home or Search to save them here."
                        },
                        color = SpotifySubtext,
                        fontSize = 13.sp
                    )
                }
            }
        }

        itemsIndexed(visibleTracks, key = { _, track -> track.id }) { index, track ->
            if (index == visibleTracks.lastIndex && visibleCount < displayedTracks.size) {
                LaunchedEffect(visibleCount, displayedTracks.size, selectedFilter) {
                    onLoadMore(displayedTracks.size)
                }
            }
            val isCurrent = playerState.currentTrack?.id == track.id
            TrackItem(
                track = track,
                isPlaying = isCurrent && playerState.isPlaying,
                isCurrentTrack = isCurrent,
                isLiked = likedTrackIds.contains(track.id),
                onTrackClick = { onTrackClick(track) },
                onLikeClick = { onLikeClick(track) }
            )
        }

        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = SpotifyGreen,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryCard(
    title: String,
    subtitle: String,
    gradient: List<Color>,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.height(18.dp))
        Text(text = title, color = SpotifyWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(text = subtitle, color = SpotifyWhite.copy(alpha = 0.8f), fontSize = 12.sp)
    }
}
