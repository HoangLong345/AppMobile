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
import com.example.nhatky.ui.theme.PaperDot
import com.example.nhatky.ui.theme.PaperIvory

@Composable
fun PaperSurface(
    modifier: Modifier = Modifier,
    showGrid: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PaperIvory)
    ) {
        if (showGrid) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dotRadius = 1.dp.toPx()
                val spacing = 24.dp.toPx()
                
                for (x in 0..(size.width / spacing).toInt()) {
                    for (y in 0..(size.height / spacing).toInt()) {
                        drawCircle(
                            color = PaperDot,
                            radius = dotRadius,
                            center = Offset(x * spacing, y * spacing)
                        )
                    }
                }
            }
        }
        content()
    }
}
