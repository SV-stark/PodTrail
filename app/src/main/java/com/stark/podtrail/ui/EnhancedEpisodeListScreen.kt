package com.stark.podtrail.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stark.podtrail.data.EpisodeListItem
import com.stark.podtrail.data.SortOption
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedEpisodeListScreen(
    vm: com.stark.podtrail.ui.PodcastViewModel,
    podcastId: Long,
    onBack: () -> Unit,
    onDetails: (EpisodeListItem) -> Unit
) {
    val episodes by vm.episodesFor(podcastId).collectAsState(initial = emptyList())
    val sortOption by vm.sortOption.collectAsState()
    val listState = rememberLazyListState()
    
    // Selection state
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedEpisodes by remember { mutableStateOf(setOf<Long>()) }
    
    // Filter states
    var selectedFilter by remember { mutableStateOf(EpisodeFilter.ALL) }
    var selectedDateRange by remember { mutableStateOf(DateRange.ALL_TIME) }
    var selectedDuration by remember { mutableStateOf(DurationFilter.ALL) }
    var hideListened by remember { mutableStateOf(false) }
    
    // Show filters panel
    var showFilters by remember { mutableStateOf(false) }
    var hasAutoScrolled by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    
    // Auto-scroll to first unlistened episode on initial load only
    LaunchedEffect(episodes) {
        if (episodes.isNotEmpty() && !hasAutoScrolled && selectedFilter == EpisodeFilter.ALL) {
            val firstUnlistenedIndex = episodes.indexOfFirst { !it.listened }
            if (firstUnlistenedIndex != -1) {
                listState.animateScrollToItem(firstUnlistenedIndex)
                hasAutoScrolled = true
            }
        }
    }
    
    // Reset auto-scroll flag when explicitly requested (e.g., returning to list)
    fun resetAutoScroll() {
        hasAutoScrolled = false
    }

    // Apply filters
    val filteredEpisodes = remember(episodes, selectedFilter, selectedDateRange, selectedDuration, hideListened) {
        var result = episodes

        // Apply main filter
        result = when (selectedFilter) {
            EpisodeFilter.UNLISTENED -> result.filter { !it.listened }
            EpisodeFilter.LISTENED -> result.filter { it.listened }
            EpisodeFilter.IN_PROGRESS -> result.filter { it.playbackPosition > 0 && !it.listened }
            EpisodeFilter.DOWNLOADED -> result.filter { /* TODO: Check if downloaded */ true }
            else -> result
        }

        // Apply date filter
        val now = Calendar.getInstance()
        val cutoffTime = when (selectedDateRange) {
            DateRange.TODAY -> {
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                today.timeInMillis
            }
            DateRange.THIS_WEEK -> {
                val weekAgo = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, -7)
                }
                weekAgo.timeInMillis
            }
            DateRange.THIS_MONTH -> {
                val monthAgo = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -1)
                }
                monthAgo.timeInMillis
            }
            DateRange.PAST_3_MONTHS -> {
                val monthsAgo = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -3)
                }
                monthsAgo.timeInMillis
            }
            DateRange.PAST_6_MONTHS -> {
                val monthsAgo = Calendar.getInstance().apply {
                    add(Calendar.MONTH, -6)
                }
                monthsAgo.timeInMillis
            }
            DateRange.PAST_YEAR -> {
                val yearAgo = Calendar.getInstance().apply {
                    add(Calendar.YEAR, -1)
                }
                yearAgo.timeInMillis
            }
            else -> 0L
        }

        if (cutoffTime > 0) {
            result = result.filter { it.pubDate >= cutoffTime }
        }

        // Apply duration filter
        val durationMs = when (selectedDuration) {
            DurationFilter.UNDER_15 -> 15 * 60 * 1000L
            DurationFilter.UNDER_30 -> 30 * 60 * 1000L
            DurationFilter.UNDER_60 -> 60 * 60 * 1000L
            DurationFilter.OVER_30 -> 30 * 60 * 1000L
            DurationFilter.OVER_60 -> 60 * 60 * 1000L
            else -> null
        }

        if (durationMs != null) {
            result = when (selectedDuration) {
                DurationFilter.UNDER_15, DurationFilter.UNDER_30, DurationFilter.UNDER_60 ->
                    result.filter { it.durationMillis != null && it.durationMillis!! <= durationMs }
                DurationFilter.OVER_30, DurationFilter.OVER_60 ->
                    result.filter { it.durationMillis != null && it.durationMillis!! >= durationMs }
                else -> result
            }
        }

        // Additional hide listened filter
        if (hideListened) {
            result = result.filter { !it.listened }
        }

        result
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { 
                    Text(
                        "Episodes (${filteredEpisodes.size})",
                        fontWeight = FontWeight.Bold 
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { 
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") 
                    }
                },
                actions = {
                    // Selection mode toggle
                    IconButton(onClick = { 
                        isSelectionMode = !isSelectionMode
                        selectedEpisodes = emptySet()
                    }) {
                        Icon(
                            imageVector = if (isSelectionMode) Icons.Default.Close else Icons.Default.Checklist,
                            contentDescription = if (isSelectionMode) "Exit selection" else "Select episodes"
                        )
                    }
                    
                    // Filters toggle
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filters"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Filter panels (animated)
            AnimatedVisibility(
                visible = showFilters,
                enter = slideInVertically() + fadeIn(),
                exit = slideOutVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = ResponsiveDimensions.spacingSmall())
                ) {
                    // Main filter chips
                    EpisodeFilterChips(
                        selectedFilter = selectedFilter,
                        onFilterChanged = { selectedFilter = it }
                    )
                    
                    Spacer(modifier = Modifier.height(ResponsiveDimensions.spacingSmall()))
                    
                    // Date range filter
                    DateRangeFilter(
                        selectedRange = selectedDateRange,
                        onRangeChanged = { selectedDateRange = it }
                    )
                    
                    Spacer(modifier = Modifier.height(ResponsiveDimensions.spacingSmall()))
                    
                    // Duration filter
                    DurationFilter(
                        selectedDuration = selectedDuration,
                        onDurationChanged = { selectedDuration = it }
                    )
                    
                    Spacer(modifier = Modifier.height(ResponsiveDimensions.spacingSmall()))
                    
                    // Sort dropdown
                    Row(
                        modifier = Modifier.padding(horizontal = ResponsiveDimensions.spacingSmall()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sort by:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        EpisodeSortMenu(
                            selectedSort = sortOption,
                            onSortChanged = { vm.setSortOption(it) },
                            modifier = Modifier.weight(2f)
                        )
                    }
                }
            }
            
            HorizontalDivider()
            
            // Episode list
            Box(modifier = Modifier.weight(1f)) {
                if (filteredEpisodes.isEmpty()) {
                    EpisodeEmptyState()
                } else {
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(
                            top = paddingValues.calculateTopPadding() + ResponsiveDimensions.spacingSmall(),
                            bottom = ResponsiveDimensions.spacingLarge() + 
                                    if (isSelectionMode) 80.dp else 16.dp, // Space for batch actions
                            start = ResponsiveDimensions.spacingSmall(),
                            end = ResponsiveDimensions.spacingSmall()
                        ),
                        verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.spacingSmall())
                    ) {
                    // Find the index of first unlistened episode for separator
                    val firstUnlistenedIndex = filteredEpisodes.indexOfFirst { !it.listened }
                    
                    itemsIndexed(filteredEpisodes) { index, episode ->
                        // Add "Continue Here" separator before first unlistened episode
                        if (index == firstUnlistenedIndex && firstUnlistenedIndex > 0) {
                            ContinueHereSeparator()
                        }
                        
                        SwipeableEpisodeCard(
                            episode = episode,
                            isSelectionMode = isSelectionMode,
                            isSelected = episode.id in selectedEpisodes,
                            onSwipeLeft = { vm.setListened(episode, !episode.listened) },
                            onSwipeRight = { /* Handle delete if needed */ },
                            onTap = { onDetails(episode) },
                            onSelect = { 
                                selectedEpisodes = if (episode.id in selectedEpisodes) {
                                    selectedEpisodes - episode.id
                                } else {
                                    selectedEpisodes + episode.id
                                }
                            }
                        ) {
                            EnhancedEpisodeCard(
                                episode = episode,
                                isSelectionMode = isSelectionMode,
                                isSelected = episode.id in selectedEpisodes,
                                showNewBadge = index == firstUnlistenedIndex && !episode.listened,
                                onToggle = { vm.setListened(episode, !episode.listened) },
                                onSelect = { 
                                    selectedEpisodes = if (episode.id in selectedEpisodes) {
                                        selectedEpisodes - episode.id
                                    } else {
                                        selectedEpisodes + episode.id
                                    }
                                },
                                onDetails = { onDetails(episode) }
                            )
                        }
                    }
                    }
                }
            }
        }
        
        // Batch actions bar (animated)
        AnimatedVisibility(
            visible = isSelectionMode && selectedEpisodes.isNotEmpty(),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            BatchActionBar(
                selectedCount = selectedEpisodes.size,
                onMarkListened = { 
                    selectedEpisodes.forEach { episodeId ->
                        episodes.find { it.id == episodeId }?.let { episode ->
                            vm.setListened(episode, true)
                        }
                    }
                    selectedEpisodes = emptySet()
                },
                onMarkUnlistened = { 
                    selectedEpisodes.forEach { episodeId ->
                        episodes.find { it.id == episodeId }?.let { episode ->
                            vm.setListened(episode, false)
                        }
                    }
                    selectedEpisodes = emptySet()
                },
                onDelete = { 
                    // TODO: Implement batch delete
                    selectedEpisodes = emptySet()
                },
                onAddToPlaylist = { 
                    // TODO: Implement add to playlist
                    selectedEpisodes = emptySet()
                },
                onClearSelection = { selectedEpisodes = emptySet() },
                modifier = Modifier.padding(ResponsiveDimensions.spacingSmall())
            )
        }
    }
}