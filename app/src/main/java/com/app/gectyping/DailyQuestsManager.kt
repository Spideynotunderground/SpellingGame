package com.app.gectyping

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.random.Random

/**
 * ============================================================================
 * DAILY QUESTS SYSTEM — Duolingo-style daily challenges
 * 
 * Features:
 * - 3 daily quests refreshed at midnight
 * - Various quest types (XP, words, streaks, etc.)
 * - Bonus chest for completing all quests
 * - Quest difficulty scales with player level
 * ============================================================================
 */

enum class QuestType(val emoji: String, val description: String) {
    EARN_XP("⚡", "Earn %d XP"),
    CORRECT_WORDS("✅", "Get %d words correct"),
    PERFECT_LESSONS("🎯", "Complete %d perfect lessons"),
    PRACTICE_MINUTES("⏱️", "Practice for %d minutes"),
    LEARN_NEW_WORDS("📚", "Learn %d new words"),
    COMBO_STREAK("🔥", "Reach a %dx combo"),
    NO_MISTAKES("💎", "Complete a lesson with no mistakes"),
    USE_HINT("💡", "Complete without using hints"),
    COMPLETE_LESSONS("📖", "Complete %d lessons"),
    EARN_DIAMONDS("💎", "Earn %d diamonds"),
    DAILY_LOGIN("📅", "Open the app today"),
    SPEED_ROUND("⚡", "Answer %d words in under 3 seconds")
}

data class DailyQuest(
    val id: String,
    val type: QuestType,
    val targetValue: Int,
    val currentProgress: Int = 0,
    val rewardDiamonds: Int,
    val rewardXP: Int,
    val isCompleted: Boolean = false,
    val isClaimed: Boolean = false
)

data class DailyQuestsState(
    val quests: List<DailyQuest>,
    val allCompleted: Boolean,
    val bonusChestClaimed: Boolean,
    val lastRefreshDate: String,
    val timeUntilRefresh: Long
)

object DailyQuestsManager {
    private const val PREFS = "daily_quests"
    
    private const val KEY_QUESTS = "quests"
    private const val KEY_LAST_REFRESH = "last_refresh_date"
    private const val KEY_BONUS_CLAIMED = "bonus_chest_claimed"
    
    // Progress keys
    private const val KEY_TODAY_XP = "today_xp"
    private const val KEY_TODAY_CORRECT = "today_correct"
    private const val KEY_TODAY_PERFECT = "today_perfect"
    private const val KEY_TODAY_MINUTES = "today_minutes"
    private const val KEY_TODAY_NEW_WORDS = "today_new_words"
    private const val KEY_TODAY_MAX_COMBO = "today_max_combo"
    private const val KEY_TODAY_NO_MISTAKES = "today_no_mistakes"
    private const val KEY_TODAY_NO_HINTS = "today_no_hints"
    private const val KEY_TODAY_LESSONS = "today_lessons"
    private const val KEY_TODAY_DIAMONDS = "today_diamonds_earned"
    private const val KEY_TODAY_LOGIN = "today_login"
    private const val KEY_TODAY_SPEED = "today_speed_answers"
    
    private const val BONUS_CHEST_DIAMONDS = 150
    private const val BONUS_CHEST_XP = 75
    
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    
    /**
     * Get current daily quests state
     */
    fun getQuestsState(ctx: Context): DailyQuestsState {
        checkAndRefreshQuests(ctx)
        
        val p = prefs(ctx)
        val quests = loadQuests(ctx)
        val allCompleted = quests.all { it.isCompleted }
        val bonusClaimed = p.getBoolean(KEY_BONUS_CLAIMED, false)
        val lastRefresh = p.getString(KEY_LAST_REFRESH, "") ?: ""
        
        return DailyQuestsState(
            quests = quests,
            allCompleted = allCompleted,
            bonusChestClaimed = bonusClaimed,
            lastRefreshDate = lastRefresh,
            timeUntilRefresh = getTimeUntilMidnight()
        )
    }
    
    /**
     * Update progress for a quest type
     */
    fun updateProgress(ctx: Context, type: QuestType, amount: Int = 1) {
        val p = prefs(ctx)
        checkAndRefreshQuests(ctx)
        
        val key = when (type) {
            QuestType.EARN_XP -> KEY_TODAY_XP
            QuestType.CORRECT_WORDS -> KEY_TODAY_CORRECT
            QuestType.PERFECT_LESSONS -> KEY_TODAY_PERFECT
            QuestType.PRACTICE_MINUTES -> KEY_TODAY_MINUTES
            QuestType.LEARN_NEW_WORDS -> KEY_TODAY_NEW_WORDS
            QuestType.COMBO_STREAK -> KEY_TODAY_MAX_COMBO
            QuestType.NO_MISTAKES -> KEY_TODAY_NO_MISTAKES
            QuestType.USE_HINT -> KEY_TODAY_NO_HINTS
            QuestType.COMPLETE_LESSONS -> KEY_TODAY_LESSONS
            QuestType.EARN_DIAMONDS -> KEY_TODAY_DIAMONDS
            QuestType.DAILY_LOGIN -> KEY_TODAY_LOGIN
            QuestType.SPEED_ROUND -> KEY_TODAY_SPEED
        }
        
        val current = p.getInt(key, 0)
        val newValue = if (type == QuestType.COMBO_STREAK) {
            maxOf(current, amount) // For combo, track max
        } else {
            current + amount
        }
        
        p.edit().putInt(key, newValue).apply()
        
        // Update quest completion status
        updateQuestCompletion(ctx)
    }
    
