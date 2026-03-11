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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * PRACTICE SECTION — Spaced Repetition Practice Mode
 * 
 * Features:
 * - Due words review
 * - Weak words practice
 * - Word bank with strength indicators
 * - Practice statistics
 * ============================================================================
 */

@Composable
fun PracticeSection(
    context: Context,
    onStartPractice: (PracticeType) -> Unit
) {
    val colors = LocalGameColors.current
    
    var stats by remember { 
        mutableStateOf(try { SpacedRepetitionManager.getWordBankStats(context) } catch (e: Exception) { WordBankStats(0,0,0,0,0,0,0f) }) 
    }
    var dueWords by remember { 
        mutableStateOf(try { SpacedRepetitionManager.getDueWords(context) } catch (e: Exception) { emptyList() }) 
    }
    var weakWords by remember { 
        mutableStateOf(try { SpacedRepetitionManager.getWeakWords(context) } catch (e: Exception) { emptyList() }) 
    }
    var showWordBank by remember { mutableStateOf(false) }
    
    // Refresh data
    fun refreshData() {
        try {
            stats = SpacedRepetitionManager.getWordBankStats(context)
            dueWords = SpacedRepetitionManager.getDueWords(context)
            weakWords = SpacedRepetitionManager.getWeakWords(context)
        } catch (e: Exception) { /* Silently handle */ }
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "Practice",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Stats overview
        item {
            PracticeStatsCard(stats = stats)
        }
        
        // Due words card
        if (dueWords.isNotEmpty()) {
            item {
                DueWordsCard(
                    dueCount = dueWords.size,
                    onStartPractice = { onStartPractice(PracticeType.DUE_WORDS) }
                )
            }
        }
        
        // Weak words card
        if (weakWords.isNotEmpty()) {
            item {
                WeakWordsCard(
                    weakCount = weakWords.size,
                    onStartPractice = { onStartPractice(PracticeType.WEAK_WORDS) }
                )
            }
        }
        
        // Practice options
        item {
            PracticeOptionsCard(
                onStartPractice = onStartPractice,
                hasNewWords = true,
                hasMistakes = SpacedRepetitionManager.getRecentMistakes(context).isNotEmpty()
            )
        }
        
        // Word bank button
        item {
            WordBankButton(
                totalWords = stats.totalWords,
                onClick = { showWordBank = true }
            )
        }
        
        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
    
    // Word bank bottom sheet
    if (showWordBank) {
        WordBankSheet(
            context = context,
            onDismiss = { showWordBank = false }
        )
    }
}

/**
 * Practice statistics card
 */
@Composable
private fun PracticeStatsCard(stats: WordBankStats) {
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
            Text(
                text = "Your Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            
            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCircle(
                    value = stats.totalWords,
                    label = "Total",
                    color = colors.accent
                )
                StatCircle(
                    value = stats.masteredWords,
                    label = "Mastered",
                    color = Color(0xFF58CC02)
                )
                StatCircle(
                    value = stats.learningWords,
                    label = "Learning",
                    color = Color(0xFFFF9500)
                )
                StatCircle(
                    value = stats.dueToday,
                    label = "Due",
                    color = Color(0xFFFF4B4B)
                )
            }
            
            // Average strength bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Average Strength",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "${(stats.averageStrength * 100).toInt()}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.accent
                    )
                }
                
                StrengthBar(strength = stats.averageStrength)
            }
        }
    }
}

@Composable
private fun StatCircle(value: Int, label: String, color: Color) {
    val colors = LocalGameColors.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f))
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = value.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.textSecondary
        )
    }
}

/**
 * Strength bar (5 segments like Duolingo)
 */
@Composable
fun StrengthBar(
    strength: Float,
    modifier: Modifier = Modifier
) {
    val colors = LocalGameColors.current
    val segments = SpacedRepetitionManager.getStrengthBars(strength)
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(5) { index ->
            val isFilled = index < segments
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        if (isFilled) {
                            when {
                                segments >= 4 -> Color(0xFF58CC02)
                                segments >= 2 -> Color(0xFFFF9500)
                                else -> Color(0xFFFF4B4B)
                            }
                        } else {
                            colors.textSecondary.copy(alpha = 0.2f)
                        }
                    )
            )
        }
    }
}

/**
 * Due words urgent card
 */
