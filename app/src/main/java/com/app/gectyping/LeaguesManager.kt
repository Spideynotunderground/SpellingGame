package com.app.gectyping

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.random.Random

/**
 * ============================================================================
 * LEAGUES SYSTEM — Duolingo-style weekly competitions
 * 
 * Features:
 * - 10 league tiers (Bronze → Diamond → Obsidian)
 * - Weekly XP competition
 * - Promotion/demotion zones
 * - AI competitors for single-player experience
 * - Rewards for top positions
 * ============================================================================
 */

enum class LeagueTier(
    val displayName: String,
    val emoji: String,
    val color: Long,
    val minXPToPromote: Int,
    val promotionSlots: Int, // Top N players get promoted
    val demotionSlots: Int,  // Bottom N players get demoted
    val weeklyRewardCoins: Int,
    val weeklyRewardGems: Int
) {
    BRONZE("Bronze", "🥉", 0xFFCD7F32, 100, 10, 0, 10, 1),
    SILVER("Silver", "🥈", 0xFFC0C0C0, 200, 10, 5, 25, 2),
    GOLD("Gold", "🥇", 0xFFFFD700, 350, 10, 5, 50, 5),
    SAPPHIRE("Sapphire", "💎", 0xFF0F52BA, 500, 7, 5, 75, 8),
    RUBY("Ruby", "❤️", 0xFFE0115F, 700, 7, 5, 100, 10),
    EMERALD("Emerald", "💚", 0xFF50C878, 900, 5, 5, 150, 15),
    AMETHYST("Amethyst", "💜", 0xFF9966CC, 1200, 5, 7, 200, 20),
    PEARL("Pearl", "🤍", 0xFFFDEEF4, 1500, 3, 7, 300, 30),
    OBSIDIAN("Obsidian", "🖤", 0xFF3D3D3D, 2000, 3, 10, 500, 50),
    DIAMOND("Diamond", "💠", 0xFFB9F2FF, 0, 0, 10, 1000, 100)
}

data class LeaguePlayer(
    val id: String,
    val name: String,
    val avatarEmoji: String,
    val weeklyXP: Int,
    val isCurrentUser: Boolean = false,
    val isAI: Boolean = true
)

data class LeagueState(
    val currentTier: LeagueTier = LeagueTier.BRONZE,
    val weeklyXP: Int = 0,
    val currentPosition: Int = 1,
    val totalPlayers: Int = 30,
    val weekStartTime: Long = 0,
    val weekEndTime: Long = 0,
    val hasClaimedWeeklyReward: Boolean = false,
    val lastWeekPosition: Int = 0,
    val wasPromoted: Boolean = false,
    val wasDemoted: Boolean = false
)

// AI player name pools
private val AI_FIRST_NAMES = listOf(
    "Alex", "Jordan", "Taylor", "Morgan", "Casey", "Riley", "Quinn", "Avery",
    "Blake", "Cameron", "Dakota", "Emery", "Finley", "Harper", "Jamie", "Kendall",
    "Logan", "Madison", "Noah", "Olivia", "Parker", "Reese", "Sage", "Sydney",
    "Emma", "Liam", "Sophia", "Mason", "Isabella", "Lucas", "Mia", "Ethan",
    "Charlotte", "Aiden", "Amelia", "Oliver", "Ava", "Elijah", "Luna", "James"
)

private val AI_AVATARS = listOf(
    "🦊", "🐼", "🦁", "🐯", "🐨", "🐸", "🦉", "🦋", "🐙", "🦄",
    "🐶", "🐱", "🐰", "🐻", "🐵", "🦈", "🦅", "🐢", "🦩", "🐝"
)

object LeaguesManager {
    private const val PREFS = "leagues_system"
    
    private const val KEY_CURRENT_TIER = "current_tier"
    private const val KEY_WEEKLY_XP = "weekly_xp"
    private const val KEY_WEEK_START = "week_start"
    private const val KEY_WEEK_END = "week_end"
    private const val KEY_AI_PLAYERS = "ai_players"
    private const val KEY_CLAIMED_REWARD = "claimed_weekly_reward"
    private const val KEY_LAST_POSITION = "last_week_position"
    private const val KEY_WAS_PROMOTED = "was_promoted"
    private const val KEY_WAS_DEMOTED = "was_demoted"
    
