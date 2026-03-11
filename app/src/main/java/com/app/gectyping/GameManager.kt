package com.app.gectyping

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * =============================================================================
 * GAME CONFIGURATION CONSTANTS
 * =============================================================================
 */
object GameConfig {
    // Time system
    const val WORD_TIME_SECONDS = 10              // Each word gives exactly 10 seconds
    const val TIMER_UPDATE_INTERVAL_MS = 50L      // Smooth 20 FPS timer updates
    
    // Lives system
    const val MAX_LIVES = 3
    
    // Revive system
    const val REVIVE_COST_STARS = 15              // Cost to buy one extra life
    
    // Level unlock requirements (stars needed)
    val LEVEL_UNLOCK_STARS = mapOf(
        1 to 0,      // Level 1 is free
        2 to 35,     // Need 35 stars for Level 2
        3 to 72,     // Need 72 stars for Level 3
        4 to 105,    // Need 105 stars for Level 4
        5 to 170,    // Need 170 stars for Level 5
        6 to 250,    // Need 250 stars for Level 6
        7 to 350     // Need 350 stars for Level 7 (endless)
    )
    
    // Base time per level (increases with level)
    fun getBaseTotalTime(level: Int): Float {
        return when (level) {
            1 -> 60f      // 60 seconds base for level 1
            2 -> 75f      // 75 seconds base for level 2
            3 -> 90f      // 90 seconds base for level 3
            4 -> 105f     // 105 seconds base for level 4
            5 -> 120f     // 120 seconds base for level 5
            6 -> 135f     // 135 seconds base for level 6
            7 -> 150f     // 150 seconds base for level 7 (endless)
            else -> 60f
        }
    }
    
    // Max total time cap per level
    fun getMaxTotalTime(level: Int): Float {
        return when (level) {
            1 -> 120f
            2 -> 150f
            3 -> 180f
            4 -> 210f
            5 -> 300f     // Endless mode has higher cap
            6 -> 350f
            7 -> 400f     // Level 7 endless mode
            else -> 120f
        }
    }
    
    // Rewards per correct answer
    fun getRewards(level: Int): Pair<Int, Int> {  // (diamonds, stars)
        return when (level) {
            1 -> 5 to 1
            2 -> 10 to 2
            3 -> 15 to 3
            4 -> 20 to 4
            5 -> 25 to 5
            6 -> 30 to 6
            7 -> 35 to 7
            else -> 5 to 1
        }
    }
}

/**
 * =============================================================================
 * GAME STATE DATA MODEL
 * Immutable data class representing the complete game state at any moment.
 * =============================================================================
 */
data class GameState(
    // Core game status
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isGameOver: Boolean = false,
    
    // Level & progression
    val currentLevel: Int = 1,
    val totalStarsEarned: Int = 0,        // Lifetime stars (for level unlock)
    val currentSessionStars: Int = 0,      // Stars earned this session
    val currentSessionDiamonds: Int = 0,    // Diamonds earned this session
    
    // Time system
    val overallTimeLeft: Float = 60f,      // Total time remaining (in seconds)
    val maxOverallTime: Float = 120f,      // Maximum time allowed
    val wordTimeLeft: Float = 10f,         // Time left for current word
    val wordTimeMax: Float = 10f,          // Max time per word (always 10)
    
    // Lives & revive
    val lives: Int = GameConfig.MAX_LIVES,
    val hasUsedRevive: Boolean = false,    // Can only revive once per game
    val canRevive: Boolean = false,        // Computed: has stars and hasn't used revive
    
    // Word state
    val wordsCompleted: Int = 0,
    val currentCombo: Int = 0,
    val maxCombo: Int = 0,
    
    // UI state
    val showReviveDialog: Boolean = false,
    val showGameOverDialog: Boolean = false
) {
    // Computed properties
    val overallTimeProgress: Float get() = (overallTimeLeft / maxOverallTime).coerceIn(0f, 1f)
    val wordTimeProgress: Float get() = (wordTimeLeft / wordTimeMax).coerceIn(0f, 1f)
    val isWordTimeCritical: Boolean get() = wordTimeLeft <= 3f
    val isOverallTimeCritical: Boolean get() = overallTimeLeft <= 10f
    
    // Check if player can unlock a specific level
    fun canUnlockLevel(level: Int): Boolean {
        val required = GameConfig.LEVEL_UNLOCK_STARS[level] ?: Int.MAX_VALUE
        return totalStarsEarned >= required
    }
    
    // Get next level unlock progress
    fun getUnlockProgress(level: Int): Float {
        val required = GameConfig.LEVEL_UNLOCK_STARS[level] ?: return 1f
        if (required == 0) return 1f
        return (totalStarsEarned.toFloat() / required).coerceIn(0f, 1f)
    }
}

