package com.example.nhatky.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.nhatky.ui.theme.PaperIvory

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val gradientColors = if (isDark) {
        listOf(Color(0xFF1B1B18), Color(0xFF2C2C28))
    } else {
        listOf(Color(0xFFFFF9F0), PaperIvory)
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = gradientColors
                )
            )
    ) {
        content()
    }
}
