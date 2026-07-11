package com.example.nhatky.ui.components

import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.util.Collections

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerView(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    
    // 1. Khởi tạo ExoPlayer duy nhất một lần và ghi nhớ nó
    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(DefaultLoadControl.Builder()
                .setBufferDurationsMs(32000, 64000, 1500, 2000).build())
            .build().apply {
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                repeatMode = Player.REPEAT_MODE_ONE
                playWhenReady = true
                
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isLoading = playbackState == Player.STATE_BUFFERING || playbackState == Player.STATE_IDLE
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("VideoPlayer", "Lỗi phát Video: ${error.message}", error)
                        isLoading = false
                    }
                })
            }
    }

    // 2. Giải phóng ExoPlayer khi Composable bị hủy hoàn toàn
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    // 3. Theo dõi sự thay đổi của videoUrl để tải nội dung mới
    LaunchedEffect(videoUrl) {
        isLoading = true
        val isGoogleDriveUrl = videoUrl.contains("googleapis.com")
        
        var token: String? = null
        if (isGoogleDriveUrl) {
            withContext(Dispatchers.IO) {
                try {
                    val account = GoogleSignIn.getLastSignedInAccount(context)
                    if (account != null) {
                        val credential = GoogleAccountCredential.usingOAuth2(
                            context, Collections.singleton(DriveScopes.DRIVE_FILE)
                        )
                        credential.selectedAccount = account.account
                        token = credential.token
                    }
                } catch (e: Exception) {
                    Log.e("VideoPlayer", "Lỗi lấy token Drive: ${e.message}")
                }
                Unit
            }
        }

        // Cấu hình MediaSource dựa trên loại URL
        val okHttpClient = OkHttpClient.Builder().build()
        val okHttpDataSourceFactory = OkHttpDataSource.Factory(okHttpClient).apply {
            if (isGoogleDriveUrl && !token.isNullOrEmpty()) {
                setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
            }
        }
        val dataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        
        val finalUrl = if (isGoogleDriveUrl) {
            if (videoUrl.contains("alt=media")) videoUrl else {
                if (videoUrl.contains("?")) "$videoUrl&alt=media" else "$videoUrl?alt=media"
            }
        } else {
            videoUrl
        }

        val mediaItem = MediaItem.fromUri(finalUrl)
        val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

        // Thực hiện gán mediaSource và prepare trên Main Thread
        exoPlayer.stop()
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
    }

    Box(
        modifier = modifier.background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                }
            },
            update = { view ->
                view.player = exoPlayer
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun VideoPlayerDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            VideoPlayerView(
                videoUrl = videoUrl,
                modifier = Modifier.fillMaxSize()
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.Black.copy(alpha = 0.5f), shape = MaterialTheme.shapes.small)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Đóng video",
                    tint = Color.White
                )
            }
        }
    }
}
