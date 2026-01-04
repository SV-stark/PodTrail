package com.example.podtrail.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "episodes",
    indices = [Index(value = ["guid"], unique = true)]
)
data class Episode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val podcastId: Long,
    val title: String,
    val guid: String,
    val pubDate: Long = 0,
    val audioUrl: String? = null,

    // iTunes / enhanced fields:
    val episodeNumber: Int? = null,
    val durationMillis: Long? = null,

    // playback tracking:
    val playbackPositionMillis: Long = 0,
    val listened: Boolean = false
)