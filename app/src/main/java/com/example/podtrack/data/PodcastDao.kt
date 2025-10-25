package com.example.podtrack.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Query("SELECT * FROM podcasts ORDER BY title")
    fun getAllPodcasts(): Flow<List<Podcast>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPodcast(podcast: Podcast): Long

    @Query("SELECT * FROM podcasts WHERE feedUrl = :feedUrl LIMIT 1")
    suspend fun getPodcastByFeedUrl(feedUrl: String): Podcast?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEpisodes(episodes: List<Episode>)

    @Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY pubDate DESC")
    fun getEpisodesForPodcast(podcastId: Long): Flow<List<Episode>>

    @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
    suspend fun getEpisodeById(episodeId: Long): Episode?

    @Update
    suspend fun updateEpisode(episode: Episode)
}