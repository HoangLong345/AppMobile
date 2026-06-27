package com.example.nhatky.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import com.example.nhatky.ui.theme.BackgroundGradientDark
import com.example.nhatky.ui.theme.BackgroundGradientLight

@Composable
fun AppBackground(content: @Composable () -> Unit) {
    val isDark = isSystemInDarkTheme()
    val gradientColors = if (isDark) BackgroundGradientDark else BackgroundGradientLight
    
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
