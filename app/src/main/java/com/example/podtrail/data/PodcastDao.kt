package com.example.podtrail.data

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

    @androidx.room.Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY pubDate ASC")
    fun getEpisodesForPodcastAsc(podcastId: Long): kotlinx.coroutines.flow.Flow<List<Episode>>

    @androidx.room.Query("SELECT * FROM episodes WHERE listened = 1 ORDER BY listenedAt DESC")
    fun getHistory(): kotlinx.coroutines.flow.Flow<List<Episode>>

    @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
    suspend fun getEpisodeById(episodeId: Long): Episode?

    @Update
    suspend fun updateEpisode(episode: Episode)

    @Query("DELETE FROM podcasts")
    suspend fun deleteAllPodcasts()

    @Query("DELETE FROM episodes")
    suspend fun deleteAllEpisodes()
}