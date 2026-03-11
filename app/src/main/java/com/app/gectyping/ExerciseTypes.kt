package com.app.gectyping

import android.content.Context
import kotlin.random.Random

/**
 * ============================================================================
 * EXERCISE TYPES — Duolingo-style varied exercises
 * 
 * Types:
 * 1. LISTEN_AND_TYPE - Hear word, type spelling (current default)
 * 2. MULTIPLE_CHOICE - Choose correct spelling from 4 options
 * 3. WORD_SCRAMBLE - Arrange scrambled letters
 * 4. FILL_BLANK - Complete the word with missing letters
 * 5. MATCH_PAIRS - Match words with meanings (for batch)
 * 6. TRANSLATE - Type translation (if translations available)
 * ============================================================================
 */

enum class ExerciseType {
    LISTEN_AND_TYPE,
    MULTIPLE_CHOICE,
    WORD_SCRAMBLE,
    FILL_BLANK,
    MATCH_PAIRS,
    TRANSLATE
}

/**
 * Base exercise data
 */
sealed class Exercise {
    abstract val word: String
    abstract val type: ExerciseType
    abstract val xpReward: Int
    
    /**
     * Listen and type - hear the word, type the spelling
     */
    data class ListenAndType(
        override val word: String,
        val hint: String = "",
        override val xpReward: Int = 10
    ) : Exercise() {
        override val type = ExerciseType.LISTEN_AND_TYPE
    }
    
    /**
     * Multiple choice - select correct spelling from options
     */
    data class MultipleChoice(
        override val word: String,
        val options: List<String>, // 4 options, one correct
        val correctIndex: Int,
        override val xpReward: Int = 8
    ) : Exercise() {
        override val type = ExerciseType.MULTIPLE_CHOICE
    }
    
    /**
     * Word scramble - arrange letters to form the word
     */
    data class WordScramble(
        override val word: String,
        val scrambledLetters: List<Char>,
        override val xpReward: Int = 12
    ) : Exercise() {
        override val type = ExerciseType.WORD_SCRAMBLE
    }
    
    /**
     * Fill in the blank - complete word with missing letters
     */
    data class FillBlank(
        override val word: String,
        val displayWord: String, // e.g., "a_p_e" for "apple"
        val missingIndices: List<Int>,
        val missingLetters: List<Char>,
        override val xpReward: Int = 10
    ) : Exercise() {
        override val type = ExerciseType.FILL_BLANK
    }
    
    /**
     * Match pairs - match words with translations/meanings
     */
    data class MatchPairs(
        override val word: String, // Not used directly, represents the exercise
        val pairs: List<Pair<String, String>>, // word to meaning/translation
        override val xpReward: Int = 15
    ) : Exercise() {
        override val type = ExerciseType.MATCH_PAIRS
    }
    
    /**
     * Translate - type the translation
     */
    data class Translate(
        override val word: String,
        val translation: String,
        val fromLanguage: String = "English",
        val toLanguage: String = "Russian",
        override val xpReward: Int = 10
    ) : Exercise() {
        override val type = ExerciseType.TRANSLATE
    }
}

/**
 * Exercise generator - creates varied exercises from words
 */
object ExerciseGenerator {
    
    /**
     * Generate a random exercise for a word
     */
    fun generateExercise(
        word: String,
        allWords: List<String>,
        translation: String = "",
        preferredTypes: List<ExerciseType> = ExerciseType.entries
    ): Exercise {
        val availableTypes = preferredTypes.filter { type ->
            when (type) {
                ExerciseType.TRANSLATE -> translation.isNotBlank()
                ExerciseType.MATCH_PAIRS -> allWords.size >= 4
                else -> true
            }
        }
        
        val selectedType = availableTypes.randomOrNull() ?: ExerciseType.LISTEN_AND_TYPE
        
        return when (selectedType) {
            ExerciseType.LISTEN_AND_TYPE -> createListenAndType(word)
            ExerciseType.MULTIPLE_CHOICE -> createMultipleChoice(word, allWords)
            ExerciseType.WORD_SCRAMBLE -> createWordScramble(word)
            ExerciseType.FILL_BLANK -> createFillBlank(word)
            ExerciseType.MATCH_PAIRS -> createMatchPairs(word, allWords)
            ExerciseType.TRANSLATE -> createTranslate(word, translation)
        }
    }
    
    /**
     * Generate a session of varied exercises
     */
    fun generateSession(
        words: List<String>,
        translations: Map<String, String> = emptyMap(),
        count: Int = 10,
        variety: Boolean = true
    ): List<Exercise> {
        if (words.isEmpty()) return emptyList()
        
        val exercises = mutableListOf<Exercise>()
        val shuffledWords = words.shuffled().take(count)
        
        for ((index, word) in shuffledWords.withIndex()) {
            val translation = translations[word] ?: ""
            
            val exercise = if (variety) {
                // Vary exercise types throughout session
                val preferredType = when (index % 5) {
                    0 -> listOf(ExerciseType.LISTEN_AND_TYPE)
                    1 -> listOf(ExerciseType.MULTIPLE_CHOICE)
                    2 -> listOf(ExerciseType.WORD_SCRAMBLE)
                    3 -> listOf(ExerciseType.FILL_BLANK)
                    else -> ExerciseType.entries
                }
                generateExercise(word, words, translation, preferredType)
            } else {
                createListenAndType(word)
            }
            
            exercises.add(exercise)
        }
        
        return exercises
    }
    
