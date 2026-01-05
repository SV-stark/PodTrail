package com.stark.podtrail.data

data class EpisodeListItem(
    val id: Long,
    val podcastId: Long,
    val title: String,
    val pubDate: Long,
    val imageUrl: String?,
    val episodeNumber: Int?,
    val durationMillis: Long?,
    val listened: Boolean,
    val listenedAt: Long?,
    val playbackPosition: Long = 0,
    val lastPlayedTimestamp: Long = 0
)

