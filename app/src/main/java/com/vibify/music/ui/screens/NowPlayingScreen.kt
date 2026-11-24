package com.vibify.music.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlin.math.abs
import kotlin.math.roundToInt
import com.vibify.music.R
import com.vibify.music.player.PlayerState
import com.vibify.music.player.RepeatMode
import com.vibify.music.ui.components.SpotifyCanvasVisualizer
import com.vibify.music.ui.theme.LocalOpenTrackDetails
import com.vibify.music.ui.theme.SpotifyBlack
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifySurfaceLight
import com.vibify.music.ui.theme.SpotifySurfaceVariant
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.cleanTrackTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    playerState: PlayerState,
    isLiked: Boolean,
    onCollapseClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onRewindClick: () -> Unit = {},
    onForwardClick: () -> Unit = {},
    onSpeedClick: () -> Unit = {},
    skipSeconds: Int = 15,
    onLikeClick: () -> Unit,
    onQueueClick: () -> Unit,
    onShareClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val track = playerState.currentTrack ?: return
    val isPodcast = track.isSpokenWord

    var isUserDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var showCanvas by remember { mutableStateOf(false) }

    val currentSliderVal = if (isUserDragging) sliderPosition else playerState.progress
    val openDetails = LocalOpenTrackDetails.current
    val collapse by rememberUpdatedState(onCollapseClick)
    var swipeOffset by remember(track.id) { mutableFloatStateOf(0f) }
    var draggingSheet by remember { mutableStateOf(false) }
    var dragY by remember { mutableFloatStateOf(0f) }
    val sheetOffset by animateFloatAsState(
        targetValue = dragY,
        animationSpec = if (draggingSheet) snap() else spring(),
        label = "nowPlayingDrag"
    )
    val swipeThreshold = with(LocalDensity.current) { 72.dp.toPx() }
    val dismissThreshold = with(LocalDensity.current) { 120.dp.toPx() }

    fun finishSheetDrag() {
        if (dragY > dismissThreshold) {
            collapse()
        } else {
            draggingSheet = false
            dragY = 0f
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val sheetProgress = (sheetOffset / with(LocalDensity.current) { maxHeight.toPx() }).coerceIn(0f, 1f)
        val sheetShape: Shape = RoundedCornerShape((sheetProgress * 28f).dp)
        val screenWidth = maxWidth
        val screenHeight = maxHeight
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = sheetOffset
                    val scale = 1f - (0.08f * sheetProgress)
                    scaleX = scale
                    scaleY = scale
                    clip = true
                    shape = sheetShape
                    alpha = 1f - (0.15f * sheetProgress)
                }
                .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1B4332),
                        Color(0xFF121212),
                        SpotifyBlack
                    )
                )
            )
            .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp)
                .pointerInput(Unit) {
                    val touchSlop = viewConfiguration.touchSlop
                    awaitEachGesture {
                        val down = awaitFirstDown(
                            requireUnconsumed = false,
                            pass = PointerEventPass.Initial
                        )
                        var totalX = 0f
                        var totalY = 0f
                        var vertical = false
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val change = event.changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            val delta = change.positionChange()
                            totalX += delta.x
                            totalY += delta.y
                            if (!vertical && totalY > touchSlop && abs(totalY) > abs(totalX)) {
                                vertical = true
                                draggingSheet = true
                            }
                            if (vertical) {
                                change.consume()
                                dragY = (dragY + delta.y).coerceAtLeast(0f)
                            }
                        }
                        if (vertical) finishSheetDrag()
                    }
                },
    ) {
        val compact = screenHeight < 700.dp
        val artSize = minOf(screenWidth, screenHeight * if (compact) 0.34f else 0.42f).coerceAtLeast(160.dp)
        val titleSize = if (compact) 18.sp else 22.sp
        val playSize = if (compact) 56.dp else 64.dp
        val gap = if (compact) 8.dp else 12.dp

        Column(
            modifier = Modifier
                .width(minOf(screenWidth, 520.dp))
                .fillMaxHeight()
                .align(Alignment.TopCenter)
                .padding(top = 4.dp, bottom = if (compact) 8.dp else 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .padding(bottom = 2.dp)
                            .width(42.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(SpotifyWhite.copy(alpha = if (sheetProgress > 0.08f) 1f else 0.45f))
                    )
                    if (sheetProgress > 0.08f) {
                        Text(
                            text = if (dragY > dismissThreshold) "Release to minimize" else "Slide down",
                            color = SpotifyWhite.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onCollapseClick) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "NOW PLAYING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotifySubtext,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (track.isLocal) "On this device" else track.album.ifBlank { "Vibify Music" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SpotifyWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = SpotifyWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(gap))

            Box(
                modifier = Modifier
                    .size(artSize)
                    .offset { IntOffset(swipeOffset.roundToInt(), 0) }
                    .shadow(22.dp, RoundedCornerShape(18.dp), clip = false)
                    .clip(RoundedCornerShape(18.dp))
                    .pointerInput(onNextClick, onPreviousClick, onCollapseClick) {
                        val touchSlop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            var totalX = 0f
                            var totalY = 0f
                            var axis = 0
                            drag(down.id) { change ->
                                val delta = change.positionChange()
                                totalX += delta.x
                                totalY += delta.y
                                if (axis == 0 && (abs(totalX) > touchSlop || abs(totalY) > touchSlop)) {
                                    axis = if (abs(totalY) > abs(totalX)) 2 else 1
                                }
                                when (axis) {
                                    1 -> {
                                        change.consume()
                                        swipeOffset = (swipeOffset + delta.x)
                                            .coerceIn(-size.width * 0.45f, size.width * 0.45f)
                                    }
                                    2 -> Unit
                                }
                            }
                            when (axis) {
                                1 -> {
                                    when {
                                        swipeOffset <= -swipeThreshold -> onNextClick()
                                        swipeOffset >= swipeThreshold -> onPreviousClick()
                                    }
                                    swipeOffset = 0f
                                }
                                2 -> Unit
                            }
                        }
                    }
                    .clickable { showCanvas = !showCanvas }
                    .background(SpotifySurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (showCanvas) {
                    SpotifyCanvasVisualizer(
                        isPlaying = playerState.isPlaying,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (track.artworkUri != null) {
                        Box(
                            modifier = Modifier
                                .size(if (compact) 72.dp else 96.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.5.dp, SpotifyWhite.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        ) {
                            AsyncImage(
                                model = track.artworkUri,
                                contentDescription = track.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                                error = painterResource(id = R.drawable.app_logo)
                            )
                        }
                    }
                } else if (track.artworkUri != null) {
                    AsyncImage(
                        model = track.artworkUri,
                        contentDescription = track.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(id = R.drawable.app_logo)
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_vibify_mark),
                        contentDescription = null,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SpotifyBlack.copy(alpha = 0.7f))
                        .clickable { showCanvas = !showCanvas }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (showCanvas) "Canvas" else "Cover",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (showCanvas) SpotifyGreen else SpotifyWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(gap))

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { openDetails(track) }
                ) {
                    Text(
                        text = cleanTrackTitle(track.title),
                        color = SpotifyWhite,
                        fontSize = titleSize,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artist,
                        color = SpotifySubtext,
                        fontSize = if (compact) 14.sp else 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onLikeClick) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isLiked) SpotifyGreen else SpotifyWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = currentSliderVal,
                    onValueChange = {
                        isUserDragging = true
                        sliderPosition = it
                    },
                    onValueChangeFinished = {
                        isUserDragging = false
                        if (playerState.durationMs > 0) {
                            val targetMs = (sliderPosition * playerState.durationMs).toLong()
                                .coerceIn(0L, playerState.durationMs)
                            onSeekTo(targetMs)
                        }
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = SpotifyWhite,
                        activeTrackColor = SpotifyWhite,
                        inactiveTrackColor = SpotifySurfaceLight
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val currentMs = if (isUserDragging) {
                        (sliderPosition * playerState.durationMs).toLong()
                    } else {
                        playerState.currentPositionMs
                    }
                    Text(text = formatMs(currentMs), color = SpotifySubtext, fontSize = 12.sp)
                    if (isPodcast) {
                        Text(
                            text = speedLabel(playerState.playbackSpeed),
                            color = SpotifyGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable(onClick = onSpeedClick)
                        )
                    }
                    Text(text = formatMs(playerState.durationMs), color = SpotifySubtext, fontSize = 12.sp)
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = if (isPodcast) onRewindClick else onShuffleClick) {
                    if (isPodcast) {
                        Text("-$skipSeconds", color = SpotifyWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (playerState.isShuffle) SpotifyGreen else SpotifySubtext,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                IconButton(onClick = onPreviousClick) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(if (compact) 34.dp else 38.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(playSize)
                        .clip(CircleShape)
                        .background(SpotifyWhite),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (playerState.isLoading && !playerState.isPlaying) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp),
                                color = SpotifyBlack,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play/Pause",
                                tint = SpotifyBlack,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                IconButton(onClick = onNextClick) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(if (compact) 34.dp else 38.dp)
                    )
                }

                IconButton(onClick = if (isPodcast) onForwardClick else onRepeatClick) {
                    if (isPodcast) {
                        Text("+$skipSeconds", color = SpotifyWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    } else {
                        when (playerState.repeatMode) {
                            RepeatMode.OFF -> Icon(
                                Icons.Default.Repeat, "Repeat Off",
                                tint = SpotifySubtext, modifier = Modifier.size(24.dp)
                            )
                            RepeatMode.ALL -> Icon(
                                Icons.Default.Repeat, "Repeat All",
                                tint = SpotifyGreen, modifier = Modifier.size(24.dp)
                            )
                            RepeatMode.ONE -> Icon(
                                Icons.Default.RepeatOne, "Repeat One",
                                tint = SpotifyGreen, modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onShareClick) {
                    Icon(Icons.Rounded.Share, "Share", tint = SpotifySubtext, modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onQueueClick) {
                    Icon(Icons.Rounded.QueueMusic, "Queue", tint = SpotifySubtext, modifier = Modifier.size(24.dp))
                }
                IconButton(onClick = onMoreClick) {
                    Icon(Icons.Default.MoreVert, "More", tint = SpotifySubtext, modifier = Modifier.size(22.dp))
                }
            }
        }
        }
    }
}

private fun speedLabel(speed: Float): String {
    val text = if (speed % 1f == 0f) speed.toInt().toString() else speed.toString()
    return "${text}x"
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
