package com.app.gectyping

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors

/**
 * ============================================================================
 * PROFILE SECTION — User profile, stats, settings, achievements
 * ============================================================================
 */

private val DuoGreen = Color(0xFF58CC02)
private val DuoBlue = Color(0xFF1CB0F6)
private val DuoOrange = Color(0xFFFF9600)

@Composable
fun ProfileSection(
    context: Context,
    playerName: String,
    avatarEmoji: String,
    selectedAvatarId: String = "space",
    diamonds: Int,
    stars: Int,
    lives: Int,
    maxLives: Int,
    onNameChange: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAchievements: () -> Unit,
    onOpenShop: () -> Unit,
    hasUnclaimedAchievements: Boolean,
    onDiamondsChange: (Int) -> Unit,
    onStarsChange: (Int) -> Unit
) {
    val colors = LocalGameColors.current
    
    val streakState = remember { 
        try { StreakManager.getStreakState(context) } catch (e: Exception) { StreakState() } 
    }
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile header
        item {
            ProfileHeader(
                playerName = playerName,
                avatarEmoji = avatarEmoji,
                selectedAvatarId = selectedAvatarId,
                onNameChange = onNameChange
            )
        }
        
        // Stats cards
        item {
            StatsRow(
                streak = streakState.currentStreak,
                totalXP = streakState.totalXP,
                longestStreak = streakState.longestStreak,
                diamonds = diamonds
            )
        }
        
        // Streak card
        item {
            StreakCard(
                context = context,
                streakState = streakState,
                diamonds = diamonds,
                stars = stars,
                onDiamondsChange = onDiamondsChange,
                onStarsChange = onStarsChange
            )
        }
        
        // Menu items
        item {
            ProfileMenuCard(
                onOpenAchievements = onOpenAchievements,
                onOpenShop = onOpenShop,
                onOpenSettings = onOpenSettings,
                hasUnclaimedAchievements = hasUnclaimedAchievements
            )
        }
        
        // Bottom spacer
        item {
            Spacer(modifier = Modifier.height(60.dp))
        }
    }
}

@Composable
private fun ProfileHeader(
    playerName: String,
    avatarEmoji: String,
    selectedAvatarId: String = "space",
    onNameChange: (String) -> Unit
) {
    val colors = LocalGameColors.current
    var isEditing by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf(playerName) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(DuoGreen.copy(alpha = 0.2f))
                    .border(4.dp, DuoGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AvatarIcon(avatar = avatarById(selectedAvatarId), size = 64.dp, fontSize = 48.sp)
            }
            
            // Name
            if (isEditing) {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { isEditing = false }) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onNameChange(editedName)
                            isEditing = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DuoGreen)
                    ) {
                        Text("Save")
                    }
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { isEditing = true }
                ) {
                    Text(
                        text = playerName.ifBlank { "Player" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit name",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatsRow(
    streak: Int,
    totalXP: Int,
    longestStreak: Int,
    diamonds: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            emoji = "🔥",
            value = streak.toString(),
            label = "Day Streak",
            color = DuoOrange,
            modifier = Modifier.weight(1f)
        )
        StatCard(
            emoji = "⚡",
            value = totalXP.toString(),
            label = "Total XP",
            color = DuoBlue,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    emoji: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalGameColors.current
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                color = color
            )
            Text(
                text = label,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun StreakCard(
    context: Context,
    streakState: StreakState,
    diamonds: Int,
    stars: Int,
    onDiamondsChange: (Int) -> Unit,
    onStarsChange: (Int) -> Unit
) {
    val colors = LocalGameColors.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FireIcon(size = 20.dp)
                    Text(
                        text = "Streak",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
                Text(
                    text = "${streakState.currentStreak} days",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DuoOrange
                )
            }
            
            // Streak freezes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "❄️", fontSize = 20.sp)
                    Text(
                        text = "Streak Freezes: ${streakState.streakFreezes}",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                }
                
                TextButton(
                    onClick = {
                        try {
                            val result = StreakManager.buyStreakFreezeWithCoins(context, diamonds)
                            if (result is BuyResult.Success) {
                                onDiamondsChange(diamonds - StreakManager.STREAK_FREEZE_COST_COINS)
                            }
                        } catch (e: Exception) { /* Handle */ }
                    },
                    enabled = diamonds >= StreakManager.STREAK_FREEZE_COST_COINS
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Buy (${StreakManager.STREAK_FREEZE_COST_COINS} ")
                        DiamondIcon(size = 14.dp)
                        Text(")")
                    }
                }
            }
            
            // Daily goal progress
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Daily Goal",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "${streakState.todayXP}/${streakState.dailyGoalXP} XP",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (streakState.todayGoalMet) DuoGreen else colors.textPrimary
                    )
                }
                
                LinearProgressIndicator(
                    progress = { (streakState.todayXP.toFloat() / streakState.dailyGoalXP).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (streakState.todayGoalMet) DuoGreen else DuoBlue,
                    trackColor = colors.textSecondary.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
private fun ProfileMenuCard(
    onOpenAchievements: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenSettings: () -> Unit,
    hasUnclaimedAchievements: Boolean
) {
    val colors = LocalGameColors.current
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column {
            ProfileMenuItem(
                icon = Icons.Default.EmojiEvents,
                title = "Achievements",
                subtitle = if (hasUnclaimedAchievements) "Rewards available!" else "View your progress",
                showBadge = hasUnclaimedAchievements,
                onClick = onOpenAchievements
            )
            
            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.1f))
            
            ProfileMenuItem(
                icon = Icons.Default.ShoppingBag,
                title = "Shop",
                subtitle = "Avatars, themes, and more",
                onClick = onOpenShop
            )
            
            HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.1f))
            
            ProfileMenuItem(
                icon = Icons.Default.Settings,
                title = "Settings",
                subtitle = "Sound, vibration, and more",
                onClick = onOpenSettings
            )
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    showBadge: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalGameColors.current
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = colors.accent,
                modifier = Modifier.size(24.dp)
            )
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 2.dp, y = (-2).dp)
                        .background(Color(0xFFFF4B4B), shape = CircleShape)
                )
            }
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
