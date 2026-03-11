package com.app.gectyping

import android.content.Context
import android.content.SharedPreferences

/**
 * ============================================================================
 * ACHIEVEMENT SYSTEM — 15 goals across 4 categories
 * ============================================================================
 */

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val rewardCoins: Int,
    val targetValue: Int,
    val iconEmoji: String = "🏆"
)

val ALL_ACHIEVEMENTS = listOf(
    // --- Словесный мастер (Word count) ---
    Achievement("words_10",   "First Steps",        "Answer 10 words correctly",        "Word Master", 20,   10,  "📖"),
    Achievement("words_50",   "Word Marathon",       "Answer 50 words correctly",        "Word Master", 100,  50,  "📚"),
    Achievement("words_100",  "Language Expert",     "Answer 100 words correctly",       "Word Master", 250,  100, "🎓"),
    Achievement("words_200",  "Phonetic Giant",      "Answer 200 words correctly",       "Word Master", 500,  200, "🗣️"),
    Achievement("words_500",  "Word Legend",         "Conquer 500 words",                "Word Master", 1000, 500, "👑"),

    // --- Серии побед (Streaks) ---
    Achievement("streak_5",   "Warming Up",          "5 correct answers in a row",       "Win Streaks", 30,   5,   "🔥"),
    Achievement("streak_15",  "On Fire!",            "15 correct answers in a row",      "Win Streaks", 100,  15,  "💥"),
    Achievement("perfect_lvl","Perfection",          "Complete a level with no mistakes", "Win Streaks", 200,  1,   "💎"),

    // --- Прогресс по уровням ---
    Achievement("level_1",    "Beginner",            "Complete Level 1",                 "Progress",    50,   1,   "🌱"),
    Achievement("level_3",    "Equator",             "Complete Level 3",                 "Progress",    150,  3,   "🌍"),
    Achievement("level_5",    "Graduation",          "Complete all 5 levels",            "Progress",    500,  5,   "🎉"),

    // --- Активность и кастомизация ---
    Achievement("daily_2",    "Loyal Fan",           "Log in 2 days in a row",           "Activity",    40,   2,   "📅"),
    Achievement("new_avatar", "New Face",            "Buy or upload a new avatar",       "Activity",    20,   1,   "🎭"),
    Achievement("theme_swap", "Change of Scenery",   "Switch the theme",                 "Activity",    10,   1,   "🎨"),
    Achievement("first_buy",  "Shopaholic",          "Make your first purchase",         "Activity",    25,   1,   "🛍️")
)

object AchievementManager {
    private const val PREFS = "achievements"
    private const val KEY_TOTAL_CORRECT = "total_correct_words"
    private const val KEY_MAX_STREAK = "max_streak"
    private const val KEY_PERFECT_LEVELS = "perfect_levels_count"
    private const val KEY_MAX_LEVEL = "max_level_completed"
    private const val KEY_DAILY_STREAK = "daily_streak"
    private const val KEY_LAST_LOGIN_DAY = "last_login_day"
    private const val KEY_AVATAR_CHANGED = "avatar_changed"
    private const val KEY_THEME_SWAPPED = "theme_swapped"
    private const val KEY_FIRST_PURCHASE = "first_purchase"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // --- Progress getters ---
    fun getTotalCorrect(ctx: Context): Int = prefs(ctx).getInt(KEY_TOTAL_CORRECT, 0)
    fun getMaxStreak(ctx: Context): Int = prefs(ctx).getInt(KEY_MAX_STREAK, 0)
    fun getMaxLevelCompleted(ctx: Context): Int = prefs(ctx).getInt(KEY_MAX_LEVEL, 0)
    fun getDailyStreak(ctx: Context): Int = prefs(ctx).getInt(KEY_DAILY_STREAK, 0)
    fun getPerfectLevels(ctx: Context): Int = prefs(ctx).getInt(KEY_PERFECT_LEVELS, 0)

    // --- Progress setters ---
    fun addCorrectWord(ctx: Context) {
        val p = prefs(ctx)
        p.edit().putInt(KEY_TOTAL_CORRECT, p.getInt(KEY_TOTAL_CORRECT, 0) + 1).apply()
    }

