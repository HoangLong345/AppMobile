package com.example.nhatky.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WallBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFEFBF3)) // Paper-like color
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 30.dp.toPx()
            var y = step
            while (y < size.height) {
                drawLine(
                    color = Color(0xFFE5E7EB),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += step
            }
            // Vertical margin line
            drawLine(
                color = Color(0xFFFFE4E6),
                start = Offset(40.dp.toPx(), 0f),
                end = Offset(40.dp.toPx(), size.height),
                strokeWidth = 2.dp.toPx()
            )
        }
        content()
    }
}
