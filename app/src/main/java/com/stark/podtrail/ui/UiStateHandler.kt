package com.stark.podtrail.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String, val exception: Throwable? = null) : UiState<Nothing>()
    data object Empty : UiState<Nothing>()
}

@Composable
fun ErrorState(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Refresh
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Something went wrong",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onRetry != null) {
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Try Again")
            }
        }
    }
}

@Composable
fun <T> UiStateHandler(
    uiState: UiState<T>,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit = { LoadingFullScreen(modifier = modifier) },
    errorContent: @Composable (String, (() -> Unit)?) -> Unit = { message, onRetry -> 
        ErrorState(message = message, onRetry = onRetry, modifier = modifier)
    },
    emptyContent: @Composable () -> Unit = { 
        EmptyState(
            icon = Icons.Default.Refresh,
            title = "No data available",
            message = "There's nothing to show here."
        )
    },
    onRetry: (() -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    when (uiState) {
        is UiState.Loading -> loadingContent()
        is UiState.Success -> content(uiState.data)
        is UiState.Error -> errorContent(uiState.message, onRetry)
        is UiState.Empty -> emptyContent()
    }
}

@Composable
fun <T> AsyncContent(
    isLoading: Boolean,
    error: Throwable? = null,
    data: T? = null,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit = { LoadingFullScreen(modifier = modifier) },
    errorContent: @Composable (String, (() -> Unit)?) -> Unit = { message, onRetry -> 
        ErrorState(message = message, onRetry = onRetry, modifier = modifier)
    },
    emptyContent: @Composable () -> Unit = { 
        EmptyState(
            icon = Icons.Default.Refresh,
            title = "No data available", 
            message = "There's nothing to show here."
        )
    },
    content: @Composable (T) -> Unit
) {
    when {
        isLoading -> loadingContent()
        error != null -> errorContent(error.message ?: "Unknown error occurred", onRetry)
        data == null -> emptyContent()
        else -> content(data)
    }
}