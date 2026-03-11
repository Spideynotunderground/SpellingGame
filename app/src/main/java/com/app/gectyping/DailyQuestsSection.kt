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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors

/**
 * ============================================================================
 * DAILY QUESTS SECTION — Daily challenges UI
 * ============================================================================
 */

@Composable
fun DailyQuestsSection(
    context: Context,
    onRewardClaimed: (diamonds: Int, xp: Int) -> Unit
) {
    val colors = LocalGameColors.current
    
    var state by remember { 
        mutableStateOf(try { DailyQuestsManager.getQuestsState(context) } catch (e: Exception) { DailyQuestsState(emptyList(), false, false, "", 0) }) 
    }
    var timeRemaining by remember { 
        mutableStateOf(try { DailyQuestsManager.formatTimeUntilRefresh(context) } catch (e: Exception) { "0h" }) 
    }
    var showBonusDialog by remember { mutableStateOf(false) }
    var bonusReward by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    
    // Refresh timer
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60000)
            try {
                timeRemaining = DailyQuestsManager.formatTimeUntilRefresh(context)
                state = DailyQuestsManager.getQuestsState(context)
            } catch (e: Exception) { /* Silently handle */ }
        }
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
            DailyQuestsHeader(
                timeRemaining = timeRemaining,
                completedCount = state.quests.count { it.isCompleted },
                totalCount = state.quests.size
            )
        }
        
        // Bonus chest
        item {
            BonusChestCard(
                allCompleted = state.allCompleted,
                isClaimed = state.bonusChestClaimed,
                onClaim = {
                    val reward = DailyQuestsManager.claimBonusChest(context)
                    if (reward != null) {
                        bonusReward = reward
                        showBonusDialog = true
                        onRewardClaimed(reward.first, reward.second)
                        state = DailyQuestsManager.getQuestsState(context)
                    }
                }
            )
        }
        
        // Quests
        items(state.quests) { quest ->
            QuestCard(
                quest = quest,
                onClaim = {
                    val reward = DailyQuestsManager.claimQuest(context, quest.id)
                    if (reward != null) {
                        onRewardClaimed(reward.first, reward.second)
                        state = DailyQuestsManager.getQuestsState(context)
                    }
                }
            )
        }
        
        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
    
    // Bonus reward dialog
    if (showBonusDialog && bonusReward != null) {
        BonusChestDialog(
            diamonds = bonusReward!!.first,
            xp = bonusReward!!.second,
            onDismiss = { showBonusDialog = false }
        )
    }
}

@Composable
private fun DailyQuestsHeader(
    timeRemaining: String,
    completedCount: Int,
    totalCount: Int
) {
    val colors = LocalGameColors.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
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
                    Text(text = "📋", fontSize = 28.sp)
                    Text(
                        text = "Daily Quests",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
                
                // Timer
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "⏰", fontSize = 14.sp)
                    Text(
                        text = timeRemaining,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
            
            // Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$completedCount of $totalCount completed",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
                
                // Progress dots
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(totalCount) { index ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index < completedCount) Color(0xFF58CC02)
                                    else colors.textSecondary.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BonusChestCard(
    allCompleted: Boolean,
    isClaimed: Boolean,
    onClaim: () -> Unit
) {
    val colors = LocalGameColors.current
    
    // Chest animation
    val infiniteTransition = rememberInfiniteTransition(label = "chest")
    val chestScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (allCompleted && !isClaimed) 1.1f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "chestScale"
    )
    
    val backgroundColor = when {
        isClaimed -> colors.textSecondary.copy(alpha = 0.1f)
        allCompleted -> Color(0xFFFFD700).copy(alpha = 0.2f)
        else -> colors.cardBackground
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (allCompleted && !isClaimed) Modifier.clickable { onClaim() }
                else Modifier
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Chest icon
            Text(
                text = when {
                    isClaimed -> "📦"
                    allCompleted -> "🎁"
                    else -> "🔒"
                },
                fontSize = 48.sp,
                modifier = Modifier.scale(chestScale)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Bonus Chest",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = when {
                        isClaimed -> "Claimed! Come back tomorrow"
                        allCompleted -> "Tap to open!"
                        else -> "Complete all quests to unlock"
                    },
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
            
            if (allCompleted && !isClaimed) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🪙 100", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LightningIcon(size = 16.dp)
                        Text(text = "50", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else if (isClaimed) {
                Text(text = "✅", fontSize = 32.sp)
            }
        }
    }
}

@Composable
private fun QuestCard(
    quest: DailyQuest,
    onClaim: () -> Unit
) {
    val colors = LocalGameColors.current
    val progress = (quest.currentProgress.toFloat() / quest.targetValue).coerceIn(0f, 1f)
    
    val backgroundColor = when {
        quest.isClaimed -> colors.textSecondary.copy(alpha = 0.1f)
        quest.isCompleted -> Color(0xFF58CC02).copy(alpha = 0.15f)
        else -> colors.cardBackground
    }
    
    val borderColor = when {
        quest.isCompleted && !quest.isClaimed -> Color(0xFF58CC02)
        else -> Color.Transparent
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (quest.isCompleted && !quest.isClaimed) Modifier.clickable { onClaim() }
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = if (borderColor != Color.Transparent) {
            CardDefaults.outlinedCardBorder().copy(brush = Brush.linearGradient(listOf(borderColor, borderColor)))
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quest icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            quest.isClaimed -> colors.textSecondary.copy(alpha = 0.2f)
                            quest.isCompleted -> Color(0xFF58CC02)
                            else -> colors.accent.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = quest.type.emoji,
                    fontSize = 24.sp
                )
            }
            
            // Quest info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = quest.type.description.format(quest.targetValue),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
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
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (quest.isCompleted) Color(0xFF58CC02)
                                else colors.accent
                            )
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${quest.currentProgress}/${quest.targetValue}",
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
            
            // Reward / Status
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when {
                    quest.isClaimed -> {
                        Text(text = "✅", fontSize = 24.sp)
                        Text(
                            text = "Done",
                            fontSize = 10.sp,
                            color = Color(0xFF58CC02),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    quest.isCompleted -> {
                        Text(text = "🎁", fontSize = 24.sp)
                        Text(
                            text = "Claim!",
                            fontSize = 10.sp,
                            color = Color(0xFF58CC02),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    else -> {
                        Row(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DiamondIcon(size = 13.dp)
                            Text(
                                text = "${quest.rewardDiamonds}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1CB0F6)
                            )
                        }
                        Row(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LightningIcon(size = 14.dp)
                            Text(
                                text = "${quest.rewardXP}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.accent
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BonusChestDialog(
    diamonds: Int,
    xp: Int,
    onDismiss: () -> Unit
) {
    val colors = LocalGameColors.current
    
    // Celebration animation
    val infiniteTransition = rememberInfiniteTransition(label = "bonusCelebration")
    val celebrationScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebrationScale"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.cardBackground,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🎁",
                    fontSize = 80.sp,
                    modifier = Modifier.scale(celebrationScale)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Bonus Chest Opened!",
                    fontSize = 22.sp,
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
                    text = "You completed all daily quests!",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DiamondIcon(size = 44.dp)
                        Text(
                            text = "+$diamonds",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1CB0F6)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LightningIcon(size = 44.dp)
                        Text(
                            text = "+$xp",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58CC02)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Awesome!", fontWeight = FontWeight.Bold)
            }
        }
    )
}
