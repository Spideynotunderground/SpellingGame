package com.app.gectyping

import android.content.Context
import android.content.SharedPreferences
import java.util.Calendar
import java.util.TimeZone

/**
 * ============================================================================
 * STREAK SYSTEM — Duolingo-style daily streak tracking
 * 
 * Features:
 * - Daily streak counter with calendar tracking
 * - Streak Freeze protection (purchasable)
 * - Streak milestones with rewards (7, 14, 30, 60, 100, 365 days)
 * - XP (Experience Points) system
 * - Weekly XP goals
 * ============================================================================
 */

data class StreakMilestone(
    val days: Int,
    val title: String,
    val description: String,
    val rewardCoins: Int,
    val rewardGems: Int,
    val emoji: String
)

val STREAK_MILESTONES = listOf(
    StreakMilestone(7,   "Week Warrior",      "7 days in a row!",       50,   5,   "🔥"),
    StreakMilestone(14,  "Two Week Champion", "14 days streak!",        100,  10,  "⚡"),
    StreakMilestone(30,  "Monthly Master",    "30 days of dedication!", 250,  25,  "🌟"),
    StreakMilestone(60,  "Unstoppable",       "60 days strong!",        500,  50,  "💎"),
    StreakMilestone(100, "Century Legend",    "100 days achieved!",     1000, 100, "👑"),
    StreakMilestone(365, "Year Champion",     "365 days - Incredible!", 5000, 500, "🏆")
)

data class DayActivity(
    val dayOfYear: Int,
    val year: Int,
    val xpEarned: Int,
    val lessonsCompleted: Int,
    val wordsLearned: Int,
    val goalMet: Boolean
)

data class StreakState(
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalXP: Int = 0,
    val todayXP: Int = 0,
    val weeklyXP: Int = 0,
    val dailyGoalXP: Int = 50,
    val weeklyGoalXP: Int = 500,
    val streakFreezes: Int = 0,
    val streakFreezeUsedToday: Boolean = false,
    val lastActiveDay: Int = 0,
    val lastActiveYear: Int = 0,
    val todayGoalMet: Boolean = false,
    val weekStartDay: Int = 0,
    val weekStartYear: Int = 0
)

object StreakManager {
    private const val PREFS = "streak_system"
    
    // Keys
    private const val KEY_CURRENT_STREAK = "current_streak"
    private const val KEY_LONGEST_STREAK = "longest_streak"
    private const val KEY_TOTAL_XP = "total_xp"
    private const val KEY_TODAY_XP = "today_xp"
    private const val KEY_WEEKLY_XP = "weekly_xp"
    private const val KEY_DAILY_GOAL_XP = "daily_goal_xp"
    private const val KEY_WEEKLY_GOAL_XP = "weekly_goal_xp"
    private const val KEY_STREAK_FREEZES = "streak_freezes"
    private const val KEY_FREEZE_USED_TODAY = "freeze_used_today"
    private const val KEY_LAST_ACTIVE_DAY = "last_active_day"
    private const val KEY_LAST_ACTIVE_YEAR = "last_active_year"
    private const val KEY_TODAY_GOAL_MET = "today_goal_met"
    private const val KEY_WEEK_START_DAY = "week_start_day"
    private const val KEY_WEEK_START_YEAR = "week_start_year"
    private const val KEY_MILESTONE_CLAIMED_PREFIX = "milestone_claimed_"
    private const val KEY_ACTIVITY_PREFIX = "activity_"
    
    // Streak Freeze cost
    const val STREAK_FREEZE_COST_GEMS = 10
    const val STREAK_FREEZE_COST_COINS = 200
    
    // XP rewards
    const val XP_PER_CORRECT_WORD = 10
    const val XP_PER_LESSON_COMPLETE = 25
    const val XP_BONUS_PERFECT_LESSON = 15
    const val XP_BONUS_STREAK_MULTIPLIER = 5 // +5 XP per 10 streak days
    
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    
    private fun getCurrentDayOfYear(): Int {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        return cal.get(Calendar.DAY_OF_YEAR)
    }
    
    private fun getCurrentYear(): Int {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        return cal.get(Calendar.YEAR)
    }
    
