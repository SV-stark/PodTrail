package com.stark.podtrail.data

import com.stark.podtrail.network.FeedParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

class PodcastRepository(private val dao: PodcastDao) {
    private val parser = FeedParser()


    fun allPodcasts() = dao.getAllPodcasts()
    
    suspend fun allPodcastsDirect() = dao.getAllPodcasts().first().map { it.podcast }

    fun favoritePodcasts() = dao.getFavoritePodcasts()

    suspend fun toggleFavorite(id: Long, currentStatus: Boolean) {
        dao.updateFavoriteStatus(id, !currentStatus)
    }

    suspend fun addPodcast(feedUrl: String, genre: String? = null): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val (mappedPodcast, episodes) = parser.fetchFeed(feedUrl)
            val podcastTitle = mappedPodcast?.title ?: feedUrl
            val podcastImage = mappedPodcast?.imageUrl
            val podcastDesc = mappedPodcast?.description
            val podcastGenre = genre ?: mappedPodcast?.genre ?: "Uncategorized"
            
            // If podcast exists, reuse id; otherwise insert
            val existing = dao.getPodcastByFeedUrl(feedUrl)
            val podcastId = existing?.id ?: dao.insertPodcast(Podcast(
                title = podcastTitle, 
                feedUrl = feedUrl, 
                imageUrl = podcastImage,
                description = podcastDesc,
                primaryGenre = podcastGenre
            )).let { id ->
                if (id <= 0 && existing != null) existing.id else id
            }

            if (podcastId <= 0) return@withContext Result.failure<Long>(Exception("Failed to insert podcast"))

            val eps = episodes.map {
                val safeDesc = it.description?.take(200) // Store only a snippet initially to save space
                Episode(
                    podcastId = podcastId,
                    title = it.title,
                    guid = it.guid,
                    pubDate = it.pubDateMillis,
                    audioUrl = it.audioUrl,
                    imageUrl = it.imageUrl ?: podcastImage, // fallback to podcast image if episode image missing
                    episodeNumber = it.episodeNumber,
                    durationMillis = it.durationMillis,
                    description = safeDesc
                )
            }
            dao.upsertEpisodesMetadata(eps)
            Result.success(podcastId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshAllPodcasts() = withContext(Dispatchers.IO) {
        val podcasts = dao.getAllPodcasts().first().map { it.podcast }
        podcasts.forEach { podcast ->
            try {
                val (mappedPodcast, episodes) = parser.fetchFeed(podcast.feedUrl)
                
                // Update podcast details if changed (e.g. image, title, description)
                if (mappedPodcast != null) {
                     val updatedPodcast = podcast.copy(
                        title = mappedPodcast.title ?: podcast.title,
                        imageUrl = mappedPodcast.imageUrl,
                        description = mappedPodcast.description,
                        primaryGenre = mappedPodcast.genre ?: podcast.primaryGenre ?: "Uncategorized"
                    )
                    // Only update if something relevant successfully parsed and is different
                    if (updatedPodcast != podcast) {
                        dao.updatePodcast(updatedPodcast)
                    }
                }

                val eps = episodes.map {
                     val safeDesc = it.description?.take(200)
                     Episode(
                        podcastId = podcast.id,
                        title = it.title,
                        guid = it.guid,
                        pubDate = it.pubDateMillis,
                        audioUrl = it.audioUrl,
                        imageUrl = it.imageUrl ?: mappedPodcast?.imageUrl ?: podcast.imageUrl,
                        episodeNumber = it.episodeNumber,
                        durationMillis = it.durationMillis,
                        description = safeDesc
                    )
                }
                dao.upsertEpisodesMetadata(eps)
            } catch (e: Exception) {
                // Ignore failure for individual podcast refresh
                e.printStackTrace()
            }
        }
    }

    fun episodesForPodcast(podcastId: Long, sortOption: com.stark.podtrail.ui.SortOption = com.stark.podtrail.ui.SortOption.DATE_NEWEST) = 
        when (sortOption) {
            com.stark.podtrail.ui.SortOption.DATE_NEWEST -> dao.getEpisodesForPodcastLite(podcastId)
            com.stark.podtrail.ui.SortOption.DATE_OLDEST -> dao.getEpisodesForPodcastLiteAsc(podcastId)
            com.stark.podtrail.ui.SortOption.DURATION_SHORTEST -> dao.getEpisodesForPodcastLiteDurationAsc(podcastId)
            com.stark.podtrail.ui.SortOption.DURATION_LONGEST -> dao.getEpisodesForPodcastLiteDurationDesc(podcastId)
        }

    suspend fun getEpisode(id: Long) = dao.getEpisodeById(id)
    
    fun getEpisodeFlow(id: Long) = dao.getEpisodeByIdFlow(id)

    fun getHistory() = dao.getHistory()
    
    fun getAllEpisodesLite() = dao.getAllEpisodesLite()

    fun getTotalTimeListened() = dao.getTotalDurationListened().map { it ?: 0L }

    fun getCurrentStreak() = dao.getAllLastPlayedTimestamps().map { timestamps ->
        if (timestamps.isEmpty()) return@map 0
        
        val uniqueDays = sortedSetOf<Long>()
        val cal = Calendar.getInstance()
        
        timestamps.forEach { ts ->
            cal.timeInMillis = ts
            // Create a unique day key: YYYYMMDD or just epoch day (local)
            // Using year * 1000 + dayOfYear for simplicity/uniqueness
            val dayKey = (cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)).toLong()
            uniqueDays.add(dayKey)
        }
        
        // Calculate streak
        var streak = 0
        val rightNow = Calendar.getInstance()
        val todayKey = (rightNow.get(Calendar.YEAR) * 1000 + rightNow.get(Calendar.DAY_OF_YEAR)).toLong()
        
        // Check today or yesterday to start streak
        if (uniqueDays.contains(todayKey)) {
            streak++
            // Check previous days
            var checkDay = Calendar.getInstance()
            checkDay.add(Calendar.DAY_OF_YEAR, -1)
            while (true) {
                 val key = (checkDay.get(Calendar.YEAR) * 1000 + checkDay.get(Calendar.DAY_OF_YEAR)).toLong()
                 if (uniqueDays.contains(key)) {
                     streak++
                     checkDay.add(Calendar.DAY_OF_YEAR, -1)
                 } else {
                     break
                 }
            }
        } else {
            // Check if yesterday was active (streak continues but not incremented for today yet? 
            // Usually streak implies consecutive days ending Today or Yesterday.
            // If I listened yesterday, my streak is valid.
            var checkDay = Calendar.getInstance()
            checkDay.add(Calendar.DAY_OF_YEAR, -1)
            val yesterdayKey = (checkDay.get(Calendar.YEAR) * 1000 + checkDay.get(Calendar.DAY_OF_YEAR)).toLong()
            
            if (uniqueDays.contains(yesterdayKey)) {
                streak++ // Counts yesterday
                checkDay.add(Calendar.DAY_OF_YEAR, -1) 
                 while (true) {
                     val key = (checkDay.get(Calendar.YEAR) * 1000 + checkDay.get(Calendar.DAY_OF_YEAR)).toLong()
                     if (uniqueDays.contains(key)) {
                         streak++
                         checkDay.add(Calendar.DAY_OF_YEAR, -1)
                     } else {
                         break
                     }
                }
            }
        }
        
        streak
    }

