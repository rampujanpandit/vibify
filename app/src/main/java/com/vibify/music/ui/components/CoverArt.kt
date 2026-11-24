package com.vibify.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vibify.music.R
import kotlin.math.abs

private val coverPalettes = listOf(
    listOf(Color(0xFF1ED760), Color(0xFF0E3B24)),
    listOf(Color(0xFF509BF5), Color(0xFF142850)),
    listOf(Color(0xFFE45BB0), Color(0xFF3A1230)),
    listOf(Color(0xFFFF7A45), Color(0xFF3D1608)),
    listOf(Color(0xFFB084E0), Color(0xFF24143C))
)

@Composable
fun CoverArt(
    artworkUri: String?,
    title: String,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp
) {
    val palette = remember(title) {
        coverPalettes[abs(title.hashCode()) % coverPalettes.size]
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(palette)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.72f)
                .border(1.dp, Color.White.copy(alpha = 0.22f), CircleShape)
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.28f)
                .background(Color.Black.copy(alpha = 0.28f), CircleShape)
        )
        if (!artworkUri.isNullOrBlank()) {
            AsyncImage(
                model = artworkUri,
                contentDescription = title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                error = painterResource(id = R.drawable.ic_vibify_mark)
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.ic_vibify_mark),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(0.42f)
            )
        }
    }
}
