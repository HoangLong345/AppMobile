package com.example.nhatky.ui.components

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
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Collections

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayerDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(videoUrl) {
        withContext(Dispatchers.IO) {
            // 1. Lấy Token đăng nhập của người dùng dưới chế độ nền (IO Thread)
            var token: String? = null
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
                e.printStackTrace()
            }

            // 2. Chuyển lên Main Thread để cấu hình Trình phát Video
            withContext(Dispatchers.Main) {
                val loadControl = DefaultLoadControl.Builder()
                    .setBufferDurationsMs(32000, 64000, 2500, 5000).build()
                val extractorsFactory = DefaultExtractorsFactory().setConstantBitrateSeekingEnabled(true)

                // 3. NẠP CHÌA KHÓA BẢO MẬT VÀO NGUỒN TẢI VIDEO
                val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
                    if (!token.isNullOrEmpty()) {
                        setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
                    }
                }

                val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory, extractorsFactory)

                exoPlayer = ExoPlayer.Builder(context)
                    .setMediaSourceFactory(mediaSourceFactory)
                    .setLoadControl(loadControl)
                    .build().apply {
                        setMediaItem(MediaItem.fromUri(videoUrl))
                        videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
                        prepare()
                        playWhenReady = true
                        addListener(object : Player.Listener {
                            override fun onPlaybackStateChanged(playbackState: Int) {
                                isLoading = playbackState == Player.STATE_BUFFERING
                            }
                        })
                    }
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer?.release()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (exoPlayer != null) {
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
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

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