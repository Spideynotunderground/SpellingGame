package com.app.gectyping

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding

// Duolingo-style colors
private val DuoGreen = Color(0xFF58CC02)
private val DuoGreenDark = Color(0xFF46A302)
private val DuoBlue = Color(0xFF1CB0F6)
private val DuoPurple = Color(0xFFA560E8)
private val DuoOrange = Color(0xFFFF9600)
private val DuoRed = Color(0xFFFF4B4B)
private val DuoYellow = Color(0xFFFFDE00)
private val DuoBg = Color(0xFF27304E)
private val DuoBgLight = Color(0xFF27304E)

data class OnboardingPage(
    val emoji: String,
    val title: String,
    val description: String,
    val accentColor: Color,
    val iconRes: Int? = null
)

private val onboardingPages = listOf(
    OnboardingPage(
        emoji = "🎯",
        title = "Master English Spelling",
        description = "Learn to spell 1000+ words correctly with fun exercises and instant feedback",
        accentColor = DuoGreen,
        iconRes = R.drawable.icons8_goal_100
    ),
    OnboardingPage(
        emoji = "🎧",
        title = "Listen & Type",
        description = "Hear native pronunciation and practice spelling words you hear every day",
        accentColor = DuoBlue,
        iconRes = R.drawable.icons8_airpods_pro_max_100
    ),
    OnboardingPage(
        emoji = "📚",
        title = "Learn by Topics",
        description = "Study vocabulary organized by themes: Education, Business, Health, Technology and more",
        accentColor = DuoPurple,
        iconRes = R.drawable.icons8_books_96
    ),
    OnboardingPage(
        emoji = "🏆",
        title = "Earn Rewards",
        description = "Collect stars, diamonds, and crowns as you progress. Unlock new levels and achievements!",
        accentColor = DuoOrange,
        iconRes = R.drawable.icons8_trophy_100
    ),
    OnboardingPage(
        emoji = "🔥",
        title = "Build Your Streak",
        description = "Practice daily to maintain your streak and become a spelling champion!",
        accentColor = DuoRed,
        iconRes = R.drawable.icons8_fire_96
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onComplete: (String) -> Unit
) {
    var showNameInput by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("") }
    val pagerState = rememberPagerState(pageCount = { onboardingPages.size })
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(DuoBg, DuoBgLight, DuoBg)
                )
            )
    ) {
        AnimatedVisibility(
            visible = !showNameInput,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Pager with onboarding pages
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    OnboardingPageContent(
                        page = onboardingPages[page],
                        isActive = pagerState.currentPage == page
                    )
                }
                
                // Page indicators
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(onboardingPages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 12.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) onboardingPages[index].accentColor
                                    else Color.White.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
                
                // Navigation buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Skip button
                    TextButton(
                        onClick = { showNameInput = true }
                    ) {
                        Text(
                            "Skip",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 16.sp
                        )
                    }
                    
                    // Next / Get Started button
                    val isLastPage = pagerState.currentPage == onboardingPages.size - 1
                    Button(
                        onClick = {
                            if (isLastPage) {
                                showNameInput = true
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = onboardingPages[pagerState.currentPage].accentColor
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp))
                    ) {
                        Text(
                            text = if (isLastPage) "Get Started!" else "Next",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        }
        
        // Name input screen
        AnimatedVisibility(
            visible = showNameInput,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut()
        ) {
            NameInputScreen(
                userName = userName,
                onNameChange = { userName = it },
                onComplete = {
                    if (userName.isNotBlank()) {
                        onComplete(userName.trim())
                    }
                },
                onBack = { showNameInput = false }
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(
    page: OnboardingPage,
    isActive: Boolean
) {
    // Animations
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.8f),
        label = "scale"
    )
    
    val infiniteTransition = rememberInfiniteTransition(label = "float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated emoji with glow effect
        Box(
            modifier = Modifier
                .scale(scale)
                .offset(y = floatOffset.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow background
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                page.accentColor.copy(alpha = 0.4f),
                                page.accentColor.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // PNG icon (preferred) or fallback to emoji
            if (page.iconRes != null) {
                Image(
                    painter = painterResource(id = page.iconRes),
                    contentDescription = page.title,
                    modifier = Modifier.size(120.dp)
                )
            } else if (page.emoji == "🔥") {
                FireIcon(size = 100.dp)
            } else {
                Text(
                    text = page.emoji,
                    fontSize = 100.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Title
        Text(
            text = page.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description
        Text(
            text = page.description,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NameInputScreen(
    userName: String,
    onNameChange: (String) -> Unit,
    onComplete: () -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    
    // Floating animation for mascot
    val infiniteTransition = rememberInfiniteTransition(label = "mascot")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascotFloat"
    )
    
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .imePadding()
            .navigationBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Mascot with glow
        Box(
            modifier = Modifier.offset(y = floatOffset.dp),
            contentAlignment = Alignment.Center
        ) {
            // Glow
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                DuoGreen.copy(alpha = 0.4f),
                                DuoGreen.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
            
            // Owl mascot
            Image(
                painter = painterResource(id = R.drawable.icons8_owl_96),
                contentDescription = "Mascot",
                modifier = Modifier.size(120.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Welcome text
        Text(
            text = "What's your name?",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "We'll personalize your learning experience",
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Name input field
        OutlinedTextField(
            value = userName,
            onValueChange = { if (it.length <= 20) onNameChange(it) },
            placeholder = {
                Text(
                    "Enter your name",
                    color = Color.White.copy(alpha = 0.4f)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    if (userName.isNotBlank()) onComplete()
                }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = DuoGreen,
                focusedBorderColor = DuoGreen,
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.05f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Continue button
        Button(
            onClick = onComplete,
            enabled = userName.isNotBlank(),
            colors = ButtonDefaults.buttonColors(
                containerColor = DuoGreen,
                disabledContainerColor = DuoGreen.copy(alpha = 0.3f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(
                    elevation = if (userName.isNotBlank()) 8.dp else 0.dp,
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Text(
                text = "Let's Go! 🚀",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Back button
        TextButton(onClick = onBack) {
            Text(
                "← Back",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        }
    }
}