    fun getTopPodcastsByDuration() = dao.getTopPodcastsByDuration()

    fun getLast7DaysActivity(): Flow<Map<Int, Long>> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6) // 7 days inclusive
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        val since = cal.timeInMillis

        return dao.getListenedEpisodesSince(since).map { activityData ->
             val dailyMap = mutableMapOf<Int, Long>()
             // Initialize last 7 days with 0
             for (i in 0..6) {
                 val dayCal = Calendar.getInstance()
                 dayCal.add(Calendar.DAY_OF_YEAR, -i)
                 val dayKey = dayCal.get(Calendar.DAY_OF_YEAR)
                 dailyMap[dayKey] = 0L
             }

             activityData.forEach { data ->
                 val c = Calendar.getInstance()
                 c.timeInMillis = data.lastPlayedTimestamp
                 val dayKey = c.get(Calendar.DAY_OF_YEAR)
                 // Only sum up if it's within our window (query handles this mostly, but exact day boundary might vary)
                 if (dailyMap.containsKey(dayKey)) {
                     dailyMap[dayKey] = (dailyMap[dayKey] ?: 0L) + (data.durationMillis ?: 0L)
                 }
             }
             dailyMap
        }
    }
    
    suspend fun getUpNext(): List<Episode> = withContext(Dispatchers.IO) {
        val podcasts = dao.getAllPodcasts().first()
        val upNextList = mutableListOf<Episode>()
        for (p in podcasts) {
            val episodes = dao.getEpisodesForPodcastAsc(p.podcast.id).first()
            val next = episodes.firstOrNull { !it.listened }
            if (next != null) {
                upNextList.add(next)
            }
        }
        upNextList
    }

    suspend fun markEpisodeListened(episode: EpisodeListItem, listened: Boolean) {
        val fullEpisode = dao.getEpisodeById(episode.id) ?: return
        val now = System.currentTimeMillis()
        dao.updateEpisode(fullEpisode.copy(
            listened = listened,
            listenedAt = if (listened) now else null,
            lastPlayedTimestamp = if (listened) now else fullEpisode.lastPlayedTimestamp,
            playbackPosition = if (listened) 0 else fullEpisode.playbackPosition
        ))
    }
    
    // overload for full episode object if needed
    suspend fun markEpisodeListened(episode: Episode, listened: Boolean) {
        val now = System.currentTimeMillis()
        dao.updateEpisode(episode.copy(
            listened = listened,
            listenedAt = if (listened) now else null,
            lastPlayedTimestamp = if (listened) now else episode.lastPlayedTimestamp,
            playbackPosition = if (listened) 0 else episode.playbackPosition
        ))
    }

    suspend fun deletePodcast(podcastId: Long) {
        dao.deletePodcast(podcastId)
    }

    suspend fun fetchRemoteEpisodeDescription(podcastId: Long, episodeGuid: String): String? = withContext(Dispatchers.IO) {
        val podcast = dao.getPodcastById(podcastId) ?: return@withContext null
        try {
            val (_, episodes) = parser.fetchFeed(podcast.feedUrl)
            val match = episodes.find { (it.guid == episodeGuid) || (it.title == episodeGuid) } // guid fallback to title
            match?.description
        } catch (e: Exception) {
            null
        }
    }

    suspend fun checkAndFixRestoringEpisodes() = withContext(Dispatchers.IO) {
        val count = dao.getRestoringCount()
        if (count > 0) {
            refreshAllPodcasts()
        }
    }
}