    private fun getCurrentWeekOfYear(): Int {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        return cal.get(Calendar.WEEK_OF_YEAR)
    }
    
    /**
     * Get current streak state
     */
    fun getStreakState(ctx: Context): StreakState {
        val p = prefs(ctx)
        return StreakState(
            currentStreak = p.getInt(KEY_CURRENT_STREAK, 0),
            longestStreak = p.getInt(KEY_LONGEST_STREAK, 0),
            totalXP = p.getInt(KEY_TOTAL_XP, 0),
            todayXP = p.getInt(KEY_TODAY_XP, 0),
            weeklyXP = p.getInt(KEY_WEEKLY_XP, 0),
            dailyGoalXP = p.getInt(KEY_DAILY_GOAL_XP, 50),
            weeklyGoalXP = p.getInt(KEY_WEEKLY_GOAL_XP, 500),
            streakFreezes = p.getInt(KEY_STREAK_FREEZES, 0),
            streakFreezeUsedToday = p.getBoolean(KEY_FREEZE_USED_TODAY, false),
            lastActiveDay = p.getInt(KEY_LAST_ACTIVE_DAY, 0),
            lastActiveYear = p.getInt(KEY_LAST_ACTIVE_YEAR, 0),
            todayGoalMet = p.getBoolean(KEY_TODAY_GOAL_MET, false),
            weekStartDay = p.getInt(KEY_WEEK_START_DAY, 0),
            weekStartYear = p.getInt(KEY_WEEK_START_YEAR, 0)
        )
    }
    
    /**
     * Called when app opens - checks streak continuity
     */
    fun onAppOpen(ctx: Context): StreakCheckResult {
        val p = prefs(ctx)
        val today = getCurrentDayOfYear()
        val year = getCurrentYear()
        val lastDay = p.getInt(KEY_LAST_ACTIVE_DAY, 0)
        val lastYear = p.getInt(KEY_LAST_ACTIVE_YEAR, 0)
        val currentStreak = p.getInt(KEY_CURRENT_STREAK, 0)
        val freezes = p.getInt(KEY_STREAK_FREEZES, 0)
        
        // Calculate days difference
        val daysDiff = calculateDaysDifference(lastDay, lastYear, today, year)
        
        return when {
            // Same day - no change
            daysDiff == 0 -> {
                StreakCheckResult.SameDay(currentStreak)
            }
            // Next day - streak continues!
            daysDiff == 1 -> {
                resetDailyCounters(ctx)
                StreakCheckResult.StreakContinues(currentStreak)
            }
            // Missed one day but have freeze
            daysDiff == 2 && freezes > 0 -> {
                p.edit()
                    .putInt(KEY_STREAK_FREEZES, freezes - 1)
                    .putBoolean(KEY_FREEZE_USED_TODAY, true)
                    .apply()
                resetDailyCounters(ctx)
                StreakCheckResult.FreezeUsed(currentStreak, freezes - 1)
            }
            // Streak broken
            else -> {
                val lostStreak = currentStreak
                p.edit()
                    .putInt(KEY_CURRENT_STREAK, 0)
                    .putBoolean(KEY_FREEZE_USED_TODAY, false)
                    .apply()
                resetDailyCounters(ctx)
                StreakCheckResult.StreakLost(lostStreak)
            }
        }
    }
    
    private fun calculateDaysDifference(day1: Int, year1: Int, day2: Int, year2: Int): Int {
        if (year1 == 0) return Int.MAX_VALUE // First time
        if (year1 == year2) return day2 - day1
        // Different years
        val daysInYear1 = if (isLeapYear(year1)) 366 else 365
        return (daysInYear1 - day1) + day2 + (year2 - year1 - 1) * 365
    }
    
    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
    
    private fun resetDailyCounters(ctx: Context) {
        val p = prefs(ctx)
        val today = getCurrentDayOfYear()
        val year = getCurrentYear()
        val week = getCurrentWeekOfYear()
        
        val lastWeekStart = p.getInt(KEY_WEEK_START_DAY, 0)
        val lastWeekYear = p.getInt(KEY_WEEK_START_YEAR, 0)
        
        // Check if new week
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_YEAR, lastWeekStart)
        cal.set(Calendar.YEAR, lastWeekYear)
        val lastWeek = if (lastWeekYear > 0) cal.get(Calendar.WEEK_OF_YEAR) else -1
        