/**
 * =============================================================================
 * GAME EVENTS
 * Sealed class for all possible game events that can occur.
 * =============================================================================
 */
sealed class GameEvent {
    object WordCompleted : GameEvent()
    object WordTimedOut : GameEvent()
    object OverallTimedOut : GameEvent()
    object LifeLost : GameEvent()
    object GameOver : GameEvent()
    object ReviveUsed : GameEvent()
    object LevelUp : GameEvent()
    data class StarsEarned(val amount: Int) : GameEvent()
    data class DiamondsEarned(val amount: Int) : GameEvent()
}

/**
 * =============================================================================
 * PERSISTENT STORAGE KEYS
 * =============================================================================
 */
private object StorageKeys {
    const val PREFS_NAME = "spelling_game_v2"
    const val KEY_TOTAL_STARS = "total_stars_earned"
    const val KEY_TOTAL_DIAMONDS = "total_diamonds"
    const val KEY_HIGHEST_LEVEL = "highest_level_reached"
    const val KEY_LIFETIME_WORDS = "lifetime_words_completed"
    const val KEY_BEST_COMBO = "best_combo"
}

/**
 * =============================================================================
 * GAME MANAGER
 * Central controller for all game logic. Uses coroutines for smooth timers.
 * =============================================================================
 */
class GameManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        StorageKeys.PREFS_NAME, Context.MODE_PRIVATE
    )
    
    // Coroutine scope for timer management
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var timerJob: Job? = null
    
    // Observable game state
    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    // Event callbacks
    var onGameEvent: ((GameEvent) -> Unit)? = null
    
    // Current state accessor
    private var state: GameState
        get() = _gameState.value
        set(value) { _gameState.value = value }
    
    init {
        loadPersistentData()
    }
    
    /**
     * =========================================================================
     * PERSISTENT DATA MANAGEMENT
     * =========================================================================
     */
    private fun loadPersistentData() {
        val totalStars = prefs.getInt(StorageKeys.KEY_TOTAL_STARS, 0)
        val highestLevel = prefs.getInt(StorageKeys.KEY_HIGHEST_LEVEL, 1)
        
        state = state.copy(
            totalStarsEarned = totalStars,
            currentLevel = highestLevel.coerceIn(1, 7)
        )
    }
    
    private fun savePersistentData() {
        prefs.edit().apply {
            putInt(StorageKeys.KEY_TOTAL_STARS, state.totalStarsEarned)
            putInt(StorageKeys.KEY_HIGHEST_LEVEL, state.currentLevel)
            apply()
        }
    }
    
    fun getTotalDiamonds(): Int = prefs.getInt(StorageKeys.KEY_TOTAL_DIAMONDS, 0)
    
    fun addDiamonds(amount: Int) {
        val current = getTotalDiamonds()
        prefs.edit().putInt(StorageKeys.KEY_TOTAL_DIAMONDS, current + amount).apply()
    }
    
    fun spendDiamonds(amount: Int): Boolean {
        val current = getTotalDiamonds()
        if (current < amount) return false
        prefs.edit().putInt(StorageKeys.KEY_TOTAL_DIAMONDS, current - amount).apply()
        return true
    }
    
    fun getTotalStars(): Int = state.totalStarsEarned
    
    /**
     * =========================================================================
     * GAME LIFECYCLE
     * =========================================================================
     */
    
    /**
     * Start a new game at the specified level.
     * Validates that the level is unlocked before starting.
     */
    fun startGame(level: Int = 1) {
        val targetLevel = level.coerceIn(1, 7)
        
        // Check if level is unlocked
        if (!state.canUnlockLevel(targetLevel)) {
            return // Cannot start - level locked
        }
        
        val baseTime = GameConfig.getBaseTotalTime(targetLevel)
        val maxTime = GameConfig.getMaxTotalTime(targetLevel)
        
        state = state.copy(
            isPlaying = true,
            isPaused = false,
            isGameOver = false,
            currentLevel = targetLevel,
            overallTimeLeft = baseTime,
            maxOverallTime = maxTime,
            wordTimeLeft = GameConfig.WORD_TIME_SECONDS.toFloat(),
            wordTimeMax = GameConfig.WORD_TIME_SECONDS.toFloat(),
            lives = GameConfig.MAX_LIVES,
            hasUsedRevive = false,
            canRevive = false,
            wordsCompleted = 0,
            currentCombo = 0,
            currentSessionStars = 0,
            currentSessionDiamonds = 0,
            showReviveDialog = false,
            showGameOverDialog = false
        )
        
        startTimers()
    }
    
    /**
     * Pause the game (stops timers but preserves state).
     */
    fun pauseGame() {
        if (!state.isPlaying || state.isGameOver) return
        
        state = state.copy(isPaused = true)
        stopTimers()
    }
    
    /**
     * Resume a paused game.
     */
    fun resumeGame() {
        if (!state.isPlaying || state.isGameOver || !state.isPaused) return
        
        state = state.copy(isPaused = false)
        startTimers()
    }
    
    /**
     * End the current game session.
     */
    fun endGame() {
        stopTimers()
        
        // Save earned stars to persistent storage
        val newTotalStars = state.totalStarsEarned + state.currentSessionStars
        state = state.copy(
            isPlaying = false,
            isGameOver = true,
            totalStarsEarned = newTotalStars
        )
        
        // Add session diamonds to total
        addDiamonds(state.currentSessionDiamonds)
        savePersistentData()
    }
    
    /**
     * Clean up resources when the manager is no longer needed.
     */
    fun destroy() {
        stopTimers()
        managerScope.cancel()
    }
    
    /**
     * =========================================================================
     * TIMER SYSTEM
     * Uses coroutines for smooth, non-blocking timer updates.
     * =========================================================================
     */
    private fun startTimers() {
        stopTimers()
        
        timerJob = managerScope.launch {
            var lastUpdateTime = System.currentTimeMillis()
            
            while (isActive && state.isPlaying && !state.isPaused && !state.isGameOver) {
                val currentTime = System.currentTimeMillis()
                val deltaSeconds = (currentTime - lastUpdateTime) / 1000f
                lastUpdateTime = currentTime
                
                // Update both timers
                updateTimers(deltaSeconds)
                
                // Smooth update interval (50ms = 20 FPS for timer)
                delay(GameConfig.TIMER_UPDATE_INTERVAL_MS)
            }
        }
    }
    
    private fun stopTimers() {
        timerJob?.cancel()
        timerJob = null
    }
    
    /**
     * Update both timers by the elapsed time delta.
     * This ensures smooth countdown regardless of frame rate.
     */
    private fun updateTimers(deltaSeconds: Float) {
        val newWordTime = (state.wordTimeLeft - deltaSeconds).coerceAtLeast(0f)
        val newOverallTime = (state.overallTimeLeft - deltaSeconds).coerceAtLeast(0f)
        
        state = state.copy(
            wordTimeLeft = newWordTime,
            overallTimeLeft = newOverallTime
        )
        
        // Check for timeout conditions
        when {
            newOverallTime <= 0f -> handleOverallTimeout()
            newWordTime <= 0f -> handleWordTimeout()
        }
    }
    
    /**
     * =========================================================================
     * WORD COMPLETION & TIMEOUT HANDLING
     * =========================================================================
     */
    
    /**
     * Called when player correctly completes a word.
     * Adds remaining word time to overall time and awards rewards.
     */
    fun onWordCompleted() {
        if (!state.isPlaying || state.isGameOver) return
        
        // Calculate time bonus: remaining word time adds to overall time
        val timeBonus = state.wordTimeLeft
        val newOverallTime = (state.overallTimeLeft + timeBonus)
            .coerceAtMost(state.maxOverallTime)
        
        // Calculate rewards with combo multiplier
        val newCombo = state.currentCombo + 1
        val comboMultiplier = (1 + (newCombo - 1) * 0.1f).coerceAtMost(2f)  // Max 2x
        val (baseCoins, baseStars) = GameConfig.getRewards(state.currentLevel)
        val earnedCoins = (baseCoins * comboMultiplier).toInt()
        val earnedStars = baseStars  // Stars not affected by combo
        
        state = state.copy(
            overallTimeLeft = newOverallTime,
            wordTimeLeft = GameConfig.WORD_TIME_SECONDS.toFloat(),  // Reset word timer
            wordsCompleted = state.wordsCompleted + 1,
            currentCombo = newCombo,
            maxCombo = maxOf(state.maxCombo, newCombo),
            currentSessionStars = state.currentSessionStars + earnedStars,
            currentSessionDiamonds = state.currentSessionDiamonds + earnedCoins
        )
        
        // Emit events
        onGameEvent?.invoke(GameEvent.WordCompleted)
        onGameEvent?.invoke(GameEvent.StarsEarned(earnedStars))
        onGameEvent?.invoke(GameEvent.DiamondsEarned(earnedCoins))
        
        // Check for level up (not in endless mode level 5)
        checkLevelProgression()
    }
    
    /**
     * Called when word timer runs out.
     * Player loses a life but overall time continues.
     */
    private fun handleWordTimeout() {
        if (!state.isPlaying || state.isGameOver) return
        
        // Reset combo on timeout
        state = state.copy(
            currentCombo = 0,
            wordTimeLeft = GameConfig.WORD_TIME_SECONDS.toFloat()  // Reset for next word
        )
        
        onGameEvent?.invoke(GameEvent.WordTimedOut)
        loseLife()
    }
    
    /**
     * Called when overall timer runs out.
     * This triggers defeat sequence.
     */
    private fun handleOverallTimeout() {
        if (!state.isPlaying || state.isGameOver) return
        
        onGameEvent?.invoke(GameEvent.OverallTimedOut)
        triggerDefeat()
    }
    
    /**
     * Called when player answers incorrectly.
     */
    fun onWrongAnswer() {
        if (!state.isPlaying || state.isGameOver) return
        
        // Reset combo and word timer
        state = state.copy(
            currentCombo = 0,
            wordTimeLeft = GameConfig.WORD_TIME_SECONDS.toFloat()
        )
        
        loseLife()
    }
    
    /**
     * =========================================================================
     * LIVES & DEFEAT SYSTEM
     * =========================================================================
     */
    
    /**
     * Reduce player lives by 1. Triggers defeat if no lives remaining.
     */
    private fun loseLife() {
        val newLives = state.lives - 1
        
        state = state.copy(lives = newLives.coerceAtLeast(0))
        onGameEvent?.invoke(GameEvent.LifeLost)
        
        if (newLives <= 0) {
            triggerDefeat()
        }
    }
    
    /**
     * Trigger defeat sequence - shows revive dialog if available.
     */
    private fun triggerDefeat() {
        stopTimers()
        
        // Check if player can revive (has enough stars and hasn't used revive)
        val canRevive = !state.hasUsedRevive && 
                        (state.totalStarsEarned + state.currentSessionStars) >= GameConfig.REVIVE_COST_STARS
        
        if (canRevive) {
            // Show revive dialog
            state = state.copy(
                showReviveDialog = true,
                canRevive = true
            )
        } else {
            // Direct game over
            finalGameOver()
        }
    }
    
    /**
     * Player chooses to use their ONE revive chance.
     * Costs 15 stars from their total.
     */
    fun useRevive(): Boolean {
        if (state.hasUsedRevive) return false
        
        val totalAvailableStars = state.totalStarsEarned + state.currentSessionStars
        if (totalAvailableStars < GameConfig.REVIVE_COST_STARS) return false
        
        // Deduct stars (prefer from session first, then from total)
        var starsToDeduct = GameConfig.REVIVE_COST_STARS
        var newSessionStars = state.currentSessionStars
        var newTotalStars = state.totalStarsEarned
        
        if (newSessionStars >= starsToDeduct) {
            newSessionStars -= starsToDeduct
        } else {
            starsToDeduct -= newSessionStars
            newSessionStars = 0
            newTotalStars -= starsToDeduct
        }
        
        // Revive with 1 life and some time bonus
        val reviveTimeBonus = 15f  // Give 15 seconds on revive
        
        state = state.copy(
            lives = 1,
            hasUsedRevive = true,
            canRevive = false,
            showReviveDialog = false,
            currentSessionStars = newSessionStars,
            totalStarsEarned = newTotalStars,
            overallTimeLeft = (state.overallTimeLeft + reviveTimeBonus)
                .coerceAtMost(state.maxOverallTime),
            wordTimeLeft = GameConfig.WORD_TIME_SECONDS.toFloat()
        )
        
        onGameEvent?.invoke(GameEvent.ReviveUsed)
        
        // Resume game
        startTimers()
        return true
    }
    
    /**
     * Player declines revive or cannot afford it.
     */
    fun declineRevive() {
        state = state.copy(showReviveDialog = false)
        finalGameOver()
    }
    
    /**
     * Final game over - no more chances.
     */
    private fun finalGameOver() {
        // Save session progress before reset
        val newTotalStars = state.totalStarsEarned + state.currentSessionStars
        addDiamonds(state.currentSessionDiamonds)
        
        state = state.copy(
            isPlaying = false,
            isGameOver = true,
            showGameOverDialog = true,
            showReviveDialog = false,
            totalStarsEarned = newTotalStars
        )
        
        savePersistentData()
        onGameEvent?.invoke(GameEvent.GameOver)
    }
    
    /**
     * =========================================================================
     * LEVEL PROGRESSION
     * =========================================================================
     */
    
    /**
     * Check if player should level up based on stars earned.
     */
    private fun checkLevelProgression() {
        if (state.currentLevel >= 5) return  // Already at max (endless)
        
        val nextLevel = state.currentLevel + 1
        val requiredStars = GameConfig.LEVEL_UNLOCK_STARS[nextLevel] ?: return
        val totalStars = state.totalStarsEarned + state.currentSessionStars
        
        if (totalStars >= requiredStars) {
            performLevelUp(nextLevel)
        }
    }
    
    /**
     * Level up to the next level.
     */
    private fun performLevelUp(newLevel: Int) {
        val newMaxTime = GameConfig.getMaxTotalTime(newLevel)
        val timeBonus = 30f  // Bonus time on level up
        
        state = state.copy(
            currentLevel = newLevel,
            maxOverallTime = newMaxTime,
            overallTimeLeft = (state.overallTimeLeft + timeBonus).coerceAtMost(newMaxTime)
        )
        
        savePersistentData()
        onGameEvent?.invoke(GameEvent.LevelUp)
    }
    
    /**
     * Get stars needed to unlock a specific level.
     */
    fun getStarsNeededForLevel(level: Int): Int {
        return GameConfig.LEVEL_UNLOCK_STARS[level] ?: 0
    }
    
    /**
     * Check if a level is unlocked.
     */
    fun isLevelUnlocked(level: Int): Boolean {
        return state.canUnlockLevel(level)
    }
    
    /**
     * =========================================================================
     * RESTART & RESET
     * =========================================================================
     */
    
    /**
     * Restart from Level 1 after final game over.
     * Preserves lifetime stats but resets session progress.
     */
    fun restartFromBeginning() {
        state = state.copy(
            isPlaying = false,
            isGameOver = false,
            showGameOverDialog = false,
            showReviveDialog = false,
            currentLevel = 1,
            currentSessionStars = 0,
            currentSessionDiamonds = 0,
            wordsCompleted = 0,
            currentCombo = 0,
            hasUsedRevive = false
        )
    }
    
    /**
     * Skip to next word (resets word timer).
     */
    fun skipWord() {
        if (!state.isPlaying || state.isGameOver) return
        
        state = state.copy(
            wordTimeLeft = GameConfig.WORD_TIME_SECONDS.toFloat(),
            currentCombo = 0  // Reset combo on skip
        )
    }
}
