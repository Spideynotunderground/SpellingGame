package com.app.gectyping

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors

/**
 * ============================================================================
 * DUOLINGO HOME SECTION — Main learning screen like Duolingo
 * 
 * Features:
 * - Top bar with streak, gems, hearts, leagues
 * - Daily goal progress (circular XP)
 * - Mascot with speech bubble
 * - START button
 * - Learning path preview
 * ============================================================================
 */

// Duolingo colors
private val DuoGreen = Color(0xFF58CC02)
private val DuoBlue = Color(0xFF1CB0F6)
private val DuoOrange = Color(0xFFFF9600)
private val DuoRed = Color(0xFFFF4B4B)
private val DuoPurple = Color(0xFFCE82FF)

@Composable
fun DuolingoHomeSection(
    context: Context,
    playerName: String,
    avatarEmoji: String,
    lives: Int,
    maxLives: Int,
    diamonds: Int,
    stars: Int,
    dailyStreak: Int,
    onStartGame: () -> Unit,
    onOpenShop: () -> Unit,
    onStartLesson: (LearningUnit, Lesson) -> Unit
) {
    val colors = LocalGameColors.current
    
    // Get streak and XP data
    val streakState = remember { 
        try { StreakManager.getStreakState(context) } catch (e: Exception) { StreakState() } 
    }
    val leagueState = remember { 
        try { LeaguesManager.getLeagueState(context) } catch (e: Exception) { LeagueState() } 
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        // Top stats bar
        item {
            DuoTopBar(
                streak = streakState.currentStreak,
                diamonds = diamonds,
                lives = lives,
                maxLives = maxLives,
                leagueTier = leagueState.currentTier,
                onOpenShop = onOpenShop
            )
        }
        
        // Daily goal card
        item {
            DailyGoalCard(
                todayXP = streakState.todayXP,
                dailyGoal = streakState.dailyGoalXP,
                goalMet = streakState.todayGoalMet
            )
        }
        
        // Mascot with START button
        item {
            MascotSection(
                playerName = playerName,
                onStartGame = onStartGame
            )
        }
        
        // Learning path preview - temporarily disabled to debug
        // item {
        //     LearningPathPreview(
        //         context = context,
        //         onStartLesson = onStartLesson
        //     )
        // }
    }
}

/**
 * Top bar with streak, diamonds, hearts, league
 */
@Composable
private fun DuoTopBar(
    streak: Int,
    diamonds: Int,
    lives: Int,
    maxLives: Int,
    leagueTier: LeagueTier,
    onOpenShop: () -> Unit
) {
    val colors = LocalGameColors.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.cardBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Streak
        TopBarItem(
            emoji = "🔥",
            value = streak.toString(),
            color = DuoOrange
        )
        
        // Diamonds
        TopBarItem(
            emoji = "💎",
            value = diamonds.toString(),
            color = DuoBlue,
            onClick = onOpenShop
        )
        
        // Hearts
        TopBarItem(
            emoji = "❤️",
            value = "$lives",
            color = DuoRed,
            onClick = onOpenShop
        )
        
        // League
        TopBarItem(
            emoji = leagueTier.emoji,
            value = "",
            color = Color(leagueTier.color)
        )
    }
}

