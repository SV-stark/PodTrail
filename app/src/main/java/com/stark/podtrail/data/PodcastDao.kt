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
            SUM(CASE WHEN e.listened = 1 THEN 1 ELSE 0 END) as listenedEpisodes,
            SUM(CASE WHEN e.listened = 1 THEN e.durationMillis ELSE 0 END) as timeListened
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

    @Query("UPDATE episodes SET listened = :listened, listenedAt = :timestamp WHERE podcastId = :podcastId")
    abstract suspend fun markPodcastEpisodesListened(podcastId: Long, listened: Boolean, timestamp: Long)

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

    @Query("""
        SELECT 
            p.*, 
            COUNT(e.id) as totalEpisodes,
            SUM(CASE WHEN e.listened = 1 THEN 1 ELSE 0 END) as listenedEpisodes,
            SUM(CASE WHEN e.listened = 1 THEN e.durationMillis ELSE 0 END) as timeListened
        FROM podcasts p
        LEFT JOIN episodes e ON p.id = e.podcastId
        GROUP BY p.id
        ORDER BY timeListened DESC
        LIMIT 5
    """)
    abstract fun getTopPodcastsByDuration(): Flow<List<PodcastWithStats>>

    @Query("SELECT lastPlayedTimestamp, durationMillis FROM episodes WHERE listened = 1 AND lastPlayedTimestamp > :since")
    abstract fun getListenedEpisodesSince(since: Long): Flow<List<EpisodeActivityData>>

    // Playlist operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPlaylist(playlist: Playlist): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPlaylistCollection(collection: PlaylistCollection): Long

    @Query("SELECT * FROM playlist_collections ORDER BY position ASC")
    abstract fun getAllPlaylistCollections(): Flow<List<PlaylistCollection>>

    @Query("""
        SELECT p.*, e.* FROM playlists p 
        INNER JOIN episodes e ON p.episodeId = e.id 
        WHERE p.name = :collectionName 
        ORDER BY p.position ASC
    """)
    abstract fun getEpisodesInPlaylist(collectionName: String): Flow<List<Episode>>

    @Query("DELETE FROM playlists WHERE episodeId = :episodeId")
    abstract suspend fun removeFromPlaylist(episodeId: Long)

    @Query("DELETE FROM playlist_collections WHERE id = :collectionId")
    abstract suspend fun deletePlaylistCollection(collectionId: Long)

    @Query("UPDATE playlists SET position = :position WHERE id = :id")
    abstract suspend fun updatePlaylistPosition(id: Long, position: Int)

    @Query("UPDATE playlist_collections SET position = :position WHERE id = :id")
    abstract suspend fun updatePlaylistCollectionPosition(id: Long, position: Int)

    // Recommendations based on listening history
    @Query("""
        SELECT e.*, p.title as podcastTitle, p.imageUrl as podcastImage, p.primaryGenre 
        FROM episodes e 
        INNER JOIN podcasts p ON e.podcastId = p.id 
        WHERE e.listened = 1 
        ORDER BY e.listenedAt DESC 
        LIMIT 50
    """)
    abstract suspend fun getRecentListenedEpisodes(): List<EpisodeWithPodcastInfo>

    @Query("""
        SELECT p.* FROM podcasts p
        INNER JOIN episodes e ON p.id = e.podcastId
        WHERE e.listened = 1
        GROUP BY p.id
        ORDER BY COUNT(e.id) DESC
        LIMIT 5
    """)
    abstract suspend fun getMostListenedPodcasts(): List<Podcast>

    @Query("""
        SELECT p.* FROM podcasts p
        INNER JOIN episodes e ON p.id = e.podcastId
        WHERE p.primaryGenre = :genre AND e.listened = 0
        GROUP BY p.id
        ORDER BY RANDOM()
        LIMIT 3
    """)
    abstract suspend fun getRecommendationsByGenre(genre: String): List<Podcast>

    @androidx.room.Query("""
        SELECT id, podcastId, title, pubDate, imageUrl, episodeNumber, durationMillis, listened, listenedAt, playbackPosition, lastPlayedTimestamp 
        FROM episodes 
        WHERE pubDate >= :startTime AND pubDate < :endTime
        ORDER BY pubDate DESC
    """)
    abstract fun getEpisodesForMonth(startTime: Long, endTime: Long): Flow<List<EpisodeListItem>>

    @androidx.room.Query("""
        SELECT id, podcastId, title, pubDate, imageUrl, episodeNumber, durationMillis, listened, listenedAt, playbackPosition, lastPlayedTimestamp 
        FROM episodes 
        WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'
        ORDER BY pubDate DESC
    """)
    abstract fun searchEpisodes(query: String): Flow<List<EpisodeListItem>>

    @androidx.room.Query("""
        SELECT id, podcastId, title, pubDate, imageUrl, episodeNumber, durationMillis, listened, listenedAt, playbackPosition, lastPlayedTimestamp 
        FROM episodes 
        WHERE podcastId = :podcastId AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY pubDate DESC
    """)
    abstract fun searchEpisodesInPodcast(podcastId: Long, query: String): Flow<List<EpisodeListItem>>

    @androidx.room.Query("SELECT * FROM podcasts WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    abstract fun searchPodcasts(query: String): Flow<List<Podcast>>
}

data class EpisodeActivityData(
    val lastPlayedTimestamp: Long,
    val durationMillis: Long?
)

data class EpisodeWithPodcastInfo(
    val id: Long,
    val podcastId: Long,
    val title: String,
    val description: String?,
    val audioUrl: String,
    val imageUrl: String,
    val episodeNumber: Int?,
    val durationMillis: Long?,
    val pubDate: Long,
    val guid: String,
    val listened: Boolean,
    val listenedAt: Long?,
    val playbackPosition: Long,
    val lastPlayedTimestamp: Long,
    val podcastTitle: String,
    val podcastImage: String,
    val primaryGenre: String
)
