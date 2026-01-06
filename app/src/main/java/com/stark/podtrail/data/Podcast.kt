package com.stark.podtrail.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Embedded

@Entity(tableName = "podcasts")
data class Podcast(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val feedUrl: String,
    val imageUrl: String? = null,
    val description: String? = null,
    val primaryGenre: String? = null,
    val isFavorite: Boolean = false
)

data class PodcastWithStats(
    @Embedded val podcast: Podcast,
    val totalEpisodes: Int,
    val listenedEpisodes: Int,
    val timeListened: Long? = 0L // New field
)
