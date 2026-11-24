package com.vibify.music.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.vibify.music.data.model.HomeFeed
import com.vibify.music.data.model.Track
import com.vibify.music.player.PlayerState
import com.vibify.music.ui.components.CoverArt
import com.vibify.music.ui.components.TrackItem
import com.vibify.music.ui.theme.SpotifyActiveCard
import com.vibify.music.ui.theme.SpotifyBlack
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifySurfaceVariant
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.VibifyDimens
import com.vibify.music.ui.theme.carouselCardSize
import com.vibify.music.ui.theme.cleanTrackTitle
import com.vibify.music.ui.theme.gridColumnCount
import com.vibify.music.ui.theme.playlistCardSize
import com.vibify.music.ui.theme.vibifyListContentPadding
import java.util.Calendar

@Composable
fun HomeScreen(
    homeFeed: HomeFeed,
    isLoading: Boolean,
    trendingVisibleCount: Int,
    isLoadingMoreTrending: Boolean,
    playerState: PlayerState,
    likedTrackIds: Set<String>,
    notificationCount: Int,
    onTrackClick: (Track) -> Unit,
    onLikeClick: (Track) -> Unit,
    onRefresh: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onLoadMoreTrending: () -> Unit,
    onOpenPlaylist: (AutoPlaylist) -> Unit,
    modifier: Modifier = Modifier
) {
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 4..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    val isEmpty = !isLoading &&
        homeFeed.quickAccess.isEmpty() &&
        homeFeed.trending.isEmpty() &&
        homeFeed.localSongs.isEmpty()

    val visibleTrending = remember(homeFeed.trending, trendingVisibleCount) {
        homeFeed.trending.take(trendingVisibleCount)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF143026), Color(0x00121212))
                    )
                )
        )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = vibifyListContentPadding()
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = VibifyDimens.screenPadding)
                    .padding(top = 6.dp, bottom = 18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "Vibify",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Vibify",
                        color = SpotifyWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Rounded.Refresh, "Refresh", tint = SpotifyWhite)
                    }
                    IconButton(onClick = onOpenNotifications) {
                        Box {
                            Icon(Icons.Outlined.Notifications, "Notifications", tint = SpotifyWhite)
                            if (notificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(SpotifyGreen)
                                )
                            }
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, "Settings", tint = SpotifyWhite)
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = greeting,
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    lineHeight = 34.sp
                )
            }
        }

        if (isLoading && homeFeed.trending.isEmpty() && homeFeed.localSongs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = SpotifyGreen)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Loading live charts…",
                            color = SpotifySubtext,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        if (isEmpty) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nothing to play yet",
                        color = SpotifyWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Connect to internet for trending charts, or scan local MP3s from Library.",
                        color = SpotifySubtext,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tap refresh",
                        color = SpotifyGreen,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onRefresh() }
                    )
                }
            }
        }

        if (homeFeed.quickAccess.isNotEmpty()) {
            item {
                val columns = gridColumnCount()
                val quickTracks = homeFeed.quickAccess.take(columns * 3)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = VibifyDimens.screenPadding),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (i in quickTracks.indices step columns) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until columns) {
                                val index = i + col
                                if (index < quickTracks.size) {
                                    QuickTile(
                                        track = quickTracks[index],
                                        onClick = { onTrackClick(quickTracks[index]) },
                                        modifier = Modifier.weight(1f)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (homeFeed.autoPlaylists.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Made for you",
                    subtitle = "Personalized from your likes & listens"
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = VibifyDimens.screenPadding),
                    horizontalArrangement = Arrangement.spacedBy(VibifyDimens.itemSpacing)
                ) {
                    items(homeFeed.autoPlaylists, key = { it.id }) { playlist ->
                        AutoPlaylistCard(
                            playlist = playlist,
                            onClick = { onOpenPlaylist(playlist) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        } else if (homeFeed.madeForYou.isNotEmpty()) {
            item {
                SectionHeader(title = "Picked for you")
                HorizontalTrackRow(
                    tracks = homeFeed.madeForYou.take(10),
                    onTrackClick = onTrackClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (homeFeed.localSongs.isNotEmpty()) {
            item {
                SectionHeader(title = "On your phone")
                HorizontalTrackRow(
                    tracks = homeFeed.localSongs.take(10),
                    onTrackClick = onTrackClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        homeFeed.chartSections.forEach { section ->
            if (section.tracks.isNotEmpty()) {
                item {
                    SectionHeader(title = section.title)
                    HorizontalTrackRow(
                        tracks = section.tracks.take(10),
                        onTrackClick = onTrackClick
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (homeFeed.trending.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Trending Now",
                    subtitle = "Showing ${visibleTrending.size} of ${homeFeed.trending.size}"
                )
            }
            itemsIndexed(visibleTrending, key = { _, track -> track.id }) { index, track ->
                if (index == visibleTrending.lastIndex) {
                    LaunchedEffect(trendingVisibleCount, homeFeed.trending.size) {
                        if (trendingVisibleCount < homeFeed.trending.size) {
                            onLoadMoreTrending()
                        }
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

            if (isLoadingMoreTrending) {
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
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String? = null) {
    Column(
        modifier = Modifier.padding(
            horizontal = VibifyDimens.screenPadding,
            vertical = 8.dp
        )
    ) {
        Text(
            text = title,
            color = SpotifyWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = SpotifySubtext,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun HorizontalTrackRow(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit
) {
    val cardSize = carouselCardSize()
    LazyRow(
        contentPadding = PaddingValues(horizontal = VibifyDimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(VibifyDimens.itemSpacing)
    ) {
        items(tracks, key = { it.id }) { track ->
            CarouselCard(
                track = track,
                cardSize = cardSize,
                onClick = { onTrackClick(track) }
            )
        }
    }
}

@Composable
private fun QuickTile(
    track: Track,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(SpotifyActiveCard)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        CoverArt(
            artworkUri = track.artworkUri,
            title = track.title,
            modifier = Modifier.size(56.dp),
            cornerRadius = 0.dp
        )
        Text(
            text = cleanTrackTitle(track.title),
            color = SpotifyWhite,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .weight(1f)
        )
    }
}

@Composable
private fun AutoPlaylistCard(
    playlist: AutoPlaylist,
    onClick: () -> Unit
) {
    val cover = playlist.tracks.firstOrNull()?.artworkUri
    val cardSize = playlistCardSize()
    Column(
        modifier = Modifier
            .width(cardSize)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(cardSize)
                .clip(RoundedCornerShape(VibifyDimens.tileRadius))
                .background(
                    Brush.linearGradient(
                        listOf(Color(playlist.accentColor), SpotifyBlack)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (cover != null) {
                AsyncImage(
                    model = cover,
                    contentDescription = playlist.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.app_logo)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )
            Text(
                text = playlist.title,
                color = SpotifyWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = playlist.subtitle,
            color = SpotifySubtext,
            fontSize = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CarouselCard(
    track: Track,
    cardSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(cardSize)
            .clickable { onClick() }
    ) {
        CoverArt(
            artworkUri = track.artworkUri,
            title = track.title,
            modifier = Modifier.size(cardSize),
            cornerRadius = VibifyDimens.tileRadius
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = cleanTrackTitle(track.title),
            color = SpotifyWhite,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            color = SpotifySubtext,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
