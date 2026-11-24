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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibify.music.data.model.PodcastEpisode
import com.vibify.music.data.model.PodcastShow
import com.vibify.music.ui.components.CoverArt
import com.vibify.music.ui.theme.SpotifyBlack
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifySurface
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.VibifyDimens
import com.vibify.music.ui.theme.vibifyListContentPadding

private val podcastTopics = listOf("Hindi", "News", "Comedy", "Business", "Technology", "Stories")

@Composable
fun PodcastsScreen(
    query: String,
    shows: List<PodcastShow>,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onOpenShow: (PodcastShow) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        if (shows.isEmpty() && query.isBlank()) onSearch("Hindi")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        contentPadding = vibifyListContentPadding()
    ) {
        item {
            Text(
                text = "Podcasts",
                color = SpotifyWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                modifier = Modifier.padding(horizontal = VibifyDimens.screenPadding, vertical = 12.dp)
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = VibifyDimens.screenPadding)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpotifySurface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = SpotifySubtext)
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = SpotifyWhite, fontSize = 16.sp),
                    cursorBrush = SolidColor(SpotifyGreen),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("Search shows", color = SpotifySubtext, fontSize = 16.sp)
                        }
                        inner()
                    }
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = VibifyDimens.screenPadding, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(podcastTopics) { topic ->
                    Text(
                        text = topic,
                        color = SpotifyWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(SpotifySurface)
                            .clickable { onSearch(topic) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
        }

        items(shows, key = { it.id + it.feedUrl }) { show ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenShow(show) }
                    .padding(horizontal = VibifyDimens.screenPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverArt(
                    artworkUri = show.artworkUrl,
                    title = show.title,
                    modifier = Modifier.size(64.dp),
                    cornerRadius = 8.dp
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = show.title,
                        color = SpotifyWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOf(show.author, show.genre).filter { it.isNotBlank() }.joinToString(" · "),
                        color = SpotifySubtext,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun PodcastShowScreen(
    show: PodcastShow,
    episodes: List<PodcastEpisode>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onPlay: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = SpotifyWhite)
                }
                Text(
                    text = "Show",
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CoverArt(
                    artworkUri = show.artworkUrl,
                    title = show.title,
                    modifier = Modifier.size(160.dp),
                    cornerRadius = 12.dp
                )
                Text(
                    text = show.title,
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(top = 14.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = show.author,
                    color = SpotifySubtext,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
        }

        items(episodes.size) { index ->
            val episode = episodes[index]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(index) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = episode.title,
                    color = SpotifyWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val meta = buildList {
                    if (episode.publishedLabel.isNotBlank()) add(episode.publishedLabel)
                    if (episode.durationMs > 0) add(formatMinutes(episode.durationMs))
                }.joinToString(" · ")
                if (meta.isNotBlank()) {
                    Text(text = meta, color = SpotifySubtext, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
    }
}

private fun formatMinutes(ms: Long): String {
    val minutes = (ms / 60000).coerceAtLeast(1)
    return "$minutes min"
}
