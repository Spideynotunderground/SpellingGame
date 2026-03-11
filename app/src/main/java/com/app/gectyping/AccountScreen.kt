package com.app.gectyping

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors
import java.util.Calendar

/**
 * ============================================================================
 * ACCOUNT SCREEN — Player profile, Weekly Activity, Leaderboard
 * ============================================================================
 */

data class LeaderboardEntry(
    val rank: Int,
    val name: String,
    val xp: Int = 0,
    val diamonds: Int = 0,
    val isCurrentPlayer: Boolean = false
)

object LeaderboardManager {
    private const val PREFS = "leaderboard_system"
    private const val KEY_WEEKLY_DIAMONDS = "weekly_diamonds"
    private const val KEY_LAST_RESET_WEEK = "last_reset_week"
    private const val KEY_LAST_RESET_YEAR = "last_reset_year"

    // Simulated bot players for leaderboard
    private val BOT_NAMES = listOf(
        "Alex_Pro", "Maria_IELTS", "John_Study", "Emma_Words",
        "David_Learn", "Sophie_Eng", "Michael_Top", "Anna_Best",
        "James_Star", "Olivia_Win", "Daniel_Go", "Lisa_Smart",
        "Robert_Ace", "Sarah_Fast", "Thomas_Up", "Emily_Gem",
        "William_Fly", "Jessica_Run", "Chris_Max", "Laura_Zen"
    )

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getWeeklyDiamonds(ctx: Context): Int {
        checkWeeklyReset(ctx)
        return prefs(ctx).getInt(KEY_WEEKLY_DIAMONDS, 0)
    }

    fun addWeeklyDiamonds(ctx: Context, amount: Int) {
        val p = prefs(ctx)
        checkWeeklyReset(ctx)
        val current = p.getInt(KEY_WEEKLY_DIAMONDS, 0)
        p.edit().putInt(KEY_WEEKLY_DIAMONDS, current + amount).apply()
    }

    fun getLeaderboard(ctx: Context, playerName: String, playerDiamonds: Int, playerXp: Int = 0): List<LeaderboardEntry> {
        val weeklyPlayerDiamonds = getWeeklyDiamonds(ctx)

        // Generate bot scores based on current week seed
        val cal = Calendar.getInstance()
        val weekSeed = cal.get(Calendar.WEEK_OF_YEAR) * 1000 + cal.get(Calendar.YEAR)
        val random = java.util.Random(weekSeed.toLong())

        val entries = mutableListOf<LeaderboardEntry>()

        // Add bots — XP is the primary ranking metric
        BOT_NAMES.forEach { name ->
            val botXp = random.nextInt(500) + 50
            val botDiamonds = random.nextInt(200) + 10
            entries.add(LeaderboardEntry(
                rank = 0,
                name = name,
                xp = botXp,
                diamonds = botDiamonds
            ))
        }

        // Add current player — XP is primary
        entries.add(LeaderboardEntry(
            rank = 0,
            name = playerName.ifEmpty { "You" },
            xp = playerXp,
            diamonds = weeklyPlayerDiamonds,
            isCurrentPlayer = true
        ))

        // Sort by XP descending (primary), then diamonds (tiebreaker)
        return entries
            .sortedWith(compareByDescending<LeaderboardEntry> { it.xp }.thenByDescending { it.diamonds })
            .mapIndexed { index, entry ->
                entry.copy(rank = index + 1)
            }
    }

    private fun checkWeeklyReset(ctx: Context) {
        val p = prefs(ctx)
        val cal = Calendar.getInstance()
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val currentYear = cal.get(Calendar.YEAR)
        val lastWeek = p.getInt(KEY_LAST_RESET_WEEK, 0)
        val lastYear = p.getInt(KEY_LAST_RESET_YEAR, 0)

        if (currentWeek != lastWeek || currentYear != lastYear) {
            p.edit()
                .putInt(KEY_WEEKLY_DIAMONDS, 0)
                .putInt(KEY_LAST_RESET_WEEK, currentWeek)
                .putInt(KEY_LAST_RESET_YEAR, currentYear)
                .apply()
        }
    }
}

