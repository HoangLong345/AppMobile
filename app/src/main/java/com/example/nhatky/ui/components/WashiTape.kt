package com.example.nhatky.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WashiTape(
    color: Color,
    rotation: Float = 0f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .rotate(rotation)
            .width(80.dp)
            .height(24.dp)
            .alpha(0.7f)
            .background(color)
    )
}