@Composable
private fun DueWordsCard(
    dueCount: Int,
    onStartPractice: () -> Unit
) {
    val colors = LocalGameColors.current
    
    // Pulse animation for urgency
    val infiniteTransition = rememberInfiniteTransition(label = "duePulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStartPractice() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF4B4B).copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF4B4B))
                    .graphicsLayer { alpha = pulseAlpha },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⏰", fontSize = 28.sp)
            }
            
            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$dueCount words due!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Review now to keep your streak",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
            
            // Arrow
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Start",
                tint = Color(0xFFFF4B4B),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Weak words card
 */
@Composable
private fun WeakWordsCard(
    weakCount: Int,
    onStartPractice: () -> Unit
) {
    val colors = LocalGameColors.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onStartPractice() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF9500).copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9500)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "💪", fontSize = 28.sp)
            }
            
            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$weakCount weak words",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Strengthen your knowledge",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
            
            // Arrow
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Start",
                tint = Color(0xFFFF9500),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Practice options card
 */
@Composable
private fun PracticeOptionsCard(
    onStartPractice: (PracticeType) -> Unit,
    hasNewWords: Boolean,
    hasMistakes: Boolean
) {
    val colors = LocalGameColors.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Practice Modes",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            
            // New words
            if (hasNewWords) {
                PracticeOptionRow(
                    emoji = "✨",
                    title = "Learn New Words",
                    subtitle = "Expand your vocabulary",
                    color = Color(0xFF58CC02),
                    onClick = { onStartPractice(PracticeType.NEW_WORDS) }
                )
            }
            
            // Mistakes
            if (hasMistakes) {
                PracticeOptionRow(
                    emoji = "🔄",
                    title = "Review Mistakes",
                    subtitle = "Practice recent errors",
                    color = Color(0xFFCE82FF),
                    onClick = { onStartPractice(PracticeType.MISTAKES) }
                )
            }
            
            // Mixed practice
            PracticeOptionRow(
                emoji = "🎯",
                title = "Mixed Practice",
                subtitle = "A bit of everything",
                color = Color(0xFF1CB0F6),
                onClick = { onStartPractice(PracticeType.MIXED) }
            )
        }
    }
}

@Composable
private fun PracticeOptionRow(
    emoji: String,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit
) {
    val colors = LocalGameColors.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .clickable { onClick() }
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = emoji, fontSize = 24.sp)
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }
        
        Icon(
            imageVector = Icons.Default.PlayArrow,
            contentDescription = "Start",
            tint = color,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Word bank button
 */
@Composable
private fun WordBankButton(
    totalWords: Int,
    onClick: () -> Unit
) {
    val colors = LocalGameColors.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.MenuBook,
                contentDescription = "Word Bank",
                tint = colors.accent,
                modifier = Modifier.size(28.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Word Bank",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "$totalWords words learned",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = colors.textSecondary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Word bank bottom sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WordBankSheet(
    context: Context,
    onDismiss: () -> Unit
) {
    val colors = LocalGameColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    var searchQuery by remember { mutableStateOf("") }
    var sortByStrength by remember { mutableStateOf(true) }
    
    val allWords = remember(sortByStrength) {
        SpacedRepetitionManager.getWordsSortedByStrength(context, ascending = sortByStrength)
    }
    
    val filteredWords = remember(searchQuery, allWords) {
        if (searchQuery.isBlank()) allWords
        else allWords.filter { it.word.contains(searchQuery.lowercase()) }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Word Bank",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                
                Text(
                    text = "${allWords.size} words",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search words...") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Sort toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = sortByStrength,
                    onClick = { sortByStrength = true },
                    label = { Text("Weakest first") }
                )
                FilterChip(
                    selected = !sortByStrength,
                    onClick = { sortByStrength = false },
                    label = { Text("Strongest first") }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Word list
            if (filteredWords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "📚", fontSize = 48.sp)
                        Text(
                            text = if (searchQuery.isNotBlank()) "No words found" else "No words learned yet",
                            fontSize = 16.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredWords) { wordState ->
                        WordBankItem(wordState = wordState)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun WordBankItem(wordState: WordLearningState) {
    val colors = LocalGameColors.current
    
    val statusColor = when (wordState.status) {
        WordStatus.MASTERED -> Color(0xFF58CC02)
        WordStatus.REVIEWING -> Color(0xFF1CB0F6)
        WordStatus.LEARNING -> Color(0xFFFF9500)
        WordStatus.NEW -> colors.textSecondary
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.background)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        
        // Word
        Text(
            text = wordState.word,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        
        // Strength bar
        Box(modifier = Modifier.width(60.dp)) {
            StrengthBar(strength = wordState.strength)
        }
        
        // Accuracy
        Text(
            text = "${(wordState.accuracy * 100).toInt()}%",
            fontSize = 12.sp,
            color = colors.textSecondary
        )
    }
}
