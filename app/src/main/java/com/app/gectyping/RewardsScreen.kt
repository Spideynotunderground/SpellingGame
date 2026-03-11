package com.app.gectyping

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors
import java.util.Calendar

/**
 * ============================================================================
 * REWARDS SCREEN — Daily/Weekly tasks, gifts, diamond rewards
 * ============================================================================
 */

data class RewardTask(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val reward: Int,
    val targetProgress: Int,
    val type: RewardTaskType
)

enum class RewardTaskType { DAILY, WEEKLY }

object RewardsManager {
    private const val PREFS = "rewards_system"
    private const val KEY_LAST_DAILY_RESET_DAY = "last_daily_reset_day"
    private const val KEY_LAST_WEEKLY_RESET_WEEK = "last_weekly_reset_week"
    private const val KEY_TASK_PROGRESS_PREFIX = "task_progress_"
    private const val KEY_TASK_CLAIMED_PREFIX = "task_claimed_"
    private const val KEY_DAILY_LOGIN_CLAIMED = "daily_login_claimed_"
    private const val KEY_LOGIN_STREAK = "login_streak"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val DAILY_TASKS = listOf(
        RewardTask("daily_lesson", "Complete a Lesson", "Finish any lesson today", "📖", 10, 1, RewardTaskType.DAILY),
        RewardTask("daily_3lessons", "Study Hard", "Complete 3 lessons today", "🔥", 25, 3, RewardTaskType.DAILY),
        RewardTask("daily_50xp", "Earn 50 XP", "Earn at least 50 XP today", "⚡", 15, 50, RewardTaskType.DAILY),
        RewardTask("daily_perfect", "Perfect Score", "Get a perfect lesson score", "🎯", 20, 1, RewardTaskType.DAILY)
    )

    val WEEKLY_TASKS = listOf(
        RewardTask("weekly_10lessons", "Weekly Scholar", "Complete 10 lessons this week", "📚", 50, 10, RewardTaskType.WEEKLY),
        RewardTask("weekly_500xp", "XP Hunter", "Earn 500 XP this week", "💪", 75, 500, RewardTaskType.WEEKLY),
        RewardTask("weekly_5days", "5-Day Streak", "Be active 5 days this week", "🗓", 60, 5, RewardTaskType.WEEKLY),
        RewardTask("weekly_3perfect", "Perfectionist", "Get 3 perfect scores this week", "👑", 100, 3, RewardTaskType.WEEKLY)
    )

    fun getTaskProgress(ctx: Context, taskId: String): Int {
        checkAndResetTasks(ctx)
        return prefs(ctx).getInt("${KEY_TASK_PROGRESS_PREFIX}$taskId", 0)
    }

    fun isTaskClaimed(ctx: Context, taskId: String): Boolean {
        checkAndResetTasks(ctx)
        return prefs(ctx).getBoolean("${KEY_TASK_CLAIMED_PREFIX}$taskId", false)
    }

    fun claimTask(ctx: Context, taskId: String): Int {
        val task = (DAILY_TASKS + WEEKLY_TASKS).find { it.id == taskId } ?: return 0
        val progress = getTaskProgress(ctx, taskId)
        if (progress < task.targetProgress || isTaskClaimed(ctx, taskId)) return 0
        prefs(ctx).edit().putBoolean("${KEY_TASK_CLAIMED_PREFIX}$taskId", true).apply()
        return task.reward
    }

    fun addTaskProgress(ctx: Context, taskId: String, amount: Int = 1) {
        val p = prefs(ctx)
        val current = p.getInt("${KEY_TASK_PROGRESS_PREFIX}$taskId", 0)
        p.edit().putInt("${KEY_TASK_PROGRESS_PREFIX}$taskId", current + amount).apply()
    }

    fun isDailyLoginClaimed(ctx: Context): Boolean {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val year = Calendar.getInstance().get(Calendar.YEAR)
        return prefs(ctx).getBoolean("${KEY_DAILY_LOGIN_CLAIMED}${year}_$today", false)
    }

    fun claimDailyLogin(ctx: Context): Int {
        val today = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val year = Calendar.getInstance().get(Calendar.YEAR)
        val p = prefs(ctx)
        if (p.getBoolean("${KEY_DAILY_LOGIN_CLAIMED}${year}_$today", false)) return 0
        val streak = p.getInt(KEY_LOGIN_STREAK, 0) + 1
        val reward = 5 + (streak.coerceAtMost(7) * 2) // 7 to 19 diamonds based on streak
        p.edit()
            .putBoolean("${KEY_DAILY_LOGIN_CLAIMED}${year}_$today", true)
            .putInt(KEY_LOGIN_STREAK, streak)
            .apply()
        return reward
    }

    fun getLoginStreak(ctx: Context): Int = prefs(ctx).getInt(KEY_LOGIN_STREAK, 0)

