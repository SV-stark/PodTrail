package com.example.podtrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.podtrack.data.PodcastDatabase
import com.example.podtrack.data.PodcastRepository
import com.example.podtrack.data.Episode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PodcastViewModel(app: Application) : AndroidViewModel(app) {
    private val db = PodcastDatabase.getInstance(app)
    private val repo = PodcastRepository(db.podcastDao())

    val podcasts = repo.allPodcasts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addPodcast(feedUrl: String, onResult: (Result<Long>) -> Unit) {
        viewModelScope.launch {
            val res = repo.addPodcast(feedUrl)
            onResult(res)
        }
    }

    fun episodesFor(podcastId: Long) = repo.episodesForPodcast(podcastId)

    fun setListened(e: Episode, listened: Boolean) {
        viewModelScope.launch {
            repo.markEpisodeListened(e, listened)
        }
    }

    fun reportPlaybackProgress(episodeId: Long, positionMillis: Long, durationMillis: Long?) {
        viewModelScope.launch {
            repo.reportPlaybackProgress(episodeId, positionMillis, durationMillis)
        }
    }
}