@Composable
private fun TopBarItem(
    emoji: String,
    value: String,
    color: Color,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (emoji == "🔥") {
            FireIcon(size = 22.dp)
        } else if (emoji == "💎") {
            DiamondIcon(size = 22.dp)
        } else {
            Text(text = emoji, fontSize = 20.sp)
        }
        if (value.isNotEmpty()) {
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

/**
 * Daily goal progress card
 */
@Composable
private fun DailyGoalCard(
    todayXP: Int,
    dailyGoal: Int,
    goalMet: Boolean
) {
    val colors = LocalGameColors.current
    val progress = (todayXP.toFloat() / dailyGoal).coerceIn(0f, 1f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (goalMet) DuoGreen.copy(alpha = 0.15f) else colors.cardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular progress
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = if (goalMet) DuoGreen else DuoBlue,
                    trackColor = colors.textSecondary.copy(alpha = 0.2f),
                    strokeWidth = 6.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LightningIcon(size = 18.dp)
                    Text(
                        text = "$todayXP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (goalMet) "Daily goal complete! 🎉" else "Daily Goal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "$todayXP / $dailyGoal XP",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
            
            if (goalMet) {
                Text(text = "✅", fontSize = 28.sp)
            }
        }
    }
}

/**
 * Mascot section with speech bubble and START button
 */
@Composable
private fun MascotSection(
    playerName: String,
    onStartGame: () -> Unit
) {
    val colors = LocalGameColors.current
    
    // Bounce animation for mascot
    val infiniteTransition = rememberInfiniteTransition(label = "mascotBounce")
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )
    
    // Pulse animation for button
    val buttonScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonPulse"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Speech bubble
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Text(
                text = getMotivationalMessage(playerName),
                fontSize = 16.sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        // Mascot (owl emoji as placeholder)
        Text(
            text = "🦉",
            fontSize = 80.sp,
            modifier = Modifier.offset(y = (-bounce).dp)
        )
        
        // START button - Duolingo style
        Button(
            onClick = onStartGame,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .scale(buttonScale),
            colors = ButtonDefaults.buttonColors(
                containerColor = DuoGreen
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 4.dp,
                pressedElevation = 0.dp
            )
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "START",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun getMotivationalMessage(playerName: String): String {
    val messages = listOf(
        "Ready to learn, $playerName?",
        "Let's keep your streak going!",
        "Time for a quick lesson!",
        "You're doing amazing!",
        "Practice makes perfect!",
        "Let's learn something new!",
        "Your brain will thank you!"
    )
    return messages.random()
}

/**
 * Learning path preview - shows next lessons
 */
@Composable
private fun LearningPathPreview(
    context: Context,
    onStartLesson: (LearningUnit, Lesson) -> Unit
) {
    val colors = LocalGameColors.current
    
    val unitsWithProgress = remember {
        try { LearningPathManager.getUnitsWithProgress(context).take(2) } 
        catch (e: Exception) { emptyList() }
    }
    
    if (unitsWithProgress.isEmpty()) return
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Continue Learning",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        
        unitsWithProgress.forEach { (unit, progress) ->
            if (progress.isUnlocked) {
                UnitPreviewCard(
                    unit = unit,
                    progress = progress,
                    context = context,
                    onStartLesson = { lesson -> onStartLesson(unit, lesson) }
                )
            }
        }
    }
}

@Composable
private fun UnitPreviewCard(
    unit: LearningUnit,
    progress: LearningUnitProgress,
    context: Context,
    onStartLesson: (Lesson) -> Unit
) {
    val colors = LocalGameColors.current
    val unitColor = Color(unit.color)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Unit header
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(unitColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = unit.iconEmoji, fontSize = 24.sp)
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = unit.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${progress.lessonsCompleted}/${progress.totalLessons} lessons",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
            
            // Lessons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                unit.lessons.take(3).forEach { lesson ->
                    val lessonProgress = try { 
                        LearningPathManager.getLessonProgress(context, lesson.id) 
                    } catch (e: Exception) { LessonProgress(lesson.id) }
                    
                    LessonButton(
                        lesson = lesson,
                        crowns = lessonProgress.crowns,
                        onClick = { onStartLesson(lesson) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun LessonButton(
    lesson: Lesson,
    crowns: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGameColors.current
    
    val buttonColor = when {
        crowns >= 5 -> Color(0xFFFFD700) // Gold - legendary
        crowns >= 3 -> DuoGreen
        crowns >= 1 -> DuoBlue
        else -> colors.textSecondary.copy(alpha = 0.3f)
    }
    
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = lesson.iconEmoji, fontSize = 16.sp)
            if (crowns > 0) {
                Text(
                    text = "👑$crowns",
                    fontSize = 10.sp
                )
            }
        }
    }
}
