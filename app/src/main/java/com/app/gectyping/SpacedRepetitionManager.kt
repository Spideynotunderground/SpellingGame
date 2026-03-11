package com.app.gectyping

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.pow

/**
 * ============================================================================
 * SPACED REPETITION SYSTEM — SM-2 Algorithm Implementation
 * 
 * Based on the SuperMemo SM-2 algorithm for optimal learning intervals.
 * 
 * Features:
 * - Tracks individual word mastery
 * - Calculates optimal review intervals
 * - Strength decay over time
 * - Practice mode for weak words
 * - Word bank with all learned words
 * ============================================================================
 */

/**
 * Word learning state based on SM-2 algorithm
 */
data class WordLearningState(
    val word: String,
    val easeFactor: Float = 2.5f,      // EF: 1.3 to 2.5+
    val interval: Int = 0,              // Days until next review
    val repetitions: Int = 0,           // Successful repetitions in a row
    val lastReview: Long = 0,           // Timestamp of last review
    val nextReview: Long = 0,           // Timestamp when review is due
    val totalReviews: Int = 0,          // Total times reviewed
    val correctCount: Int = 0,          // Total correct answers
    val incorrectCount: Int = 0,        // Total incorrect answers
    val strength: Float = 0f,           // 0-1 mastery strength
    val status: WordStatus = WordStatus.NEW
) {
    val accuracy: Float
        get() = if (totalReviews > 0) correctCount.toFloat() / totalReviews else 0f
    
    val isDue: Boolean
        get() = System.currentTimeMillis() >= nextReview
    
    val daysUntilReview: Int
        get() {
            val diff = nextReview - System.currentTimeMillis()
            return if (diff <= 0) 0 else (diff / DAY_MS).toInt()
        }
    
    companion object {
        const val DAY_MS = 24 * 60 * 60 * 1000L
    }
}

enum class WordStatus {
    NEW,        // Never reviewed
    LEARNING,   // In initial learning phase (interval < 1 day)
    REVIEWING,  // In review phase (interval >= 1 day)
    MASTERED    // High ease factor and long interval
}

/**
 * Quality rating for SM-2 algorithm (0-5)
 */
enum class ReviewQuality(val value: Int, val description: String) {
    BLACKOUT(0, "Complete blackout"),
    INCORRECT(1, "Incorrect, but remembered upon seeing answer"),
    INCORRECT_EASY(2, "Incorrect, but answer seemed easy to recall"),
    CORRECT_DIFFICULT(3, "Correct with serious difficulty"),
    CORRECT_HESITATION(4, "Correct after hesitation"),
    PERFECT(5, "Perfect response")
}

data class PracticeSession(
    val words: List<WordLearningState>,
    val sessionType: PracticeType,
    val targetCount: Int = 10
)

enum class PracticeType {
    DUE_WORDS,      // Words that are due for review
    WEAK_WORDS,     // Words with low strength
    NEW_WORDS,      // New words to learn
    MISTAKES,       // Recently incorrect words
    MIXED           // Mix of all types
}

data class WordBankStats(
    val totalWords: Int,
    val newWords: Int,
    val learningWords: Int,
    val reviewingWords: Int,
    val masteredWords: Int,
    val dueToday: Int,
    val averageStrength: Float
)

object SpacedRepetitionManager {
    private const val PREFS = "spaced_repetition"
    private const val KEY_WORD_STATES = "word_states"
    private const val KEY_LAST_DECAY_CHECK = "last_decay_check"
    
    // SM-2 Algorithm constants
    private const val MIN_EASE_FACTOR = 1.3f
    private const val INITIAL_EASE_FACTOR = 2.5f
    private const val DAY_MS = 24 * 60 * 60 * 1000L
    
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    
    /**
     * Get learning state for a specific word
     */
    fun getWordState(ctx: Context, word: String): WordLearningState {
        val states = loadAllStates(ctx)
        return states[word.lowercase()] ?: WordLearningState(word = word.lowercase())
    }
    
    /**
     * Get all word states
     */
    fun getAllWordStates(ctx: Context): Map<String, WordLearningState> {
        return loadAllStates(ctx)
    }
    
    /**
     * Record a review result using SM-2 algorithm
     */
    fun recordReview(
        ctx: Context,
        word: String,
        isCorrect: Boolean,
        responseTimeMs: Long = 0
    ): WordLearningState {
        val quality = when {
            !isCorrect -> ReviewQuality.INCORRECT
            responseTimeMs > 10000 -> ReviewQuality.CORRECT_DIFFICULT
            responseTimeMs > 5000 -> ReviewQuality.CORRECT_HESITATION
            else -> ReviewQuality.PERFECT
        }
        
        return recordReviewWithQuality(ctx, word, quality)
    }
    
