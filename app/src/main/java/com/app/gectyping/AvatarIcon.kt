package com.app.gectyping

import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.os.Build
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Unified avatar display composable.
 *
 * Handles three avatar types automatically:
 *  • isAnimated = true  → plays the GIF via AnimatedImageDrawable (API 28+) or ImageView fallback
 *  • iconRes != null    → shows a static PNG drawable
 *  • fallback           → renders the emoji string as Text
 */
@Composable
fun AvatarIcon(
    avatar: AvatarItem,
    size: Dp = 40.dp,
    fontSize: TextUnit = 28.sp,
    modifier: Modifier = Modifier
) {
    when {
        avatar.isAnimated && avatar.iconRes != null -> {
            AnimatedGifImage(
                resId = avatar.iconRes,
                modifier = modifier.size(size)
            )
        }
        avatar.iconRes != null -> {
            Image(
                painter = painterResource(id = avatar.iconRes),
                contentDescription = avatar.title,
                modifier = modifier.size(size)
            )
        }
        else -> {
            Text(
                text = avatar.emoji,
                fontSize = fontSize,
                modifier = modifier
            )
        }
    }
}

/**
 * Animated GIF via Android's built-in AnimatedImageDrawable (API 28+).
 * Falls back to a static ImageView on older devices.
 */
@Composable
private fun AnimatedGifImage(resId: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.FIT_CENTER
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    try {
                        val source = ImageDecoder.createSource(ctx.resources, resId)
                        val drawable = ImageDecoder.decodeDrawable(source)
                        setImageDrawable(drawable)
                        (drawable as? AnimatedImageDrawable)?.apply {
                            repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
                            start()
                        }
                    } catch (e: Exception) {
                        setImageResource(resId)
                    }
                } else {
                    setImageResource(resId)
                }
            }
        },
        modifier = modifier
    )
}