    // ========== Exercise Creators ==========
    
    private fun createListenAndType(word: String): Exercise.ListenAndType {
        return Exercise.ListenAndType(
            word = word,
            hint = "${word.length} letters"
        )
    }
    
    private fun createMultipleChoice(word: String, allWords: List<String>): Exercise.MultipleChoice {
        val distractors = generateDistractors(word, allWords, count = 3)
        val options = (distractors + word).shuffled()
        val correctIndex = options.indexOf(word)
        
        return Exercise.MultipleChoice(
            word = word,
            options = options,
            correctIndex = correctIndex
        )
    }
    
    private fun createWordScramble(word: String): Exercise.WordScramble {
        var scrambled = word.toList().shuffled()
        // Ensure it's actually scrambled
        var attempts = 0
        while (scrambled.joinToString("") == word && attempts < 10) {
            scrambled = word.toList().shuffled()
            attempts++
        }
        
        return Exercise.WordScramble(
            word = word,
            scrambledLetters = scrambled
        )
    }
    
    private fun createFillBlank(word: String): Exercise.FillBlank {
        // Remove 30-50% of letters
        val numToRemove = (word.length * Random.nextFloat() * 0.2f + 0.3f).toInt()
            .coerceIn(1, word.length - 1)
        
        val indices = word.indices.shuffled().take(numToRemove).sorted()
        val missingLetters = indices.map { word[it] }
        
        val displayWord = word.mapIndexed { index, c ->
            if (index in indices) '_' else c
        }.joinToString("")
        
        return Exercise.FillBlank(
            word = word,
            displayWord = displayWord,
            missingIndices = indices,
            missingLetters = missingLetters
        )
    }
    
    private fun createMatchPairs(word: String, allWords: List<String>): Exercise.MatchPairs {
        // Create 4 pairs for matching
        val selectedWords = (listOf(word) + allWords.filter { it != word }.shuffled().take(3))
            .shuffled()
        
        // For now, use word length as "meaning" (in real app, use translations)
        val pairs = selectedWords.map { w ->
            w to "${w.length} letters"
        }
        
        return Exercise.MatchPairs(
            word = word,
            pairs = pairs
        )
    }
    
    private fun createTranslate(word: String, translation: String): Exercise.Translate {
        return Exercise.Translate(
            word = word,
            translation = translation
        )
    }
    
    // ========== Distractor Generation ==========
    
    /**
     * Generate plausible wrong answers for multiple choice
     */
    private fun generateDistractors(
        correctWord: String,
        allWords: List<String>,
        count: Int = 3
    ): List<String> {
        val distractors = mutableListOf<String>()
        
        // Strategy 1: Similar length words from pool
        val similarLength = allWords
            .filter { it != correctWord && kotlin.math.abs(it.length - correctWord.length) <= 2 }
            .shuffled()
            .take(count)
        distractors.addAll(similarLength)
        
        // Strategy 2: Generate typos if not enough
        while (distractors.size < count) {
            val typo = generateTypo(correctWord)
            if (typo != correctWord && typo !in distractors) {
                distractors.add(typo)
            }
        }
        
        return distractors.take(count)
    }
    
    /**
     * Generate a plausible typo of a word
     */
    private fun generateTypo(word: String): String {
        if (word.length < 2) return word + "s"
        
        return when (Random.nextInt(5)) {
            0 -> {
                // Swap two adjacent letters
                val i = Random.nextInt(word.length - 1)
                word.substring(0, i) + word[i + 1] + word[i] + word.substring(i + 2)
            }
            1 -> {
                // Double a letter
                val i = Random.nextInt(word.length)
                word.substring(0, i) + word[i] + word.substring(i)
            }
            2 -> {
                // Remove a letter
                val i = Random.nextInt(word.length)
                word.removeRange(i, i + 1)
            }
            3 -> {
                // Replace vowel
                val vowels = "aeiou"
                val vowelIndices = word.indices.filter { word[it].lowercaseChar() in vowels }
                if (vowelIndices.isNotEmpty()) {
                    val i = vowelIndices.random()
                    val newVowel = vowels.filter { it != word[i].lowercaseChar() }.random()
                    word.substring(0, i) + newVowel + word.substring(i + 1)
                } else word + "e"
            }
            else -> {
                // Add common suffix
                word + listOf("s", "ed", "ing", "ly", "er").random()
            }
        }
    }
}

/**
 * Exercise result
 */
data class ExerciseResult(
    val exercise: Exercise,
    val isCorrect: Boolean,
    val userAnswer: String,
    val timeSpentMs: Long,
    val xpEarned: Int
)

/**
 * Session result
 */
data class SessionResult(
    val exercises: List<ExerciseResult>,
    val totalXP: Int,
    val correctCount: Int,
    val accuracy: Float,
    val totalTimeMs: Long,
    val isPerfect: Boolean
) {
    companion object {
        fun fromResults(results: List<ExerciseResult>): SessionResult {
            val correct = results.count { it.isCorrect }
            val totalXP = results.sumOf { it.xpEarned }
            val totalTime = results.sumOf { it.timeSpentMs }
            
            return SessionResult(
                exercises = results,
                totalXP = totalXP,
                correctCount = correct,
                accuracy = if (results.isNotEmpty()) correct.toFloat() / results.size else 0f,
                totalTimeMs = totalTime,
                isPerfect = correct == results.size
            )
        }
    }
}