    /**
     * Claim a completed quest reward
     */
    fun claimQuest(ctx: Context, questId: String): Pair<Int, Int>? {
        val quests = loadQuests(ctx).toMutableList()
        val index = quests.indexOfFirst { it.id == questId }
        
        if (index == -1) return null
        
        val quest = quests[index]
        if (!quest.isCompleted || quest.isClaimed) return null
        
        quests[index] = quest.copy(isClaimed = true)
        saveQuests(ctx, quests)
        
        return quest.rewardDiamonds to quest.rewardXP
    }
    
    /**
     * Claim bonus chest for completing all quests
     */
    fun claimBonusChest(ctx: Context): Pair<Int, Int>? {
        val p = prefs(ctx)
        val state = getQuestsState(ctx)
        
        if (!state.allCompleted || state.bonusChestClaimed) return null
        
        p.edit().putBoolean(KEY_BONUS_CLAIMED, true).apply()
        
        return BONUS_CHEST_DIAMONDS to BONUS_CHEST_XP
    }
    
    /**
     * Format time until refresh
     */
    fun formatTimeUntilRefresh(ctx: Context): String {
        val remaining = getTimeUntilMidnight()
        
        val hours = remaining / (60 * 60 * 1000)
        val minutes = (remaining % (60 * 60 * 1000)) / (60 * 1000)
        
        return "${hours}h ${minutes}m"
    }
    
    // ========== Internal ==========
    
    private fun checkAndRefreshQuests(ctx: Context) {
        val p = prefs(ctx)
        val today = getTodayString()
        val lastRefresh = p.getString(KEY_LAST_REFRESH, "") ?: ""
        
        if (lastRefresh != today) {
            generateNewQuests(ctx)
            resetDailyProgress(ctx)
            p.edit()
                .putString(KEY_LAST_REFRESH, today)
                .putBoolean(KEY_BONUS_CLAIMED, false)
                .apply()
        }
    }
    
    private fun generateNewQuests(ctx: Context) {
        // Always include DAILY_LOGIN + EARN_XP, then pick 3 more random
        val guaranteed = listOf(QuestType.DAILY_LOGIN, QuestType.EARN_XP)
        val remaining = QuestType.entries.filter { it !in guaranteed }.shuffled().take(3)
        val questTypes = guaranteed + remaining
        
        val quests = questTypes.mapIndexed { index, type ->
            val (target, diamonds, xp) = getQuestParams(type, index)
            
            DailyQuest(
                id = "quest_${System.currentTimeMillis()}_$index",
                type = type,
                targetValue = target,
                currentProgress = 0,
                rewardDiamonds = diamonds,
                rewardXP = xp,
                isCompleted = false,
                isClaimed = false
            )
        }
        
        saveQuests(ctx, quests)
    }
    
    private fun getQuestParams(type: QuestType, difficulty: Int): Triple<Int, Int, Int> {
        val multiplier = 1 + difficulty * 0.3f
        
        return when (type) {
            QuestType.EARN_XP -> Triple(
                (50 * multiplier).toInt(),
                (15 * multiplier).toInt(),
                (10 * multiplier).toInt()
            )
            QuestType.CORRECT_WORDS -> Triple(
                (10 * multiplier).toInt(),
                (20 * multiplier).toInt(),
                (15 * multiplier).toInt()
            )
            QuestType.PERFECT_LESSONS -> Triple(
                (1 * multiplier).toInt().coerceAtLeast(1),
                (30 * multiplier).toInt(),
                (20 * multiplier).toInt()
            )
            QuestType.PRACTICE_MINUTES -> Triple(
                (5 * multiplier).toInt(),
                (15 * multiplier).toInt(),
                (10 * multiplier).toInt()
            )
            QuestType.LEARN_NEW_WORDS -> Triple(
                (5 * multiplier).toInt(),
                (25 * multiplier).toInt(),
                (15 * multiplier).toInt()
            )
            QuestType.COMBO_STREAK -> Triple(
                (5 * multiplier).toInt(),
                (20 * multiplier).toInt(),
                (15 * multiplier).toInt()
            )
            QuestType.NO_MISTAKES -> Triple(
                1,
                (40 * multiplier).toInt(),
                (25 * multiplier).toInt()
            )
            QuestType.USE_HINT -> Triple(
                1,
                (35 * multiplier).toInt(),
                (20 * multiplier).toInt()
            )
            QuestType.COMPLETE_LESSONS -> Triple(
                (2 * multiplier).toInt().coerceAtLeast(1),
                (20 * multiplier).toInt(),
                (15 * multiplier).toInt()
            )
            QuestType.EARN_DIAMONDS -> Triple(
                (20 * multiplier).toInt(),
                (10 * multiplier).toInt(),
                (15 * multiplier).toInt()
            )
            QuestType.DAILY_LOGIN -> Triple(
                1,
                10,
                5
            )
            QuestType.SPEED_ROUND -> Triple(
                (3 * multiplier).toInt(),
                (25 * multiplier).toInt(),
                (20 * multiplier).toInt()
            )
        }
    }
    
