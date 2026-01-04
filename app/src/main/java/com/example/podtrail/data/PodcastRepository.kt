package com.example.podtrail.data

import com.example.podtrail.network.FeedParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PodcastRepository(private val dao: PodcastDao) {
    private val parser = FeedParser()
    private val MARK_LISTENED_THRESHOLD = 0.90 // 90%

    fun allPodcasts() = dao.getAllPodcasts()

    suspend fun addPodcast(feedUrl: String): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val (title, episodes) = parser.fetchFeed(feedUrl)
            val podcastTitle = title ?: feedUrl

            // If podcast exists, reuse id; otherwise insert
            val existing = dao.getPodcastByFeedUrl(feedUrl)
            val podcastId = existing?.id ?: dao.insertPodcast(Podcast(title = podcastTitle, feedUrl = feedUrl)).let { id ->
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
                    episodeNumber = it.episodeNumber,
                    durationMillis = it.durationMillis
                )
            }
            dao.insertEpisodes(eps) // IGNORE on conflict
            Result.success(podcastId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun episodesForPodcast(podcastId: Long) = dao.getEpisodesForPodcast(podcastId)

    suspend fun markEpisodeListened(episode: Episode, listened: Boolean) {
        dao.updateEpisode(episode.copy(listened = listened))
    }

    suspend fun reportPlaybackProgress(episodeId: Long, positionMillis: Long, durationMillis: Long?) {
        withContext(Dispatchers.IO) {
            val ep = dao.getEpisodeById(episodeId) ?: return@withContext
            val dur = durationMillis ?: ep.durationMillis
            val listened = if (dur != null && dur > 0) {
                positionMillis.toDouble() / dur.toDouble() >= MARK_LISTENED_THRESHOLD
            } else ep.listened
            dao.updateEpisode(ep.copy(playbackPositionMillis = positionMillis, durationMillis = dur, listened = listened))
        }
    }
}