package com.app.gectyping

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors

/**
 * ============================================================================
 * SPELLING GAME SECTION — Hub for Spelling Game modes
 * 
 * - Classic spelling game
 * - Different modes (Sprint, Hardcore, etc.)
 * - Stats and progress
 * ============================================================================
 */

@Composable
fun SpellingGameSection(
    playerName: String,
    avatarEmoji: String,
    lives: Int,
    maxLives: Int,
    diamonds: Int,
    stars: Int,
    dailyStreak: Int,
    currentLevel: Int = 1,
    totalStarsEarned: Int = 0,
    nextLevelStars: Int = 35,
    onStartGame: () -> Unit
) {
    val colors = LocalGameColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // ---------- Header ----------
        Text(
            text = "🎯 Spelling Game",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        
        Text(
            text = "Listen and spell the words correctly!",
            fontSize = 14.sp,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ---------- Level Progress ----------
        LevelProgressCard(
            currentLevel = currentLevel,
            totalStarsEarned = totalStarsEarned,
            nextLevelStars = nextLevelStars
        )

        Spacer(modifier = Modifier.height(4.dp))

        // ---------- Game Modes ----------
        Text(
            text = "Choose Mode",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier.align(Alignment.Start)
        )

        // Classic Mode
        GameModeCard(
            emoji = "🎯",
            title = "Classic",
            description = "Standard spelling game",
            color = Color(0xFF58CC02),
            onClick = onStartGame
        )

        // Sprint Mode (coming soon)
        GameModeCard(
            emoji = "⚡",
            title = "Sprint",
            description = "60 seconds - max words!",
            color = Color(0xFFFF9600),
            onClick = { /* TODO */ },
            isLocked = true
        )

        // Hardcore Mode (coming soon)
        GameModeCard(
            emoji = "💀",
            title = "Hardcore",
            description = "One mistake = game over",
            color = Color(0xFFFF4B4B),
            onClick = { /* TODO */ },
            isLocked = true
        )

        Spacer(modifier = Modifier.weight(1f))

        // ---------- Quick Play Button ----------
        ScaleBounceButton(
            text = "Quick Play",
            icon = null,
            onClick = onStartGame,
            containerColor = colors.accent,
            contentColor = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun LevelProgressCard(
    currentLevel: Int,
    totalStarsEarned: Int,
    nextLevelStars: Int
) {
    val colors = LocalGameColors.current
    val isMaxLevel = currentLevel >= 7
    val progress = if (isMaxLevel || nextLevelStars <= 0) 1f
        else (totalStarsEarned.toFloat() / nextLevelStars).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "levelProgress"
    )
    val starsRemaining = if (isMaxLevel) 0 else (nextLevelStars - totalStarsEarned).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Level label row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StarIcon(size = 18.dp)
                    Text(
                        text = if (isMaxLevel) "Level $currentLevel — MAX" else "Level $currentLevel",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
                if (!isMaxLevel) {
                    Text(
                        text = "Level ${currentLevel + 1}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF58CC02)
                    )
                }
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(colors.textSecondary.copy(alpha = 0.12f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            androidx.compose.ui.graphics.Brush.horizontalGradient(
                                listOf(Color(0xFF58CC02), Color(0xFF7AE033))
                            )
                        )
                )
            }

            // Stars remaining text
            if (!isMaxLevel) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    StarIcon(size = 15.dp)
                    Text(
                        text = "$starsRemaining stars to Level ${currentLevel + 1}",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    StarIcon(size = 15.dp)
                    Text(
                        text = "$totalStarsEarned total stars earned",
                        fontSize = 13.sp,
                        color = Color(0xFF58CC02)
                    )
                }
            }
        }
    }
}

@Composable
private fun GameModeCard(
    emoji: String,
    title: String,
    description: String,
    color: Color,
    onClick: () -> Unit,
    isLocked: Boolean = false
) {
    val colors = LocalGameColors.current
    
    Card(
        onClick = { if (!isLocked) onClick() },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) colors.cardBackground.copy(alpha = 0.5f) else colors.cardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Emoji icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (isLocked) 0.2f else 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                if (!isLocked && emoji == "⚡") {
                    LightningIcon(size = 26.dp)
                } else {
                    Text(
                        text = if (isLocked) "🔒" else emoji,
                        fontSize = 24.sp,
                        modifier = Modifier.graphicsLayer {
                            alpha = if (isLocked) 0.5f else 1f
                        }
                    )
                }
            }
            
            // Text
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) colors.textSecondary else colors.textPrimary
                )
                Text(
                    text = if (isLocked) "Coming soon" else description,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
            
            // Arrow or lock
            if (!isLocked) {
                Text(text = "▶", fontSize = 20.sp, color = color)
            }
        }
    }
}
