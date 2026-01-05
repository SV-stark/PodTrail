package com.stark.podtrail.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PodcastDao {
    @Transaction
    @Query("""
        SELECT 
            p.*, 
            COUNT(e.id) as totalEpisodes,
            SUM(CASE WHEN e.listened = 1 THEN 1 ELSE 0 END) as listenedEpisodes
        FROM podcasts p
        LEFT JOIN episodes e ON p.id = e.podcastId
        GROUP BY p.id
        ORDER BY p.title
    """)
    fun getAllPodcasts(): Flow<List<PodcastWithStats>>

    @Query("SELECT * FROM podcasts WHERE isFavorite = 1")
    fun getFavoritePodcasts(): Flow<List<Podcast>>

    @Query("UPDATE podcasts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPodcast(podcast: Podcast): Long

    @Query("SELECT * FROM podcasts WHERE feedUrl = :feedUrl LIMIT 1")
    suspend fun getPodcastByFeedUrl(feedUrl: String): Podcast?

    @Query("SELECT * FROM podcasts WHERE id = :id LIMIT 1")
    suspend fun getPodcastById(id: Long): Podcast?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEpisodes(episodes: List<Episode>)

    @androidx.room.Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY pubDate DESC")
    fun getEpisodesForPodcast(podcastId: Long): kotlinx.coroutines.flow.Flow<List<Episode>>

    @androidx.room.Query("SELECT id, podcastId, title, pubDate, imageUrl, episodeNumber, durationMillis, listened, listenedAt, playbackPosition, lastPlayedTimestamp FROM episodes WHERE podcastId = :podcastId ORDER BY pubDate DESC")
    fun getEpisodesForPodcastLite(podcastId: Long): kotlinx.coroutines.flow.Flow<List<EpisodeListItem>>

    @androidx.room.Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY pubDate ASC")
    fun getEpisodesForPodcastAsc(podcastId: Long): kotlinx.coroutines.flow.Flow<List<Episode>>
    
    @androidx.room.Query("SELECT id, podcastId, title, pubDate, imageUrl, episodeNumber, durationMillis, listened, listenedAt, playbackPosition, lastPlayedTimestamp FROM episodes WHERE podcastId = :podcastId ORDER BY pubDate ASC")
    fun getEpisodesForPodcastLiteAsc(podcastId: Long): kotlinx.coroutines.flow.Flow<List<EpisodeListItem>>

    @androidx.room.Query("SELECT id, podcastId, title, pubDate, imageUrl, episodeNumber, durationMillis, listened, listenedAt, playbackPosition, lastPlayedTimestamp FROM episodes WHERE podcastId = :podcastId ORDER BY durationMillis ASC")
    fun getEpisodesForPodcastLiteDurationAsc(podcastId: Long): kotlinx.coroutines.flow.Flow<List<EpisodeListItem>>

    @androidx.room.Query("SELECT id, podcastId, title, pubDate, imageUrl, episodeNumber, durationMillis, listened, listenedAt, playbackPosition, lastPlayedTimestamp FROM episodes WHERE podcastId = :podcastId ORDER BY durationMillis DESC")
    fun getEpisodesForPodcastLiteDurationDesc(podcastId: Long): kotlinx.coroutines.flow.Flow<List<EpisodeListItem>>

    @androidx.room.Query("SELECT * FROM episodes WHERE listened = 1 ORDER BY listenedAt DESC")
    fun getHistory(): kotlinx.coroutines.flow.Flow<List<Episode>>

    @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
    suspend fun getEpisodeById(episodeId: Long): Episode?

    @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
    fun getEpisodeByIdFlow(episodeId: Long): kotlinx.coroutines.flow.Flow<Episode?>

    @Update
    suspend fun updateEpisode(episode: Episode)

    @Query("DELETE FROM podcasts")
    suspend fun deleteAllPodcasts()

    @Query("DELETE FROM episodes")
    suspend fun deleteAllEpisodes()

    @Query("DELETE FROM podcasts WHERE id = :podcastId")
    suspend fun deletePodcastById(podcastId: Long)

    @Query("DELETE FROM episodes WHERE podcastId = :podcastId")
    suspend fun deleteEpisodesByPodcastId(podcastId: Long)

    @Transaction
    suspend fun deletePodcast(podcastId: Long) {
        deleteEpisodesByPodcastId(podcastId)
        deletePodcastById(podcastId)
    }

    @Query("SELECT SUM(durationMillis) FROM episodes WHERE listened = 1")
    fun getTotalDurationListened(): Flow<Long?>

    @androidx.room.Query("SELECT id, podcastId, title, pubDate, imageUrl, episodeNumber, durationMillis, listened, listenedAt, playbackPosition, lastPlayedTimestamp FROM episodes ORDER BY pubDate DESC")
    fun getAllEpisodesLite(): Flow<List<EpisodeListItem>>

    @Query("SELECT lastPlayedTimestamp FROM episodes WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC")
    fun getAllLastPlayedTimestamps(): Flow<List<Long>>

    // --- Bulk Export/Import ---

    @Query("SELECT * FROM podcasts")
    suspend fun getAllPodcastsSync(): List<Podcast>

    @Query("SELECT * FROM episodes")
    suspend fun getAllEpisodesSync(): List<Episode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPodcasts(podcasts: List<Podcast>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllEpisodes(episodes: List<Episode>)
}
