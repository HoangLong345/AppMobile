package com.example.nhatky.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage

@Composable
fun PolaroidFrame(
    imageUrl: String,
    caption: String = "",
    rotation: Float = 0f,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .rotate(rotation)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(2.dp),
                ambientColor = Color.Black.copy(alpha = 0.2f)
            ),
        color = Color.White,
        shape = RoundedCornerShape(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxWidth()
                    .background(Color(0xFFEEEEEE))
                    .border(1.dp, Color(0xFFDDDDDD))
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            if (caption.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp,
                        color = Color(0xFF555555)
                    ),
                    maxLines = 1
                )
            } else {
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}