    private const val PLAYERS_PER_LEAGUE = 30
    private const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L
    
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    
    /**
     * Get current league state
     */
    fun getLeagueState(ctx: Context): LeagueState {
        val p = prefs(ctx)
        checkAndResetWeek(ctx)
        
        val tierName = p.getString(KEY_CURRENT_TIER, LeagueTier.BRONZE.name) ?: LeagueTier.BRONZE.name
        val tier = LeagueTier.valueOf(tierName)
        val weeklyXP = p.getInt(KEY_WEEKLY_XP, 0)
        val players = getLeaderboard(ctx)
        val position = players.indexOfFirst { it.isCurrentUser } + 1
        
        return LeagueState(
            currentTier = tier,
            weeklyXP = weeklyXP,
            currentPosition = if (position > 0) position else 1,
            totalPlayers = players.size,
            weekStartTime = p.getLong(KEY_WEEK_START, 0),
            weekEndTime = p.getLong(KEY_WEEK_END, 0),
            hasClaimedWeeklyReward = p.getBoolean(KEY_CLAIMED_REWARD, false),
            lastWeekPosition = p.getInt(KEY_LAST_POSITION, 0),
            wasPromoted = p.getBoolean(KEY_WAS_PROMOTED, false),
            wasDemoted = p.getBoolean(KEY_WAS_DEMOTED, false)
        )
    }
    
    /**
     * Add XP to weekly total
     */
    fun addWeeklyXP(ctx: Context, amount: Int) {
        val p = prefs(ctx)
        checkAndResetWeek(ctx)
        
        val current = p.getInt(KEY_WEEKLY_XP, 0)
        p.edit().putInt(KEY_WEEKLY_XP, current + amount).apply()
        
        // Update AI players to simulate competition
        simulateAIProgress(ctx)
    }
    
    /**
     * Get leaderboard sorted by XP
     */
    fun getLeaderboard(ctx: Context): List<LeaguePlayer> {
        val p = prefs(ctx)
        checkAndResetWeek(ctx)
        
        val userXP = p.getInt(KEY_WEEKLY_XP, 0)
        val userName = ctx.getSharedPreferences("spelling_game", Context.MODE_PRIVATE)
            .getString("player_name", "You") ?: "You"
        val userAvatar = ctx.getSharedPreferences("spelling_game", Context.MODE_PRIVATE)
            .getString("selected_avatar", "space") ?: "space"
        val userAvatarEmoji = try {
            AVATARS.find { it.id == userAvatar }?.emoji ?: "🚀"
        } catch (e: Exception) {
            "🚀"
        }
        
        val aiPlayers = loadAIPlayers(ctx)
        
        val currentUser = LeaguePlayer(
            id = "current_user",
            name = userName,
            avatarEmoji = userAvatarEmoji,
            weeklyXP = userXP,
            isCurrentUser = true,
            isAI = false
        )
        
        return (aiPlayers + currentUser)
            .sortedByDescending { it.weeklyXP }
    }
    
    /**
     * Get time remaining in current week
     */
    fun getTimeRemaining(ctx: Context): Long {
        val p = prefs(ctx)
        val weekEnd = p.getLong(KEY_WEEK_END, 0)
        val remaining = weekEnd - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }
    
