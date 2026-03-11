package com.app.gectyping

import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors

/**
 * ============================================================================
 * LEAGUES SECTION — Weekly competition leaderboard
 * ============================================================================
 */

@Composable
fun LeaguesSection(
    context: Context,
    onClaimReward: (diamonds: Int, gems: Int) -> Unit
) {
    val colors = LocalGameColors.current
    
    var state by remember { 
        mutableStateOf(try { LeaguesManager.getLeagueState(context) } catch (e: Exception) { LeagueState() }) 
    }
    var leaderboard by remember { 
        mutableStateOf(try { LeaguesManager.getLeaderboard(context) } catch (e: Exception) { emptyList() }) 
    }
    var timeRemaining by remember { 
        mutableStateOf(try { LeaguesManager.formatTimeRemaining(context) } catch (e: Exception) { "0h" }) 
    }
    var showRewardDialog by remember { mutableStateOf(false) }
    var rewardAmount by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    
    // Update time remaining
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60000) // Update every minute
            try {
                timeRemaining = LeaguesManager.formatTimeRemaining(context)
                state = LeaguesManager.getLeagueState(context)
                leaderboard = LeaguesManager.getLeaderboard(context)
            } catch (e: Exception) { /* Silently handle */ }
        }
    }
    
    val zone = try { LeaguesManager.getZone(context) } catch (e: Exception) { LeagueZone.SAFE }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // League header
        item {
            LeagueHeader(
                tier = state.currentTier,
                position = state.currentPosition,
                totalPlayers = state.totalPlayers,
                weeklyXP = state.weeklyXP,
                timeRemaining = timeRemaining,
                zone = zone
            )
        }
        
        // Promotion/Demotion status
        if (state.wasPromoted || state.wasDemoted) {
            item {
                StatusCard(
                    wasPromoted = state.wasPromoted,
                    wasDemoted = state.wasDemoted,
                    lastPosition = state.lastWeekPosition,
                    canClaimReward = !state.hasClaimedWeeklyReward,
                    onClaimReward = {
                        val reward = LeaguesManager.claimWeeklyReward(context)
                        if (reward != null) {
                            rewardAmount = reward
                            showRewardDialog = true
                            onClaimReward(reward.first, reward.second)
                        }
                    }
                )
            }
        }
        
        // Leaderboard header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Leaderboard",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "${state.totalPlayers} players",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }
        
        // Leaderboard
        itemsIndexed(leaderboard) { index, player ->
            val position = index + 1
            val isInPromotionZone = position <= state.currentTier.promotionSlots
            val isInDemotionZone = position > state.totalPlayers - state.currentTier.demotionSlots
            
            LeaderboardRow(
                position = position,
                player = player,
                isPromotionZone = isInPromotionZone && state.currentTier != LeagueTier.DIAMOND,
                isDemotionZone = isInDemotionZone && state.currentTier != LeagueTier.BRONZE
            )
        }
        
        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
    
    // Reward dialog
    if (showRewardDialog && rewardAmount != null) {
        RewardClaimedDialog(
            diamonds = rewardAmount!!.first,
            gems = rewardAmount!!.second,
            onDismiss = { showRewardDialog = false }
        )
    }
}

