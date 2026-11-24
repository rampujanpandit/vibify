package com.vibify.music.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vibify.music.R
import com.vibify.music.data.model.Track
import com.vibify.music.ui.theme.SpotifyBlack
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifySurface
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.cleanTrackTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailsSheet(
    track: Track,
    isLiked: Boolean,
    onDismiss: () -> Unit,
    onPlay: () -> Unit,
    onLike: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SpotifySurface,
        contentColor = SpotifyWhite
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = track.artworkUri,
                contentDescription = track.title,
                modifier = Modifier
                    .size(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpotifyBlack),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.app_logo)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = cleanTrackTitle(track.title),
                color = SpotifyWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = track.artist.ifBlank { "Unknown artist" },
                color = SpotifySubtext,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            DetailRow(
                icon = { Icon(Icons.Rounded.Person, null, tint = SpotifyGreen, modifier = Modifier.size(18.dp)) },
                label = "Artist",
                value = track.artist.ifBlank { "Unknown artist" }
            )
            DetailRow(
                icon = { Icon(Icons.Rounded.Album, null, tint = SpotifyGreen, modifier = Modifier.size(18.dp)) },
                label = "Album",
                value = track.album.ifBlank { "Single" }
            )
            DetailRow(
                icon = { Icon(Icons.Rounded.Schedule, null, tint = SpotifyGreen, modifier = Modifier.size(18.dp)) },
                label = "Duration",
                value = if (track.durationMs > 0) track.formattedDuration else "Unknown"
            )
            DetailRow(
                icon = { Icon(Icons.Rounded.Album, null, tint = SpotifyGreen, modifier = Modifier.size(18.dp)) },
                label = "Source",
                value = if (track.isLocal) "On this phone" else "Streaming"
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onPlay,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpotifyGreen,
                        contentColor = SpotifyBlack
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                    Text("Play", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                }
                OutlinedButton(
                    onClick = onLike,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        tint = if (isLiked) SpotifyGreen else SpotifyWhite
                    )
                    Text(
                        text = if (isLiked) "Liked" else "Like",
                        color = SpotifyWhite,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            text = label,
            color = SpotifySubtext,
            fontSize = 13.sp,
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(0.35f)
        )
        Text(
            text = value,
            color = SpotifyWhite,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.65f)
        )
    }
}
