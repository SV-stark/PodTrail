package com.stark.podtrail.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.toArgb

@Composable
fun ColorPickerDialog(
    initialColor: Int,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit
) {
    var red by remember { mutableFloatStateOf(android.graphics.Color.red(initialColor).toFloat()) }
    var green by remember { mutableFloatStateOf(android.graphics.Color.green(initialColor).toFloat()) }
    var blue by remember { mutableFloatStateOf(android.graphics.Color.blue(initialColor).toFloat()) }

    val currentColor = Color(red.toInt(), green.toInt(), blue.toInt())
    
    // Hex string state
    var hexString by remember(currentColor) { 
        mutableStateOf(String.format("#%06X", (0xFFFFFF and currentColor.toArgb()))) 
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom Color") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Preview
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(currentColor, RoundedCornerShape(12.dp))
                        .padding(bottom = 16.dp)
                )

                Spacer(Modifier.height(16.dp))

                // Hex Input
                OutlinedTextField(
                    value = hexString,
                    onValueChange = { newValue ->
                        hexString = newValue
                        if (newValue.matches(Regex("^#([A-Fa-f0-9]{6})$"))) {
                            try {
                                val colorInt = android.graphics.Color.parseColor(newValue)
                                red = android.graphics.Color.red(colorInt).toFloat()
                                green = android.graphics.Color.green(colorInt).toFloat()
                                blue = android.graphics.Color.blue(colorInt).toFloat()
                            } catch (e: Exception) { /* Ignore invalid parse */ }
                        }
                    },
                    label = { Text("Hex Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // Sliders
                ColorSlider(label = "R", value = red, color = Color.Red) { red = it }
                ColorSlider(label = "G", value = green, color = Color.Green) { green = it }
                ColorSlider(label = "B", value = blue, color = Color.Blue) { blue = it }
            }
        },
        confirmButton = {
            Button(onClick = { onColorSelected(currentColor.toArgb()) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ColorSlider(label: String, value: Float, color: Color, onValueChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(24.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            ),
            modifier = Modifier.weight(1f)
        )
        Text("${value.toInt()}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(32.dp))
    }
}
