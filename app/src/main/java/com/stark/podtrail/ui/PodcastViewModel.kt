package com.stark.podtrail.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stark.podtrail.data.PodcastRepository
import com.stark.podtrail.data.Episode
import com.stark.podtrail.data.EpisodeListItem
import com.stark.podtrail.data.SortOption
import com.stark.podtrail.data.SettingsRepository
import com.stark.podtrail.network.ItunesPodcastSearcher
import com.stark.podtrail.network.SearchResult
import kotlinx.coroutines.flow.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.FlowPreview
import androidx.paging.cachedIn

data class Badge(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val unlocked: Boolean
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class PodcastViewModel @Inject constructor(
    val repo: PodcastRepository,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    val podcasts = repo.allPodcasts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val favoritePodcasts = repo.favoritePodcasts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

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

    private val _sortOption = MutableStateFlow(SortOption.DATE_NEWEST)
    val sortOption = _sortOption.asStateFlow()
    
    private val searcher = ItunesPodcastSearcher()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<SearchResult>> = _searchQuery
        .debounce(500L)
        .filter { it.isNotBlank() }
        .map { query ->
            searcher.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _discoverPodcasts = MutableStateFlow<List<SearchResult>>(emptyList())
    val discoverPodcasts = _discoverPodcasts.asStateFlow()
    
    private val _discoverTitle = MutableStateFlow("Top Podcasts")
    val discoverTitle = _discoverTitle.asStateFlow()

    private var selectedGenreId: Long? = null

    fun selectDiscoverGenre(genreId: Long?, genreName: String?) {
        selectedGenreId = genreId
        refreshDiscover()
    }

    fun refreshDiscover() {
        viewModelScope.launch {
            if (selectedGenreId != null) {
                 _discoverPodcasts.value = searcher.getTopPodcasts(genreId = selectedGenreId)
                 return@launch
            }

            val localPodcasts = repo.allPodcastsDirect()
            if (localPodcasts.isNotEmpty()) {
                val randomPod = localPodcasts.random()
                val details = searcher.search(randomPod.title).firstOrNull() 
                if (details?.primaryGenreId != null) {
                    _discoverTitle.value = "Top in ${details.primaryGenreName ?: "suggested"}"
                    _discoverPodcasts.value = searcher.getTopPodcasts(genreId = details.primaryGenreId)
                    return@launch
                }
            }
            
            _discoverTitle.value = "Top Podcasts"
            _discoverPodcasts.value = searcher.getTopPodcasts()
        }
    }
    
    fun subscribeToSearchResult(result: SearchResult) {
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
        _searchQuery.value = query
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun episodesFor(podcastId: Long) = _sortOption.flatMapLatest { option ->
        repo.episodesForPodcast(podcastId, option)
    }

    fun episodesForPaging(podcastId: Long) = _sortOption.flatMapLatest { option ->
        repo.episodesForPodcastPaging(podcastId, option)
            .cachedIn(viewModelScope)
    }

    suspend fun getEpisode(id: Long) = repo.getEpisode(id)
    
    fun getEpisodeFlow(id: Long) = repo.getEpisodeFlow(id)

    fun fetchAndUpdateDescription(episodeId: Long) {
        viewModelScope.launch {
            val ep = repo.getEpisode(episodeId) ?: return@launch
            val fullDesc = repo.fetchRemoteEpisodeDescription(ep.podcastId, ep.guid)
            if (fullDesc != null && fullDesc != ep.description) {
                 repo.markEpisodeListened(ep.copy(description = fullDesc), ep.listened)
            }
        }
    }

    fun setListened(e: EpisodeListItem, listened: Boolean) {
        viewModelScope.launch {
            repo.markEpisodeListened(e, listened)
            refreshUpNext()
        }
    }
    
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

    fun getEpisodesForMonth(year: Int, month: Int) = repo.getEpisodesForMonth(year, month)

    fun searchEpisodes(query: String) = repo.searchEpisodes(query)
    
    fun searchEpisodesInPodcast(podcastId: Long, query: String) = repo.searchEpisodesInPodcast(podcastId, query)
    
    fun searchPodcasts(query: String) = repo.searchPodcasts(query)
        
    val currentStreak = repo.getCurrentStreak()
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)

    val weeklyActivity = repo.getLast7DaysActivity()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    val topPodcasts = repo.getTopPodcastsByDuration()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        
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
        )
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _upNext = MutableStateFlow<List<Episode>>(emptyList())
    val upNext = _upNext.asStateFlow()

    init {
        refreshUpNext()
        viewModelScope.launch {
            repo.checkAndFixRestoringEpisodes()
            refreshUpNext()
        }
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

    fun markPodcastListened(podcastId: Long, listened: Boolean) {
        viewModelScope.launch {
            repo.markPodcastListened(podcastId, listened)
            refreshUpNext()
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    fun refreshAllPodcasts() {
        if (_isRefreshing.value) return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                repo.refreshAllPodcasts()
                refreshUpNext()
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to refresh podcasts: ${e.message}"
            } finally {
                _isRefreshing.value = false
            }
        }
    }
    
    fun refreshAllFeeds() {
        refreshAllPodcasts()
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
