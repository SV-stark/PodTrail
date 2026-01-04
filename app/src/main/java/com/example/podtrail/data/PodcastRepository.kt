package com.example.podtrail.data

import com.example.podtrail.network.FeedParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first

class PodcastRepository(private val dao: PodcastDao) {
    private val parser = FeedParser()


    fun allPodcasts() = dao.getAllPodcasts()

    suspend fun addPodcast(feedUrl: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val (mappedPodcast, episodes) = parser.fetchFeed(feedUrl)
            val podcastTitle = mappedPodcast?.title ?: feedUrl
            val podcastImage = mappedPodcast?.imageUrl
            
            // If podcast exists, reuse id; otherwise insert
            val existing = dao.getPodcastByFeedUrl(feedUrl)
            val podcastId = existing?.id ?: dao.insertPodcast(Podcast(title = podcastTitle, feedUrl = feedUrl, imageUrl = podcastImage)).let { id ->
                if (id <= 0 && existing != null) existing.id else id
            }

            if (podcastId <= 0) return@withContext Result.failure<Long>(Exception("Failed to insert podcast"))

            val eps = episodes.map {
                Episode(
                    podcastId = podcastId,
                    title = it.title,
                    guid = it.guid,
                    pubDate = it.pubDateMillis,
                    audioUrl = it.audioUrl,
                    imageUrl = it.imageUrl ?: podcastImage, // fallback to podcast image if episode image missing
                    episodeNumber = it.episodeNumber,
                    durationMillis = it.durationMillis,
                    description = it.description
                )
            }
            dao.insertEpisodes(eps) // IGNORE on conflict
            Result.success(podcastId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun episodesForPodcast(podcastId: Long, isAsc: Boolean = false) = 
        if (isAsc) dao.getEpisodesForPodcastAsc(podcastId) else dao.getEpisodesForPodcast(podcastId)

    fun getHistory() = dao.getHistory()
    
    suspend fun getUpNext(): List<Episode> = withContext(Dispatchers.IO) {
        val podcasts = dao.getAllPodcasts().first()
        val upNextList = mutableListOf<Episode>()
        for (p in podcasts) {
            val episodes = dao.getEpisodesForPodcastAsc(p.id).first()
            val next = episodes.firstOrNull { !it.listened }
            if (next != null) {
                upNextList.add(next)
            }
        }
        upNextList
    }

    suspend fun markEpisodeListened(episode: Episode, listened: Boolean) {
        dao.updateEpisode(episode.copy(
            listened = listened,
            listenedAt = if (listened) System.currentTimeMillis() else null
        ))
    }
}
