package com.vibify.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibify.music.data.model.StoryBook
import com.vibify.music.data.model.StoryChapter
import com.vibify.music.ui.components.CoverArt
import com.vibify.music.ui.theme.SpotifyBlack
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifySurface
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.VibifyDimens
import com.vibify.music.ui.theme.vibifyListContentPadding

private val storyTopics = listOf("Hindi", "Premchand", "Panchatantra", "Classics")

@Composable
fun KahaniScreen(
    query: String,
    books: List<StoryBook>,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onOpenBook: (StoryBook) -> Unit,
    modifier: Modifier = Modifier
) {
    LaunchedEffect(Unit) {
        if (books.isEmpty() && query.isBlank()) onSearch("Hindi")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack),
        contentPadding = vibifyListContentPadding()
    ) {
        item {
            Text(
                text = "Kahani",
                color = SpotifyWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                modifier = Modifier.padding(start = VibifyDimens.screenPadding, end = VibifyDimens.screenPadding, top = 12.dp)
            )
            Text(
                text = "Public-domain audiobooks",
                color = SpotifySubtext,
                fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = VibifyDimens.screenPadding, vertical = 4.dp)
            )
            Row(
                modifier = Modifier
                    .padding(horizontal = VibifyDimens.screenPadding, vertical = 8.dp)
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
                    textStyle = TextStyle(color = SpotifyWhite, fontSize = 16.sp),
                    cursorBrush = SolidColor(SpotifyGreen),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp),
                    decorationBox = { inner ->
                        if (query.isEmpty()) {
                            Text("Search stories", color = SpotifySubtext, fontSize = 16.sp)
                        }
                        inner()
                    }
                )
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = VibifyDimens.screenPadding, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(storyTopics) { topic ->
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

        items(books, key = { it.id }) { book ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenBook(book) }
                    .padding(horizontal = VibifyDimens.screenPadding, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoverArt(
                    artworkUri = book.artworkUrl,
                    title = book.title,
                    modifier = Modifier.size(64.dp),
                    cornerRadius = 8.dp
                )
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = book.title,
                        color = SpotifyWhite,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (book.author.isNotBlank()) {
                        Text(
                            text = book.author,
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
}

@Composable
fun StoryBookScreen(
    book: StoryBook,
    chapters: List<StoryChapter>,
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
                    text = "Kahani",
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
                    artworkUri = book.artworkUrl,
                    title = book.title,
                    modifier = Modifier.size(160.dp),
                    cornerRadius = 12.dp
                )
                Text(
                    text = book.title,
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(top = 14.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.author.isNotBlank()) {
                    Text(
                        text = book.author,
                        color = SpotifySubtext,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (chapters.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(SpotifyGreen)
                            .clickable { onPlay(0) }
                            .padding(horizontal = 22.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = SpotifyBlack)
                        Text(
                            text = "Play",
                            color = SpotifyBlack,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
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

        items(chapters.size) { index ->
            val chapter = chapters[index]
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPlay(index) }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = chapter.title,
                    color = SpotifyWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (chapter.durationMs > 0) {
                    val minutes = (chapter.durationMs / 60000).coerceAtLeast(1)
                    Text(
                        text = "$minutes min",
                        color = SpotifySubtext,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
            }
        }
    }
}
