package com.app.gectyping

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors

/**
 * ============================================================================
 * DUOLINGO-STYLE LEARNING PATH — Beautiful zigzag path with topic circles
 * 
 * Structure:
 * - Topics (themes like Food, Animals, etc.) shown as large circles
 * - Each topic has 5 lessons
 * - Each lesson teaches 10 words with 4 exercise types
 * - Zigzag path connecting all lessons
 * ============================================================================
 */

// Duolingo colors
private val DuoGreen = Color(0xFF58CC02)
private val DuoGreenDark = Color(0xFF4CAD00)
private val DuoBlue = Color(0xFF1CB0F6)
private val DuoOrange = Color(0xFFFF9600)
private val DuoRed = Color(0xFFFF4B4B)
private val DuoPurple = Color(0xFFCE82FF)
private val DuoGold = Color(0xFFFFD700)

// Motivational quotes between topics — short & powerful
private val MOTIVATIONAL_QUOTES = listOf(
    "Every word you learn is a step forward. Keep going!" to "\uD83D\uDE80",
    "Small daily progress leads to big results." to "\uD83C\uDF1F",
    "You're building something amazing — one lesson at a time." to "\uD83C\uDFD7\uFE0F",
    "The best time to learn was yesterday. The next best is now." to "\u23F0",
    "Don't stop until you're proud." to "\uD83D\uDCAA",
    "Mistakes mean you're trying. Keep pushing!" to "\uD83D\uDD25",
    "Your future self will thank you for today's effort." to "\uD83C\uDF1E",
    "Consistency beats talent. Show up every day." to "\uD83C\uDFC6",
    "You're closer to fluency than you think." to "\uD83C\uDFAF",
    "Great things never come from comfort zones." to "\u26A1",
    "One more lesson. One more step. You've got this." to "\uD83D\uDC4A",
    "Believe in the power of your daily practice." to "\u2728",
    "Champions are made in the sessions nobody sees." to "\uD83E\uDD47",
    "Progress, not perfection." to "\uD83D\uDCC8",
    "You didn't come this far to only come this far." to "\uD83D\uDEA9",
    "Dream big. Start small. Act now." to "\uD83D\uDCA1",
    "The only limit is the one you set yourself." to "\uD83C\uDF0D",
    "Stay hungry for knowledge. Stay humble in learning." to "\uD83E\uDDE0"
)

// Topic data class
data class Topic(
    val id: String,
    val title: String,
    val emoji: String,
    val color: Long,
    val lessons: List<TopicLesson>
)

data class TopicLesson(
    val id: String,
    val lessonNumber: Int,
    val words: List<LessonWord> = emptyList()
)

data class LessonWord(
    val word: String,
    val translation: String,
    val example: String = ""
)

// Convert LearningPathData to Topics for display
fun getTopicsFromLearningPath(): List<Topic> {
    return LearningPathData.allUnits.map { unit ->
        Topic(
            id = unit.id,
            title = unit.title,
            emoji = unit.iconEmoji,
            color = unit.color,
            lessons = unit.lessons.mapIndexed { index, lesson ->
                TopicLesson(
                    id = lesson.id,
                    lessonNumber = index + 1,
                    words = lesson.words.map { word ->
                        LessonWord(word.word, word.translation, word.example)
                    }
                )
            }
        )
    }
}

val SAMPLE_TOPICS = getTopicsFromLearningPath()

/**
 * Main Learning Path Screen with zigzag path
 */
