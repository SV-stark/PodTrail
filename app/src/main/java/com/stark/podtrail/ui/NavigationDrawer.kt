package com.stark.podtrail.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun SidebarDrawer(
    onNavigate: (Int) -> Unit,
    onClose: () -> Unit,
    onSettings: () -> Unit
) {
    ModalDrawerSheet {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .padding(16.dp),
            contentAlignment = Alignment.BottomStart
        ) {
            Column {
                Text("PodTrack", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Text("Your personal podcast companion", style = MaterialTheme.typography.bodySmall)
            }
        }
        
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        NavigationDrawerItem(
            label = { Text("Home") },
            selected = false,
            onClick = { onNavigate(0); onClose() },
            icon = { Icon(Icons.Default.Home, null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Discover") },
            selected = false,
            onClick = { onNavigate(1); onClose() },
            icon = { Icon(Icons.Default.Explore, null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Calendar") },
            selected = false,
            onClick = { onNavigate(2); onClose() },
            icon = { Icon(Icons.Default.DateRange, null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(
            label = { Text("Profile") },
            selected = false,
            onClick = { onNavigate(3); onClose() },
            icon = { Icon(Icons.Default.Person, null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        
        Spacer(Modifier.weight(1f))
        
        NavigationDrawerItem(
            label = { Text("Settings") },
            selected = false,
            onClick = { onSettings(); onClose() },
            icon = { Icon(Icons.Default.Settings, null) },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        
        Spacer(Modifier.height(16.dp))
    }
}
