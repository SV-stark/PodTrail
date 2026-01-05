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
    
    private val searcher = com.example.podtrail.network.ItunesPodcastSearcher()
    private val _searchResults = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.podtrail.network.SearchResult>>(emptyList())
    val searchResults = _searchResults.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _discoverPodcasts = kotlinx.coroutines.flow.MutableStateFlow<List<com.example.podtrail.network.SearchResult>>(emptyList())
    val discoverPodcasts = _discoverPodcasts.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    
    private val _discoverTitle = kotlinx.coroutines.flow.MutableStateFlow("Top Podcasts")
    val discoverTitle = _discoverTitle.stateIn(viewModelScope, SharingStarted.Lazily, "Top Podcasts")

    fun refreshDiscover() {
        viewModelScope.launch {
            // Logic: Pick a random podcast from user's library, find its genre, and get top charts for that genre.
            // If empty, get generic top podcasts.
            val localPodcasts = repo.allPodcastsDirect()
            if (localPodcasts.isNotEmpty()) {
                val randomPod = localPodcasts.random()
                // Lookup genre
                val details = searcher.search(randomPod.title).firstOrNull() 
                if (details?.primaryGenreId != null) {
                    _discoverTitle.value = "Top in ${details.primaryGenreName ?: "suggested"}"
                    _discoverPodcasts.value = searcher.getTopPodcasts(genreId = details.primaryGenreId)
                    return@launch
                }
            }
            
            // Fallback
            _discoverTitle.value = "Top Podcasts"
            _discoverPodcasts.value = searcher.getTopPodcasts()
        }
    }
    
    fun subscribeToSearchResult(result: com.example.podtrail.network.SearchResult) {
         viewModelScope.launch {
             val feedUrl = result.feedUrl ?: result.collectionId?.let { id ->
                 searcher.lookup(id)?.feedUrl
             }
             
             if (feedUrl != null) {
                 repo.addPodcast(feedUrl)
             }
         }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _searchResults.value = searcher.search(query)
        }
    }

    fun clearSearch() {
        _searchResults.value = emptyList()
    }

    fun toggleSortOrder() {
        _sortOrder.value = !_sortOrder.value
    }

    fun episodesFor(podcastId: Long) = _sortOrder.flatMapLatest { isAsc ->
        repo.episodesForPodcast(podcastId, isAsc)
    }

    suspend fun getEpisode(id: Long) = repo.getEpisode(id)

    fun setListened(e: com.example.podtrail.data.EpisodeListItem, listened: Boolean) {
        viewModelScope.launch {
            repo.markEpisodeListened(e, listened)
            refreshUpNext()
        }
    }
    
    // overload for full object
    fun setListened(e: Episode, listened: Boolean) {
        viewModelScope.launch {
            repo.markEpisodeListened(e, listened)
            refreshUpNext()
        }
    }

    val history = repo.getHistory().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _upNext = kotlinx.coroutines.flow.MutableStateFlow<List<Episode>>(emptyList())
    val upNext = _upNext.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    init {
        refreshUpNext()
    }

    private fun refreshUpNext() {
        viewModelScope.launch {
            _upNext.value = repo.getUpNext()
        }
    }

    fun deletePodcast(podcastId: Long) {
        viewModelScope.launch {
            repo.deletePodcast(podcastId)
        }
    }


}