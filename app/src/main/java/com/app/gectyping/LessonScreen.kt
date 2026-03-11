package com.app.gectyping

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors
import kotlinx.coroutines.delay

// ============================================================================
// Duolingo-style colors
// ============================================================================
private val DuoGreen = Color(0xFF58CC02)
private val DuoGreenDark = Color(0xFF46A302)
private val DuoGreenLight = Color(0xFFD7FFB8)
private val DuoRed = Color(0xFFFF4B4B)
private val DuoRedLight = Color(0xFFFFDFE0)
private val DuoBlue = Color(0xFF1CB0F6)
private val DuoBlueDark = Color(0xFF1899D6)
private val DuoGold = Color(0xFFFFD700)
private val DuoGray = Color(0xFFE5E5E5)
private val DuoDarkGray = Color(0xFF4B4B4B)
private val DuoWhite = Color(0xFFFFFFFF)
private val DuoBg = Color(0xFF131F24)
private val DuoCard = Color(0xFF1A2B32)

// Exercise types
enum class LessonExerciseType {
    INTRODUCTION,
    CHOOSE_TRANSLATION,
    REVERSE_CHOICE,
    SPELL_WORD
}

// Word data for exercises
data class ExerciseWord(
    val english: String,
    val russian: String = "",
    val example: String = ""
)

// Exercise state
data class ExerciseState(
    val type: LessonExerciseType,
    val word: ExerciseWord,
    val options: List<String> = emptyList()
)

