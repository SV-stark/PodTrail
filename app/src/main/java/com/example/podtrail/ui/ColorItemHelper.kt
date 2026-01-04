
@Composable
fun ColorItem(color: Int, selectedColor: Int, scope: kotlinx.coroutines.CoroutineScope, repo: SettingsRepository) {
    androidx.compose.foundation.layout.Box(
        androidx.compose.ui.Modifier
            .androidx.compose.foundation.layout.size(36.dp)
            .androidx.compose.foundation.background(androidx.compose.ui.graphics.Color(color), androidx.compose.foundation.shape.CircleShape)
            .androidx.compose.foundation.clickable { scope.launch { repo.setCustomColor(color) } }
    ) {
        if (selectedColor == color) {
            androidx.compose.material3.Icon(
                androidx.compose.material.icons.Icons.Default.Check, 
                contentDescription = null, 
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.Center)
            )
        }
    }
}
