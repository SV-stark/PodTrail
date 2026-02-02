package com.stark.podtrail.data

import androidx.room.*

@Entity(
    tableName = "playlists",
    foreignKeys = [
        ForeignKey(
            entity = Episode::class,
            parentColumns = ["id"],
            childColumns = ["episodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Playlist(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val episodeId: Long,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "playlist_collections"
)
data class PlaylistCollection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val position: Int = 0
)

data class PlaylistWithDetails(
    val playlist: Playlist,
    val episode: Episode
)

data class PlaylistCollectionWithItems(
    val collection: PlaylistCollection,
    val episodes: List<Episode>
)