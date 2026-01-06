package com.stark.podtrail.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PodcastDao {
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
    abstract fun getAllPodcasts(): Flow<List<PodcastWithStats>>

    @Query("SELECT * FROM podcasts WHERE isFavorite = 1")
    abstract fun getFavoritePodcasts(): Flow<List<Podcast>>

    @Query("UPDATE podcasts SET isFavorite = :isFavorite WHERE id = :id")
    abstract suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertPodcast(podcast: Podcast): Long

    @Query("SELECT * FROM podcasts WHERE feedUrl = :feedUrl LIMIT 1")
    abstract suspend fun getPodcastByFeedUrl(feedUrl: String): Podcast?

    @Query("SELECT * FROM podcasts WHERE id = :id LIMIT 1")
    abstract suspend fun getPodcastById(id: Long): Podcast?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertEpisodes(episodes: List<Episode>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertEpisode(episode: Episode): Long

    @androidx.room.Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY pubDate DESC")
    abstract fun getEpisodesForPodcast(podcastId: Long): kotlinx.coroutines.flow.Flow<List<Episode>>

    @androidx.room.Query("SELECT id, podcastId, title, pubDate, imageUrl, episodeNumber, durationMillis, listened, listenedAt, playbackPosition, lastPlayedTimestamp FROM episodes WHERE podcastId = :podcastId ORDER BY pubDate DESC")
    abstract fun getEpisodesForPodcastLite(podcastId: Long): kotlinx.coroutines.flow.Flow<List<EpisodeListItem>>

    @androidx.room.Query("SELECT * FROM episodes WHERE podcastId = :podcastId ORDER BY pubDate ASC")
    abstract fun getEpisodesForPodcastAsc(podcastId: Long): kotlinx.coroutines.flow.Flow<List<Episode>>
    
    @androidx.room.Query("SELECT id, podcastId, title, pubDate, imageUrl, episodeNumber, durationMillis, listened, listenedAt, playbackPosition, lastPlayedTimestamp FROM episodes WHERE podcastId = :podcastId ORDER BY pubDate ASC")
    abstract fun getEpisodesForPodcastLiteAsc(podcastId: Long): kotlinx.coroutines.flow.Flow<List<EpisodeListItem>>

    @androidx.room.Query("SELECT id, podcastId, title, pubDate, imageUrl, episodeNumber, durationMillis, listened, listenedAt, playbackPosition, lastPlayedTimestamp FROM episodes WHERE podcastId = :podcastId ORDER BY durationMillis ASC")
    abstract fun getEpisodesForPodcastLiteDurationAsc(podcastId: Long): kotlinx.coroutines.flow.Flow<List<EpisodeListItem>>

    @androidx.room.Query("SELECT id, podcastId, title, pubDate, imageUrl, episodeNumber, durationMillis, listened, listenedAt, playbackPosition, lastPlayedTimestamp FROM episodes WHERE podcastId = :podcastId ORDER BY durationMillis DESC")
    abstract fun getEpisodesForPodcastLiteDurationDesc(podcastId: Long): kotlinx.coroutines.flow.Flow<List<EpisodeListItem>>

    @androidx.room.Query("SELECT * FROM episodes WHERE listened = 1 ORDER BY listenedAt DESC")
    abstract fun getHistory(): kotlinx.coroutines.flow.Flow<List<Episode>>

    @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
    abstract suspend fun getEpisodeById(episodeId: Long): Episode?

    @Query("SELECT * FROM episodes WHERE id = :episodeId LIMIT 1")
    abstract fun getEpisodeByIdFlow(episodeId: Long): kotlinx.coroutines.flow.Flow<Episode?>

    @Update
    abstract suspend fun updateEpisode(episode: Episode)

    @Update
    abstract suspend fun updatePodcast(podcast: Podcast)

    @Query("DELETE FROM podcasts")
    abstract suspend fun deleteAllPodcasts()

    @Query("DELETE FROM episodes")
    abstract suspend fun deleteAllEpisodes()

    @Query("DELETE FROM podcasts WHERE id = :podcastId")
    abstract suspend fun deletePodcastById(podcastId: Long)

    @Query("DELETE FROM episodes WHERE podcastId = :podcastId")
    abstract suspend fun deleteEpisodesByPodcastId(podcastId: Long)

    @Transaction
    open suspend fun deletePodcast(podcastId: Long) {
        deleteEpisodesByPodcastId(podcastId)
        deletePodcastById(podcastId)
    }

    @Query("SELECT SUM(durationMillis) FROM episodes WHERE listened = 1")
    abstract fun getTotalDurationListened(): Flow<Long?>

    @androidx.room.Query("SELECT id, podcastId, title, pubDate, imageUrl, episodeNumber, durationMillis, listened, listenedAt, playbackPosition, lastPlayedTimestamp FROM episodes ORDER BY pubDate DESC")
    abstract fun getAllEpisodesLite(): Flow<List<EpisodeListItem>>

    @Query("SELECT lastPlayedTimestamp FROM episodes WHERE lastPlayedTimestamp > 0 ORDER BY lastPlayedTimestamp DESC")
    abstract fun getAllLastPlayedTimestamps(): Flow<List<Long>>

    // --- Bulk Export/Import ---

    @Query("SELECT * FROM podcasts")
    abstract suspend fun getAllPodcastsSync(): List<Podcast>

    @Query("SELECT * FROM episodes")
    abstract suspend fun getAllEpisodesSync(): List<Episode>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPodcasts(podcasts: List<Podcast>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAllEpisodes(episodes: List<Episode>)

    @Query("SELECT * FROM episodes WHERE guid = :guid LIMIT 1")
    abstract suspend fun getEpisodeByGuid(guid: String): Episode?

    @Query("SELECT COUNT(*) FROM episodes WHERE title = 'Restoring...'")
    abstract suspend fun getRestoringCount(): Int

    @Transaction
    open suspend fun upsertEpisodesMetadata(newEpisodes: List<Episode>) {
        for (newEp in newEpisodes) {
            val existing = getEpisodeByGuid(newEp.guid)
            if (existing != null) {
                // Update metadata, preserve playback state
                val updated = existing.copy(
                    title = newEp.title,
                    pubDate = newEp.pubDate,
                    audioUrl = newEp.audioUrl,
                    imageUrl = newEp.imageUrl,
                    episodeNumber = newEp.episodeNumber,
                    durationMillis = newEp.durationMillis,
                    description = newEp.description
                    // listened, playbackPosition, lastPlayedTimestamp are from 'existing'
                )
                updateEpisode(updated)
            } else {
                insertEpisode(newEp)
            }
        }
    }
}
