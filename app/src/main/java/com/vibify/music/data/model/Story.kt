package com.vibify.music.data.model

data class StoryBook(
    val id: String,
    val title: String,
    val author: String,
    val artworkUrl: String
)

data class StoryChapter(
    val id: String,
    val title: String,
    val audioUrl: String,
    val durationMs: Long
)