    /**
     * Record review with specific quality rating (SM-2)
     */
    fun recordReviewWithQuality(
        ctx: Context,
        word: String,
        quality: ReviewQuality
    ): WordLearningState {
        val states = loadAllStates(ctx).toMutableMap()
        val currentState = states[word.lowercase()] ?: WordLearningState(word = word.lowercase())
        
        val now = System.currentTimeMillis()
        val isCorrect = quality.value >= 3
        
        // SM-2 Algorithm implementation
        val newState = if (quality.value < 3) {
            // Incorrect response - reset repetitions
            currentState.copy(
                repetitions = 0,
                interval = 1,
                lastReview = now,
                nextReview = now + DAY_MS,
                totalReviews = currentState.totalReviews + 1,
                incorrectCount = currentState.incorrectCount + 1,
                strength = (currentState.strength * 0.8f).coerceIn(0f, 1f),
                status = WordStatus.LEARNING
            )
        } else {
            // Correct response - calculate new interval
            val newRepetitions = currentState.repetitions + 1
            
            val newInterval = when (newRepetitions) {
                1 -> 1
                2 -> 6
                else -> (currentState.interval * currentState.easeFactor).toInt()
            }
            
            // Update ease factor based on quality
            val newEaseFactor = (currentState.easeFactor + 
                (0.1f - (5 - quality.value) * (0.08f + (5 - quality.value) * 0.02f)))
                .coerceAtLeast(MIN_EASE_FACTOR)
            
            // Calculate new strength
            val strengthGain = when (quality) {
                ReviewQuality.PERFECT -> 0.15f
                ReviewQuality.CORRECT_HESITATION -> 0.10f
                ReviewQuality.CORRECT_DIFFICULT -> 0.05f
                else -> 0f
            }
            val newStrength = (currentState.strength + strengthGain).coerceIn(0f, 1f)
            
            // Determine status
            val newStatus = when {
                newInterval >= 21 && newStrength >= 0.8f -> WordStatus.MASTERED
                newInterval >= 1 -> WordStatus.REVIEWING
                else -> WordStatus.LEARNING
            }
            
            currentState.copy(
                easeFactor = newEaseFactor,
                interval = newInterval,
                repetitions = newRepetitions,
                lastReview = now,
                nextReview = now + (newInterval * DAY_MS),
                totalReviews = currentState.totalReviews + 1,
                correctCount = currentState.correctCount + 1,
                strength = newStrength,
                status = newStatus
            )
        }
        
        states[word.lowercase()] = newState
        saveAllStates(ctx, states)
        
        return newState
    }
    
    /**
     * Get words due for review today
     */
    fun getDueWords(ctx: Context, limit: Int = 20): List<WordLearningState> {
        val now = System.currentTimeMillis()
        return loadAllStates(ctx).values
            .filter { it.nextReview <= now && it.status != WordStatus.NEW }
            .sortedBy { it.nextReview }
            .take(limit)
    }
    
    /**
     * Get weak words that need practice
     */
    fun getWeakWords(ctx: Context, limit: Int = 20): List<WordLearningState> {
        return loadAllStates(ctx).values
            .filter { it.strength < 0.5f && it.totalReviews > 0 }
            .sortedBy { it.strength }
            .take(limit)
    }
    
    /**
     * Get new words to learn
     */
    fun getNewWords(ctx: Context, fromWords: List<String>, limit: Int = 10): List<String> {
        val states = loadAllStates(ctx)
        return fromWords
            .filter { !states.containsKey(it.lowercase()) || states[it.lowercase()]?.status == WordStatus.NEW }
            .take(limit)
    }
    
    /**
     * Get recently incorrect words
     */
    fun getRecentMistakes(ctx: Context, limit: Int = 20): List<WordLearningState> {
        val oneDayAgo = System.currentTimeMillis() - DAY_MS
        return loadAllStates(ctx).values
            .filter { it.lastReview > oneDayAgo && it.incorrectCount > 0 }
            .sortedByDescending { it.incorrectCount.toFloat() / it.totalReviews }
            .take(limit)
    }
    
    /**
     * Create a practice session
     */
    fun createPracticeSession(
        ctx: Context,
        type: PracticeType,
        availableWords: List<String> = emptyList(),
        targetCount: Int = 10
    ): PracticeSession {
        val words = when (type) {
            PracticeType.DUE_WORDS -> getDueWords(ctx, targetCount)
            PracticeType.WEAK_WORDS -> getWeakWords(ctx, targetCount)
            PracticeType.NEW_WORDS -> getNewWords(ctx, availableWords, targetCount)
                .map { WordLearningState(word = it) }
            PracticeType.MISTAKES -> getRecentMistakes(ctx, targetCount)
            PracticeType.MIXED -> {
                val due = getDueWords(ctx, targetCount / 3)
                val weak = getWeakWords(ctx, targetCount / 3)
                val new = getNewWords(ctx, availableWords, targetCount / 3)
                    .map { WordLearningState(word = it) }
                (due + weak + new).shuffled().take(targetCount)
            }
        }
        
        return PracticeSession(
            words = words,
            sessionType = type,
            targetCount = targetCount
        )
    }
    