@Composable
fun AccountScreen(
    context: Context,
    playerName: String,
    avatarEmoji: String,
    selectedAvatarId: String = "space",
    diamonds: Int,
    xp: Int = 0
) {
    val colors = LocalGameColors.current
    val streakState = remember { StreakManager.getStreakState(context) }
    val weeklyMinutes = remember { StreakManager.getWeeklyActivityMinutes(context) }
    val totalWeeklyMinutes = remember { weeklyMinutes.sumOf { it.minutes } }
    val leaderboard = remember(diamonds, xp) {
        LeaderboardManager.getLeaderboard(context, playerName, diamonds, xp)
    }
    val playerRank = leaderboard.find { it.isCurrentPlayer }?.rank ?: 0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Profile header
        item {
            ProfileHeader(
                playerName = playerName,
                avatarEmoji = avatarEmoji,
                selectedAvatarId = selectedAvatarId,
                diamonds = diamonds,
                streak = streakState.currentStreak,
                totalXP = streakState.totalXP,
                rank = playerRank
            )
        }

        // Daily Activity Line Graph (Screen Time style)
        item {
            WeeklyActivityLineGraph(
                data = weeklyMinutes,
                totalMinutes = totalWeeklyMinutes
            )
        }

        // Leaderboard header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🏆 Weekly Leaderboard",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "Resets weekly",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        // Top 3 podium
        item {
            TopThreePodium(leaderboard.take(3))
        }

        // Rest of leaderboard
        val restEntries = leaderboard.drop(3)
        itemsIndexed(restEntries) { _, entry ->
            LeaderboardRow(entry = entry)
        }

        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
private fun ProfileHeader(
    playerName: String,
    avatarEmoji: String,
    selectedAvatarId: String = "space",
    diamonds: Int,
    streak: Int,
    totalXP: Int,
    rank: Int
) {
    val colors = LocalGameColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF1CB0F6).copy(alpha = 0.15f),
                            colors.cardBackground
                        )
                    )
                )
                .border(1.dp, Color(0xFF1CB0F6).copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1CB0F6).copy(alpha = 0.2f))
                        .border(3.dp, Color(0xFF1CB0F6).copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarIcon(avatar = avatarById(selectedAvatarId), size = 52.dp, fontSize = 40.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = playerName.ifEmpty { "Player" },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem(emoji = "💎", value = "$diamonds", label = "Diamonds")
                    StatItem(emoji = "🔥", value = "$streak", label = "Streak")
                    StatItem(emoji = "⚡", value = "$totalXP", label = "Total XP")
                    StatItem(emoji = "🏅", value = "#$rank", label = "Rank")
                }
            }
        }
    }
}

@Composable
private fun StatItem(emoji: String, value: String, label: String) {
    val colors = LocalGameColors.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (emoji == "⚡") {
            LightningIcon(size = 22.dp)
        } else if (emoji == "🔥") {
            FireIcon(size = 22.dp)
        } else if (emoji == "💎") {
            DiamondIcon(size = 22.dp)
        } else {
            Text(text = emoji, fontSize = 20.sp)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            fontSize = 16.sp,
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

@Composable
private fun WeeklyActivityCard(
    weeklyActivity: List<DayActivityDisplay>,
    streakState: StreakState
) {
    val colors = LocalGameColors.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "📊 Weekly Activity",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Day circles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                weeklyActivity.forEach { day ->
                    DayCircle(day = day)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // XP progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Weekly XP: ${streakState.weeklyXP}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Text(
                    text = "Goal: ${streakState.weeklyGoalXP}",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            val weeklyProgress = (streakState.weeklyXP.toFloat() / streakState.weeklyGoalXP).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(colors.textSecondary.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(weeklyProgress)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF58CC02), Color(0xFF4CAF50))
                            )
                        )
                )
            }
        }
    }
}