        val editor = p.edit()
            .putInt(KEY_TODAY_XP, 0)
            .putBoolean(KEY_TODAY_GOAL_MET, false)
            .putBoolean(KEY_FREEZE_USED_TODAY, false)
        
        // Reset weekly XP if new week
        if (week != lastWeek || year != lastWeekYear) {
            editor.putInt(KEY_WEEKLY_XP, 0)
                .putInt(KEY_WEEK_START_DAY, today)
                .putInt(KEY_WEEK_START_YEAR, year)
        }
        
        editor.apply()
    }
    
    /**
     * Add XP for correct word
     */
    fun addXPForCorrectWord(ctx: Context, streakBonus: Boolean = true): Int {
        val p = prefs(ctx)
        val currentStreak = p.getInt(KEY_CURRENT_STREAK, 0)
        
        var xp = XP_PER_CORRECT_WORD
        if (streakBonus && currentStreak >= 10) {
            xp += (currentStreak / 10) * XP_BONUS_STREAK_MULTIPLIER
        }
        
        addXP(ctx, xp)
        return xp
    }
    
    /**
     * Add XP for completing a lesson
     */
    fun addXPForLessonComplete(ctx: Context, isPerfect: Boolean = false): Int {
        var xp = XP_PER_LESSON_COMPLETE
        if (isPerfect) {
            xp += XP_BONUS_PERFECT_LESSON
        }
        addXP(ctx, xp)
        return xp
    }
    
    /**
     * Add XP and check for daily goal
     */
    fun addXP(ctx: Context, amount: Int) {
        val p = prefs(ctx)
        val today = getCurrentDayOfYear()
        val year = getCurrentYear()
        
        val newTodayXP = p.getInt(KEY_TODAY_XP, 0) + amount
        val newWeeklyXP = p.getInt(KEY_WEEKLY_XP, 0) + amount
        val newTotalXP = p.getInt(KEY_TOTAL_XP, 0) + amount
        val dailyGoal = p.getInt(KEY_DAILY_GOAL_XP, 50)
        val wasGoalMet = p.getBoolean(KEY_TODAY_GOAL_MET, false)
        val goalNowMet = newTodayXP >= dailyGoal
        
        val editor = p.edit()
            .putInt(KEY_TODAY_XP, newTodayXP)
            .putInt(KEY_WEEKLY_XP, newWeeklyXP)
            .putInt(KEY_TOTAL_XP, newTotalXP)
            .putInt(KEY_LAST_ACTIVE_DAY, today)
            .putInt(KEY_LAST_ACTIVE_YEAR, year)
        
        // First time meeting daily goal today - increment streak!
        if (goalNowMet && !wasGoalMet) {
            val currentStreak = p.getInt(KEY_CURRENT_STREAK, 0) + 1
            val longestStreak = p.getInt(KEY_LONGEST_STREAK, 0)
            editor.putInt(KEY_CURRENT_STREAK, currentStreak)
                .putBoolean(KEY_TODAY_GOAL_MET, true)
            if (currentStreak > longestStreak) {
                editor.putInt(KEY_LONGEST_STREAK, currentStreak)
            }
        }
        
        editor.apply()
    }
    
    /**
     * Purchase a Streak Freeze with coins
     */
    fun buyStreakFreezeWithCoins(ctx: Context, currentCoins: Int): BuyResult {
        if (currentCoins < STREAK_FREEZE_COST_COINS) {
            return BuyResult.NotEnoughCurrency
        }
        val p = prefs(ctx)
        val freezes = p.getInt(KEY_STREAK_FREEZES, 0)
        if (freezes >= 2) {
            return BuyResult.MaxOwned
        }
        p.edit().putInt(KEY_STREAK_FREEZES, freezes + 1).apply()
        return BuyResult.Success(STREAK_FREEZE_COST_COINS)
    }
    
    /**
     * Purchase a Streak Freeze with gems (stars)
     */
    fun buyStreakFreezeWithGems(ctx: Context, currentGems: Int): BuyResult {
        if (currentGems < STREAK_FREEZE_COST_GEMS) {
            return BuyResult.NotEnoughCurrency
        }
        val p = prefs(ctx)
        val freezes = p.getInt(KEY_STREAK_FREEZES, 0)
        if (freezes >= 2) {
            return BuyResult.MaxOwned
        }
        p.edit().putInt(KEY_STREAK_FREEZES, freezes + 1).apply()
        return BuyResult.Success(STREAK_FREEZE_COST_GEMS)
    }
    
    /**
     * Get streak freezes count
     */
    fun getStreakFreezes(ctx: Context): Int = prefs(ctx).getInt(KEY_STREAK_FREEZES, 0)
    
    /**
     * Check if milestone is claimed
     */
    fun isMilestoneClaimed(ctx: Context, days: Int): Boolean =
        prefs(ctx).getBoolean("${KEY_MILESTONE_CLAIMED_PREFIX}$days", false)
    
    /**
     * Claim milestone reward
     */
    fun claimMilestone(ctx: Context, milestone: StreakMilestone): ClaimResult {
        val p = prefs(ctx)
        val currentStreak = p.getInt(KEY_CURRENT_STREAK, 0)
        val longestStreak = p.getInt(KEY_LONGEST_STREAK, 0)
        val maxStreak = maxOf(currentStreak, longestStreak)
        
        if (maxStreak < milestone.days) {
            return ClaimResult.NotUnlocked
        }
        if (isMilestoneClaimed(ctx, milestone.days)) {
            return ClaimResult.AlreadyClaimed
        }
        
        p.edit().putBoolean("${KEY_MILESTONE_CLAIMED_PREFIX}${milestone.days}", true).apply()
        return ClaimResult.Success(milestone.rewardCoins, milestone.rewardGems)
    }
    
    /**
     * Get unclaimed milestones
     */
    fun getUnclaimedMilestones(ctx: Context): List<StreakMilestone> {
        val p = prefs(ctx)
        val currentStreak = p.getInt(KEY_CURRENT_STREAK, 0)
        val longestStreak = p.getInt(KEY_LONGEST_STREAK, 0)
        val maxStreak = maxOf(currentStreak, longestStreak)
        
        return STREAK_MILESTONES.filter { milestone ->
            maxStreak >= milestone.days && !isMilestoneClaimed(ctx, milestone.days)
        }
    }
    
    /**
     * Get next milestone to achieve
     */
    fun getNextMilestone(ctx: Context): StreakMilestone? {
        val p = prefs(ctx)
        val currentStreak = p.getInt(KEY_CURRENT_STREAK, 0)
        return STREAK_MILESTONES.firstOrNull { it.days > currentStreak }
    }
    
    /**
     * Get progress to next milestone (0.0 to 1.0)
     */
    fun getProgressToNextMilestone(ctx: Context): Float {
        val p = prefs(ctx)
        val currentStreak = p.getInt(KEY_CURRENT_STREAK, 0)
        val nextMilestone = getNextMilestone(ctx) ?: return 1f
        val prevMilestone = STREAK_MILESTONES.lastOrNull { it.days <= currentStreak }
        val prevDays = prevMilestone?.days ?: 0
        val range = nextMilestone.days - prevDays
        if (range <= 0) return 1f
        return ((currentStreak - prevDays).toFloat() / range).coerceIn(0f, 1f)
    }
    
    /**
     * Set daily XP goal
     */
    fun setDailyGoal(ctx: Context, xp: Int) {
        prefs(ctx).edit().putInt(KEY_DAILY_GOAL_XP, xp.coerceIn(10, 200)).apply()
    }
    
    /**
     * Get last 7 days activity for calendar view
     */
    fun getLast7DaysActivity(ctx: Context): List<DayActivityDisplay> {
        val p = prefs(ctx)
        val today = getCurrentDayOfYear()
        val year = getCurrentYear()
        val dailyGoal = p.getInt(KEY_DAILY_GOAL_XP, 50)
        val todayXP = p.getInt(KEY_TODAY_XP, 0)
        val todayGoalMet = p.getBoolean(KEY_TODAY_GOAL_MET, false)
        val lastActiveDay = p.getInt(KEY_LAST_ACTIVE_DAY, 0)
        val lastActiveYear = p.getInt(KEY_LAST_ACTIVE_YEAR, 0)
        
        // Find Monday of current week
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        
        val result = mutableListOf<DayActivityDisplay>()
        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        
        for (i in 0..6) {
            val dayCal = cal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_YEAR, i)
            val dayOfYear = dayCal.get(Calendar.DAY_OF_YEAR)
            val dayYear = dayCal.get(Calendar.YEAR)
            
            val isToday = dayOfYear == today && dayYear == year
            val isFuture = dayYear > year || (dayYear == year && dayOfYear > today)
            val xp = when {
                isFuture -> 0
                isToday -> todayXP
                else -> p.getInt("${KEY_ACTIVITY_PREFIX}${dayYear}_${dayOfYear}_xp", 0)
            }
            val goalMet = when {
                isFuture -> false
                isToday -> todayGoalMet
                else -> p.getBoolean("${KEY_ACTIVITY_PREFIX}${dayYear}_${dayOfYear}_goal", false)
            }
            val wasActive = when {
                isFuture -> false
                isToday -> lastActiveDay == today && lastActiveYear == year
                else -> p.getBoolean("${KEY_ACTIVITY_PREFIX}${dayYear}_${dayOfYear}_active", false)
            }
            
            result.add(DayActivityDisplay(
                dayName = dayNames[i],
                isToday = isToday,
                goalMet = goalMet,
                xpEarned = xp,
                dailyGoal = dailyGoal,
                wasActive = wasActive || goalMet
            ))
        }
        
        return result
    }
    
    /**
     * Save today's activity when day ends or app closes
     */
    fun saveTodayActivity(ctx: Context) {
        val p = prefs(ctx)
        val today = getCurrentDayOfYear()
        val year = getCurrentYear()
        val todayXP = p.getInt(KEY_TODAY_XP, 0)
        val todayGoalMet = p.getBoolean(KEY_TODAY_GOAL_MET, false)
        
        if (todayXP > 0) {
            p.edit()
                .putInt("${KEY_ACTIVITY_PREFIX}${year}_${today}_xp", todayXP)
                .putBoolean("${KEY_ACTIVITY_PREFIX}${year}_${today}_goal", todayGoalMet)
                .putBoolean("${KEY_ACTIVITY_PREFIX}${year}_${today}_active", true)
                .apply()
        }
    }
    
    // ========== WEEKLY ACTIVITY MINUTES TRACKING ==========
    
    private const val KEY_SESSION_START = "session_start_ms"
    private const val KEY_ACTIVITY_MINUTES_PREFIX = "activity_min_"
    private const val KEY_ACTIVITY_WEEK_RESET = "activity_week_reset"
    private const val KEY_ACTIVITY_WEEK_RESET_YEAR = "activity_week_reset_year"
    
    /**
     * Call when user opens the app / starts a session
     */
    fun startSession(ctx: Context) {
        prefs(ctx).edit().putLong(KEY_SESSION_START, System.currentTimeMillis()).apply()
    }
    
    /**
     * Call when user leaves the app / ends a session. Adds elapsed minutes to today.
     */
    fun endSession(ctx: Context) {
        val p = prefs(ctx)
        val startMs = p.getLong(KEY_SESSION_START, 0L)
        if (startMs <= 0L) return
        
        val elapsedMinutes = ((System.currentTimeMillis() - startMs) / 60_000).toInt().coerceAtLeast(1)
        addActivityMinutes(ctx, elapsedMinutes)
        p.edit().putLong(KEY_SESSION_START, 0L).apply()
    }
    
    /**
     * Add activity minutes to today's total
     */
    fun addActivityMinutes(ctx: Context, minutes: Int) {
        val p = prefs(ctx)
        checkWeeklyActivityReset(ctx)
        val today = getCurrentDayOfYear()
        val year = getCurrentYear()
        val key = "${KEY_ACTIVITY_MINUTES_PREFIX}${year}_${today}"
        val current = p.getInt(key, 0)
        p.edit().putInt(key, current + minutes).apply()
    }
    
    /**
     * Get weekly activity data for line graph (last 7 days, minutes per day)
     */
    fun getWeeklyActivityMinutes(ctx: Context): List<DayActivityMinutes> {
        val p = prefs(ctx)
        checkWeeklyActivityReset(ctx)
        
        val today = getCurrentDayOfYear()
        val year = getCurrentYear()
        
        // Check if there's an active session — add partial minutes for today
        val sessionStart = p.getLong(KEY_SESSION_START, 0L)
        val activeSessionMinutes = if (sessionStart > 0L) {
            ((System.currentTimeMillis() - sessionStart) / 60_000).toInt().coerceAtLeast(0)
        } else 0
        
        // Find Monday of current week
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysFromMonday = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday)
        
        val result = mutableListOf<DayActivityMinutes>()
        val dayLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        
        for (i in 0..6) {
            val dayCal = cal.clone() as Calendar
            dayCal.add(Calendar.DAY_OF_YEAR, i)
            val dayOfYear = dayCal.get(Calendar.DAY_OF_YEAR)
            val dayYear = dayCal.get(Calendar.YEAR)
            
            val isToday = dayOfYear == today && dayYear == year
            val isFuture = dayYear > year || (dayYear == year && dayOfYear > today)
            
            val key = "${KEY_ACTIVITY_MINUTES_PREFIX}${dayYear}_${dayOfYear}"
            var minutes = if (isFuture) 0 else p.getInt(key, 0)
            if (isToday) minutes += activeSessionMinutes
            
            result.add(DayActivityMinutes(
                dayLabel = dayLabels[i],
                minutes = minutes,
                isToday = isToday
            ))
        }
        
        return result
    }
    
    /**
     * Get total weekly minutes
     */
    fun getWeeklyTotalMinutes(ctx: Context): Int {
        return getWeeklyActivityMinutes(ctx).sumOf { it.minutes }
    }
    
    private fun checkWeeklyActivityReset(ctx: Context) {
        val p = prefs(ctx)
        val cal = Calendar.getInstance()
        val currentWeek = cal.get(Calendar.WEEK_OF_YEAR)
        val currentYear = cal.get(Calendar.YEAR)
        val lastResetWeek = p.getInt(KEY_ACTIVITY_WEEK_RESET, -1)
        val lastResetYear = p.getInt(KEY_ACTIVITY_WEEK_RESET_YEAR, -1)
        
        if (currentWeek != lastResetWeek || currentYear != lastResetYear) {
            // New week — clear old activity minutes (previous 14 days to be safe)
            val editor = p.edit()
            for (i in 7..20) {
                cal.timeInMillis = System.currentTimeMillis()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val d = cal.get(Calendar.DAY_OF_YEAR)
                val y = cal.get(Calendar.YEAR)
                editor.remove("${KEY_ACTIVITY_MINUTES_PREFIX}${y}_${d}")
            }
            editor.putInt(KEY_ACTIVITY_WEEK_RESET, currentWeek)
            editor.putInt(KEY_ACTIVITY_WEEK_RESET_YEAR, currentYear)
            editor.apply()
        }
    }
}

data class DayActivityMinutes(
    val dayLabel: String,
    val minutes: Int,
    val isToday: Boolean
)

data class DayActivityDisplay(
    val dayName: String,
    val isToday: Boolean,
    val goalMet: Boolean,
    val xpEarned: Int,
    val dailyGoal: Int,
    val wasActive: Boolean
)

sealed class StreakCheckResult {
    data class SameDay(val streak: Int) : StreakCheckResult()
    data class StreakContinues(val streak: Int) : StreakCheckResult()
    data class FreezeUsed(val streak: Int, val freezesLeft: Int) : StreakCheckResult()
    data class StreakLost(val lostStreak: Int) : StreakCheckResult()
}

sealed class BuyResult {
    data class Success(val cost: Int) : BuyResult()
    object NotEnoughCurrency : BuyResult()
    object MaxOwned : BuyResult()
}

sealed class ClaimResult {
    data class Success(val coins: Int, val gems: Int) : ClaimResult()
    object NotUnlocked : ClaimResult()
    object AlreadyClaimed : ClaimResult()
}
