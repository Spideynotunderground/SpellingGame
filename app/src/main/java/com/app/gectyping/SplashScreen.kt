package com.app.gectyping

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

// Match the dark navy edge color of the video so sidebars are invisible
private val SplashBg = Color(0xFF27304E)

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun SplashScreen(onComplete: () -> Unit) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            val uri = Uri.parse("android.resource://${context.packageName}/${R.raw.splash_video}")
            setMediaItem(MediaItem.fromUri(uri))
            repeatMode = Player.REPEAT_MODE_OFF
            volume = 1f          // ✅ sound ON
            playWhenReady = true
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onComplete()
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SplashBg),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    // FIT = keeps original aspect ratio, fills remaining space with bg color
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.parseColor("#27304E"))
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}