@Composable
private fun LeagueHeader(
    tier: LeagueTier,
    position: Int,
    totalPlayers: Int,
    weeklyXP: Int,
    timeRemaining: String,
    zone: LeagueZone
) {
    val colors = LocalGameColors.current
    val tierColor = Color(tier.color)
    
    // Pulse animation for current tier
    val infiniteTransition = rememberInfiniteTransition(label = "tierPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = tierColor.copy(alpha = 0.15f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tier badge
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tierColor,
                                tierColor.copy(alpha = 0.7f)
                            )
                        )
                    )
                    .border(4.dp, tierColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tier.emoji,
                    fontSize = 48.sp
                )
            }
            
            // Tier name
            Text(
                text = "${tier.displayName} League",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
            
            // Position
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val zoneColor = when (zone) {
                    LeagueZone.PROMOTION -> Color(0xFF58CC02)
                    LeagueZone.DEMOTION -> Color(0xFFFF4B4B)
                    LeagueZone.SAFE -> colors.textSecondary
                }
                
                Text(
                    text = "#$position",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = zoneColor
                )
                Text(
                    text = "of $totalPlayers",
                    fontSize = 16.sp,
                    color = colors.textSecondary
                )
            }
            
            // Zone indicator
            val zoneText = when (zone) {
                LeagueZone.PROMOTION -> "🚀 Promotion Zone!"
                LeagueZone.DEMOTION -> "⚠️ Demotion Zone"
                LeagueZone.SAFE -> "Safe Zone"
            }
            val zoneColor = when (zone) {
                LeagueZone.PROMOTION -> Color(0xFF58CC02)
                LeagueZone.DEMOTION -> Color(0xFFFF4B4B)
                LeagueZone.SAFE -> colors.textSecondary
            }
            
            Text(
                text = zoneText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = zoneColor
            )
            
            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.2f))
            
            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LightningIcon(size = 22.dp)
                    Text(
                        text = "$weeklyXP",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Weekly XP",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "⏰", fontSize = 20.sp)
                    Text(
                        text = timeRemaining,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Time Left",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "🎁", fontSize = 20.sp)
                    Text(
                        text = "${tier.weeklyRewardCoins}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Reward",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    wasPromoted: Boolean,
    wasDemoted: Boolean,
    lastPosition: Int,
    canClaimReward: Boolean,
    onClaimReward: () -> Unit
) {
    val colors = LocalGameColors.current
    
    val (emoji, title, subtitle, cardColor) = when {
        wasPromoted -> listOf("🎉", "Promoted!", "You moved up a league!", Color(0xFF58CC02))
        wasDemoted -> listOf("😢", "Demoted", "Keep practicing!", Color(0xFFFF4B4B))
        else -> listOf("✨", "Week Complete", "Position #$lastPosition", colors.accent)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = (cardColor as Color).copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji as String, fontSize = 40.sp)
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title as String,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = subtitle as String,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
            
            if (canClaimReward) {
                Button(
                    onClick = onClaimReward,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF58CC02)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Claim", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    position: Int,
    player: LeaguePlayer,
    isPromotionZone: Boolean,
    isDemotionZone: Boolean
) {
    val colors = LocalGameColors.current
    
    val backgroundColor = when {
        player.isCurrentUser -> colors.accent.copy(alpha = 0.15f)
        isPromotionZone -> Color(0xFF58CC02).copy(alpha = 0.1f)
        isDemotionZone -> Color(0xFFFF4B4B).copy(alpha = 0.1f)
        else -> colors.cardBackground
    }
    
    val borderColor = when {
        player.isCurrentUser -> colors.accent
        else -> Color.Transparent
    }
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(
                width = if (player.isCurrentUser) 2.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Position
        Box(
            modifier = Modifier.width(32.dp),
            contentAlignment = Alignment.Center
        ) {
            when (position) {
                1 -> Text(text = "🥇", fontSize = 24.sp)
                2 -> Text(text = "🥈", fontSize = 24.sp)
                3 -> Text(text = "🥉", fontSize = 24.sp)
                else -> Text(
                    text = "#$position",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
            }
        }
        
        // Avatar
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.textSecondary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = player.avatarEmoji, fontSize = 20.sp)
        }
        
        // Name
        Text(
            text = if (player.isCurrentUser) "${player.name} (You)" else player.name,
            fontSize = 14.sp,
            fontWeight = if (player.isCurrentUser) FontWeight.Bold else FontWeight.Medium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        
        // XP
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LightningIcon(size = 16.dp)
            Text(
                text = "${player.weeklyXP}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        }
        
        // Zone indicator
        if (isPromotionZone) {
            Icon(
                imageVector = Icons.Default.ArrowUpward,
                contentDescription = "Promotion",
                tint = Color(0xFF58CC02),
                modifier = Modifier.size(16.dp)
            )
        } else if (isDemotionZone) {
            Icon(
                imageVector = Icons.Default.ArrowDownward,
                contentDescription = "Demotion",
                tint = Color(0xFFFF4B4B),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun RewardClaimedDialog(
    diamonds: Int,
    gems: Int,
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
                Text(text = "🎉", fontSize = 64.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reward Claimed!",
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DiamondIcon(size = 36.dp)
                        Text(
                            text = "+$diamonds",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1CB0F6)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        DiamondIcon(size = 36.dp)
                        Text(
                            text = "+$gems",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1CB0F6)
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
