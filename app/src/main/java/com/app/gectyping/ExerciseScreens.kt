package com.app.gectyping

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors

/**
 * ============================================================================
 * EXERCISE SCREENS — UI for different exercise types
 * ============================================================================
 */

/**
 * Multiple Choice Exercise Screen
 */
@Composable
fun MultipleChoiceExercise(
    exercise: Exercise.MultipleChoice,
    onAnswer: (Int) -> Unit,
    onPlayAudio: () -> Unit,
    isAnswered: Boolean = false,
    selectedIndex: Int? = null
) {
    val colors = LocalGameColors.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Instruction
        Text(
            text = "Select the correct spelling",
            fontSize = 16.sp,
            color = colors.textSecondary
        )
        
        // Audio button
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(colors.accent)
                .clickable { onPlayAudio() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Play audio",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Text(
            text = "Tap to hear the word",
            fontSize = 12.sp,
            color = colors.textSecondary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Options
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            exercise.options.forEachIndexed { index, option ->
                val isSelected = selectedIndex == index
                val isCorrect = index == exercise.correctIndex
                
                val backgroundColor = when {
                    !isAnswered -> colors.cardBackground
                    isCorrect -> Color(0xFF58CC02).copy(alpha = 0.2f)
                    isSelected && !isCorrect -> Color(0xFFFF4B4B).copy(alpha = 0.2f)
                    else -> colors.cardBackground
                }
                
                val borderColor = when {
                    !isAnswered && isSelected -> colors.accent
                    isAnswered && isCorrect -> Color(0xFF58CC02)
                    isAnswered && isSelected && !isCorrect -> Color(0xFFFF4B4B)
                    else -> colors.textSecondary.copy(alpha = 0.2f)
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(backgroundColor)
                        .border(2.dp, borderColor, RoundedCornerShape(16.dp))
                        .clickable(enabled = !isAnswered) { onAnswer(index) }
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Option letter
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(borderColor.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = ('A' + index).toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                    
                    Text(
                        text = option,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // Result icon
                    if (isAnswered) {
                        if (isCorrect) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Correct",
                                tint = Color(0xFF58CC02),
                                modifier = Modifier.size(24.dp)
                            )
                        } else if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Incorrect",
                                tint = Color(0xFFFF4B4B),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Word Scramble Exercise Screen
 */
@Composable
fun WordScrambleExercise(
    exercise: Exercise.WordScramble,
    onAnswer: (String) -> Unit,
    onPlayAudio: () -> Unit,
    isAnswered: Boolean = false
) {
    val colors = LocalGameColors.current
    
    var selectedLetters by remember { mutableStateOf<List<Int>>(emptyList()) }
    var availableIndices by remember { mutableStateOf(exercise.scrambledLetters.indices.toList()) }
    
    val currentAnswer = selectedLetters.map { exercise.scrambledLetters[it] }.joinToString("")
    
    // Reset when exercise changes
    LaunchedEffect(exercise) {
        selectedLetters = emptyList()
        availableIndices = exercise.scrambledLetters.indices.toList()
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Instruction
        Text(
            text = "Arrange the letters",
            fontSize = 16.sp,
            color = colors.textSecondary
        )
        
        // Audio button
        IconButton(
            onClick = onPlayAudio,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.accent)
        ) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Play audio",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Answer area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.cardBackground)
                .border(2.dp, colors.accent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selectedLetters.isEmpty()) {
                Text(
                    text = "Tap letters below",
                    fontSize = 16.sp,
                    color = colors.textSecondary
                )
            } else {
                selectedLetters.forEachIndexed { index, letterIndex ->
                    LetterTile(
                        letter = exercise.scrambledLetters[letterIndex],
                        isSelected = true,
                        onClick = {
                            // Remove letter
                            selectedLetters = selectedLetters.toMutableList().also { it.removeAt(index) }
                            availableIndices = availableIndices + letterIndex
                        }
                    )
                    if (index < selectedLetters.size - 1) {
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Available letters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            availableIndices.sortedBy { it }.forEach { index ->
                LetterTile(
                    letter = exercise.scrambledLetters[index],
                    isSelected = false,
                    onClick = {
                        selectedLetters = selectedLetters + index
                        availableIndices = availableIndices - index
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Check button
        Button(
            onClick = { onAnswer(currentAnswer) },
            enabled = selectedLetters.size == exercise.scrambledLetters.size && !isAnswered,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF58CC02),
                disabledContainerColor = colors.textSecondary.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "CHECK",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LetterTile(
    letter: Char,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalGameColors.current
    
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) colors.accent else colors.cardBackground)
            .border(
                2.dp,
                if (isSelected) colors.accent else colors.textSecondary.copy(alpha = 0.3f),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter.uppercaseChar().toString(),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else colors.textPrimary
        )
    }
}

/**
 * Fill in the Blank Exercise Screen
 */
@Composable
fun FillBlankExercise(
    exercise: Exercise.FillBlank,
    onAnswer: (String) -> Unit,
    onPlayAudio: () -> Unit,
    isAnswered: Boolean = false
) {
    val colors = LocalGameColors.current
    
    var userInput by remember { mutableStateOf("") }
    
    // Reset when exercise changes
    LaunchedEffect(exercise) {
        userInput = ""
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Instruction
        Text(
            text = "Fill in the missing letters",
            fontSize = 16.sp,
            color = colors.textSecondary
        )
        
        // Audio button
        IconButton(
            onClick = onPlayAudio,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.accent)
        ) {
            Icon(
                imageVector = Icons.Default.VolumeUp,
                contentDescription = "Play audio",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Display word with blanks
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            var inputIndex = 0
            exercise.displayWord.forEachIndexed { index, char ->
                if (char == '_') {
                    // Blank space - show user input or empty
                    val inputChar = userInput.getOrNull(inputIndex)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.cardBackground)
                            .border(2.dp, colors.accent, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = inputChar?.uppercaseChar()?.toString() ?: "",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                    inputIndex++
                } else {
                    // Regular letter
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.uppercaseChar().toString(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }
                }
                
                if (index < exercise.displayWord.length - 1) {
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Input field
        OutlinedTextField(
            value = userInput,
            onValueChange = { 
                if (it.length <= exercise.missingLetters.size) {
                    userInput = it.filter { c -> c.isLetter() }
                }
            },
            label = { Text("Type missing letters") },
            placeholder = { Text("${exercise.missingLetters.size} letters") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { 
                    if (userInput.length == exercise.missingLetters.size) {
                        onAnswer(userInput)
                    }
                }
            ),
            enabled = !isAnswered
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Check button
        Button(
            onClick = { onAnswer(userInput) },
            enabled = userInput.length == exercise.missingLetters.size && !isAnswered,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF58CC02),
                disabledContainerColor = colors.textSecondary.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "CHECK",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Exercise result feedback
 */
@Composable
fun ExerciseResultFeedback(
    isCorrect: Boolean,
    correctAnswer: String,
    xpEarned: Int,
    onContinue: () -> Unit
) {
    val colors = LocalGameColors.current
    
    // Animation
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "resultScale"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(
                if (isCorrect) Color(0xFF58CC02) else Color(0xFFFF4B4B)
            )
            .padding(24.dp)
            .scale(scale)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                
                Text(
                    text = if (isCorrect) "Correct!" else "Incorrect",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                if (isCorrect) {
                    Text(
                        text = "+$xpEarned XP",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
            
            if (!isCorrect) {
                Text(
                    text = "Correct answer: $correctAnswer",
                    fontSize = 16.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = if (isCorrect) Color(0xFF58CC02) else Color(0xFFFF4B4B)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "CONTINUE",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Session progress bar
 */
@Composable
fun SessionProgressBar(
    current: Int,
    total: Int,
    modifier: Modifier = Modifier
) {
    val colors = LocalGameColors.current
    val progress = if (total > 0) current.toFloat() / total else 0f
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "sessionProgress"
    )
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Close button placeholder
        IconButton(onClick = { /* Handle close */ }) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = colors.textSecondary
            )
        }
        
        // Progress bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(colors.textSecondary.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF58CC02))
            )
        }
        
        // Progress text
        Text(
            text = "$current/$total",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary
        )
    }
}

/**
 * Session complete screen
 */
@Composable
fun SessionCompleteScreen(
    result: SessionResult,
    onContinue: () -> Unit,
    onPracticeAgain: () -> Unit
) {
    val colors = LocalGameColors.current
    
    // Celebration animation
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val celebrationScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "celebrationScale"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Celebration icon
        val celebrationEmoji = when {
            result.isPerfect -> "🎉"
            result.accuracy >= 0.8f -> null  // будет StarIcon
            result.accuracy >= 0.6f -> "👍"
            else -> "💪"
        }
        if (celebrationEmoji != null) {
            Text(
                text = celebrationEmoji,
                fontSize = 80.sp,
                modifier = Modifier.scale(celebrationScale)
            )
        } else {
            StarIcon(
                size = 80.dp,
                modifier = Modifier.scale(celebrationScale)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = when {
                result.isPerfect -> "Perfect!"
                result.accuracy >= 0.8f -> "Great job!"
                result.accuracy >= 0.6f -> "Good work!"
                else -> "Keep practicing!"
            },
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Stats card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = colors.cardBackground)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // XP earned
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LightningIcon(size = 26.dp)
                        Text(
                            text = "XP Earned",
                            fontSize = 16.sp,
                            color = colors.textSecondary
                        )
                    }
                    Text(
                        text = "+${result.totalXP}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF58CC02)
                    )
                }
                
                HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.2f))
                
                // Accuracy
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🎯", fontSize = 24.sp)
                        Text(
                            text = "Accuracy",
                            fontSize = 16.sp,
                            color = colors.textSecondary
                        )
                    }
                    Text(
                        text = "${(result.accuracy * 100).toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
                
                // Correct answers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "✅", fontSize = 24.sp)
                        Text(
                            text = "Correct",
                            fontSize = 16.sp,
                            color = colors.textSecondary
                        )
                    }
                    Text(
                        text = "${result.correctCount}/${result.exercises.size}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Buttons
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF58CC02)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "CONTINUE",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (!result.isPerfect) {
            OutlinedButton(
                onClick = onPracticeAgain,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "PRACTICE AGAIN",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }
        }
    }
}
