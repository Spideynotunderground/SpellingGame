package com.app.gectyping

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors

/**
 * ============================================================================
 * LEARNING PATH SECTION — Duolingo-style visual learning journey
 * 
 * Features:
 * - Vertical scrolling path with connected nodes
 * - Units as sections with lessons as nodes
 * - Crown progress indicators
 * - Animated path connections
 * - Locked/unlocked states
 * ============================================================================
 */

@Composable
fun LearningPathSection(
    context: Context,
    onStartLesson: (LearningUnit, Lesson) -> kotlin.Unit
) {
    // Use the new Duolingo-style learning path
    DuolingoLearningPath(
        context = context,
        topics = SAMPLE_TOPICS,
        onStartLesson = { topic, lesson ->
            // Convert to old format for compatibility
            // TODO: Update when user provides real words
        }
    )
}

/**
 * Header showing overall progress
 */
@Composable
private fun LearningPathHeader(stats: LearningPathStats) {
    val colors = LocalGameColors.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Learning Path",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                
                // Crown count
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "👑", fontSize = 20.sp)
                    Text(
                        text = "${stats.totalCrowns}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD700)
                    )
                }
            }
            
            // Progress bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${stats.completedLessons}/${stats.totalLessons} lessons",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "${(stats.completionPercent * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(colors.textSecondary.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(stats.completionPercent)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF58CC02),
                                        Color(0xFF7ED321)
                                    )
                                )
                            )
                    )
                }
            }
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBadge(emoji = "📚", value = "${stats.learnedWords}", label = "Words")
                StatBadge(emoji = "🎯", value = "${stats.completedLessons}", label = "Lessons")
                StatBadge(emoji = "👑", value = "${stats.totalCrowns}", label = "Crowns")
            }
        }
    }
}

@Composable
private fun StatBadge(emoji: String, value: String, label: String) {
    val colors = LocalGameColors.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(text = emoji, fontSize = 24.sp)
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.textSecondary
        )
    }
}

/**
 * Path connector between units
 */
@Composable
private fun PathConnector(
    isUnlocked: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = LocalGameColors.current
    val pathColor = if (isUnlocked) Color(0xFF58CC02) else colors.textSecondary.copy(alpha = 0.3f)
    
    Box(
        modifier = modifier
            .width(4.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(pathColor)
    )
}

/**
 * Unit node with expandable lessons
 */
@Composable
private fun UnitNode(
    unit: LearningUnit,
    progress: LearningUnitProgress,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onLessonClick: (Lesson) -> Unit,
    getLessonProgress: (String) -> LessonProgress
) {
    val colors = LocalGameColors.current
    val unitColor = Color(unit.color)
    
    // Animation for locked state
    val alpha = if (progress.isUnlocked) 1f else 0.5f
    val scale by animateFloatAsState(
        targetValue = if (isExpanded) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "unitScale"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            }
    ) {
        // Unit header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = progress.isUnlocked) { onToggleExpand() },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (progress.isUnlocked) unitColor.copy(alpha = 0.15f) else colors.cardBackground
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Unit icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (progress.isUnlocked) unitColor else colors.textSecondary.copy(alpha = 0.3f)
                        )
                        .border(3.dp, unitColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (progress.isUnlocked) {
                        Text(text = unit.iconEmoji, fontSize = 28.sp)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Locked",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Unit info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = unit.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (progress.isUnlocked) colors.textPrimary else colors.textSecondary
                    )
                    Text(
                        text = unit.description,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    // Progress indicator
                    if (progress.isUnlocked) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${progress.lessonsCompleted}/${progress.totalLessons}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = unitColor
                            )
                            
                            // Mini progress bar
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(unitColor.copy(alpha = 0.2f))
                            ) {
                                val progressFraction = if (progress.totalLessons > 0) {
                                    progress.lessonsCompleted.toFloat() / progress.totalLessons
                                } else 0f
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progressFraction)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(unitColor)
                                )
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "🔒 ${unit.requiredXP} XP to unlock",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                
                // Expand indicator
                if (progress.isUnlocked) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = unitColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        // Expanded lessons
        AnimatedVisibility(
            visible = isExpanded && progress.isUnlocked,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 28.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                unit.lessons.forEachIndexed { index, lesson ->
                    val lessonProgress = getLessonProgress(lesson.id)
                    val isLast = index == unit.lessons.size - 1
                    
                    // Check if lesson is unlocked (first lesson or previous completed)
                    val isLessonUnlocked = if (index == 0) {
                        true // First lesson always unlocked
                    } else {
                        val prevLesson = unit.lessons[index - 1]
                        getLessonProgress(prevLesson.id).crowns > 0
                    }
                    
                    // Connector line
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .padding(start = 20.dp)
                                .width(3.dp)
                                .height(16.dp)
                                .background(
                                    if (lessonProgress.crowns > 0) unitColor
                                    else colors.textSecondary.copy(alpha = 0.3f)
                                )
                        )
                    }
                    
                    LessonNode(
                        lesson = lesson,
                        progress = lessonProgress,
                        unitColor = unitColor,
                        isUnlocked = isLessonUnlocked,
                        onClick = { if (isLessonUnlocked) onLessonClick(lesson) }
                    )
                }
            }
        }
    }
}