    /**
     * Get word bank statistics
     */
    fun getWordBankStats(ctx: Context): WordBankStats {
        val states = loadAllStates(ctx)
        val now = System.currentTimeMillis()
        
        var newCount = 0
        var learningCount = 0
        var reviewingCount = 0
        var masteredCount = 0
        var dueCount = 0
        var totalStrength = 0f
        
        for (state in states.values) {
            when (state.status) {
                WordStatus.NEW -> newCount++
                WordStatus.LEARNING -> learningCount++
                WordStatus.REVIEWING -> reviewingCount++
                WordStatus.MASTERED -> masteredCount++
            }
            
            if (state.nextReview <= now && state.status != WordStatus.NEW) {
                dueCount++
            }
            
            totalStrength += state.strength
        }
        
        return WordBankStats(
            totalWords = states.size,
            newWords = newCount,
            learningWords = learningCount,
            reviewingWords = reviewingCount,
            masteredWords = masteredCount,
            dueToday = dueCount,
            averageStrength = if (states.isNotEmpty()) totalStrength / states.size else 0f
        )
    }
    
    /**
     * Decay word strength over time (call daily)
     */
    fun decayStrength(ctx: Context) {
        val p = prefs(ctx)
        val now = System.currentTimeMillis()
        val lastCheck = p.getLong(KEY_LAST_DECAY_CHECK, 0)
        
        // Only decay once per day
        if (now - lastCheck < DAY_MS) return
        
        val states = loadAllStates(ctx).toMutableMap()
        var changed = false
        
        for ((word, state) in states) {
            if (state.status == WordStatus.NEW) continue
            
            val daysSinceReview = ((now - state.lastReview) / DAY_MS).toInt()
            if (daysSinceReview > state.interval) {
                // Word is overdue - decay strength
                val overdueDays = daysSinceReview - state.interval
                val decayRate = 0.02f * overdueDays // 2% per overdue day
                val newStrength = (state.strength - decayRate).coerceIn(0f, 1f)
                
                if (newStrength != state.strength) {
                    states[word] = state.copy(strength = newStrength)
                    changed = true
                }
            }
        }
        
        if (changed) {
            saveAllStates(ctx, states)
        }
        
        p.edit().putLong(KEY_LAST_DECAY_CHECK, now).apply()
    }
    
    /**
     * Get strength bar segments for UI (5 segments like Duolingo)
     */
    fun getStrengthBars(strength: Float): Int {
        return when {
            strength >= 0.9f -> 5
            strength >= 0.7f -> 4
            strength >= 0.5f -> 3
            strength >= 0.3f -> 2
            strength > 0f -> 1
            else -> 0
        }
    }
    
    /**
     * Get words sorted by strength (for word bank display)
     */
    fun getWordsSortedByStrength(ctx: Context, ascending: Boolean = true): List<WordLearningState> {
        val states = loadAllStates(ctx).values.toList()
        return if (ascending) {
            states.sortedBy { it.strength }
        } else {
            states.sortedByDescending { it.strength }
        }
    }
    
    /**
     * Search words in word bank
     */
    fun searchWords(ctx: Context, query: String): List<WordLearningState> {
        if (query.isBlank()) return emptyList()
        val lowerQuery = query.lowercase()
        return loadAllStates(ctx).values
            .filter { it.word.contains(lowerQuery) }
            .sortedBy { it.word }
    }
    
    // ========== Persistence ==========
    
    private fun loadAllStates(ctx: Context): Map<String, WordLearningState> {
        val p = prefs(ctx)
        val json = p.getString(KEY_WORD_STATES, null) ?: return emptyMap()
        
        return try {
            val arr = JSONArray(json)
            val result = mutableMapOf<String, WordLearningState>()
            
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val state = WordLearningState(
                    word = obj.getString("word"),
                    easeFactor = obj.optDouble("easeFactor", 2.5).toFloat(),
                    interval = obj.optInt("interval", 0),
                    repetitions = obj.optInt("repetitions", 0),
                    lastReview = obj.optLong("lastReview", 0),
                    nextReview = obj.optLong("nextReview", 0),
                    totalReviews = obj.optInt("totalReviews", 0),
                    correctCount = obj.optInt("correctCount", 0),
                    incorrectCount = obj.optInt("incorrectCount", 0),
                    strength = obj.optDouble("strength", 0.0).toFloat(),
                    status = WordStatus.valueOf(obj.optString("status", "NEW"))
                )
                result[state.word] = state
            }
            
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    private fun saveAllStates(ctx: Context, states: Map<String, WordLearningState>) {
        val arr = JSONArray()
        
        for (state in states.values) {
            val obj = JSONObject().apply {
                put("word", state.word)
                put("easeFactor", state.easeFactor.toDouble())
                put("interval", state.interval)
                put("repetitions", state.repetitions)
                put("lastReview", state.lastReview)
                put("nextReview", state.nextReview)
                put("totalReviews", state.totalReviews)
                put("correctCount", state.correctCount)
                put("incorrectCount", state.incorrectCount)
                put("strength", state.strength.toDouble())
                put("status", state.status.name)
            }
            arr.put(obj)
        }
        
        prefs(ctx).edit().putString(KEY_WORD_STATES, arr.toString()).apply()
    }
    
    /**
     * Reset all learning data (for testing)
     */
    fun resetAllData(ctx: Context) {
        prefs(ctx).edit().clear().apply()
    }
}