    private fun updateQuestCompletion(ctx: Context) {
        val p = prefs(ctx)
        val quests = loadQuests(ctx).toMutableList()
        
        quests.forEachIndexed { index, quest ->
            val progress = when (quest.type) {
                QuestType.EARN_XP -> p.getInt(KEY_TODAY_XP, 0)
                QuestType.CORRECT_WORDS -> p.getInt(KEY_TODAY_CORRECT, 0)
                QuestType.PERFECT_LESSONS -> p.getInt(KEY_TODAY_PERFECT, 0)
                QuestType.PRACTICE_MINUTES -> p.getInt(KEY_TODAY_MINUTES, 0)
                QuestType.LEARN_NEW_WORDS -> p.getInt(KEY_TODAY_NEW_WORDS, 0)
                QuestType.COMBO_STREAK -> p.getInt(KEY_TODAY_MAX_COMBO, 0)
                QuestType.NO_MISTAKES -> p.getInt(KEY_TODAY_NO_MISTAKES, 0)
                QuestType.USE_HINT -> p.getInt(KEY_TODAY_NO_HINTS, 0)
                QuestType.COMPLETE_LESSONS -> p.getInt(KEY_TODAY_LESSONS, 0)
                QuestType.EARN_DIAMONDS -> p.getInt(KEY_TODAY_DIAMONDS, 0)
                QuestType.DAILY_LOGIN -> p.getInt(KEY_TODAY_LOGIN, 0)
                QuestType.SPEED_ROUND -> p.getInt(KEY_TODAY_SPEED, 0)
            }
            
            val isCompleted = progress >= quest.targetValue
            quests[index] = quest.copy(
                currentProgress = progress,
                isCompleted = isCompleted
            )
        }
        
        saveQuests(ctx, quests)
    }
    
    private fun resetDailyProgress(ctx: Context) {
        prefs(ctx).edit()
            .putInt(KEY_TODAY_XP, 0)
            .putInt(KEY_TODAY_CORRECT, 0)
            .putInt(KEY_TODAY_PERFECT, 0)
            .putInt(KEY_TODAY_MINUTES, 0)
            .putInt(KEY_TODAY_NEW_WORDS, 0)
            .putInt(KEY_TODAY_MAX_COMBO, 0)
            .putInt(KEY_TODAY_NO_MISTAKES, 0)
            .putInt(KEY_TODAY_NO_HINTS, 0)
            .putInt(KEY_TODAY_LESSONS, 0)
            .putInt(KEY_TODAY_DIAMONDS, 0)
            .putInt(KEY_TODAY_LOGIN, 0)
            .putInt(KEY_TODAY_SPEED, 0)
            .apply()
    }
    
    private fun loadQuests(ctx: Context): List<DailyQuest> {
        val p = prefs(ctx)
        val json = p.getString(KEY_QUESTS, null) ?: return emptyList()
        
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                DailyQuest(
                    id = obj.getString("id"),
                    type = QuestType.valueOf(obj.getString("type")),
                    targetValue = obj.getInt("target"),
                    currentProgress = obj.getInt("progress"),
                    rewardDiamonds = obj.optInt("diamonds", 0),
                    rewardXP = obj.getInt("xp"),
                    isCompleted = obj.getBoolean("completed"),
                    isClaimed = obj.getBoolean("claimed")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveQuests(ctx: Context, quests: List<DailyQuest>) {
        val arr = JSONArray()
        quests.forEach { quest ->
            arr.put(JSONObject().apply {
                put("id", quest.id)
                put("type", quest.type.name)
                put("target", quest.targetValue)
                put("progress", quest.currentProgress)
                put("diamonds", quest.rewardDiamonds)
                put("xp", quest.rewardXP)
                put("completed", quest.isCompleted)
                put("claimed", quest.isClaimed)
            })
        }
        prefs(ctx).edit().putString(KEY_QUESTS, arr.toString()).apply()
    }
    
    private fun getTodayString(): String {
        val cal = Calendar.getInstance()
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }
    
    private fun getTimeUntilMidnight(): Long {
        val now = Calendar.getInstance()
        val midnight = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return midnight.timeInMillis - now.timeInMillis
    }
}
