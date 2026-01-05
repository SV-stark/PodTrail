package com.example.podtrail.data

import com.example.podtrail.network.FeedParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class PodcastRepository(private val dao: PodcastDao) {
    private val parser = FeedParser()


    fun allPodcasts() = dao.getAllPodcasts()
    
    suspend fun allPodcastsDirect() = dao.getAllPodcasts().first().map { it.podcast }

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
                val safeDesc = it.description?.take(1000) // Truncate to 1000 chars for DB optimization
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
            dao.insertEpisodes(eps) // IGNORE on conflict
            Result.success(podcastId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun episodesForPodcast(podcastId: Long, isAsc: Boolean = false) = 
        if (isAsc) dao.getEpisodesForPodcastLiteAsc(podcastId) else dao.getEpisodesForPodcastLite(podcastId)

    suspend fun getEpisode(id: Long) = dao.getEpisodeById(id)

    fun getHistory() = dao.getHistory()
    
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
        dao.updateEpisode(fullEpisode.copy(
            listened = listened,
            listenedAt = if (listened) System.currentTimeMillis() else null
        ))
    }
    
    // overload for full episode object if needed
    suspend fun markEpisodeListened(episode: Episode, listened: Boolean) {
        dao.updateEpisode(episode.copy(
            listened = listened,
            listenedAt = if (listened) System.currentTimeMillis() else null
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
}