@Composable
private fun DayCircle(day: DayActivityDisplay) {
    val colors = LocalGameColors.current
    val bgColor = when {
        day.goalMet -> Color(0xFF58CC02)
        day.wasActive -> Color(0xFFFF9600)
        day.isToday -> Color(0xFF1CB0F6).copy(alpha = 0.3f)
        else -> colors.textSecondary.copy(alpha = 0.1f)
    }
    val borderColor = when {
        day.isToday -> Color(0xFF1CB0F6)
        day.goalMet -> Color(0xFF4CAF50)
        else -> Color.Transparent
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(bgColor)
                .then(
                    if (borderColor != Color.Transparent)
                        Modifier.border(2.dp, borderColor, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (day.goalMet) {
                Text(text = "✓", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
            } else if (day.wasActive) {
                Text(text = "·", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = day.dayName,
            fontSize = 11.sp,
            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
            color = if (day.isToday) colors.textPrimary else colors.textSecondary
        )
    }
}

@Composable
private fun TopThreePodium(topThree: List<LeaderboardEntry>) {
    val colors = LocalGameColors.current
    if (topThree.size < 3) return

    val medalColors = listOf(
        Color(0xFFFFD700), // Gold
        Color(0xFFC0C0C0), // Silver
        Color(0xFFCD7F32)  // Bronze
    )
    val medalEmojis = listOf("🥇", "🥈", "🥉")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        // 2nd place (left, shorter)
        PodiumItem(
            entry = topThree[1],
            medalColor = medalColors[1],
            medalEmoji = medalEmojis[1],
            height = 100.dp
        )

        // 1st place (center, tallest)
        PodiumItem(
            entry = topThree[0],
            medalColor = medalColors[0],
            medalEmoji = medalEmojis[0],
            height = 130.dp
        )

        // 3rd place (right, shortest)
        PodiumItem(
            entry = topThree[2],
            medalColor = medalColors[2],
            medalEmoji = medalEmojis[2],
            height = 80.dp
        )
    }
}

@Composable
private fun PodiumItem(
    entry: LeaderboardEntry,
    medalColor: Color,
    medalEmoji: String,
    height: androidx.compose.ui.unit.Dp
) {
    val colors = LocalGameColors.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(100.dp)
    ) {
        // Medal
        Text(text = medalEmoji, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(4.dp))

        // Name
        Text(
            text = entry.name,
            fontSize = 12.sp,
            fontWeight = if (entry.isCurrentPlayer) FontWeight.ExtraBold else FontWeight.Medium,
            color = if (entry.isCurrentPlayer) Color(0xFF1CB0F6) else colors.textPrimary,
            maxLines = 1,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Podium bar
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(height)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            medalColor.copy(alpha = 0.4f),
                            medalColor.copy(alpha = 0.15f)
                        )
                    )
                )
                .border(
                    1.dp,
                    medalColor.copy(alpha = 0.3f),
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                LightningIcon(size = 20.dp)
                Text(
                    text = "${entry.xp}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun LeaderboardRow(entry: LeaderboardEntry) {
    val colors = LocalGameColors.current
    val isPlayer = entry.isCurrentPlayer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPlayer) Color(0xFF1CB0F6).copy(alpha = 0.1f) else colors.cardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isPlayer) Modifier.border(1.dp, Color(0xFF1CB0F6).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    else Modifier
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Text(
                text = "#${entry.rank}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPlayer) Color(0xFF1CB0F6) else colors.textSecondary,
                modifier = Modifier.width(36.dp)
            )

            // Name
            Text(
                text = entry.name,
                fontSize = 15.sp,
                fontWeight = if (isPlayer) FontWeight.Bold else FontWeight.Normal,
                color = if (isPlayer) Color(0xFF1CB0F6) else colors.textPrimary,
                modifier = Modifier.weight(1f)
            )

            // XP (primary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LightningIcon(size = 16.dp)
                Text(
                    text = "${entry.xp}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF58CC02)
                )
            }

            // Diamonds (secondary)
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DiamondIcon(size = 13.dp)
                Text(
                    text = "${entry.diamonds}",
                    fontSize = 13.sp,
                    color = Color(0xFF1CB0F6).copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Weekly Activity Line Graph — Screen Time style
 * Shows minutes of activity per day for the last 7 days
 */
@Composable
private fun WeeklyActivityLineGraph(
    data: List<DayActivityMinutes>,
    totalMinutes: Int
) {
    val colors = LocalGameColors.current
    val accentBlue = Color(0xFF1CB0F6)
    val accentGreen = Color(0xFF58CC02)
    
    val hours = totalMinutes / 60
    val mins = totalMinutes % 60
    val totalLabel = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Daily Activity",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "This week",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = totalLabel,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentBlue
                    )
                    Text(
                        text = "total",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
            
            // Line graph
            val maxMinutes = (data.maxOfOrNull { it.minutes } ?: 1).coerceAtLeast(1)
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val w = size.width
                    val h = size.height
                    val paddingBottom = 24f
                    val graphH = h - paddingBottom
                    val stepX = w / (data.size - 1).coerceAtLeast(1)
                    
                    // Grid lines (3 horizontal dashed lines)
                    val gridColor = colors.textSecondary.copy(alpha = 0.15f)
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    for (i in 1..3) {
                        val y = graphH - (graphH * i / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = dashEffect
                        )
                    }
                    
                    if (data.isEmpty()) return@Canvas
                    
                    // Build points
                    val points = data.mapIndexed { index, day ->
                        val x = index * stepX
                        val yRatio = day.minutes.toFloat() / maxMinutes
                        val y = graphH - (yRatio * graphH * 0.9f)
                        Offset(x, y)
                    }
                    
                    // Fill area under curve
                    val fillPath = Path().apply {
                        moveTo(points.first().x, graphH)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, graphH)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                accentBlue.copy(alpha = 0.25f),
                                accentBlue.copy(alpha = 0.05f)
                            )
                        )
                    )
                    
                    // Draw line
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val cpx = (prev.x + curr.x) / 2f
                            cubicTo(cpx, prev.y, cpx, curr.y, curr.x, curr.y)
                        }
                    }
                    drawPath(
                        path = linePath,
                        color = accentBlue,
                        style = Stroke(
                            width = 3.5f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                    
                    // Draw dots at each point
                    points.forEachIndexed { index, point ->
                        val isToday = index == points.size - 1
                        drawCircle(
                            color = if (isToday) accentGreen else accentBlue,
                            radius = if (isToday) 7f else 5f,
                            center = point
                        )
                        drawCircle(
                            color = colors.cardBackground,
                            radius = if (isToday) 4f else 3f,
                            center = point
                        )
                    }
                }
            }
            
            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach { day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = day.dayLabel,
                            fontSize = 11.sp,
                            fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (day.isToday) accentBlue else colors.textSecondary
                        )
                        val dayH = day.minutes / 60
                        val dayM = day.minutes % 60
                        val dayLabel = when {
                            day.minutes == 0 -> "-"
                            dayH > 0 -> "${dayH}h"
                            else -> "${dayM}m"
                        }
                        Text(
                            text = dayLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (day.isToday) accentGreen else colors.textSecondary.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
