package com.vibify.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.rounded.PlaylistPlay
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.vibify.music.R
import com.vibify.music.data.model.AutoPlaylist
import com.vibify.music.data.model.Category
import com.vibify.music.data.model.Track
import com.vibify.music.player.PlayerState
import com.vibify.music.ui.components.CategoryCard
import com.vibify.music.ui.components.TrackItem
import com.vibify.music.ui.theme.LocalHideKeyboard
import com.vibify.music.ui.theme.SpotifyActiveCard
import com.vibify.music.ui.theme.SpotifyBlack
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifySurfaceLight
import com.vibify.music.ui.theme.SpotifySurfaceVariant
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.VibifyDimens
import com.vibify.music.ui.theme.gridColumnCount
import com.vibify.music.ui.theme.playlistCardSize
import com.vibify.music.ui.theme.vibifyListContentPadding

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    searchQuery: String,
    suggestions: List<String>,
    searchResults: List<Track>,
    searchPlaylists: List<AutoPlaylist>,
    visibleCount: Int,
    isLoadingMore: Boolean,
    categories: List<Category>,
    popularSearches: List<String>,
    playerState: PlayerState,
    likedTrackIds: Set<String>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSelectSuggestion: (String) -> Unit,
    onTrackClick: (Track) -> Unit,
    onLikeClick: (Track) -> Unit,
    onOpenPlaylist: (AutoPlaylist) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visibleResults = remember(searchResults, visibleCount) {
        searchResults.take(visibleCount)
    }
    val cardSize = playlistCardSize()
    val hideKeyboard = LocalHideKeyboard.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .padding(top = 12.dp)
    ) {
        Text(
            text = "Search",
            color = SpotifyWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.padding(horizontal = VibifyDimens.screenPadding)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = VibifyDimens.screenPadding)
                .height(48.dp)
                .clip(RoundedCornerShape(VibifyDimens.tileRadius))
                .background(SpotifyWhite)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = { onSelectSuggestion(searchQuery) }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = SpotifyBlack,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "What do you want to listen to?",
                            color = SpotifySurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onQueryChange,
                        textStyle = TextStyle(
                            color = SpotifyBlack,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(SpotifyBlack),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            hideKeyboard()
                            onSelectSuggestion(searchQuery)
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = SpotifyGreen,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = SpotifyBlack
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (searchQuery.isNotBlank()) {
            if (suggestions.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = VibifyDimens.screenPadding, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestions) { suggestion ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(SpotifySurfaceLight)
                                .clickable { onSelectSuggestion(suggestion) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = SpotifyGreen,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = suggestion,
                                    color = SpotifyWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            }

            LazyColumn(contentPadding = vibifyListContentPadding()) {
                if (searchPlaylists.isNotEmpty()) {
                    item {
                        Text(
                            text = "Playlists & artist mixes",
                            color = SpotifyWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            modifier = Modifier.padding(
                                horizontal = VibifyDimens.screenPadding,
                                vertical = 8.dp
                            )
                        )
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = VibifyDimens.screenPadding),
                            horizontalArrangement = Arrangement.spacedBy(VibifyDimens.itemSpacing)
                        ) {
                            items(searchPlaylists, key = { it.id }) { playlist ->
                                SearchPlaylistCard(
                                    playlist = playlist,
                                    cardSize = cardSize,
                                    onClick = {
                                        hideKeyboard()
                                        onOpenPlaylist(playlist)
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                item {
                    Text(
                        text = "Songs for \"$searchQuery\"",
                        color = SpotifyWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        modifier = Modifier.padding(
                            horizontal = VibifyDimens.screenPadding,
                            vertical = 8.dp
                        )
                    )
                    if (searchResults.isNotEmpty()) {
                        Text(
                            text = "Showing ${visibleResults.size} of ${searchResults.size}",
                            color = SpotifySubtext,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(
                                horizontal = VibifyDimens.screenPadding,
                                vertical = 2.dp
                            )
                        )
                    }
                }

                if (searchResults.isEmpty() && !isSearching) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No songs found for \"$searchQuery\".\nTry another keyword.",
                                color = SpotifySubtext,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    itemsIndexed(visibleResults, key = { _, track -> track.id }) { index, track ->
                        if (index == visibleResults.lastIndex && visibleCount < searchResults.size) {
                            LaunchedEffect(visibleCount, searchResults.size) {
                                onLoadMore()
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
        } else {
            LazyColumn(contentPadding = vibifyListContentPadding()) {
                if (popularSearches.isNotEmpty()) {
                    item {
                        Text(
                            text = "Popular searches",
                            color = SpotifyWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(
                                horizontal = VibifyDimens.screenPadding,
                                vertical = 8.dp
                            )
                        )

                        FlowRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = VibifyDimens.screenPadding),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            popularSearches.forEach { tag ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(SpotifyActiveCard)
                                        .clickable { onSelectSuggestion(tag) }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                                        contentDescription = null,
                                        tint = SpotifyGreen,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = tag,
                                        color = SpotifyWhite,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                item {
                    Text(
                        text = "Browse all",
                        color = SpotifyWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(
                            horizontal = VibifyDimens.screenPadding,
                            vertical = 8.dp
                        )
                    )
                }

                item {
                    val columns = gridColumnCount()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = VibifyDimens.screenPadding),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (i in categories.indices step columns) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (col in 0 until columns) {
                                    val index = i + col
                                    if (index < categories.size) {
                                        CategoryCard(
                                            category = categories[index],
                                            onClick = { onSelectSuggestion(categories[index].name) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchPlaylistCard(
    playlist: AutoPlaylist,
    cardSize: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val cover = playlist.tracks.firstOrNull()?.artworkUri
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
            } else {
                Icon(
                    imageVector = Icons.Rounded.PlaylistPlay,
                    contentDescription = null,
                    tint = SpotifyWhite,
                    modifier = Modifier.size(48.dp)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                        )
                    )
            )
            Text(
                text = playlist.title,
                color = SpotifyWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
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
