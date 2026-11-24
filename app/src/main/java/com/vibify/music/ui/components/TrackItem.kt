package com.vibify.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vibify.music.R
import com.vibify.music.data.model.Track
import com.vibify.music.ui.theme.LocalHideKeyboard
import com.vibify.music.ui.theme.LocalOpenTrackDetails
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifySurfaceVariant
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.VibifyDimens
import com.vibify.music.ui.theme.cleanTrackTitle

@Composable
fun TrackItem(
    track: Track,
    isPlaying: Boolean,
    isCurrentTrack: Boolean,
    isLiked: Boolean,
    onTrackClick: () -> Unit,
    onLikeClick: () -> Unit,
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val hideKeyboard = LocalHideKeyboard.current
    val openDetails = LocalOpenTrackDetails.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                hideKeyboard()
                onTrackClick()
            }
            .padding(horizontal = VibifyDimens.screenPadding, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SpotifySurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (track.artworkUri != null) {
                AsyncImage(
                    model = track.artworkUri,
                    contentDescription = track.title,
                    modifier = Modifier.size(48.dp),
                    contentScale = ContentScale.Crop,
                    error = painterResource(id = R.drawable.app_logo)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = SpotifySubtext,
                    modifier = Modifier.size(26.dp)
                )
            }
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.45f))
                )
                PlayingWave(modifier = Modifier.align(Alignment.Center))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    hideKeyboard()
                    openDetails(track)
                }
        ) {
            Text(
                text = cleanTrackTitle(track.title),
                color = if (isCurrentTrack) SpotifyGreen else SpotifyWhite,
                fontSize = 15.sp,
                fontWeight = if (isCurrentTrack) FontWeight.Bold else FontWeight.Medium,
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

        IconButton(
            onClick = onLikeClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = "Like",
                tint = if (isLiked) SpotifyGreen else SpotifySubtext,
                modifier = Modifier.size(20.dp)
            )
        }

        IconButton(
            onClick = onMoreClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Options",
                tint = SpotifySubtext,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun PlayingWave(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "playingWave")
    Row(
        modifier = modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(320, 460, 380).forEachIndexed { index, duration ->
            val scale by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = duration, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(scale)
                    .clip(RoundedCornerShape(2.dp))
                    .background(SpotifyGreen)
            )
        }
    }
}
