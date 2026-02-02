package com.stark.podtrail.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Podcast
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.stark.podtrail.data.EpisodeListItem
import com.stark.podtrail.data.Podcast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    vm: PodcastViewModel = hiltViewModel(),
    onPodcastClick: (Podcast) -> Unit,
    onEpisodeClick: (EpisodeListItem) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchScope by remember { mutableStateOf(SearchScope.ALL) }
    
    val podcasts by remember(searchQuery) { 
        if (searchQuery.isBlank() || searchScope != SearchScope.PODCASTS && searchScope != SearchScope.ALL) 
            kotlinx.coroutines.flow.MutableStateFlow(emptyList())
        else vm.searchPodcasts(searchQuery)
    }.collectAsState(initial = emptyList())
    
    val episodes by remember(searchQuery) {
        if (searchQuery.isBlank() || searchScope != SearchScope.EPISODES && searchScope != SearchScope.ALL)
            kotlinx.coroutines.flow.MutableStateFlow(emptyList())
        else vm.searchEpisodes(searchQuery)
    }.collectAsState(initial = emptyList())
    
    val isLoading = searchQuery.isNotBlank() && podcasts.isEmpty() && episodes.isEmpty()

    enum class SearchScope { ALL, PODCASTS, EPISODES }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Search Bar and Scope
        Column(
            modifier = Modifier.padding(ResponsiveDimensions.spacingSmall())
        ) {
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { /* Handle search if needed */ },
                active = false,
                onActiveChange = { },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search podcasts and episodes...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (isLoading) {
                        LoadingSpinner(size = 20.dp)
                    }
                }
            ) {
                // Search results dropdown if active
            }
            
            // Search Scope Selector
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = ResponsiveDimensions.spacingSmall()),
                horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.spacingSmall())
            ) {
                FilterChip(
                    selected = searchScope == SearchScope.ALL,
                    onClick = { searchScope = SearchScope.ALL },
                    label = { Text("All") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                FilterChip(
                    selected = searchScope == SearchScope.PODCASTS,
                    onClick = { searchScope = SearchScope.PODCASTS },
                    label = { Text("Podcasts") },
                    leadingIcon = { Icon(Icons.Default.Podcast, contentDescription = null) }
                )
                FilterChip(
                    selected = searchScope == SearchScope.EPISODES,
                    onClick = { searchScope = SearchScope.EPISODES },
                    label = { Text("Episodes") },
                    leadingIcon = { Icon(Icons.Default.Podcasts, contentDescription = null) }
                )
            }
        }
        
        HorizontalDivider()
        
        // Search Results
        when {
            searchQuery.isBlank() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = "Search for content",
                        message = "Enter keywords to find podcasts and episodes",
                        action = null
                    )
                }
            }
            isLoading -> {
                LoadingFullScreen("Searching...")
            }
            podcasts.isEmpty() && episodes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = "No results found",
                        message = "Try different keywords or check spelling",
                        action = null
                    )
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(ResponsiveDimensions.spacingSmall()),
                    verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.spacingSmall())
                ) {
                    // Podcasts Section
                    if (searchScope == SearchScope.ALL || searchScope == SearchScope.PODCASTS) {
                        if (podcasts.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Podcasts (${podcasts.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = ResponsiveDimensions.spacingTiny())
                                )
                            }
                            items(podcasts) { podcast ->
                                PodcastSearchResult(
                                    podcast = podcast,
                                    onClick = { onPodcastClick(podcast) }
                                )
                            }
                        }
                    }
                    
                    // Episodes Section
                    if (searchScope == SearchScope.ALL || searchScope == SearchScope.EPISODES) {
                        if (episodes.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Episodes (${episodes.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(vertical = ResponsiveDimensions.spacingTiny())
                                )
                            }
                            items(episodes) { episode ->
                                EpisodeSearchResult(
                                    episode = episode,
                                    onClick = { onEpisodeClick(episode) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PodcastSearchResult(
    podcast: Podcast,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(ResponsiveDimensions.spacingSmall()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = podcast.imageUrl,
                contentDescription = podcast.title,
                modifier = Modifier
                    .size(ResponsiveDimensions.iconSizeLarge())
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(ResponsiveDimensions.cornerRadiusSmall()))
            )
            Spacer(modifier = Modifier.width(ResponsiveDimensions.spacingSmall()))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = podcast.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
                if (podcast.description?.isNotBlank() == true) {
                    Text(
                        text = podcast.description.take(100),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeSearchResult(
    episode: EpisodeListItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(ResponsiveDimensions.spacingSmall()),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = episode.imageUrl,
                contentDescription = episode.title,
                modifier = Modifier
                    .size(ResponsiveDimensions.iconSizeMedium())
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(ResponsiveDimensions.cornerRadiusSmall()))
            )
            Spacer(modifier = Modifier.width(ResponsiveDimensions.spacingSmall()))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2
                )
                Text(
                    text = formatTime(episode.pubDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    return java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(millis))
}