/**
 * Individual lesson node
 */
@Composable
private fun LessonNode(
    lesson: Lesson,
    progress: LessonProgress,
    unitColor: Color,
    isUnlocked: Boolean = true,
    onClick: () -> Unit
) {
    val colors = LocalGameColors.current
    val hasStarted = progress.crowns > 0
    
    // Pulse animation for next unlocked lesson
    val infiniteTransition = rememberInfiniteTransition(label = "lessonPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (!hasStarted && isUnlocked) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    val alpha = if (isUnlocked) 1f else 0.4f
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = isUnlocked) { onClick() }
            .padding(8.dp)
            .graphicsLayer { this.alpha = alpha },
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lesson circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(
                    when {
                        !isUnlocked -> colors.textSecondary.copy(alpha = 0.1f)
                        progress.crowns >= 5 -> Color(0xFFFFD700) // Gold for legendary
                        progress.crowns > 0 -> unitColor
                        else -> colors.textSecondary.copy(alpha = 0.2f)
                    }
                )
                .border(
                    width = 2.dp,
                    color = if (hasStarted && isUnlocked) unitColor else colors.textSecondary.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isUnlocked) lesson.iconEmoji else "🔒",
                fontSize = 20.sp
            )
        }
        
        // Lesson info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = lesson.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                text = "${lesson.words.size} words",
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }
        
        // Crown indicator
        CrownIndicator(crowns = progress.crowns)
    }
}

/**
 * Crown progress indicator (0-5 crowns)
 */
@Composable
private fun CrownIndicator(crowns: Int) {
    val colors = LocalGameColors.current
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (crowns == 0) {
            // Show empty state
            Text(
                text = "Start",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colors.accent
            )
        } else {
            // Show crowns
            repeat(5) { index ->
                val isFilled = index < crowns
                Text(
                    text = "👑",
                    fontSize = if (isFilled) 14.sp else 10.sp,
                    modifier = Modifier.graphicsLayer {
                        alpha = if (isFilled) 1f else 0.2f
                    }
                )
            }
        }
    }
}

/**
 * Lesson completion celebration dialog
 */
@Composable
fun LessonCompleteDialog(
    result: LessonCompletionResult,
    lessonTitle: String,
    onContinue: () -> Unit
) {
    val colors = LocalGameColors.current
    
    // Celebration animation
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val celebrationScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebrationScale"
    )
    
    AlertDialog(
        onDismissRequest = onContinue,
        containerColor = colors.cardBackground,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Celebration emoji
                Text(
                    text = when {
                        result.isPerfect -> "🎉"
                        result.earnedNewCrown -> "👑"
                        else -> "✨"
                    },
                    fontSize = 64.sp,
                    modifier = Modifier.scale(celebrationScale)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = when {
                        result.isPerfect -> "Perfect!"
                        result.earnedNewCrown -> "New Crown!"
                        else -> "Lesson Complete!"
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = lessonTitle,
                    fontSize = 16.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                
                // XP earned
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LightningIcon(size = 26.dp)
                    Text(
                        text = "+${result.xpEarned} XP",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF58CC02)
                    )
                }
                
                // Crown progress
                if (result.earnedNewCrown) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(result.newCrowns) {
                            Text(text = "👑", fontSize = 20.sp)
                        }
                        repeat(5 - result.newCrowns) {
                            Text(
                                text = "👑",
                                fontSize = 16.sp,
                                modifier = Modifier.graphicsLayer { alpha = 0.2f }
                            )
                        }
                    }
                    
                    Text(
                        text = "${result.crownLevel.name} Level!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58CC02)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Continue",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    )
}
