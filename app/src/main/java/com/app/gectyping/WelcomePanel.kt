package com.app.gectyping

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.DarkBlueBackground
import com.app.gectyping.ui.theme.DarkBlueCard
import com.app.gectyping.ui.theme.White
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * ============================================================================
 * WELCOME PANEL
 * A smooth, animated welcome screen shown before the game begins.
 *
 * Features:
 *  - Fade-in on appearance (alpha 0 → 1, 1 second, EaseInOut)
 *  - "Welcome! Ready to start?" greeting text
 *  - Start button with press-scale interaction effect
 *  - Button becomes non-interactable after first click (prevents double-clicks)
 *  - Smooth fade-out on Start click (alpha 1 → 0, 1 second, EaseInOut)
 *  - Calls [onWelcomeComplete] once fade-out finishes
 * ============================================================================
 */
@Composable
fun WelcomePanel(
    playerName: String = "",
    onWelcomeComplete: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    // ---------- Fade-in / fade-out alpha (smooth Lerp via Animatable) ----------
    val panelAlpha = remember { Animatable(0f) }

    // ---------- Button state ----------
    var isButtonEnabled by remember { mutableStateOf(true) }

    // Smooth fade-in on first composition (1 second, EaseInOut)
    LaunchedEffect(Unit) {
        panelAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            )
        )
    }

    // ---------- Button press-scale interaction (hover-like effect) ----------
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Slight scale-up when the button is pressed (1.0 → 1.08)
    val buttonScale by animateFloatAsState(
        targetValue = if (isPressed) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "buttonScale"
    )

    // ---------- Handle Start click ----------
    fun onStartClicked() {
        if (!isButtonEnabled) return          // Guard against double-clicks
        isButtonEnabled = false               // Disable immediately

        coroutineScope.launch {
            // Smooth fade-out (alpha 1 → 0, 1 second, EaseInOut)
            panelAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 1000,
                    easing = FastOutSlowInEasing
                )
            )

            // Transition complete — notify caller
            Log.d("WelcomePanel", "Starting Loading Screen...")
            onWelcomeComplete()
        }
    }

    // ---------- Full-screen overlay ----------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(panelAlpha.value)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D1B2A),
                        DarkBlueBackground
                    )
                )
            )
            // Block touches from reaching the game layer underneath
            .pointerInput(Unit) { detectTapGestures { /* consume */ } },
        contentAlignment = Alignment.Center
    ) {
        // ---------- Card panel ----------
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(RoundedCornerShape(24.dp))
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color(0xFF3F51B5).copy(alpha = 0.3f),
                    spotColor = Color(0xFF3F51B5).copy(alpha = 0.3f)
                )
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            DarkBlueCard,
                            Color(0xFF1A2040)
                        )
                    )
                )
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Greeting text
            Text(
                text = if (playerName.isNotBlank())
                    "Welcome, $playerName!\nReady to start?"
                else
                    "Welcome!\nReady to start?",
                color = White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 34.sp
            )

            // Subtitle / flavour text
            Text(
                text = "Spell your way to the top",
                color = White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ---------- Start button ----------
            Button(
                onClick = { onStartClicked() },
                enabled = isButtonEnabled,
                interactionSource = interactionSource,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    // Apply press-scale transformation
                    .graphicsLayer {
                        scaleX = buttonScale
                        scaleY = buttonScale
                    },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF3F51B5),
                    contentColor = White,
                    disabledContainerColor = Color(0xFF3F51B5).copy(alpha = 0.5f),
                    disabledContentColor = White.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 2.dp
                )
            ) {
                Text(
                    text = if (isButtonEnabled) "Start" else "Loading...",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
