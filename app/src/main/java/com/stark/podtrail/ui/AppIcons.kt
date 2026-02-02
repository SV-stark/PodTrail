package com.stark.podtrail.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistAdd as MaterialIconsPlaylistAdd
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.ui.graphics.vector.ImageVector

// Create extension functions to handle missing icons gracefully
val Icons.Default.PlaylistAdd: ImageVector
    get() = MaterialIconsPlaylistAdd

// Define any additional custom icons needed
object AppIcons {
    val PlaylistAdd = MaterialIconsPlaylistAdd
    val PlaylistPlay = Icons.Default.PlaylistPlay
}