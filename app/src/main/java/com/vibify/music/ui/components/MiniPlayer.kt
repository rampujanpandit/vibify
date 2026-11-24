package com.vibify.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vibify.music.player.PlayerState
import com.vibify.music.ui.components.CoverArt
import com.vibify.music.ui.theme.SpotifyActiveCard
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifySurfaceLight
import com.vibify.music.ui.theme.SpotifySurfaceVariant
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.VibifyDimens
import com.vibify.music.ui.theme.cleanTrackTitle

@Composable
fun MiniPlayer(
    playerState: PlayerState,
    isLiked: Boolean,
    onMiniPlayerClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onLikeClick: () -> Unit,
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return
    var swipeOffset by remember(track.id) { mutableFloatStateOf(0f) }
    val swipeThreshold = with(LocalDensity.current) { 56.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .offset { IntOffset(swipeOffset.roundToInt(), 0) }
            .clip(RoundedCornerShape(VibifyDimens.tileRadius))
            .background(SpotifyActiveCard)
            .pointerInput(onNext, onPrevious) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { _, dragAmount ->
                        swipeOffset = (swipeOffset + dragAmount)
                            .coerceIn(-size.width * 0.35f, size.width * 0.35f)
                    },
                    onDragEnd = {
                        when {
                            swipeOffset <= -swipeThreshold -> onNext()
                            swipeOffset >= swipeThreshold -> onPrevious()
                        }
                        swipeOffset = 0f
                    },
                    onDragCancel = { swipeOffset = 0f }
                )
            }
            .clickable { onMiniPlayerClick() }
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverArt(
                    artworkUri = track.artworkUri,
                    title = track.title,
                    modifier = Modifier.size(42.dp),
                    cornerRadius = 6.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = cleanTrackTitle(track.title),
                        color = SpotifyWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
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

                // Like Button
                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) SpotifyGreen else SpotifySubtext,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Play / Pause (keep button usable; tiny spinner only while waiting to start)
                Box(contentAlignment = Alignment.Center) {
                    IconButton(onClick = onPlayPauseClick) {
                        Icon(
                            imageVector = if (playerState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play/Pause",
                            tint = SpotifyWhite,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    if (playerState.isLoading && !playerState.isPlaying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = SpotifyGreen,
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            // Bottom Progress Bar
            LinearProgressIndicator(
                progress = { playerState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = SpotifyWhite,
                trackColor = SpotifySurfaceLight,
                strokeCap = StrokeCap.Round
            )
        }
    }
}
