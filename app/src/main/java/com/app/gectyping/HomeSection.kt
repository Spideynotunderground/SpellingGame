package com.app.gectyping

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors

// Dynamic motivational phrases — picked once per session
private val MOTIVATION_PHRASES = listOf(
    "Ready to beat your record?",
    "You're doing great, keep going!",
    "Ready for a quick session?",
    "Let's learn some new words!",
    "Challenge yourself today!",
    "Your brain will thank you!",
    "One more round?",
    "Time to level up!",
    "Consistency is the key!",
    "Show them what you've got!"
)

/**
 * ============================================================================
 * HOME SECTION — Learning Path (Duolingo-style)
 *
 * - Top bar with Rewards, XP, Diamonds
 * - Learning path with topic circles
 * - Progress through lessons
 * ============================================================================
 */
@Composable
fun HomeSection(
    playerName: String,
    avatarEmoji: String,
    lives: Int,
    maxLives: Int,
    diamonds: Int,
    xp: Int,
    stars: Int,
    dailyStreak: Int,
    onStartGame: () -> Unit,
    context: android.content.Context? = null,
    onXpEarned: (Int) -> Unit = {},
    onOpenRewards: () -> Unit = {},
    hasUnclaimedRewards: Boolean = false
) {
    val colors = LocalGameColors.current
    
    // State for current lesson
    var currentLesson by remember { mutableStateOf<Pair<Topic, TopicLesson>?>(null) }

    // If lesson is active, show LessonScreen
    if (currentLesson != null && context != null) {
        val (topic, lesson) = currentLesson!!
        
        // Convert lesson words to ExerciseWords
        val exerciseWords = lesson.words.map { word ->
            ExerciseWord(
                english = word.word,
                russian = word.translation,
                example = word.example
            )
        }
        
        LessonScreen(
            context = context,
            topicTitle = topic.title,
            lessonNumber = lesson.lessonNumber,
            words = exerciseWords,
            onLessonComplete = { xpEarned ->
                onXpEarned(xpEarned)
                currentLesson = null
            },
            onExit = {
                currentLesson = null
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
        ) {
            // ---------- Top Stats Bar ----------
            TopStatsBar(
                diamonds = diamonds,
                xp = xp,
                lives = lives,
                maxLives = maxLives,
                onOpenRewards = onOpenRewards,
                hasUnclaimedRewards = hasUnclaimedRewards
            )
            
            // ---------- Learning Path ----------
            if (context != null) {
                DuolingoLearningPath(
                    context = context,
                    onStartLesson = { topic, lesson ->
                        currentLesson = Pair(topic, lesson)
                    }
                )
            } else {
                // Fallback if no context
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading...",
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

/**
 * Top stats bar — Rewards (left), Diamonds + XP (right)
 * Clean minimal look, no containers or backgrounds
 */
@Composable
private fun TopStatsBar(
    diamonds: Int,
    xp: Int,
    lives: Int = 5,
    maxLives: Int = 5,
    onOpenRewards: () -> Unit = {},
    hasUnclaimedRewards: Boolean = false
) {
    val colors = LocalGameColors.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Rewards button
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { onOpenRewards() }
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Image(
                    painter = painterResource(id = R.drawable.icons8_prize_96),
                    contentDescription = "Rewards",
                    modifier = Modifier.size(22.dp)
                )
                if (hasUnclaimedRewards) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.TopEnd)
                            .background(Color(0xFFEF4444), shape = CircleShape)
                    )
                }
            }
            Text(
                text = "Rewards",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF9600)
            )
        }
        
        // Right: Diamonds + XP (no backgrounds)
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Diamonds
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "\uD83D\uDC8E", fontSize = 16.sp)
                Text(
                    text = "$diamonds",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1CB0F6)
                )
            }
            
            // XP
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LightningIcon(size = 18.dp)
                Text(
                    text = "$xp",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF58CC02)
                )
            }
        }
    }
}

/**
 * Reusable button with scale-bounce on press.
 */
@Composable
fun ScaleBounceButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit,
    containerColor: Color = Color.Unspecified,
    contentColor: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val resolvedContainer = if (containerColor == Color.Unspecified) LocalGameColors.current.accent else containerColor
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(120, easing = FastOutSlowInEasing),
        label = "btnScale"
    )

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = resolvedContainer,
            contentColor = contentColor
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp, pressedElevation = 1.dp)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}
