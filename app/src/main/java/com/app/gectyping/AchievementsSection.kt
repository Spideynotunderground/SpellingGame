package com.app.gectyping

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors

/**
 * ============================================================================
 * ACHIEVEMENTS SECTION — 15 goals, 4 categories
 * ============================================================================
 */
@Composable
fun AchievementsSection(
    context: Context,
    diamonds: Int,
    onDiamondsChange: (Int) -> Unit
) {
    val colors = LocalGameColors.current
    val categories = ALL_ACHIEVEMENTS.groupBy { it.category }

    // Force recomposition when claiming
    var refreshKey by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(
            text = "Achievements",
            color = colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            categories.forEach { (category, achievements) ->
                item(key = "header_$category") {
                    Text(
                        text = category,
                        color = colors.textPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(achievements, key = { "${it.id}_$refreshKey" }) { achievement ->
                    AchievementCard(
                        achievement = achievement,
                        context = context,
                        onClaim = {
                            val reward = AchievementManager.claim(context, achievement.id)
                            if (reward > 0) {
                                onDiamondsChange(diamonds + reward)
                                SoundManager.claim()
                                refreshKey++
                            }
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

/**
 * Achievements Bottom Sheet — shown from Menu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsBottomSheet(
    context: Context,
    diamonds: Int,
    onDiamondsChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalGameColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBackground
    ) {
        AchievementsSection(
            context = context,
            diamonds = diamonds,
            onDiamondsChange = onDiamondsChange
        )
    }
}

@Composable
private fun AchievementCard(
    achievement: Achievement,
    context: Context,
    onClaim: () -> Unit
) {
    val colors = LocalGameColors.current
    val progress = AchievementManager.getProgress(context, achievement)
    val unlocked = progress >= achievement.targetValue
    val claimed = AchievementManager.isClaimed(context, achievement.id)
    val progressFraction = (progress.toFloat() / achievement.targetValue).coerceIn(0f, 1f)
    val accent = colors.accent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground)
            .border(
                width = if (unlocked && !claimed) 1.5.dp else 0.dp,
                color = if (unlocked && !claimed) accent else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .then(
                if (unlocked && !claimed) Modifier.clickable { onClaim() } else Modifier
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Text(
            text = achievement.iconEmoji,
            fontSize = 30.sp,
            modifier = Modifier.padding(end = 12.dp)
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = achievement.title,
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = achievement.description,
                color = colors.textSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (unlocked) Color(0xFF4CAF50) else accent,
                trackColor = colors.textSecondary.copy(alpha = 0.15f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$progress / ${achievement.targetValue}",
                color = colors.textSecondary,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Reward / Status
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (claimed) {
                Text(text = "✅", fontSize = 20.sp)
                Text("Claimed", color = Color(0xFF4CAF50), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            } else if (unlocked) {
                Text(text = "🎁", fontSize = 20.sp)
                Text("Claim!", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DiamondIcon(size = 13.dp)
                    Text("${achievement.rewardCoins}", color = Color(0xFF1CB0F6), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