@Composable
fun DuolingoLearningPath(
    context: Context,
    topics: List<Topic> = SAMPLE_TOPICS,
    onStartLesson: (Topic, TopicLesson) -> Unit
) {
    val colors = LocalGameColors.current
    
    // Track progress (simplified - will connect to real storage)
    var completedLessons by remember { mutableStateOf(setOf<String>()) }
    var currentLessonId by remember { mutableStateOf<String?>(null) }
    
    // Find first incomplete lesson
    LaunchedEffect(Unit) {
        for (topic in topics) {
            for (lesson in topic.lessons) {
                if (!completedLessons.contains(lesson.id)) {
                    currentLessonId = lesson.id
                    return@LaunchedEffect
                }
            }
        }
    }
    
    // Floating particles animation
    val infiniteTransition = rememberInfiniteTransition(label = "bgParticles")
    val particleOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing)),
        label = "p1"
    )
    val particleOffset2 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(15000, easing = LinearEasing)),
        label = "p2"
    )
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Subtle floating decorative dots in background
        Canvas(modifier = Modifier.fillMaxSize().alpha(0.04f)) {
            val w = size.width
            val h = size.height
            for (i in 0..12) {
                val x = (w * 0.1f + (i * 73f + particleOffset1) % w) % w
                val y = (h * 0.05f + (i * 131f + particleOffset2 * 0.5f) % h) % h
                drawCircle(Color.White, radius = (8f + i * 2f), center = Offset(x, y))
            }
        }
        
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 24.dp)
        ) {
            itemsIndexed(topics) { topicIndex, topic ->
                // Topic header with level badge
                TopicHeader(
                    topic = topic,
                    topicIndex = topicIndex,
                    isUnlocked = topicIndex == 0 || completedLessons.any { it.startsWith(topics[topicIndex - 1].id) }
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // Lessons in zigzag pattern
                topic.lessons.forEachIndexed { lessonIndex, lesson ->
                    val isCompleted = completedLessons.contains(lesson.id)
                    val isCurrent = lesson.id == currentLessonId
                    val isUnlocked = lessonIndex == 0 || completedLessons.contains(topic.lessons[lessonIndex - 1].id)
                    
                    // Zigzag offset
                    val offsetX = when (lessonIndex % 4) {
                        0 -> 0.dp
                        1 -> 60.dp
                        2 -> 0.dp
                        3 -> (-60).dp
                        else -> 0.dp
                    }
                    
                    // Path line to next lesson
                    if (lessonIndex < topic.lessons.size - 1) {
                        PathLine(
                            isCompleted = isCompleted,
                            topicColor = Color(topic.color),
                            offsetX = offsetX
                        )
                    }
                    
                    // Lesson circle
                    LessonCircle(
                        lesson = lesson,
                        topic = topic,
                        isCompleted = isCompleted,
                        isCurrent = isCurrent,
                        isUnlocked = isUnlocked,
                        offsetX = offsetX,
                        onClick = {
                            if (isUnlocked || isCurrent) {
                                onStartLesson(topic, lesson)
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                }
                
                // Motivational quote + divider between topics
                if (topicIndex < topics.size - 1) {
                    Spacer(modifier = Modifier.height(24.dp))
                    MotivationalQuoteCard(quoteIndex = topicIndex)
                    Spacer(modifier = Modifier.height(16.dp))
                    TopicDivider(topicIndex = topicIndex, totalTopics = topics.size)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
            
            // Bottom celebration
            item {
                Spacer(modifier = Modifier.height(32.dp))
                EndOfPathCard()
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

/**
 * Topic header with emoji, title, level badge and gradient
 */
@Composable
private fun TopicHeader(
    topic: Topic,
    topicIndex: Int,
    isUnlocked: Boolean
) {
    val colors = LocalGameColors.current
    val topicColor = Color(topic.color)
    
    // Level label
    val levelLabel = when {
        topicIndex < 5 -> "BEGINNER"
        topicIndex < 11 -> "INTERMEDIATE"
        else -> "ADVANCED"
    }
    val levelColor = when {
        topicIndex < 5 -> DuoGreen
        topicIndex < 11 -> DuoBlue
        else -> DuoPurple
    }
    
    // Show level badge at the start of each section
    val showLevelBadge = topicIndex == 0 || topicIndex == 5 || topicIndex == 11
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Level section badge
        if (showLevelBadge) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 40.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                levelColor.copy(alpha = 0.2f),
                                levelColor.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(1.dp, levelColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$levelLabel LEVEL",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = levelColor,
                    letterSpacing = 2.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // Main topic card with gradient border
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .shadow(
                    elevation = if (isUnlocked) 8.dp else 2.dp,
                    shape = RoundedCornerShape(20.dp),
                    ambientColor = topicColor.copy(alpha = 0.3f),
                    spotColor = topicColor.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isUnlocked) {
                            Brush.horizontalGradient(
                                listOf(
                                    topicColor.copy(alpha = 0.2f),
                                    topicColor.copy(alpha = 0.08f),
                                    colors.cardBackground
                                )
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(
                                    colors.cardBackground,
                                    colors.cardBackground
                                )
                            )
                        }
                    )
                    .border(
                        width = 2.dp,
                        brush = if (isUnlocked) {
                            Brush.horizontalGradient(
                                listOf(topicColor.copy(alpha = 0.6f), topicColor.copy(alpha = 0.1f))
                            )
                        } else {
                            Brush.horizontalGradient(
                                listOf(colors.textSecondary.copy(alpha = 0.2f), colors.textSecondary.copy(alpha = 0.1f))
                            )
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Topic icon with double ring
                    Box(
                        modifier = Modifier.size(68.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Outer glow ring
                        if (isUnlocked) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(topicColor.copy(alpha = 0.15f))
                            )
                        }
                        // Main circle
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isUnlocked) {
                                        Brush.radialGradient(
                                            listOf(topicColor, topicColor.copy(alpha = 0.7f))
                                        )
                                    } else {
                                        Brush.radialGradient(
                                            listOf(
                                                colors.textSecondary.copy(alpha = 0.3f),
                                                colors.textSecondary.copy(alpha = 0.2f)
                                            )
                                        )
                                    }
                                )
                                .border(3.dp, if (isUnlocked) topicColor.copy(alpha = 0.8f) else colors.textSecondary.copy(alpha = 0.3f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUnlocked) {
                                Text(text = topic.emoji, fontSize = 30.sp)
                            } else {
                                Image(
                                    painter = painterResource(id = com.app.gectyping.R.drawable.icons8_lock_96),
                                    contentDescription = "Locked",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        // Topic number
                        Text(
                            text = "TOPIC ${topicIndex + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = topicColor.copy(alpha = 0.7f),
                            letterSpacing = 1.5.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = topic.title,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) colors.textPrimary else colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${topic.lessons.size} lessons · ${topic.lessons.size * 10} words",
                            fontSize = 13.sp,
                            color = colors.textSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual lesson circle with Duolingo style
 */
@Composable
private fun LessonCircle(
    lesson: TopicLesson,
    topic: Topic,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isUnlocked: Boolean,
    offsetX: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val colors = LocalGameColors.current
    val topicColor = Color(topic.color)
    
    // Pulse animation for current lesson
    val infiniteTransition = rememberInfiniteTransition(label = "lessonPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCurrent) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isCurrent) 0.7f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Box(
        modifier = Modifier
            .offset(x = offsetX)
            .size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect for current lesson
        if (isCurrent) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(topicColor.copy(alpha = glowAlpha))
            )
        }
        
        // Main circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .scale(if (isCurrent) pulseScale else 1f)
                .shadow(
                    elevation = if (isCurrent) 12.dp else 4.dp,
                    shape = CircleShape,
                    ambientColor = topicColor,
                    spotColor = topicColor
                )
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> topicColor
                        isCurrent -> topicColor
                        isUnlocked -> colors.cardBackground
                        else -> colors.textSecondary.copy(alpha = 0.2f)
                    }
                )
                .border(
                    width = 4.dp,
                    color = when {
                        isCompleted -> topicColor.copy(alpha = 0.8f)
                        isCurrent -> DuoGold
                        isUnlocked -> topicColor.copy(alpha = 0.5f)
                        else -> colors.textSecondary.copy(alpha = 0.3f)
                    },
                    shape = CircleShape
                )
                .clickable(enabled = isUnlocked || isCurrent) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            when {
                isCompleted -> {
                    // Checkmark for completed
                    Text(text = "✓", fontSize = 32.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                isCurrent -> {
                    // Play icon for current
                    Icon(
                        painter = painterResource(id = com.app.gectyping.R.drawable.icons8_play_96),
                        contentDescription = "Start",
                        modifier = Modifier.size(36.dp)
                    )
                }
                isUnlocked -> {
                    // Lesson number
                    Text(
                        text = "${lesson.lessonNumber}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = topicColor
                    )
                }
                else -> {
                    // Lock for locked
                    Image(
                        painter = painterResource(id = com.app.gectyping.R.drawable.icons8_lock_96),
                        contentDescription = "Locked",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
        
        // Crown indicator for completed lessons
        if (isCompleted) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(DuoGold)
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "👑", fontSize = 14.sp)
            }
        }
    }
}

/**
 * Path line connecting lessons
 */
@Composable
private fun PathLine(
    isCompleted: Boolean,
    topicColor: Color,
    offsetX: androidx.compose.ui.unit.Dp
) {
    val colors = LocalGameColors.current
    val lineColor = if (isCompleted) topicColor else colors.textSecondary.copy(alpha = 0.3f)
    
    Canvas(
        modifier = Modifier
            .offset(x = offsetX / 2)
            .width(8.dp)
            .height(24.dp)
    ) {
        drawLine(
            color = lineColor,
            start = Offset(size.width / 2, 0f),
            end = Offset(size.width / 2, size.height),
            strokeWidth = 6.dp.toPx(),
            cap = StrokeCap.Round,
            pathEffect = if (!isCompleted) PathEffect.dashPathEffect(floatArrayOf(10f, 10f)) else null
        )
    }
}

/**
 * Motivational quote card between topics
 */
@Composable
private fun MotivationalQuoteCard(quoteIndex: Int) {
    val colors = LocalGameColors.current
    val (quote, emoji) = MOTIVATIONAL_QUOTES[quoteIndex % MOTIVATIONAL_QUOTES.size]
    
    // Subtle floating animation
    val infiniteTransition = rememberInfiniteTransition(label = "quoteFloat")
    val floatY by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 6f,
        animationSpec = infiniteRepeatable(
            tween(3000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "floatY"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .offset(y = floatY.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A2F38),
                        Color(0xFF152530)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = 0.08f),
                        Color.White.copy(alpha = 0.02f)
                    )
                ),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(20.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Emoji icon
            Text(
                text = emoji,
                fontSize = 28.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Quote text
            Text(
                text = quote,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

/**
 * Divider between topics with decorative elements
 */
@Composable
private fun TopicDivider(topicIndex: Int, totalTopics: Int) {
    val colors = LocalGameColors.current
    
    // Progress indicator
    val progress = (topicIndex + 1).toFloat() / totalTopics.toFloat()
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left decorative line with gradient
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                colors.textSecondary.copy(alpha = 0.3f)
                            )
                        )
                    )
            )
            
            // Center decorative element
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                DuoGold.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    )
                    .border(1.dp, DuoGold.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                StarIcon(size = 18.dp)
            }
            
            // Right decorative line with gradient
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.5.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                colors.textSecondary.copy(alpha = 0.3f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Progress text
        Text(
            text = "${topicIndex + 1} / $totalTopics topics",
            fontSize = 11.sp,
            color = colors.textSecondary.copy(alpha = 0.5f),
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * End of path celebration card
 */
@Composable
private fun EndOfPathCard() {
    val colors = LocalGameColors.current
    
    val infiniteTransition = rememberInfiniteTransition(label = "endCard")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            tween(2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "endGlow"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        DuoGold.copy(alpha = 0.15f),
                        DuoGold.copy(alpha = 0.05f),
                        Color.Transparent
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    listOf(
                        DuoGold.copy(alpha = glowAlpha),
                        DuoGold.copy(alpha = 0.1f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "\uD83C\uDFC6", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "IELTS MASTER",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DuoGold,
                letterSpacing = 3.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Complete all 19 topics to become\nan IELTS vocabulary master!",
                fontSize = 14.sp,
                color = colors.textSecondary.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "950 words · 95 lessons · 19 topics",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DuoGold.copy(alpha = 0.6f)
            )
        }
    }
}
