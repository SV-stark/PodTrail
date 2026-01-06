package com.stark.podtrail.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.stark.podtrail.data.PodcastDatabase
import com.stark.podtrail.data.PodcastRepository
import com.stark.podtrail.data.Episode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch

import kotlinx.coroutines.ExperimentalCoroutinesApi

data class Badge(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val unlocked: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastViewModel(app: Application) : AndroidViewModel(app) {
    private val db = PodcastDatabase.getInstance(app)
    private val repo = PodcastRepository(db.podcastDao())

    val podcasts = repo.allPodcasts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoritePodcasts = repo.favoritePodcasts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val settingsRepo = com.stark.podtrail.data.SettingsRepository(app)

    fun toggleFavorite(podcastId: Long, currentStatus: Boolean) {
        viewModelScope.launch {
            repo.toggleFavorite(podcastId, currentStatus)
        }
    }

    fun setUserName(name: String) {
        viewModelScope.launch {
            settingsRepo.setUserName(name)
        }
    }

    fun addPodcast(feedUrl: String, genre: String? = null, onResult: (Result<Long>) -> Unit) {
        viewModelScope.launch {
            val res = repo.addPodcast(feedUrl, genre)
            onResult(res)
        }
    }

    private val _sortOption = kotlinx.coroutines.flow.MutableStateFlow(SortOption.DATE_NEWEST)
    val sortOption = _sortOption.stateIn(viewModelScope, SharingStarted.Lazily, SortOption.DATE_NEWEST)
    
    private val searcher = com.stark.podtrail.network.ItunesPodcastSearcher()
    private val _searchResults = kotlinx.coroutines.flow.MutableStateFlow<List<com.stark.podtrail.network.SearchResult>>(emptyList())
    val searchResults = _searchResults.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _discoverPodcasts = kotlinx.coroutines.flow.MutableStateFlow<List<com.stark.podtrail.network.SearchResult>>(emptyList())
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
    
    fun subscribeToSearchResult(result: com.stark.podtrail.network.SearchResult) {
         viewModelScope.launch {
             val feedUrl = result.feedUrl ?: result.collectionId?.let { id ->
                 searcher.lookup(id)?.feedUrl
             }
             
             if (feedUrl != null) {
                 repo.addPodcast(feedUrl, result.primaryGenreName)
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

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun episodesFor(podcastId: Long) = _sortOption.flatMapLatest { option ->
        repo.episodesForPodcast(podcastId, option)
    }

    suspend fun getEpisode(id: Long) = repo.getEpisode(id)
    
    fun getEpisodeFlow(id: Long) = repo.getEpisodeFlow(id)

    fun fetchAndUpdateDescription(episodeId: Long) {
        viewModelScope.launch {
            val ep = repo.getEpisode(episodeId) ?: return@launch
            val fullDesc = repo.fetchRemoteEpisodeDescription(ep.podcastId, ep.guid)
            if (fullDesc != null && fullDesc != ep.description) {
                // Update DB with full description
                 repo.markEpisodeListened(ep.copy(description = fullDesc), ep.listened)
                 // NOTE: markEpisodeListened updates the whole object, so this works to save description too.
                 // Ideally should have a dedicated updateEpisode(episode) method but this reuses existing logic safely.
            }
        }
    }

    fun setListened(e: com.stark.podtrail.data.EpisodeListItem, listened: Boolean) {
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
    
    val totalTimeListened = repo.getTotalTimeListened()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0L)
        
    val allEpisodesLite = repo.getAllEpisodesLite()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
    val currentStreak = repo.getCurrentStreak()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
        
    val badges = combine(totalTimeListened, currentStreak, podcasts) { time, streak, podList ->
        listOf(
            Badge(
                "Newbie", 
                "Listen to your first episode", 
                Icons.Default.Mic, 
                time > 0
            ),
            Badge(
                "Regular", 
                "3 Day Streak", 
                Icons.Default.LocalFireDepartment, 
                streak >= 3
            ),
            Badge(
                "Super Fan", 
                "Listen for over 24 hours", 
                Icons.Default.Timer, 
                time >= 24 * 3600 * 1000
            ),
            Badge(
                "Collector",
                "Subscribe to 5 podcasts",
                Icons.Default.Mic,
                podList.size >= 5
            )
            // Night Owl / Early Bird require checking specific listen times history, skipping for MVP complexity
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    private val _isRefreshing = kotlinx.coroutines.flow.MutableStateFlow(false)
    val isRefreshing = _isRefreshing.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun refreshAllPodcasts() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repo.refreshAllPodcasts()
                refreshUpNext()
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
