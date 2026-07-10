package com.example.nhatky.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.nhatky.data.model.DiaryEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun StickyNote(
    diary: DiaryEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    scaledDown: Boolean = true
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val rotation = rememberRotation(diary.id)
    val color = rememberNoteColor(diary.id)
    val moodText = when (diary.mood) {
        "Vui" -> "😊 Vui"
        "Bình thường" -> "😐 Bình thường"
        "Buồn" -> "😔 Buồn"
        "Tức giận" -> "😠 Tức giận"
        else -> ""
    }

    Box(
        modifier = modifier
            .rotate(rotation)
            .padding(8.dp)
            .shadow(
                elevation = if (scaledDown) 4.dp else 0.dp,
                shape = RoundedCornerShape(2.dp),
                ambientColor = Color.Black.copy(alpha = 0.3f)
            )
            .clickable(onClick = onClick)
    ) {
        if (diary.mediaUrls.isNotEmpty()) {
            // Pinned Photo Style
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.width(if (scaledDown) 140.dp else 300.dp)
            ) {
                Column(modifier = Modifier.padding(if (scaledDown) 6.dp else 12.dp)) {

                    val realUrl = getRealDriveUrl(diary.mediaUrls.first())

                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(realUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        contentScale = ContentScale.Crop,
                        loading = {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(if (scaledDown) 16.dp else 32.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.LightGray.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.BrokenImage,
                                    contentDescription = "Lỗi",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(if (scaledDown) 24.dp else 48.dp)
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(if (scaledDown) 4.dp else 12.dp))
                    Text(
                        text = timeFormat.format(Date(diary.timestamp)),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = if (scaledDown) 8.sp else 12.sp),
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        } else {
            // Sticky Note Style
            Surface(
                color = color,
                shape = RoundedCornerShape(2.dp),
                modifier = Modifier.widthIn(min = 120.dp, max = if (scaledDown) 180.dp else 300.dp)
            ) {
                Column(modifier = Modifier.padding(if (scaledDown) 8.dp else 20.dp)) {
                    if (moodText.isNotEmpty()) {
                        Text(
                            text = moodText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = if (scaledDown) 10.sp else 16.sp
                            ),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    if (diary.title.isNotEmpty()) {
                        Text(
                            text = diary.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = if (scaledDown) 12.sp else 18.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(if (scaledDown) 4.dp else 8.dp))
                    }

                    Text(
                        text = diary.content,
                        style = MaterialTheme.typography.bodySmall.copy(
                            lineHeight = if (scaledDown) 14.sp else 22.sp,
                            fontFamily = FontFamily.Serif,
                            fontSize = if (scaledDown) 10.sp else 16.sp
                        ),
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(if (scaledDown) 6.dp else 16.dp))
                    Text(
                        text = timeFormat.format(Date(diary.timestamp)),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = if (scaledDown) 8.sp else 11.sp),
                        color = Color.Black.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
fun rememberRotation(id: String): Float {
    return remember(id) { (id.hashCode() % 8 - 4).toFloat() }
}

@Composable
fun rememberNoteColor(id: String): Color {
    val colors = listOf(
        Color(0xFFFEF3C7), // Yellow
        Color(0xFFFCE7F3), // Pink
        Color(0xFFE0F2FE), // Blue
        Color(0xFFDCFCE7)  // Green
    )
    return remember(id) { colors[Math.abs(id.hashCode()) % colors.size] }
}

private fun getRealDriveUrl(url: String): String {
    if (url.startsWith("googledrive://")) {
        val id = url.substringAfter("googledrive://").substringBeforeLast(".")
        return "https://www.googleapis.com/drive/v3/files/$id?alt=media"
    }
    return url
}