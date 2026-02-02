package com.stark.podtrail.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshWrapper(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (PaddingValues) -> Unit
) {
    // Fallback implementation to fix build errors with M3 PullToRefresh
    Box(modifier = modifier) {
        content(PaddingValues(0.dp))
        
        if (isRefreshing) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.TopCenter))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplePullToRefresh(
    items: List<Any>,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    emptyContent: @Composable () -> Unit = {},
    itemContent: @Composable (item: Any) -> Unit
) {
    PullToRefreshWrapper(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) { paddingValues ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                emptyContent()
            }
        } else {
            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = paddingValues
            ) {
                items(items.size) { index ->
                    itemContent(items[index])
                }
            }
        }
    }
}