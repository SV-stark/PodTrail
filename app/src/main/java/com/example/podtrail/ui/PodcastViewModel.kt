package com.example.podtrail.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.podtrail.data.PodcastDatabase
import com.example.podtrail.data.PodcastRepository
import com.example.podtrail.data.Episode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
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

    private val _sortOrder = kotlinx.coroutines.flow.MutableStateFlow(false) // false = DESC, true = ASC
    val sortOrder = _sortOrder.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun toggleSortOrder() {
        _sortOrder.value = !_sortOrder.value
    }

    fun episodesFor(podcastId: Long) = _sortOrder.flatMapLatest { isAsc ->
        repo.episodesForPodcast(podcastId, isAsc)
    }

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