    /**
     * Format time remaining as string
     */
    fun formatTimeRemaining(ctx: Context): String {
        val remaining = getTimeRemaining(ctx)
        if (remaining <= 0) return "Week ended"
        
        val days = remaining / (24 * 60 * 60 * 1000)
        val hours = (remaining % (24 * 60 * 60 * 1000)) / (60 * 60 * 1000)
        val minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000)
        
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }
    
    /**
     * Claim weekly reward
     */
    fun claimWeeklyReward(ctx: Context): Pair<Int, Int>? {
        val p = prefs(ctx)
        if (p.getBoolean(KEY_CLAIMED_REWARD, false)) return null
        
        val state = getLeagueState(ctx)
        val tier = state.currentTier
        
        // Bonus for top positions
        val positionMultiplier = when (state.currentPosition) {
            1 -> 3.0f
            2 -> 2.0f
            3 -> 1.5f
            in 4..10 -> 1.2f
            else -> 1.0f
        }
        
        val coins = (tier.weeklyRewardCoins * positionMultiplier).toInt()
        val gems = (tier.weeklyRewardGems * positionMultiplier).toInt()
        
        p.edit().putBoolean(KEY_CLAIMED_REWARD, true).apply()
        
        return coins to gems
    }
    
    /**
     * Check if promotion/demotion zone
     */
    fun getZone(ctx: Context): LeagueZone {
        val state = getLeagueState(ctx)
        val tier = state.currentTier
        
        return when {
            state.currentPosition <= tier.promotionSlots && tier != LeagueTier.DIAMOND -> LeagueZone.PROMOTION
            state.currentPosition > state.totalPlayers - tier.demotionSlots && tier != LeagueTier.BRONZE -> LeagueZone.DEMOTION
            else -> LeagueZone.SAFE
        }
    }
    
    // ========== Internal ==========
    
    private fun checkAndResetWeek(ctx: Context) {
        val p = prefs(ctx)
        val weekEnd = p.getLong(KEY_WEEK_END, 0)
        val now = System.currentTimeMillis()
        
        if (weekEnd == 0L || now > weekEnd) {
            // Week ended - process results and start new week
            processWeekEnd(ctx)
            startNewWeek(ctx)
        }
    }
    
    private fun processWeekEnd(ctx: Context) {
        val p = prefs(ctx)
        val state = getLeagueState(ctx)
        
        if (state.weekStartTime == 0L) return // First time
        
        val position = state.currentPosition
        val tier = state.currentTier
        val tiers = LeagueTier.entries
        val currentIndex = tiers.indexOf(tier)
        
        var newTier = tier
        var promoted = false
        var demoted = false
        
        // Check promotion
        if (position <= tier.promotionSlots && currentIndex < tiers.size - 1) {
            newTier = tiers[currentIndex + 1]
            promoted = true
        }
        // Check demotion
        else if (position > state.totalPlayers - tier.demotionSlots && currentIndex > 0) {
            newTier = tiers[currentIndex - 1]
            demoted = true
        }
        
        p.edit()
            .putString(KEY_CURRENT_TIER, newTier.name)
            .putInt(KEY_LAST_POSITION, position)
            .putBoolean(KEY_WAS_PROMOTED, promoted)
            .putBoolean(KEY_WAS_DEMOTED, demoted)
            .apply()
    }
    
    private fun startNewWeek(ctx: Context) {
        val p = prefs(ctx)
        val now = System.currentTimeMillis()
        
        // Calculate week end (next Sunday midnight)
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        cal.add(Calendar.WEEK_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val weekEnd = cal.timeInMillis
        
        p.edit()
            .putInt(KEY_WEEKLY_XP, 0)
            .putLong(KEY_WEEK_START, now)
            .putLong(KEY_WEEK_END, weekEnd)
            .putBoolean(KEY_CLAIMED_REWARD, false)
            .apply()
        
        // Generate new AI players
        generateAIPlayers(ctx)
    }
    
    private fun generateAIPlayers(ctx: Context) {
        val p = prefs(ctx)
        val tier = LeagueTier.valueOf(
            p.getString(KEY_CURRENT_TIER, LeagueTier.BRONZE.name) ?: LeagueTier.BRONZE.name
        )
        
        val players = mutableListOf<LeaguePlayer>()
        val usedNames = mutableSetOf<String>()
        
        repeat(PLAYERS_PER_LEAGUE - 1) { index ->
            var name: String
            do {
                name = AI_FIRST_NAMES.random()
            } while (name in usedNames)
            usedNames.add(name)
            
            // XP based on tier difficulty and random variance
            val baseXP = tier.minXPToPromote / 2
            val variance = (baseXP * 0.8f).toInt()
            val xp = (baseXP + Random.nextInt(-variance, variance)).coerceAtLeast(0)
            
            players.add(LeaguePlayer(
                id = "ai_$index",
                name = name,
                avatarEmoji = AI_AVATARS.random(),
                weeklyXP = xp,
                isCurrentUser = false,
                isAI = true
            ))
        }
        
        saveAIPlayers(ctx, players)
    }
    
    private fun simulateAIProgress(ctx: Context) {
        val p = prefs(ctx)
        val players = loadAIPlayers(ctx).toMutableList()
        
        // Random AI players gain XP
        val activeCount = Random.nextInt(1, 5)
        repeat(activeCount) {
            if (players.isNotEmpty()) {
                val index = Random.nextInt(players.size)
                val player = players[index]
                val xpGain = Random.nextInt(5, 25)
                players[index] = player.copy(weeklyXP = player.weeklyXP + xpGain)
            }
        }
        
        saveAIPlayers(ctx, players)
    }
    
    private fun loadAIPlayers(ctx: Context): List<LeaguePlayer> {
        val p = prefs(ctx)
        val json = p.getString(KEY_AI_PLAYERS, null) ?: return emptyList()
        
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                LeaguePlayer(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    avatarEmoji = obj.getString("avatar"),
                    weeklyXP = obj.getInt("xp"),
                    isCurrentUser = false,
                    isAI = true
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveAIPlayers(ctx: Context, players: List<LeaguePlayer>) {
        val arr = JSONArray()
        players.forEach { player ->
            arr.put(JSONObject().apply {
                put("id", player.id)
                put("name", player.name)
                put("avatar", player.avatarEmoji)
                put("xp", player.weeklyXP)
            })
        }
        prefs(ctx).edit().putString(KEY_AI_PLAYERS, arr.toString()).apply()
    }
}

enum class LeagueZone {
    PROMOTION,
    SAFE,
    DEMOTION
}
