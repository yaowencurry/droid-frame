package com.example.screenshotframer.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF006D5B),
    secondary = Color(0xFF6E5C00),
    tertiary = Color(0xFF7C4048),
    surface = Color(0xFFFBFCF8),
    background = Color(0xFFF7F9F4)
)

@Composable
fun UiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