    private fun checkAndResetTasks(ctx: Context) {
        val p = prefs(ctx)
        val cal = Calendar.getInstance()
        val today = cal.get(Calendar.DAY_OF_YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val lastDailyReset = p.getInt(KEY_LAST_DAILY_RESET_DAY, 0)
        val lastWeeklyReset = p.getInt(KEY_LAST_WEEKLY_RESET_WEEK, 0)

        val editor = p.edit()
        if (today != lastDailyReset) {
            DAILY_TASKS.forEach { task ->
                editor.putInt("${KEY_TASK_PROGRESS_PREFIX}${task.id}", 0)
                editor.putBoolean("${KEY_TASK_CLAIMED_PREFIX}${task.id}", false)
            }
            editor.putInt(KEY_LAST_DAILY_RESET_DAY, today)
        }
        if (week != lastWeeklyReset) {
            WEEKLY_TASKS.forEach { task ->
                editor.putInt("${KEY_TASK_PROGRESS_PREFIX}${task.id}", 0)
                editor.putBoolean("${KEY_TASK_CLAIMED_PREFIX}${task.id}", false)
            }
            editor.putInt(KEY_LAST_WEEKLY_RESET_WEEK, week)
        }
        editor.apply()
    }
}

@Composable
fun RewardsScreen(
    context: Context,
    diamonds: Int,
    onDiamondsChange: (Int) -> Unit,
    onBack: () -> Unit
) {
    val colors = LocalGameColors.current
    var refreshKey by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.cardBackground)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textPrimary
                )
            }
            Text(
                text = "Rewards",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            // Diamond count
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                DiamondIcon(size = 20.dp)
                Text(
                    text = "$diamonds",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1CB0F6)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Daily Login Gift
            item {
                DailyLoginCard(
                    context = context,
                    onClaim = { reward ->
                        onDiamondsChange(diamonds + reward)
                        refreshKey++
                    },
                    key = refreshKey
                )
            }

            // Daily Tasks header
            item {
                Text(
                    text = "Daily Tasks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Daily tasks
            itemsIndexed(RewardsManager.DAILY_TASKS) { _, task ->
                TaskCard(
                    context = context,
                    task = task,
                    onClaim = { reward ->
                        onDiamondsChange(diamonds + reward)
                        refreshKey++
                    },
                    key = refreshKey
                )
            }

            // Weekly Tasks header
            item {
                Text(
                    text = "Weekly Tasks",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Weekly tasks
            itemsIndexed(RewardsManager.WEEKLY_TASKS) { _, task ->
                TaskCard(
                    context = context,
                    task = task,
                    onClaim = { reward ->
                        onDiamondsChange(diamonds + reward)
                        refreshKey++
                    },
                    key = refreshKey
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun DailyLoginCard(
    context: Context,
    onClaim: (Int) -> Unit,
    key: Int
) {
    val colors = LocalGameColors.current
    val isClaimed = remember(key) { RewardsManager.isDailyLoginClaimed(context) }
    val streak = remember(key) { RewardsManager.getLoginStreak(context) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFFF9600).copy(alpha = 0.2f),
                            Color(0xFFFFD700).copy(alpha = 0.1f)
                        )
                    )
                )
                .border(1.dp, Color(0xFFFFD700).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🎁 Daily Login Gift",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Day $streak streak · Come back tomorrow for more!",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
                if (!isClaimed) {
                    Button(
                        onClick = {
                            val reward = RewardsManager.claimDailyLogin(context)
                            if (reward > 0) {
                                SoundManager.giftOpen()
                                onClaim(reward)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFFD700)
                        )
                    ) {
                        Text("Claim", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF4CAF50).copy(alpha = 0.2f))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("✓ Claimed", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                }
            }
        }
    }
}

@Composable
private fun TaskCard(
    context: Context,
    task: RewardTask,
    onClaim: (Int) -> Unit,
    key: Int
) {
    val colors = LocalGameColors.current
    val progress = remember(key) { RewardsManager.getTaskProgress(context, task.id) }
    val isClaimed = remember(key) { RewardsManager.isTaskClaimed(context, task.id) }
    val isComplete = progress >= task.targetProgress
    val progressFraction = (progress.toFloat() / task.targetProgress).coerceIn(0f, 1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isClaimed) colors.cardBackground.copy(alpha = 0.5f) else colors.cardBackground
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = task.emoji, fontSize = 28.sp)
                    Column {
                        Text(
                            text = task.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = task.description,
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }
                // Reward badge
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DiamondIcon(size = 16.dp)
                    Text(
                        text = "+${task.reward}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1CB0F6)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(colors.textSecondary.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (isComplete) Color(0xFF4CAF50)
                            else Color(0xFF1CB0F6)
                        )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$progress / ${task.targetProgress}",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
                if (isComplete && !isClaimed) {
                    Button(
                        onClick = {
                            val reward = RewardsManager.claimTask(context, task.id)
                            if (reward > 0) {
                                SoundManager.claim()
                                onClaim(reward)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Claim", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (isClaimed) {
                    Text("✓ Done", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                }
            }
        }
    }
}
