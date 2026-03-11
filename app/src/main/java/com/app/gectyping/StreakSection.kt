package com.app.gectyping

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
 * STREAK SECTION — Duolingo-style streak display
 * 
 * Features:
 * - Animated fire streak counter
 * - 7-day calendar view
 * - Daily/Weekly XP progress
 * - Streak Freeze shop
 * - Milestone rewards
 * ============================================================================
 */

@Composable
fun StreakSection(
    context: Context,
    diamonds: Int,
    stars: Int,
    onDiamondsChange: (Int) -> Unit,
    onStarsChange: (Int) -> Unit
) {
    val colors = LocalGameColors.current
    val scrollState = rememberScrollState()
    
    var streakState by remember { 
        mutableStateOf(try { StreakManager.getStreakState(context) } catch (e: Exception) { StreakState() }) 
    }
    var last7Days by remember { 
        mutableStateOf(try { StreakManager.getLast7DaysActivity(context) } catch (e: Exception) { emptyList() }) 
    }
    var unclaimedMilestones by remember { 
        mutableStateOf(try { StreakManager.getUnclaimedMilestones(context) } catch (e: Exception) { emptyList() }) 
    }
    var nextMilestone by remember { 
        mutableStateOf(try { StreakManager.getNextMilestone(context) } catch (e: Exception) { null }) 
    }
    var milestoneProgress by remember { 
        mutableFloatStateOf(try { StreakManager.getProgressToNextMilestone(context) } catch (e: Exception) { 0f }) 
    }
    
    // Refresh data
    fun refreshData() {
        try {
            streakState = StreakManager.getStreakState(context)
            last7Days = StreakManager.getLast7DaysActivity(context)
            unclaimedMilestones = StreakManager.getUnclaimedMilestones(context)
            nextMilestone = StreakManager.getNextMilestone(context)
            milestoneProgress = StreakManager.getProgressToNextMilestone(context)
        } catch (e: Exception) { /* Silently handle */ }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ========== MAIN STREAK DISPLAY ==========
        StreakHeroCard(
            currentStreak = streakState.currentStreak,
            longestStreak = streakState.longestStreak,
            streakFreezes = streakState.streakFreezes
        )
        
        // ========== 7-DAY CALENDAR ==========
        WeekCalendarCard(days = last7Days)
        
        // ========== DAILY XP PROGRESS ==========
        DailyXPCard(
            todayXP = streakState.todayXP,
            dailyGoal = streakState.dailyGoalXP,
            goalMet = streakState.todayGoalMet
        )
        
        // ========== WEEKLY XP PROGRESS ==========
        WeeklyXPCard(
            weeklyXP = streakState.weeklyXP,
            weeklyGoal = streakState.weeklyGoalXP
        )
        
        // ========== NEXT MILESTONE ==========
        nextMilestone?.let { milestone ->
            NextMilestoneCard(
                milestone = milestone,
                currentStreak = streakState.currentStreak,
                progress = milestoneProgress
            )
        }
        
        // ========== UNCLAIMED MILESTONES ==========
        if (unclaimedMilestones.isNotEmpty()) {
            UnclaimedMilestonesCard(
                milestones = unclaimedMilestones,
                onClaim = { milestone ->
                    val result = StreakManager.claimMilestone(context, milestone)
                    if (result is ClaimResult.Success) {
                        onDiamondsChange(diamonds + result.coins)
                        onStarsChange(stars + result.gems)
                        refreshData()
                    }
                }
            )
        }
        
        // ========== STREAK FREEZE SHOP ==========
        StreakFreezeShopCard(
            freezesOwned = streakState.streakFreezes,
            diamonds = diamonds,
            stars = stars,
            onBuyWithDiamonds = {
                val result = StreakManager.buyStreakFreezeWithCoins(context, diamonds)
                if (result is BuyResult.Success) {
                    onDiamondsChange(diamonds - result.cost)
                    refreshData()
                }
            },
            onBuyWithGems = {
                val result = StreakManager.buyStreakFreezeWithGems(context, stars)
                if (result is BuyResult.Success) {
                    onStarsChange(stars - result.cost)
                    refreshData()
                }
            }
        )
        
        // ========== STATS ==========
        StatsCard(
            totalXP = streakState.totalXP,
            longestStreak = streakState.longestStreak
        )
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/**
 * Main streak hero card with animated fire
 */
@Composable
private fun StreakHeroCard(
    currentStreak: Int,
    longestStreak: Int,
    streakFreezes: Int
) {
    val colors = LocalGameColors.current
    
    // Fire animation
    val infiniteTransition = rememberInfiniteTransition(label = "fire")
    val fireScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireScale"
    )
    val fireRotation by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireRotation"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFF6B35),
                            Color(0xFFFF8C42),
                            Color(0xFFFFAA5C)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Animated fire icon
                FireIcon(
                    size = 80.dp,
                    modifier = Modifier
                        .scale(fireScale)
                        .graphicsLayer { rotationZ = fireRotation }
                )
                
                // Streak count
                Text(
                    text = "$currentStreak",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                
                Text(
                    text = if (currentStreak == 1) "day streak" else "day streak",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                
                // Streak freeze indicator
                if (streakFreezes > 0) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(streakFreezes) {
                            Text(text = "❄️", fontSize = 20.sp)
                        }
                        Text(
                            text = "Streak Freeze",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                
                // Longest streak
                if (longestStreak > currentStreak) {
                    Text(
                        text = "Longest: $longestStreak days",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * 7-day calendar showing activity
 */
@Composable
private fun WeekCalendarCard(days: List<DayActivityDisplay>) {
    val colors = LocalGameColors.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "This Week",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                days.forEach { day ->
                    DayCircle(day = day)
                }
            }
        }
    }
}

@Composable
private fun DayCircle(day: DayActivityDisplay) {
    val colors = LocalGameColors.current
    
    val bgColor = when {
        day.goalMet -> Color(0xFFFF9500) // Orange for goal met
        day.wasActive -> Color(0xFF4CAF50).copy(alpha = 0.3f) // Light green for active
        day.isToday -> colors.accent.copy(alpha = 0.2f)
        else -> colors.textSecondary.copy(alpha = 0.1f)
    }
    
    val borderColor = when {
        day.isToday -> colors.accent
        day.goalMet -> Color(0xFFFF9500)
        else -> Color.Transparent
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = day.dayName,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (day.isToday) colors.accent else colors.textSecondary
        )
        
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(bgColor)
                .border(2.dp, borderColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (day.goalMet) {
                FireIcon(size = 22.dp)
            } else if (day.wasActive) {
                Text(text = "✓", fontSize = 16.sp, color = Color(0xFF4CAF50))
            }
        }
    }
}

/**
 * Daily XP progress card
 */
@Composable
private fun DailyXPCard(
    todayXP: Int,
    dailyGoal: Int,
    goalMet: Boolean
) {
    val colors = LocalGameColors.current
    val progress = (todayXP.toFloat() / dailyGoal).coerceIn(0f, 1f)
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "xpProgress"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LightningIcon(size = 26.dp)
                    Text(
                        text = "Daily Goal",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
                
                if (goalMet) {
                    Text(
                        text = "✅ Complete!",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.textSecondary.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
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
            
            Text(
                text = "$todayXP / $dailyGoal XP",
                fontSize = 14.sp,
                color = colors.textSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * Weekly XP progress card
 */
@Composable
private fun WeeklyXPCard(
    weeklyXP: Int,
    weeklyGoal: Int
) {
    val colors = LocalGameColors.current
    val progress = (weeklyXP.toFloat() / weeklyGoal).coerceIn(0f, 1f)
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "weeklyXpProgress"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "📊", fontSize = 24.sp)
                Text(
                    text = "Weekly Progress",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.textSecondary.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF1CB0F6),
                                    Color(0xFF00D4FF)
                                )
                            )
                        )
                )
            }
            
            Text(
                text = "$weeklyXP / $weeklyGoal XP",
                fontSize = 14.sp,
                color = colors.textSecondary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

/**
 * Next milestone card
 */
@Composable
private fun NextMilestoneCard(
    milestone: StreakMilestone,
    currentStreak: Int,
    progress: Float
) {
    val colors = LocalGameColors.current
    val daysLeft = milestone.days - currentStreak
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "milestoneProgress"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = milestone.emoji, fontSize = 28.sp)
                    Column {
                        Text(
                            text = milestone.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "$daysLeft days to go",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DiamondIcon(size = 13.dp)
                        Text(
                            text = "${milestone.rewardCoins}",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StarIcon(size = 14.dp)
                        Text(
                            text = "${milestone.rewardGems}",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
            
            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.textSecondary.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFFF9500))
                )
            }
        }
    }
}

/**
 * Unclaimed milestones card
 */
@Composable
private fun UnclaimedMilestonesCard(
    milestones: List<StreakMilestone>,
    onClaim: (StreakMilestone) -> Unit
) {
    val colors = LocalGameColors.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "🎁", fontSize = 24.sp)
                Text(
                    text = "Claim Your Rewards!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
            
            milestones.forEach { milestone ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.cardBackground)
                        .clickable { onClaim(milestone) }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = milestone.emoji, fontSize = 28.sp)
                        Column {
                            Text(
                                text = milestone.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = milestone.description,
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                    
                    Button(
                        onClick = { onClaim(milestone) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "Claim",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Streak Freeze shop card
 */
@Composable
private fun StreakFreezeShopCard(
    freezesOwned: Int,
    diamonds: Int,
    stars: Int,
    onBuyWithDiamonds: () -> Unit,
    onBuyWithGems: () -> Unit
) {
    val colors = LocalGameColors.current
    val canBuyMore = freezesOwned < 2
    val canAffordDiamonds = diamonds >= StreakManager.STREAK_FREEZE_COST_COINS
    val canAffordGems = stars >= StreakManager.STREAK_FREEZE_COST_GEMS
    
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "❄️", fontSize = 28.sp)
                    Column {
                        Text(
                            text = "Streak Freeze",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Protects your streak for 1 day",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                
                Text(
                    text = "$freezesOwned/2",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
            }
            
            if (canBuyMore) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Buy with diamonds
                    Button(
                        onClick = onBuyWithDiamonds,
                        enabled = canAffordDiamonds,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700),
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFFFFD700).copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DiamondIcon(size = 16.dp)
                            Text(
                                text = "${StreakManager.STREAK_FREEZE_COST_COINS}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Buy with gems
                    Button(
                        onClick = onBuyWithGems,
                        enabled = canAffordGems,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF9C27B0),
                            disabledContainerColor = Color(0xFF9C27B0).copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StarIcon(size = 16.dp)
                            Text(
                                text = "${StreakManager.STREAK_FREEZE_COST_GEMS}",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = "Maximum freezes owned!",
                    fontSize = 14.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

/**
 * Stats card
 */
@Composable
private fun StatsCard(
    totalXP: Int,
    longestStreak: Int
) {
    val colors = LocalGameColors.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(emoji = "⚡", value = totalXP.toString(), label = "Total XP")
            StatItem(emoji = "🔥", value = longestStreak.toString(), label = "Best Streak")
        }
    }
}

@Composable
private fun StatItem(emoji: String, value: String, label: String) {
    val colors = LocalGameColors.current
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (emoji == "⚡") {
            LightningIcon(size = 30.dp)
        } else if (emoji == "🔥") {
            FireIcon(size = 30.dp)
        } else {
            Text(text = emoji, fontSize = 28.sp)
        }
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textSecondary
        )
    }
}

/**
 * Streak lost dialog
 */
@Composable
fun StreakLostDialog(
    lostStreak: Int,
    onDismiss: () -> Unit,
    onBuyFreeze: () -> Unit
) {
    val colors = LocalGameColors.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "💔", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Streak Lost!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "You lost your $lostStreak day streak.",
                    fontSize = 16.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Buy a Streak Freeze to protect your streak next time!",
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onBuyFreeze,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text("Get Streak Freeze")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Start Fresh", color = colors.textSecondary)
            }
        }
    )
}

/**
 * Streak freeze used dialog
 */
@Composable
fun StreakFreezeUsedDialog(
    streak: Int,
    freezesLeft: Int,
    onDismiss: () -> Unit
) {
    val colors = LocalGameColors.current
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "❄️", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Streak Protected!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Your $streak day streak was saved by a Streak Freeze!",
                    fontSize = 16.sp,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Freezes remaining: $freezesLeft",
                    fontSize = 14.sp,
                    color = Color(0xFF1CB0F6),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
            ) {
                Text("Continue")
            }
        }
    )
}
