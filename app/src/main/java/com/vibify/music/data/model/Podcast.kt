package com.vibify.music.data.model

data class PodcastShow(
    val id: String,
    val title: String,
    val author: String,
    val artworkUrl: String?,
    val feedUrl: String,
    val genre: String = ""
)

data class PodcastEpisode(
    val id: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val artworkUrl: String?,
    val publishedLabel: String,
    val durationMs: Long
)
