package com.example.nhatky.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.example.nhatky.R

@Composable
fun ZoomableBox(
    modifier: Modifier = Modifier,
    maxScale: Float = 5f,
    minScale: Float = 1f,
    onTap: () -> Unit = {},
    content: @Composable BoxScope.(Float, androidx.compose.ui.geometry.Offset) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = modifier
            .clip(RectangleShape)
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(minScale, maxScale)

                    val maxX = (size.width * (scale - 1)) / 2
                    val minX = -maxX
                    val maxY = (size.height * (scale - 1)) / 2
                    val minY = -maxY

                    val newOffset = offset + pan
                    offset = androidx.compose.ui.geometry.Offset(
                        newOffset.x.coerceIn(minX, maxX),
                        newOffset.y.coerceIn(minY, maxY)
                    )
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1f) {
                            scale = 1f
                            offset = androidx.compose.ui.geometry.Offset.Zero
                        } else {
                            scale = 3f
                            // Optional: Center the zoom on the tap location
                            // This is a simplified version, real "zoom to point" is more complex
                        }
                    },
                    onTap = { onTap() }
                )
            }
    ) {
        Box(
            modifier = Modifier
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                )
                .fillMaxSize()
        ) {
            content(scale, offset)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ZoomableBoxPreview() {
    ZoomableBox(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { scale, offset ->
        Image(
            painter = painterResource(id = android.R.drawable.ic_menu_gallery), // Using a system resource for preview
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