// ============================================================================
// Duolingo 3D Button Component
// ============================================================================
@Composable
private fun Duo3DButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = DuoGreen,
    darkColor: Color = DuoGreenDark,
    textColor: Color = DuoWhite
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val offsetY by animateFloatAsState(
        targetValue = if (isPressed) 0f else 4f,
        animationSpec = tween(50), label = "btnOffset"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        // Shadow layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .offset(y = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) darkColor else Color(0xFF3A3A3A))
        )
        // Top layer
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .offset(y = (4f - offsetY).dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (enabled) color else Color(0xFF585858))
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled
                ) { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (enabled) textColor else Color(0xFF9A9A9A),
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ============================================================================
// Duolingo Option Button (for choice exercises)
// ============================================================================
@Composable
private fun DuoOptionButton(
    text: String,
    onClick: () -> Unit,
    state: OptionState = OptionState.DEFAULT,
    modifier: Modifier = Modifier
) {
    val borderColor = when (state) {
        OptionState.DEFAULT -> Color(0xFF37464F)
        OptionState.SELECTED -> DuoBlue
        OptionState.CORRECT -> DuoGreen
        OptionState.WRONG -> DuoRed
    }
    val bgColor = when (state) {
        OptionState.DEFAULT -> Color.Transparent
        OptionState.SELECTED -> DuoBlue.copy(alpha = 0.15f)
        OptionState.CORRECT -> DuoGreen.copy(alpha = 0.15f)
        OptionState.WRONG -> DuoRed.copy(alpha = 0.15f)
    }
    val txtColor = when (state) {
        OptionState.DEFAULT -> Color.White
        OptionState.SELECTED -> DuoBlue
        OptionState.CORRECT -> DuoGreen
        OptionState.WRONG -> DuoRed
    }
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(2.5.dp, borderColor, RoundedCornerShape(14.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            color = txtColor,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

private enum class OptionState { DEFAULT, SELECTED, CORRECT, WRONG }

// ============================================================================
// Main Lesson Screen
// ============================================================================
@Composable
fun LessonScreen(
    context: Context,
    topicTitle: String,
    lessonNumber: Int,
    words: List<ExerciseWord>,
    onLessonComplete: (Int) -> Unit,
    onExit: () -> Unit
) {
    val colors = LocalGameColors.current
    
    // Pre-cache words for TTS
    LaunchedEffect(words) {
        GoogleCloudTTSManager.preCacheWords(words.map { it.english })
    }
    
    val exercises = remember(words) { generateExercises(words) }
    
    var currentExerciseIndex by remember { mutableStateOf(0) }
    var correctAnswers by remember { mutableStateOf(0) }
    var showResult by remember { mutableStateOf(false) }
    var isCorrect by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var lessonComplete by remember { mutableStateOf(false) }
    
    val currentExercise = exercises.getOrNull(currentExerciseIndex)
    val progress = if (exercises.isNotEmpty()) (currentExerciseIndex.toFloat() / exercises.size) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress, animationSpec = tween(400), label = "progress"
    )
    
    // Stable lambda — created once, not recreated on recomposition
    val onSpeakStable: (String) -> Unit = remember { { word ->
        GoogleCloudTTSManager.speak(word, slow = false)
    }}
    
    fun onAnswer(correct: Boolean) {
        isCorrect = correct
        if (correct) {
            correctAnswers++
            SoundManager.success()
        } else {
            SoundManager.fail()
        }
        showResult = true
    }
    
    fun nextExercise() {
        showResult = false
        selectedOption = null
        if (currentExerciseIndex < exercises.size - 1) currentExerciseIndex++
        else lessonComplete = true
    }
    
    if (lessonComplete) {
        LessonCompleteScreen(topicTitle, lessonNumber, correctAnswers, exercises.size) {
            onLessonComplete(correctAnswers * 10)
        }
        return
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // ── Header ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable { onExit() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", fontSize = 20.sp, color = Color(0xFF8A9AA4), fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Progress bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF37464F))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DuoGreen)
                )
            }
        }
        
        // ── Exercise Content ──
        currentExercise?.let { exercise ->
            when (exercise.type) {
                LessonExerciseType.INTRODUCTION -> IntroductionExercise(
                    word = exercise.word,
                    exerciseIndex = currentExerciseIndex,
                    onSpeak = { onSpeakStable(exercise.word.english) },
                    onContinue = { nextExercise() }
                )
                LessonExerciseType.CHOOSE_TRANSLATION -> ChoiceExercise(
                    prompt = "Select the correct translation",
                    displayWord = exercise.word.english,
                    options = exercise.options,
                    correctAnswer = exercise.word.russian.ifEmpty { exercise.word.english },
                    showResult = showResult,
                    isCorrect = isCorrect,
                    selectedOption = selectedOption,
                    onSelectOption = { selectedOption = it },
                    onCheck = { selected -> onAnswer(selected == (exercise.word.russian.ifEmpty { exercise.word.english })) },
                    onContinue = { nextExercise() }
                )
                LessonExerciseType.REVERSE_CHOICE -> ChoiceExercise(
                    prompt = "Select the correct English word",
                    displayWord = exercise.word.russian.ifEmpty { exercise.word.english },
                    options = exercise.options,
                    correctAnswer = exercise.word.english,
                    showResult = showResult,
                    isCorrect = isCorrect,
                    selectedOption = selectedOption,
                    onSelectOption = { selectedOption = it },
                    onCheck = { selected -> onAnswer(selected == exercise.word.english) },
                    onContinue = { nextExercise() }
                )
                LessonExerciseType.SPELL_WORD -> SpellWordExercise(
                    word = exercise.word,
                    exerciseIndex = currentExerciseIndex,
                    showResult = showResult,
                    isCorrect = isCorrect,
                    onAnswer = { onAnswer(it) },
                    onContinue = { nextExercise() },
                    onSpeak = { onSpeakStable(exercise.word.english) }
                )
            }
        }
    }
}

// ============================================================================
// Generate exercises
// ============================================================================
private fun generateExercises(words: List<ExerciseWord>): List<ExerciseState> {
    val exercises = mutableListOf<ExerciseState>()
    
    words.forEach { word ->
        exercises.add(ExerciseState(LessonExerciseType.INTRODUCTION, word))
    }
    words.shuffled().forEach { word ->
        val wrong = words.filter { it != word }.shuffled().take(3).map { it.russian.ifEmpty { it.english } }
        val options = (wrong + (word.russian.ifEmpty { word.english })).shuffled()
        exercises.add(ExerciseState(LessonExerciseType.CHOOSE_TRANSLATION, word, options))
    }
    words.shuffled().forEach { word ->
        val wrong = words.filter { it != word }.shuffled().take(3).map { it.english }
        val options = (wrong + word.english).shuffled()
        exercises.add(ExerciseState(LessonExerciseType.REVERSE_CHOICE, word, options))
    }
    words.shuffled().forEach { word ->
        exercises.add(ExerciseState(LessonExerciseType.SPELL_WORD, word))
    }
    return exercises
}