    fun updateMaxStreak(ctx: Context, streak: Int) {
        val p = prefs(ctx)
        if (streak > p.getInt(KEY_MAX_STREAK, 0)) {
            p.edit().putInt(KEY_MAX_STREAK, streak).apply()
        }
    }

    fun onLevelCompleted(ctx: Context, level: Int) {
        val p = prefs(ctx)
        if (level > p.getInt(KEY_MAX_LEVEL, 0)) {
            p.edit().putInt(KEY_MAX_LEVEL, level).apply()
        }
    }

    fun onPerfectLevel(ctx: Context) {
        val p = prefs(ctx)
        p.edit().putInt(KEY_PERFECT_LEVELS, p.getInt(KEY_PERFECT_LEVELS, 0) + 1).apply()
    }

    fun onDailyLogin(ctx: Context) {
        val p = prefs(ctx)
        val today = (System.currentTimeMillis() / 86400000L).toInt() // days since epoch
        val lastDay = p.getInt(KEY_LAST_LOGIN_DAY, 0)
        val streak = p.getInt(KEY_DAILY_STREAK, 0)
        if (today == lastDay) return // already logged today
        val newStreak = if (today == lastDay + 1) streak + 1 else 1
        p.edit()
            .putInt(KEY_LAST_LOGIN_DAY, today)
            .putInt(KEY_DAILY_STREAK, newStreak)
            .apply()
    }

    fun onAvatarChanged(ctx: Context) { prefs(ctx).edit().putBoolean(KEY_AVATAR_CHANGED, true).apply() }
    fun onThemeSwapped(ctx: Context) { prefs(ctx).edit().putBoolean(KEY_THEME_SWAPPED, true).apply() }
    fun onFirstPurchase(ctx: Context) { prefs(ctx).edit().putBoolean(KEY_FIRST_PURCHASE, true).apply() }

    // --- Check if an achievement is unlocked ---
    fun isUnlocked(ctx: Context, a: Achievement): Boolean = getProgress(ctx, a) >= a.targetValue

    fun getProgress(ctx: Context, a: Achievement): Int {
        val p = prefs(ctx)
        return when (a.id) {
            "words_10", "words_50", "words_100", "words_200", "words_500" -> p.getInt(KEY_TOTAL_CORRECT, 0)
            "streak_5", "streak_15" -> p.getInt(KEY_MAX_STREAK, 0)
            "perfect_lvl" -> p.getInt(KEY_PERFECT_LEVELS, 0)
            "level_1" -> if (p.getInt(KEY_MAX_LEVEL, 0) >= 1) 1 else 0
            "level_3" -> if (p.getInt(KEY_MAX_LEVEL, 0) >= 3) 3 else p.getInt(KEY_MAX_LEVEL, 0)
            "level_5" -> if (p.getInt(KEY_MAX_LEVEL, 0) >= 5) 5 else p.getInt(KEY_MAX_LEVEL, 0)
            "daily_2" -> p.getInt(KEY_DAILY_STREAK, 0)
            "new_avatar" -> if (p.getBoolean(KEY_AVATAR_CHANGED, false)) 1 else 0
            "theme_swap" -> if (p.getBoolean(KEY_THEME_SWAPPED, false)) 1 else 0
            "first_buy" -> if (p.getBoolean(KEY_FIRST_PURCHASE, false)) 1 else 0
            else -> 0
        }
    }

    // --- Claimed state ---
    fun isClaimed(ctx: Context, id: String): Boolean = prefs(ctx).getBoolean("claimed_$id", false)

    fun claim(ctx: Context, id: String): Int {
        val a = ALL_ACHIEVEMENTS.find { it.id == id } ?: return 0
        if (isClaimed(ctx, id) || !isUnlocked(ctx, a)) return 0
        prefs(ctx).edit().putBoolean("claimed_$id", true).apply()
        return a.rewardCoins
    }

    /** Returns true if at least one achievement is unlocked but not yet claimed */
    fun hasUnclaimedRewards(ctx: Context): Boolean =
        ALL_ACHIEVEMENTS.any { isUnlocked(ctx, it) && !isClaimed(ctx, it.id) }
}
