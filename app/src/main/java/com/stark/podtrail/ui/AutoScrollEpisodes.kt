package com.stark.podtrail.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import com.stark.podtrail.data.EpisodeListItem

/**
 * Utility function to add auto-scroll to existing EpisodeListScreen
 * Automatically scrolls to the first unlistened episode
 */
@Composable
fun EpisodesWithAutoScroll(
    episodes: List<EpisodeListItem>,
    listState: LazyListState,
    isInitialLoad: Boolean = true,
    itemContent: @Composable (EpisodeListItem) -> Unit
) {
    var hasAutoScrolled by remember { mutableStateOf(false) }
    
    // Auto-scroll to first unlistened episode
    LaunchedEffect(episodes, isInitialLoad) {
        if (episodes.isNotEmpty() && !hasAutoScrolled && isInitialLoad) {
            val firstUnlistenedIndex = episodes.indexOfFirst { !it.listened }
            if (firstUnlistenedIndex != -1) {
                listState.animateScrollToItem(firstUnlistenedIndex)
                hasAutoScrolled = true
            }
        }
    }
    
    // Render episodes
    items(episodes) { episode ->
        itemContent(episode)
    }
}

/**
 * Enhanced version with "Continue Here" separator
 */
@Composable
fun EpisodesWithAutoScrollAndSeparator(
    episodes: List<EpisodeListItem>,
    listState: LazyListState,
    isInitialLoad: Boolean = true,
    itemContent: @Composable (EpisodeListItem) -> Unit
) {
    var hasAutoScrolled by remember { mutableStateOf(false) }
    
    // Auto-scroll to first unlistened episode
    LaunchedEffect(episodes, isInitialLoad) {
        if (episodes.isNotEmpty() && !hasAutoScrolled && isInitialLoad) {
            val firstUnlistenedIndex = episodes.indexOfFirst { !it.listened }
            if (firstUnlistenedIndex != -1) {
                listState.animateScrollToItem(firstUnlistenedIndex)
                hasAutoScrolled = true
            }
        }
    }
    
    // Find index of first unlistened episode
    val firstUnlistenedIndex = episodes.indexOfFirst { !it.listened }
    
    // Render episodes with separator
    items(episodes.size) { index ->
        val episode = episodes[index]
        
        // Add "Continue Here" separator before first unlistened episode
        if (index == firstUnlistenedIndex && firstUnlistenedIndex > 0) {
            ContinueHereSeparator()
        }
        
        itemContent(episode)
    }
}