// ============================================================================
// Exercise 1: Introduction
// ============================================================================
@Composable
private fun IntroductionExercise(
    word: ExerciseWord,
    exerciseIndex: Int,
    onSpeak: () -> Unit,
    onContinue: () -> Unit
) {
    val colors = LocalGameColors.current
    
    // exerciseIndex гарантирует уникальный ключ даже если то же слово встречается в другом упражнении
    val currentOnSpeak by rememberUpdatedState(onSpeak)
    LaunchedEffect(exerciseIndex) { delay(400); currentOnSpeak() }
    
    // Убираем бесполезную spring-анимацию (targetValue=1f с самого начала = нет эффекта, только CPU)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "NEW WORD",
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DuoBlue,
            letterSpacing = 2.sp
        )
        
        Spacer(modifier = Modifier.weight(0.3f))
        
        // Word card
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Shadow
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .offset(y = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1A3A47))
            )
            // Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1F3D4A))
                    .border(2.dp, Color(0xFF2A5060), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = word.english,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = word.russian.ifEmpty { "—" },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8EC8E8)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    // Speaker
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(DuoBlue)
                            .clickable { onSpeak() },
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            painter = androidx.compose.ui.res.painterResource(R.drawable.icons8_speaker_96),
                            contentDescription = "Play audio",
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(0.7f))
        
        Duo3DButton(
            text = "CONTINUE",
            onClick = onContinue,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

// ============================================================================
// Exercise 2 & 3: Choice (Translation / Reverse)
// ============================================================================
@Composable
private fun ChoiceExercise(
    prompt: String,
    displayWord: String,
    options: List<String>,
    correctAnswer: String,
    showResult: Boolean,
    isCorrect: Boolean,
    selectedOption: String?,
    onSelectOption: (String) -> Unit,
    onCheck: (String) -> Unit,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = prompt,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Display word
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DuoBlue),
                contentAlignment = Alignment.Center
            ) {
                Text("📖", fontSize = 24.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1F3D4A))
                    .border(2.dp, Color(0xFF2A5060), RoundedCornerShape(14.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(
                    text = displayWord,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        
        // Options
        options.forEach { option ->
            val state = when {
                showResult && option == correctAnswer -> OptionState.CORRECT
                showResult && option == selectedOption && !isCorrect -> OptionState.WRONG
                !showResult && option == selectedOption -> OptionState.SELECTED
                else -> OptionState.DEFAULT
            }
            
            DuoOptionButton(
                text = option,
                onClick = {
                    if (!showResult) onSelectOption(option)
                },
                state = state,
                modifier = Modifier.padding(vertical = 5.dp)
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        // Bottom area
        if (showResult) {
            ResultBanner(isCorrect = isCorrect, correctAnswer = correctAnswer)
            Spacer(modifier = Modifier.height(12.dp))
            Duo3DButton(
                text = "CONTINUE",
                onClick = onContinue,
                color = if (isCorrect) DuoGreen else DuoRed,
                darkColor = if (isCorrect) DuoGreenDark else Color(0xFFCC3B3B),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        } else {
            Duo3DButton(
                text = "CHECK",
                onClick = { selectedOption?.let { onCheck(it) } },
                enabled = selectedOption != null,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

// ============================================================================
// Exercise 4: Spell Word (was Exercise 5, BUILD_WORD removed)
// ============================================================================
@Composable
private fun SpellWordExercise(
    word: ExerciseWord,
    exerciseIndex: Int,
    showResult: Boolean,
    isCorrect: Boolean,
    onAnswer: (Boolean) -> Unit,
    onContinue: () -> Unit,
    onSpeak: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var input by remember { mutableStateOf("") }
    
    val currentOnSpeak by rememberUpdatedState(onSpeak)
    LaunchedEffect(exerciseIndex) { input = ""; delay(400); currentOnSpeak() }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Write this in English",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Translation hint
        Text(
            text = word.russian.ifEmpty { "Listen and type" },
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8EC8E8)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Speaker button
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(listOf(DuoBlue, DuoBlueDark))
                )
                .clickable { onSpeak() },
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.icons8_speaker_96),
                contentDescription = "Play audio",
                modifier = Modifier.size(44.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Input field
        OutlinedTextField(
            value = input,
            onValueChange = { input = it },
            enabled = !showResult,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Type the word...", color = Color(0xFF5A7A8A)) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DuoBlue,
                unfocusedBorderColor = Color(0xFF37464F),
                focusedContainerColor = Color(0xFF1F3D4A),
                unfocusedContainerColor = Color(0xFF1F3D4A),
                cursorColor = DuoBlue,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (input.isNotBlank()) onAnswer(input.trim().equals(word.english, ignoreCase = true))
                }
            )
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        if (showResult) {
            ResultBanner(isCorrect = isCorrect, correctAnswer = word.english)
            Spacer(modifier = Modifier.height(12.dp))
            Duo3DButton(
                text = "CONTINUE",
                onClick = onContinue,
                color = if (isCorrect) DuoGreen else DuoRed,
                darkColor = if (isCorrect) DuoGreenDark else Color(0xFFCC3B3B),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        } else {
            Duo3DButton(
                text = "CHECK",
                onClick = {
                    focusManager.clearFocus()
                    onAnswer(input.trim().equals(word.english, ignoreCase = true))
                },
                enabled = input.isNotBlank(),
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

// ============================================================================
// Result Banner
// ============================================================================
@Composable
private fun ResultBanner(isCorrect: Boolean, correctAnswer: String) {
    val bgColor = if (isCorrect) DuoGreen.copy(alpha = 0.15f) else DuoRed.copy(alpha = 0.15f)
    val textColor = if (isCorrect) DuoGreen else DuoRed
    val icon = if (isCorrect) "✓" else "✗"
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(textColor),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = if (isCorrect) "Great job!" else "Correct answer:",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textColor
                )
                if (!isCorrect) {
                    Text(
                        text = correctAnswer,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

// ============================================================================
// Lesson Complete Screen
// ============================================================================
@Composable
private fun LessonCompleteScreen(
    topicTitle: String,
    lessonNumber: Int,
    correctAnswers: Int,
    totalExercises: Int,
    onContinue: () -> Unit
) {
    val colors = LocalGameColors.current
    val accuracy = if (totalExercises > 0) (correctAnswers * 100 / totalExercises) else 0
    val xpEarned = correctAnswers * 10
    
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 200f),
        label = "completeScale"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Trophy with animation
        Text(
            text = "🏆",
            fontSize = 100.sp,
            modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = "LESSON COMPLETE!",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = DuoGold,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "$topicTitle • Lesson $lessonNumber",
            fontSize = 16.sp,
            color = Color(0xFF8A9AA4)
        )
        
        Spacer(modifier = Modifier.height(40.dp))
        
        // Stats cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatCard(emoji = "🎯", value = "$accuracy%", label = "Accuracy")
            StatCard(emoji = "⚡", value = "+$xpEarned", label = "XP")
            StatCard(emoji = "✅", value = "$correctAnswers/$totalExercises", label = "Correct")
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Duo3DButton(
            text = "CONTINUE",
            onClick = onContinue,
            color = DuoGreen,
            darkColor = DuoGreenDark
        )
    }
}

@Composable
private fun StatCard(emoji: String, value: String, label: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1F3D4A))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        if (emoji == "⚡") {
            LightningIcon(size = 26.dp)
        } else {
            Text(text = emoji, fontSize = 24.sp)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF8A9AA4)
        )
    }
}
