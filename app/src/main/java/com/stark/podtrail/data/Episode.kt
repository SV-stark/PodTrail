package com.stark.podtrail.data

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Index

@Entity(
    tableName = "episodes",
    indices = [
        Index(value = ["guid"], unique = true),
        Index(value = ["podcastId"]),
        Index(value = ["listened", "lastPlayedTimestamp"])
    ]
)
data class Episode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val podcastId: Long,
    val title: String,
    val guid: String,
    val pubDate: Long = 0,
    val audioUrl: String? = null,
    val imageUrl: String? = null,

    // iTunes / enhanced fields:
    val episodeNumber: Int? = null,
    val durationMillis: Long? = null,
    val description: String? = null,

    // playback tracking:
    val listened: Boolean = false,
    val listenedAt: Long? = null,
    val playbackPosition: Long = 0,
    val lastPlayedTimestamp: Long = 0
)
