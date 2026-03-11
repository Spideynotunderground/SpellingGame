package com.app.gectyping

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Matches the splash / onboarding background color exactly
private val NoBg      = Color(0xFF27304E)
private val NoAccent  = Color(0xFF58CC02)
private val NoRed     = Color(0xFFFF4B4B)

@Composable
fun NoInternetScreen(onRetry: () -> Unit) {

    // Pulse animation on the wifi icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue  = 1.08f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(NoBg, NoBg.copy(red = 0.12f)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(40.dp)
        ) {

            // Icon circle
            Box(
                modifier = Modifier
                    .scale(pulse)
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(NoRed.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📡", fontSize = 56.sp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "No Internet Connection",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "GecTyping requires an internet connection\nto load words and play audio.\n\nPlease connect to Wi-Fi or enable mobile data.",
                fontSize = 15.sp,
                color = Color.White.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Retry button
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NoAccent),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(
                    text = "Try Again",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
