package com.vibify.music.ui.components

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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vibify.music.R
import com.vibify.music.data.model.Track
import com.vibify.music.ui.theme.SpotifyActiveCard
import com.vibify.music.ui.theme.SpotifyBlack
import com.vibify.music.ui.theme.SpotifyDivider
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifySurfaceVariant
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.VibifyDimens
import com.vibify.music.ui.theme.cleanTrackTitle
import com.vibify.music.ui.theme.songCountLabel
import com.vibify.music.ui.theme.vibifyListContentPadding

@Composable
fun QueueScreen(
    queue: List<Track>,
    currentQueueIndex: Int,
    currentTrack: Track?,
    isPlaying: Boolean,
    onTrackClick: (Track) -> Unit,
    onRemoveFromQueue: (Int) -> Unit,
    onClearQueue: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nowPlaying = currentTrack
    val upNext = if (currentQueueIndex in queue.indices) {
        queue.mapIndexedNotNull { index, track ->
            if (index > currentQueueIndex) index to track else null
        }
    } else {
        queue.mapIndexed { index, track -> index to track }
    }

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
                Column {
                    Text(
                        text = "Queue",
                        color = SpotifyWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                    Text(
                        text = songCountLabel(queue.size),
                        color = SpotifySubtext,
                        fontSize = 13.sp
                    )
                }
                if (queue.isNotEmpty()) {
                    TextButton(onClick = onClearQueue) {
                        Text(text = "Clear", color = SpotifyGreen, fontSize = 13.sp)
                    }
                }
            }
        }

        if (queue.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillParentMaxHeight(0.55f)
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Queue is empty",
                        color = SpotifyWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Play a song from Home, Search, or Library to build your queue.",
                        color = SpotifySubtext,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            if (nowPlaying != null) {
                item {
                    Text(
                        text = "Now playing",
                        color = SpotifySubtext,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    NowPlayingRow(
                        track = nowPlaying,
                        isPlaying = isPlaying,
                        onClick = { onTrackClick(nowPlaying) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            if (upNext.isNotEmpty()) {
                item {
                    Text(
                        text = "Up next",
                        color = SpotifySubtext,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                itemsIndexed(upNext) { _, pair ->
                    val (index, track) = pair
                    QueueRow(
                        track = track,
                        isCurrent = false,
                        onClick = { onTrackClick(track) },
                        onRemove = { onRemoveFromQueue(index) }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .padding(horizontal = 16.dp)
                            .background(SpotifyDivider)
                    )
                }
            }
        }
    }
}

@Composable
private fun NowPlayingRow(
    track: Track,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(SpotifyActiveCard)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = track.artworkUri,
            contentDescription = null,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SpotifySurfaceVariant),
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.app_logo)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cleanTrackTitle(track.title),
                color = SpotifyGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = track.artist + if (isPlaying) " · Playing" else " · Paused",
                color = SpotifySubtext,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(text = "▶", color = SpotifyGreen, fontSize = 16.sp)
    }
}

@Composable
private fun QueueRow(
    track: Track,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = if (isCurrent) "▶" else "♪",
                color = if (isCurrent) SpotifyGreen else SpotifySubtext,
                fontSize = 16.sp,
                modifier = Modifier.width(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = cleanTrackTitle(track.title),
                    color = if (isCurrent) SpotifyGreen else SpotifyWhite,
                    fontSize = 15.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = track.artist,
                    color = SpotifySubtext,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Text(
            text = "×",
            color = SpotifySubtext,
            fontSize = 20.sp,
            modifier = Modifier
                .clickable(onClick = onRemove)
                .padding(8.dp)
        )
    }
}
