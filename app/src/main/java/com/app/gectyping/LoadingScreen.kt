package com.app.gectyping

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors
import kotlinx.coroutines.delay

/**
 * ============================================================================
 * LOADING SCREEN WITH QUOTES
 *
 * Показывается после Welcome Panel, пока «загружаются данные».
 *
 * Функции:
 *  - Тонкий стильный прогресс-бар с плавным заполнением (Lerp через animateFloatAsState)
 *  - Блок мотивационных цитат под баром
 *  - Рандомная цитата при каждом запуске
 *  - Плавный fade-in / fade-out всего экрана
 *  - По завершении вызывает [onLoadingComplete]
 * ============================================================================
 */

// ---------- Массив мотивационных фраз ----------
private val LOADING_QUOTES = listOf(
    "Hard words are just a combination of easy sounds.",
    "Every expert was once a beginner.",
    "Spelling is a superpower — one letter at a time.",
    "Mistakes are proof that you are trying.",
    "The more you practice, the luckier you get.",
    "Your brain is a muscle — let's train it!",
    "Great spellers aren't born, they're made.",
    "One word closer to genius.",
    "Believe in every letter you type.",
    "Champions keep playing until they get it right.",
    "Small steps lead to big words.",
    "Focus. Spell. Conquer.",
    "Today's practice is tomorrow's perfection.",
    "You don't have to be perfect — just persistent."
)

// ---------- Длительность загрузки (мс) ----------
private const val LOADING_DURATION_MS = 3000L
private const val FADE_DURATION_MS = 600

@Composable
fun LoadingScreen(
    onLoadingComplete: () -> Unit
) {
    // ---------- Fade-in / fade-out ----------
    val screenAlpha = remember { Animatable(0f) }

    // ---------- Прогресс 0 → 1 (целевое значение) ----------
    var targetProgress by remember { mutableFloatStateOf(0f) }

    // Плавная интерполяция прогресса (аналог Mathf.Lerp):
    // animateFloatAsState делает именно Lerp между текущим и целевым значением
    val smoothProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "progressLerp"
    )

    // ---------- Рандомная цитата (выбирается один раз при композиции) ----------
    val quote = remember { LOADING_QUOTES.random() }

    // ---------- Анимация загрузки ----------
    LaunchedEffect(Unit) {
        // 1. Fade-in экрана
        screenAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(FADE_DURATION_MS, easing = FastOutSlowInEasing)
        )

        // 2. Плавное заполнение прогресс-бара шагами (имитация загрузки)
        val steps = 20
        val stepDelay = LOADING_DURATION_MS / steps
        for (i in 1..steps) {
            targetProgress = i.toFloat() / steps
            delay(stepDelay)
        }

        // Гарантируем что бар дойдёт до 100%
        targetProgress = 1f
        delay(300)

        // 3. Fade-out экрана
        screenAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(FADE_DURATION_MS, easing = FastOutSlowInEasing)
        )

        // 4. Загрузка завершена
        Log.d("LoadingScreen", "Loading complete — transitioning to game")
        onLoadingComplete()
    }

    // ---------- Цвета по теме ----------
    val colors = LocalGameColors.current

    // ---------- Полноэкранный оверлей ----------
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(screenAlpha.value)
            .background(colors.background)
            // Блокируем касания к слою ниже
            .pointerInput(Unit) { detectTapGestures { /* consume */ } },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            // ---------- Заголовок ----------
            Text(
                text = "Loading...",
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            // ---------- Процент ----------
            Text(
                text = "${(smoothProgress * 100).toInt()}%",
                color = colors.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )

            // ---------- Тонкий стильный прогресс-бар ----------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(colors.textSecondary.copy(alpha = 0.15f))
            ) {
                // Заполненная часть с градиентом
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = smoothProgress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    colors.accent,
                                    colors.accent.copy(alpha = 0.7f),
                                    colors.accent.copy(alpha = 0.9f)
                                )
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ---------- Блок с цитатой ----------
            Text(
                text = "\"$quote\"",
                color = colors.textSecondary.copy(alpha = 0.7f),
                fontSize = 15.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}
