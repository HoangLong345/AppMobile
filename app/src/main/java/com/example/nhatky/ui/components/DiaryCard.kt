package com.example.nhatky.ui.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.nhatky.data.model.DiaryEntry
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DiaryCard(
    diary: DiaryEntry,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("MMM d, yyyy • HH:mm", Locale.getDefault())
    var showVideoPlayer by remember { mutableStateOf(false) }
    var videoToPlay by remember { mutableStateOf("") }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (diary.mediaUrls.isNotEmpty()) {
                val firstMedia = diary.mediaUrls.first()
                val isVideo = firstMedia.lowercase().endsWith(".mp4")
                val realUrl = getRealDriveUrl(firstMedia)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .clickable {
                            if (isVideo) {
                                videoToPlay = realUrl
                                showVideoPlayer = true
                            }
                        }
                ) {
                    if (isVideo) {
                        // Khung hiển thị Video Placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.DarkGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayCircleOutline,
                                contentDescription = "Play Video",
                                tint = Color.White,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    } else {
                        // Sử dụng SubcomposeAsyncImage để xử lý các trạng thái Loading / Error trực quan
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(realUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Diary Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(36.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 3.dp
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
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.BrokenImage,
                                            contentDescription = "Error Loading",
                                            tint = Color.Gray,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Text("Lỗi tải ảnh", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        )
                    }
                }
            }

            // Phần Text UI giữ nguyên hoàn toàn
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = dateFormat.format(Date(diary.timestamp)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (diary.title.isNotEmpty()) {
                    Text(
                        text = diary.title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = diary.content,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 24.sp
                )

                if (diary.mood.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = getMoodEmoji(diary.mood) + " " + diary.mood,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }

    if (showVideoPlayer && videoToPlay.isNotEmpty()) {
        VideoPlayerDialog(
            videoUrl = videoToPlay,
            onDismiss = { showVideoPlayer = false }
        )
    }
}

private fun getRealDriveUrl(url: String): String {
    if (url.startsWith("googledrive://")) {
        // Cắt bỏ "googledrive://" và phần mở rộng (ví dụ .jpg / .mp4) để lấy đúng ID Google Drive
        val id = url.substringAfter("googledrive://").substringBeforeLast(".")
        return "https://www.googleapis.com/drive/v3/files/$id?alt=media"
    }
    return url
}

private fun getMoodEmoji(mood: String): String = when(mood) {
    "Vui" -> "😊"
    "Bình thường" -> "😐"
    "Buồn" -> "😔"
    "Tức giận" -> "😠"
    else -> "📝"
}