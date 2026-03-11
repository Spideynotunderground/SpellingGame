package com.app.gectyping

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.Build
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
// Old TTS imports removed - using GoogleCloudTTSManager instead
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.ui.geometry.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.app.gectyping.ui.theme.*
import java.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.sin
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 🖥️ FULLSCREEN IMMERSIVE MODE - Hide status bar and navigation bar
        enableImmersiveMode()

        // Initialize Google Cloud TTS Manager
        GoogleCloudTTSManager.initialize(this)

        // Track navigation bar height using window insets so we can
        // push the custom keyboard above the system navigation buttons.
        val navigationBarHeightState = mutableIntStateOf(0)
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { v, insets ->
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
            navigationBarHeightState.intValue = systemBarsInsets.bottom
            insets
        }

        // Read persisted theme preference (default = SPACE)
        val settingsPrefs = getSharedPreferences("spelling_game", MODE_PRIVATE)
        val savedWorldId = settingsPrefs.getString("theme_world", ThemeWorld.SPACE.name) ?: ThemeWorld.SPACE.name
        val themeWorldState = mutableStateOf(ThemeWorld.fromId(savedWorldId))

        setContent {
            val currentWorld = themeWorldState.value
            GecTypingTheme(
                darkTheme = (worldPalettes[currentWorld] ?: SpacePalette).isDark,
                themeWorld = currentWorld,
                dynamicColor = false
            ) {
                val gameColors = com.app.gectyping.ui.theme.LocalGameColors.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = gameColors.background
                ) {
                    SpellingGameScreen(
                        context = this,
                        navigationBarHeightPx = navigationBarHeightState.intValue,
                        isDarkTheme = gameColors.isDark,
                        onThemeChange = { /* legacy — ignored, use onWorldChange */ },
                        themeWorld = currentWorld,
                        onWorldChange = { world ->
                            themeWorldState.value = world
                            settingsPrefs.edit().putString("theme_world", world.name).apply()
                            AchievementManager.onThemeSwapped(this)
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        GoogleCloudTTSManager.shutdown()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }

    // 🖥️ FULLSCREEN IMMERSIVE MODE - Hide status bar and navigation bar
    @Suppress("DEPRECATION")
    private fun enableImmersiveMode() {
        // Render into display cutout area (notch) on API 28+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.also {
                it.layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.let { controller ->
                controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            window.decorView.systemUiVisibility = (
                    android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }
}

private fun performSuccessHaptic(context: Context, enabled: Boolean = true) {
    if (!enabled) return
    // Light, short vibration (50ms) - crisp and satisfying success feedback
    try {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Light amplitude (80 out of 255) for a subtle, pleasant tap
            val effect = VibrationEffect.createOneShot(50, 80)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
    } catch (e: Exception) {
        // Silently fail if vibration not available
    }
}

private fun performErrorHaptic(context: Context, enabled: Boolean = true) {
    if (!enabled) return
    // Heavy double vibration - distinct "thump" error feedback
    // Pattern: 100ms on, 50ms off, 100ms on (total ~250ms)
    try {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vm?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

        if (vibrator == null || !vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Heavy amplitude (255 max) for strong error feedback
            val timings = longArrayOf(0, 100, 50, 100)
            val amplitudes = intArrayOf(0, 255, 0, 255)
            val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
            vibrator.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
        }
    } catch (e: Exception) {
        // Silently fail if vibration not available
    }
}

private const val PREFS_NAME = "spelling_game"

private const val MAX_LIVES = 3

private const val KEY_LIVES = "lives"

private const val KEY_CURRENT_LEVEL = "current_level"
private const val KEY_LEVEL_PROGRESS = "level_progress"
private const val KEY_TOTAL_STARS = "total_stars_earned"

private const val KEY_SELECTED_AVATAR = "selected_avatar"
private const val KEY_OWNED_AVATARS = "owned_avatars"
const val DEFAULT_AVATAR_ID = "space"

// Star-based level progression requirements
private val LEVEL_UNLOCK_STARS = mapOf(
    1 to 0,      // Level 1 is free
    2 to 35,     // Need 35 stars for Level 2
    3 to 72,     // Need 72 stars for Level 3
    4 to 105,    // Need 105 stars for Level 4
    5 to 170,    // Need 170 stars for Level 5
    6 to 250,    // Need 250 stars for Level 6
    7 to 350     // Need 350 stars for Level 7 (endless)
)

private const val REVIVE_COST_STARS = 10  // Cost to buy one heart (unlimited purchases allowed)

// Dynamic words per session - uses real word count from each level
private fun getLevelProgressTarget(level: Int): Int {
    return WordList.getWordCountForLevel(level)
}

// Base total time per level - Level 1 starts with 100s, decreases per level
// Each word should roughly equal 10-12 seconds of available time
private fun getBaseTotalTime(level: Int): Float = when (level) {
    1 -> 100f    // 100 seconds for Level 1
    2 -> 90f     // 90 seconds for Level 2
    3 -> 80f     // 80 seconds for Level 3
    4 -> 70f     // 70 seconds for Level 4
    5 -> 60f     // 60 seconds for Level 5 (endless, hardest)
    else -> 100f
}

// Max total time cap per level
private fun getMaxTotalTime(level: Int): Float = when (level) {
    1 -> 150f    // 150 seconds max for Level 1
    2 -> 130f    // 130 seconds max for Level 2
    3 -> 110f    // 110 seconds max for Level 3
    4 -> 100f    // 100 seconds max for Level 4
    5 -> 90f     // 90 seconds max for Level 5 (endless)
    else -> 150f
}

data class AvatarItem(
    val id: String,
    val title: String,
    val emoji: String,
    val priceDiamonds: Int,
    val iconRes: Int? = null,
    val isAnimated: Boolean = false
)

val AVATARS = listOf(
    AvatarItem(id = "space",   title = "Astronaut",     emoji = "🚀", priceDiamonds = 0,   iconRes = R.drawable.icons8_astronaut_helmet_96),
    AvatarItem(id = "forest",  title = "Forest Elf",    emoji = "🧝", priceDiamonds = 350,  iconRes = R.drawable.elf, isAnimated = true),
    AvatarItem(id = "ocean",   title = "Deep Diver",    emoji = "🤿", priceDiamonds = 100, iconRes = R.drawable.icons8_snorkel_96),
    AvatarItem(id = "gaming",  title = "Pro Gamer",     emoji = "🎮", priceDiamonds = 300, iconRes = R.drawable.sonic, isAnimated = true),
    AvatarItem(id = "desert",  title = "Sand Nomad",    emoji = "🏜", priceDiamonds = 100, iconRes = R.drawable.desert),
    AvatarItem(id = "cyber",   title = "Cyber Agent",   emoji = "🤖", priceDiamonds = 250, iconRes = R.drawable.pixel_robot, isAnimated = true),
    AvatarItem(id = "owl",     title = "Genius Owl",    emoji = "🦉", priceDiamonds = 80,  iconRes = R.drawable.icons8_owl_96),
    AvatarItem(id = "sakura",  title = "Sakura Spirit", emoji = "🌸", priceDiamonds = 120, iconRes = R.drawable.icons8_flower_96),
    AvatarItem(id = "ice",     title = "Ice Walker",    emoji = "❄",  priceDiamonds = 100, iconRes = R.drawable.icons8_snowflake_96),
    AvatarItem(id = "magma",   title = "Volcano Lord",  emoji = "🌋", priceDiamonds = 150, iconRes = R.drawable.volcano),
)

fun avatarById(id: String): AvatarItem =
    AVATARS.firstOrNull { it.id == id } ?: AVATARS.first()

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SpellingGameScreen(
    context: Context,
    navigationBarHeightPx: Int,
    isDarkTheme: Boolean = true,
    onThemeChange: (Boolean) -> Unit = {},
    themeWorld: ThemeWorld = ThemeWorld.BOG,
    onWorldChange: (ThemeWorld) -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }

    // ⚙️ Settings state (persisted)
    var vibrationEnabled by remember { mutableStateOf(prefs.getBoolean("vibration_enabled", true)) }
    var soundEnabled by remember { mutableStateOf(prefs.getBoolean("sound_enabled", true)) }
    var musicEnabled by remember { mutableStateOf(prefs.getBoolean("music_enabled", true)) }
    var masterVolume by remember { mutableFloatStateOf(prefs.getFloat("master_volume", 1.0f)) }
    // Backward compatibility: old builds saved music volume under key "volume"
    var musicVolume by remember { mutableFloatStateOf(prefs.getFloat("music_volume", prefs.getFloat("volume", 0.28f))) }
    var sfxVolume by remember { mutableFloatStateOf(prefs.getFloat("sfx_volume", 1.0f)) }
    var voiceVolume by remember { mutableFloatStateOf(prefs.getFloat("voice_volume", 1.0f)) }
    var ttsPitch by remember { mutableFloatStateOf(prefs.getFloat("tts_pitch", -1.0f)) }
    var ttsRate by remember { mutableFloatStateOf(prefs.getFloat("tts_rate", 1.0f)) }
    var showSettings by remember { mutableStateOf(false) }

    // 🏁 Screen flow: SplashScreen → NameDialog → MainMenu → LoadingScreen → Game
    var showSplash by remember { mutableStateOf(true) }
    var isOnline by remember { mutableStateOf(true) }

    // Check connectivity on every resume
    fun checkConnectivity() {
        val cm = context.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)
        isOnline = caps != null &&
            (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
             caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
             caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
    }

    // Re-check every time the composable enters composition and every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            checkConnectivity()
            kotlinx.coroutines.delay(3000)
        }
    }
    var showMainMenu by remember { mutableStateOf(true) }

    var currentLevel by remember {
        mutableIntStateOf(prefs.getInt(KEY_CURRENT_LEVEL, 1).coerceIn(1, 7))
    }

    var levelProgressCount by remember {
        mutableIntStateOf(prefs.getInt(KEY_LEVEL_PROGRESS, 0).coerceAtLeast(0))
    }

    fun buildLevelDeck(level: Int): MutableList<WordWithSynonyms> {
        val base = WordList.wordsForLevel(level)
        val deck = mutableListOf<WordWithSynonyms>()
        if (base.isEmpty()) {
            deck.add(WordWithSynonyms("", emptyList()))
            return deck
        }

        val targetSize = getLevelProgressTarget(level)
        while (deck.size < targetSize) {
            deck.addAll(base.shuffled())
        }
        val clipped = deck.take(targetSize).toMutableList()
        Collections.shuffle(clipped)
        return clipped
    }

    var levelWords by remember {
        mutableStateOf(buildLevelDeck(currentLevel))
    }
    var wordIndexInLevel by remember {
        mutableIntStateOf(
            prefs.getInt("word_index_in_level", 0).coerceIn(0, (levelWords.size - 1).coerceAtLeast(0))
        )
    }
    var currentWord by remember {
        mutableStateOf(
            levelWords.getOrElse(prefs.getInt("word_index_in_level", 0).coerceIn(0, (levelWords.size - 1).coerceAtLeast(0))) {
                levelWords.firstOrNull() ?: WordWithSynonyms("", emptyList())
            }
        )
    }
    var pendingLevelUp by remember {
        val nextLevelStars = LEVEL_UNLOCK_STARS[currentLevel + 1] ?: Int.MAX_VALUE
        mutableStateOf(currentLevel < 5 && prefs.getInt(KEY_TOTAL_STARS, 0) >= nextLevelStars)
    }

    // First-time welcome + ask name (saved locally)
    var playerName by remember { mutableStateOf(prefs.getString("player_name", "") ?: "") }
    var showNameDialog by remember { mutableStateOf(playerName.isBlank()) }
    var greetedThisSession by remember { mutableStateOf(false) }

    // Loading screen: shown after main menu, before game starts
    var showLoadingScreen by remember { mutableStateOf(false) }

    var userInput by remember { mutableStateOf("") }
    var stars by remember { mutableIntStateOf(prefs.getInt("stars", prefs.getInt("fires", 0))) }
    var diamonds by remember { mutableIntStateOf(prefs.getInt("diamonds", 0)) }
    var xp by remember { mutableIntStateOf(prefs.getInt("xp", 0)) }

    // ❤️ Health points (lives)
    var lives by remember {
        mutableIntStateOf(prefs.getInt(KEY_LIVES, MAX_LIVES).coerceIn(0, MAX_LIVES))
    }
    var showGameOverDialog by remember { mutableStateOf(false) }
    var showReviveDialog by remember { mutableStateOf(false) }
    var lastLifeLossReason by remember { mutableStateOf("") }

    // Total stars earned (for level progression)
    var totalStarsEarned by remember {
        mutableIntStateOf(prefs.getInt(KEY_TOTAL_STARS, 0))
    }

    // ⏱️ Single Timer System - Only Total Time (no per-word timer)
    var overallTimeLeft by remember { mutableFloatStateOf(getBaseTotalTime(currentLevel)) }
    var maxOverallTime by remember { mutableFloatStateOf(getMaxTotalTime(currentLevel)) }

    // 🎨 Visual feedback animations
    var showRedInputFlash by remember { mutableStateOf(false) }  // Red input border flash on incorrect
    var lastCorrectAnswer by remember { mutableStateOf("") }  // Store correct answer to display on wrong input
    var showCorrectAnswerLabel by remember { mutableStateOf(false) }  // Show "Correct answer:" label

    // 🔄 Failed words repeat system with 2x bonus
    // Use SnapshotStateList so that add/remove mutations trigger Compose recomposition
    val failedWords = remember { androidx.compose.runtime.mutableStateListOf<WordWithSynonyms>() }
    var isRetryWord by remember { mutableStateOf(false) }  // Current word is a retry (failed before)
    var showBonusIndicator by remember { mutableStateOf(false) }  // Show "×2 Bonus!" indicator

    // 📝 Error-review bonus level — mandatory after completing 35 words if mistakes exist
    var isInErrorReview by remember { mutableStateOf(false) }
    var errorReviewIndex by remember { mutableIntStateOf(0) }

    // 🛒 Customization
    var selectedAvatarId by remember {
        mutableStateOf(
            prefs.getString(KEY_SELECTED_AVATAR, DEFAULT_AVATAR_ID) ?: DEFAULT_AVATAR_ID
        )
    }
    var ownedAvatars by remember {
        mutableStateOf(
            prefs.getStringSet(KEY_OWNED_AVATARS, setOf(DEFAULT_AVATAR_ID))
                ?.toSet()
                ?: setOf(DEFAULT_AVATAR_ID)
        )
    }

    // 🎨 Theme world shop state
    var ownedWorlds by remember {
        mutableStateOf(
            prefs.getStringSet("owned_worlds", setOf(ThemeWorld.BOG.name, ThemeWorld.SPACE.name))
                ?.toSet() ?: setOf(ThemeWorld.BOG.name, ThemeWorld.SPACE.name)
        )
    }

    // 🎵 Music shop state
    var ownedMusicTracks by remember {
        mutableStateOf(
            prefs.getStringSet("owned_music_tracks", emptySet())
                ?.toSet() ?: emptySet()
        )
    }
    var selectedMusicTrackId by remember {
        mutableStateOf(prefs.getString("selected_music_track", "default") ?: "default")
    }

    var comboMultiplier by remember { mutableIntStateOf(1) }
    var isCorrect by remember { mutableStateOf<Boolean?>(null) }
    var showSynonyms by remember { mutableStateOf(false) }
    var showHint by remember { mutableStateOf(false) }
    // Separate hintWord state — must be cleared BEFORE currentWord changes
    // to prevent the new word from flashing in the hint field for one frame
    var hintWord by remember { mutableStateOf("") }
    var bgFlashColor by remember { mutableStateOf(Color.Transparent) }
    var bgFlashAlpha by remember { mutableFloatStateOf(0f) }
    var inputColor by remember { mutableStateOf(Color.Transparent) }

    var starPulseKey by remember { mutableIntStateOf(0) }

    var fetchedSynonyms by remember { mutableStateOf<List<String>>(emptyList()) }

    // Game flow control states
    var isInputDisabled by remember { mutableStateOf(false) }
    var hasAttemptedCheck by remember { mutableStateOf(false) }
    var hasAwardedForCurrentWord by remember { mutableStateOf(false) }

    // Level-to-level transition (progress bar overlay)
    var isLevelTransitioning by remember { mutableStateOf(false) }
    var levelTransitionProgress by remember { mutableFloatStateOf(0f) }
    var levelTransitionFrom by remember { mutableIntStateOf(1) }
    var levelTransitionTo by remember { mutableIntStateOf(1) }

    // Scroll + insets handling for when the keyboard / custom keyboard is visible
    val scrollState = rememberScrollState()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    var isTextFieldFocused by remember { mutableStateOf(false) }
    // Animated bottom padding so the custom keyboard never covers the Check/Next buttons
    val customKeyboardHeight = 260.dp   // approximate height of CustomKeyboard
    val keyboardBottomPadding by animateDpAsState(
        targetValue = if (isTextFieldFocused) customKeyboardHeight else 0.dp,
        animationSpec = tween(200),
        label = "kbPad"
    )
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    // Auto-play when a new word appears — normal speed (1.0)
    fun speakWord(word: String) {
        if (!soundEnabled) return
        GoogleCloudTTSManager.speak(word, slow = false)
    }

    // Replay when player taps the speaker button — slower (0.90) for clarity
    fun replayWord(word: String) {
        if (!soundEnabled) return
        GoogleCloudTTSManager.speak(word, slow = true)
    }

    fun speakMessage(message: String) {
        if (!soundEnabled) return
        GoogleCloudTTSManager.speak(message)
    }

    // Apply saved voice settings to TTS manager on startup
    LaunchedEffect(Unit) {
        GoogleCloudTTSManager.updateVoiceSettings(
            rate = prefs.getFloat("tts_rate", 1.0f),
            pitchSemitones = prefs.getFloat("tts_pitch", -1.0f)
        )
    }

    // Re-apply voice settings whenever the user changes them in Settings
    LaunchedEffect(ttsRate, ttsPitch) {
        GoogleCloudTTSManager.updateVoiceSettings(
            rate = ttsRate,
            pitchSemitones = ttsPitch
        )
    }

    // Prompt once when the name dialog appears
    LaunchedEffect(showNameDialog) {
        if (showNameDialog) {
            speakMessage("Welcome! What's your name?")
        }
    }

    // Optional: greet returning players once per app run
    LaunchedEffect(playerName, showNameDialog, showMainMenu, showLoadingScreen) {
        if (!showNameDialog && !showMainMenu && !showLoadingScreen && !greetedThisSession && playerName.isNotBlank()) {
            greetedThisSession = true
            Toast.makeText(context, "Welcome back, $playerName!", Toast.LENGTH_SHORT).show()
        }
    }

    // Auto-speak word when word changes — только currentWord.word как ключ
    // showNameDialog/showMainMenu/showLoadingScreen проверяются внутри, а не как ключи,
    // чтобы LaunchedEffect не перезапускался при их изменении и не произносил слово дважды
    LaunchedEffect(currentWord.word) {
        if (showNameDialog || showMainMenu || showLoadingScreen) return@LaunchedEffect
        kotlinx.coroutines.delay(150)
        speakWord(currentWord.word)
    }

    // Fetch synonyms separately — не мешаем speak
    LaunchedEffect(currentWord.word) {
        if (showNameDialog || showMainMenu || showLoadingScreen) return@LaunchedEffect
        fetchedSynonyms = SynonymRepository.getOrFetchFour(context, currentWord.word)
    }

    // If the app is resumed with 0 lives persisted, show Game Over immediately.
    LaunchedEffect(showNameDialog, showMainMenu, showLoadingScreen) {
        if (!showNameDialog && !showMainMenu && !showLoadingScreen && lives <= 0) {
            showGameOverDialog = true
            isInputDisabled = true
            isTextFieldFocused = false
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }


    // Streak check result for dialogs
    var showStreakLostDialog by remember { mutableStateOf(false) }
    var lostStreakAmount by remember { mutableIntStateOf(0) }
    var showStreakFreezeUsedDialog by remember { mutableStateOf(false) }
    var streakAfterFreeze by remember { mutableIntStateOf(0) }
    var freezesLeftAfterUse by remember { mutableIntStateOf(0) }
    
    // Ensure defaults are always owned
    LaunchedEffect(Unit) {
        if (!ownedWorlds.contains(ThemeWorld.BOG.name)) {
            ownedWorlds = ownedWorlds + ThemeWorld.BOG.name
        }
        if (!ownedWorlds.contains(ThemeWorld.SPACE.name)) {
            ownedWorlds = ownedWorlds + ThemeWorld.SPACE.name
        }
        if (!ownedAvatars.contains(DEFAULT_AVATAR_ID)) {
            ownedAvatars = ownedAvatars + DEFAULT_AVATAR_ID
        }
        // Track daily login for achievements
        AchievementManager.onDailyLogin(context)
        
        // Check streak status on app open
        try {
            when (val result = StreakManager.onAppOpen(context)) {
                is StreakCheckResult.StreakLost -> {
                    if (result.lostStreak > 0) {
                        lostStreakAmount = result.lostStreak
                        showStreakLostDialog = true
                    }
                }
                is StreakCheckResult.FreezeUsed -> {
                    streakAfterFreeze = result.streak
                    freezesLeftAfterUse = result.freezesLeft
                    showStreakFreezeUsedDialog = true
                }
                else -> { /* No action needed */ }
            }
        } catch (e: Exception) {
            // Silently handle streak check errors
        }
    }

    LaunchedEffect(
        stars,
        diamonds,
        xp,
        lives,
        currentLevel,
        levelProgressCount,
        totalStarsEarned,
        selectedAvatarId,
        ownedAvatars,
        ownedWorlds,
        ownedMusicTracks,
        selectedMusicTrackId,
        vibrationEnabled,
        soundEnabled,
        musicEnabled,
        masterVolume,
        musicVolume,
        sfxVolume,
        ttsPitch,
        ttsRate
    ) {
        // Debounce: wait 500ms for state to settle before writing to disk.
        // This prevents multiple sequential writes when several values change at once
        // (e.g. stars + diamonds + totalStarsEarned all change on a correct answer).
        kotlinx.coroutines.delay(500L)
        prefs.edit()
            .putInt("stars", stars)
            .putInt("diamonds", diamonds)
            .putInt("xp", xp)
            .putInt(KEY_LIVES, lives)
            .putInt(KEY_CURRENT_LEVEL, currentLevel)
            .putInt(KEY_LEVEL_PROGRESS, levelProgressCount)
            .putInt(KEY_TOTAL_STARS, totalStarsEarned)
            .putString(KEY_SELECTED_AVATAR, selectedAvatarId)
            .putStringSet(KEY_OWNED_AVATARS, ownedAvatars.toMutableSet())
            .putStringSet("owned_worlds", ownedWorlds.toMutableSet())
            .putStringSet("owned_music_tracks", ownedMusicTracks.toMutableSet())
            .putString("selected_music_track", selectedMusicTrackId)
            .putBoolean("vibration_enabled", vibrationEnabled)
            .putBoolean("sound_enabled", soundEnabled)
            .putBoolean("music_enabled", musicEnabled)
            .putFloat("master_volume", masterVolume)
            .putFloat("music_volume", musicVolume)
            .putFloat("sfx_volume", sfxVolume)
            // Backward compatibility for older builds that only used a single music volume slider
            .putFloat("volume", musicVolume)
            .putFloat("tts_pitch", ttsPitch)
            .putFloat("tts_rate", ttsRate)
            .apply()
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, context) {
        // Attach audio graph and apply persisted state at startup.
        AppAudioManager.attach(context)
        AppAudioManager.setMixerVolumes(masterVolume, musicVolume, sfxVolume)
        AppAudioManager.setToggles(soundEnabled, musicEnabled)
        AppAudioManager.applySelectedTrack(selectedMusicTrackId)

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    AppAudioManager.attach(context)
                    AppAudioManager.setMixerVolumes(masterVolume, musicVolume, sfxVolume)
                    AppAudioManager.setToggles(soundEnabled, musicEnabled)
                    AppAudioManager.applySelectedTrack(selectedMusicTrackId)
                    try { StreakManager.startSession(context) } catch (_: Exception) {}
                }
                Lifecycle.Event.ON_PAUSE -> {
                    // Stop audio when app is backgrounded.
                    MusicShopManager.stopPreview()
                    BackgroundMusicPlayer.stop()
                    try { StreakManager.endSession(context) } catch (_: Exception) {}
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            MusicShopManager.stopPreview()
            BackgroundMusicPlayer.stop()
        }
    }

    // Apply mixer changes live (log-scale handled by AppAudioMixer).
    LaunchedEffect(masterVolume, musicVolume, sfxVolume) {
        AppAudioManager.setMixerVolumes(masterVolume, musicVolume, sfxVolume)
    }

    // React to toggles at runtime.
    LaunchedEffect(soundEnabled, musicEnabled) {
        AppAudioManager.setToggles(soundEnabled, musicEnabled)
        if (!soundEnabled) GoogleCloudTTSManager.stop()
    }

    // React to selected track changes (e.g., after purchase / restore).
    LaunchedEffect(selectedMusicTrackId) {
        AppAudioManager.applySelectedTrack(selectedMusicTrackId)
    }

    // TTS pitch/rate not needed for pre-recorded audio



    fun resetCurrentLevelProgress() {
        userInput = ""
        isCorrect = null
        inputColor = Color.Transparent
        showSynonyms = false
        // Clear hintWord FIRST, then hide hint — prevents next word from flashing in hint field
        hintWord = ""
        showHint = false
        bgFlashAlpha = 0f
        // keep fetchedSynonyms, so UI doesn't flash
        isInputDisabled = false
        hasAttemptedCheck = false
        hasAwardedForCurrentWord = false
        // Reset visual feedback states
        showRedInputFlash = false
        showCorrectAnswerLabel = false
        lastCorrectAnswer = ""
        showBonusIndicator = false
    }

    // Check if player can unlock a specific level
    fun canUnlockLevel(level: Int): Boolean {
        val required = LEVEL_UNLOCK_STARS[level] ?: Int.MAX_VALUE
        return totalStarsEarned >= required
    }

    // Get progress towards unlocking next level
    fun getUnlockProgress(targetLevel: Int): Float {
        val required = LEVEL_UNLOCK_STARS[targetLevel] ?: return 1f
        if (required == 0) return 1f
        return (totalStarsEarned.toFloat() / required).coerceIn(0f, 1f)
    }

    // ⚡ Background Flash — green for correct, red for error
    fun flashSuccess() {
        bgFlashColor = Color(0xFF4ADE80)  // Green
        bgFlashAlpha = 0.30f
        coroutineScope.launch {
            kotlinx.coroutines.delay(200)
            bgFlashAlpha = 0f
        }
    }

    fun flashError() {
        bgFlashColor = Color(0xFFF87171)  // Soft red
        bgFlashAlpha = 0.30f
        coroutineScope.launch {
            kotlinx.coroutines.delay(200)
            bgFlashAlpha = 0f
        }
    }

    fun levelRewards(level: Int): Pair<Int, Int> {
        return when (level.coerceIn(1, 7)) {
            1 -> 5 to 1
            2 -> 10 to 1
            3 -> 15 to 2
            4 -> 20 to 2
            5 -> 25 to 3
            6 -> 30 to 3
            7 -> 35 to 4
            else -> 5 to 1
        }
    }

    fun levelProgressFraction(): Float {
        // Progress towards next level unlock based on stars
        if (currentLevel >= 7) return 1f  // Endless mode - always full
        val nextLevel = currentLevel + 1
        return getUnlockProgress(nextLevel)
    }

    fun loseLife(reason: String) {
        if (lives <= 0) return
        lives--
        lastLifeLossReason = reason
        Toast.makeText(context, reason, Toast.LENGTH_SHORT).show()

        if (lives <= 0) {
            // Check if player can buy a life (10 stars, unlimited purchases)
            val canRevive = stars >= REVIVE_COST_STARS
            if (canRevive) {
                showReviveDialog = true
            } else {
                showGameOverDialog = true
            }
            isInputDisabled = true
            isTextFieldFocused = false
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
        }
    }

    fun loadLevel(level: Int) {
        val l = level.coerceIn(1, 7)
        currentLevel = l
        levelWords = buildLevelDeck(l)
        wordIndexInLevel = 0
        currentWord = levelWords.firstOrNull() ?: WordWithSynonyms("", emptyList())
        fetchedSynonyms = emptyList()
        levelProgressCount = 0
        pendingLevelUp = false
        comboMultiplier = 1
        totalStarsEarned = 0  // ALL levels start from 0 stars
        // Reset error review state when loading a new level
        failedWords.clear()
        isInErrorReview = false
        errorReviewIndex = 0
        isRetryWord = false
    }

    // Handle overall time running out
    fun handleOverallTimeout() {
        if (showGameOverDialog || showReviveDialog) return
        lastLifeLossReason = "⏱️ Time's up!"

        // Check if player can buy a life (10 stars, unlimited purchases)
        val canRevive = stars >= REVIVE_COST_STARS
        if (canRevive) {
            showReviveDialog = true
        } else {
            showGameOverDialog = true
        }
        isInputDisabled = true
        isTextFieldFocused = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
    }

    // Buy one heart (costs 10 stars - unlimited purchases allowed)
    fun useRevive(): Boolean {
        if (stars < REVIVE_COST_STARS) return false

        stars -= REVIVE_COST_STARS
        lives = 1  // Restore one life
        showReviveDialog = false
        showGameOverDialog = false
        isInputDisabled = false
        // Reset word state so player can reattempt the word that killed them
        resetCurrentLevelProgress()

        // Give 30 seconds bonus time on revive
        overallTimeLeft = (overallTimeLeft + 30f).coerceAtMost(maxOverallTime)

        Toast.makeText(context, "❤️ +1 Life! +30 seconds", Toast.LENGTH_SHORT).show()
        return true
    }

    // Decline revive - go to game over
    fun declineRevive() {
        showReviveDialog = false
        showGameOverDialog = true
    }

    fun handleRestartAfterGameOver() {
        // Stay on same level, but reset progress to 0
        lives = MAX_LIVES
        showGameOverDialog = false
        showReviveDialog = false
        levelProgressCount = 0  // Reset level progress to 0
        totalStarsEarned = 0  // ALL levels start from 0 stars
        pendingLevelUp = false
        comboMultiplier = 1

        // Reset timers for current level
        overallTimeLeft = getBaseTotalTime(currentLevel)
        maxOverallTime = getMaxTotalTime(currentLevel)
        // Clear failed words and error review state on restart
        failedWords.clear()
        isRetryWord = false
        isInErrorReview = false
        errorReviewIndex = 0

        // Reload current level (stay on same level, progress reset to 0)
        loadLevel(currentLevel)
    }

    // Store: Buy extra life with stars (separate from one-time revive)
    fun tryReviveWithStars(): Boolean {
        if (stars < REVIVE_COST_STARS) return false
        if (lives >= MAX_LIVES) return false
        stars -= REVIVE_COST_STARS
        lives = (lives + 1).coerceAtMost(MAX_LIVES)
        return true
    }

    // Store: Buy extra life with diamonds
    val REVIVE_COST_DIAMONDS = 50
    fun tryReviveWithDiamonds(): Boolean {
        if (diamonds < REVIVE_COST_DIAMONDS) return false
        if (lives >= MAX_LIVES) return false
        diamonds -= REVIVE_COST_DIAMONDS
        lives = (lives + 1).coerceAtMost(MAX_LIVES)
        return true
    }

    fun purchaseOrSelectAvatar(item: AvatarItem) {
        if (ownedAvatars.contains(item.id)) {
            selectedAvatarId = item.id
            Toast.makeText(context, "Avatar selected: ${item.title}", Toast.LENGTH_SHORT).show()
            return
        }
        if (diamonds < item.priceDiamonds) {
            Toast.makeText(context, "Not enough diamonds", Toast.LENGTH_SHORT).show()
            return
        }
        diamonds -= item.priceDiamonds
        ownedAvatars = ownedAvatars + item.id
        selectedAvatarId = item.id
        AchievementManager.onAvatarChanged(context)
        AchievementManager.onFirstPurchase(context)
        if (soundEnabled) SoundManager.shopBuy()
        Toast.makeText(context, "Purchased: ${item.title}", Toast.LENGTH_SHORT).show()
    }

    // Timer should run when game is active
    val shouldRunTimer = !showNameDialog &&
            !showMainMenu &&
            !showLoadingScreen &&
            !showGameOverDialog &&
            !showReviveDialog &&
            !isLevelTransitioning &&
            !hasAwardedForCurrentWord &&
            !isInputDisabled &&
            isCorrect == null

    fun startLevelUpTransition() {
        if (!pendingLevelUp) return
        if (showGameOverDialog || showReviveDialog) return
        if (isLevelTransitioning) return

        levelTransitionFrom = currentLevel
        levelTransitionTo = (currentLevel + 1).coerceAtMost(5)

        isLevelTransitioning = true
        levelTransitionProgress = 0f

        flashSuccess()

        isInputDisabled = true
        isTextFieldFocused = false
        focusManager.clearFocus(force = true)
        keyboardController?.hide()

        coroutineScope.launch {
            val steps = 20
            repeat(steps) { idx ->
                levelTransitionProgress = (idx + 1) / steps.toFloat()
                kotlinx.coroutines.delay(30)
            }
            kotlinx.coroutines.delay(900)

            // Update timers for new level
            val newLevel = currentLevel + 1
            maxOverallTime = getMaxTotalTime(newLevel)
            overallTimeLeft = (overallTimeLeft + 30f).coerceAtMost(maxOverallTime)  // Bonus time on level up

            loadLevel(newLevel)
            AchievementManager.onLevelCompleted(context, newLevel - 1)
            isLevelTransitioning = false
            levelTransitionProgress = 0f
        }
    }

    // Single Timer System - Only Total Time (smooth 60fps updates)
    // shouldRunTimer is recomposed each frame, LaunchedEffect restarts when it changes
    LaunchedEffect(shouldRunTimer) {
        if (!shouldRunTimer) return@LaunchedEffect
        var lastTime = System.currentTimeMillis()
        while (isActive) {
            val currentTime = System.currentTimeMillis()
            val deltaSeconds = (currentTime - lastTime) / 1000f
            lastTime = currentTime

            overallTimeLeft = (overallTimeLeft - deltaSeconds).coerceAtLeast(0f)

            if (overallTimeLeft <= 0f) {
                handleOverallTimeout()
                break
            }

            // ~60fps timer update
            kotlinx.coroutines.delay(16L)
        }
    }

    fun checkWord() {
        if (showGameOverDialog) return
        if (hasAwardedForCurrentWord) {
            return
        }
        val userAnswerRaw = userInput
        val correctAnswerRaw = currentWord.word

        fun normalizeAnswer(value: String): String {
            // Hyphen-insensitive (and whitespace-insensitive) comparison:
            // "well-being" == "wellbeing" == "well being"
            return value
                .trim()
                .lowercase(Locale.ROOT)
                .replace(Regex("[\\u2010\\u2011\\u2012\\u2013\\u2014\\u2015\\-\\s]"), "")
        }

        val userAnswer = normalizeAnswer(userAnswerRaw)
        val correctAnswer = normalizeAnswer(correctAnswerRaw)

        // Mark that user has attempted to check
        hasAttemptedCheck = true

        if (userAnswer == correctAnswer) {
            hasAwardedForCurrentWord = true
            isCorrect = true
            inputColor = GreenSubmit
            isInputDisabled = false // Keep keyboard enabled on correct answer

            // TIME BONUS: Add 5 seconds for correct answer
            overallTimeLeft = (overallTimeLeft + 5f).coerceAtMost(maxOverallTime)

            // Calculate rewards - 2x bonus if this was a retry word
            val (baseDiamonds, baseStars) = levelRewards(currentLevel)
            val multiplier = if (isRetryWord) 2 else 1
            comboMultiplier = (comboMultiplier + 1).coerceAtMost(10)

            val finalDiamonds = baseDiamonds * comboMultiplier * multiplier
            val finalStars = baseStars * multiplier

            diamonds += finalDiamonds
            xp += 10 // +10 XP per correct word
            stars += finalStars
            totalStarsEarned += finalStars  // Track for level progression
            starPulseKey++

            // Show friendly bonus message if retry word (previously failed, now correct)
            // Stays visible until Next is pressed (cleared in advanceToNextWord)
            if (isRetryWord) {
                showBonusIndicator = true
                // Remove this word from failed words list (SnapshotStateList - direct mutation is fine)
                failedWords.removeAll { it.word == currentWord.word }
            }
            isRetryWord = false

            performSuccessHaptic(context, vibrationEnabled)
            if (soundEnabled) SoundManager.success()

            // Achievement/streak/spaced-rep writes — все на IO чтобы не блокировать main thread
            val capturedWord = currentWord.word
            val capturedCombo = comboMultiplier
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try { AchievementManager.addCorrectWord(context) } catch (_: Exception) {}
                try { AchievementManager.updateMaxStreak(context, capturedCombo) } catch (_: Exception) {}
                try { StreakManager.addXPForCorrectWord(context) } catch (_: Exception) {}
                try { SpacedRepetitionManager.recordReview(context, capturedWord, isCorrect = true) } catch (_: Exception) {}
            }
            
            // Daily quests progress - temporarily disabled
            // try {
            //     DailyQuestsManager.updateProgress(context, QuestType.CORRECT_WORDS, 1)
            //     DailyQuestsManager.updateProgress(context, QuestType.EARN_XP, finalStars)
            //     DailyQuestsManager.updateProgress(context, QuestType.COMBO_STREAK, comboMultiplier)
            // } catch (e: Exception) { /* Silently handle */ }
            
            // Leagues weekly XP - temporarily disabled
            // try {
            //     LeaguesManager.addWeeklyXP(context, finalStars)
            // } catch (e: Exception) { /* Silently handle */ }

            showSynonyms = true
            flashSuccess()

            // Check for star-based level progression
            val nextLevel = currentLevel + 1
            if (currentLevel < 5 && canUnlockLevel(nextLevel)) {
                pendingLevelUp = true
                coroutineScope.launch {
                    kotlinx.coroutines.delay(650)
                    if (!showGameOverDialog && !showReviveDialog) {
                        startLevelUpTransition()
                    }
                }
            }
        } else {
            hasAwardedForCurrentWord = true
            isCorrect = false
            inputColor = Color.Red
            comboMultiplier = 1
            showSynonyms = false
            isInputDisabled = true // Lock keyboard when answer is incorrect

            // 🎨 VISUAL FEEDBACK: Red input field flash
            showRedInputFlash = true
            coroutineScope.launch {
                kotlinx.coroutines.delay(600)
                showRedInputFlash = false
            }

            // AUTO-OPEN HINT: Reveal the correct word via eye icon
            hintWord = correctAnswerRaw
            showHint = true

            // NO "Correct Answer" text - just show hint with word revealed
            showCorrectAnswerLabel = false

            // Add to failed words for later retry (only if not already in list)
            if (!failedWords.any { it.word == currentWord.word }) {
                failedWords.add(currentWord)
            }
            isRetryWord = false

            performErrorHaptic(context, vibrationEnabled)
            if (soundEnabled) SoundManager.fail()
            flashError()

            isTextFieldFocused = false
            focusManager.clearFocus(force = true)
            keyboardController?.hide()

            val capturedWordWrong = currentWord.word
            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                try { SpacedRepetitionManager.recordReview(context, capturedWordWrong, isCorrect = false) } catch (_: Exception) {}
            }
            
            loseLife("❌ Wrong! -1 ❤️")
        }
    }

    fun advanceToNextWordInternal() {
        // CRITICAL: Clear all visual state BEFORE changing currentWord
        // to prevent the next word's answer from flashing for one frame
        fetchedSynonyms = emptyList()
        resetCurrentLevelProgress()

        // 📝 If we're in error-review mode, cycle through failed words
        if (isInErrorReview) {
            if (failedWords.isEmpty()) {
                // Error review complete — exit review, allow normal play
                isInErrorReview = false
                errorReviewIndex = 0
                isRetryWord = false
                Toast.makeText(context, "🎉 Mistakes reviewed! Well done!", Toast.LENGTH_SHORT).show()
                // Continue normal word progression
                if (levelWords.isNotEmpty()) {
                    wordIndexInLevel = 0
                    Collections.shuffle(levelWords)
                    currentWord = levelWords[wordIndexInLevel]
                }
            } else {
                // Next failed word to review — increment THEN mod so we cycle through all words
                errorReviewIndex = (errorReviewIndex + 1) % failedWords.size
                currentWord = failedWords[errorReviewIndex]
                isRetryWord = true
            }
            return
        }

        // Normal word progression
        if (levelWords.isNotEmpty()) {
            if (wordIndexInLevel >= levelWords.size - 1) {
                // All 35 session words completed — check for error review
                if (failedWords.isNotEmpty()) {
                    isInErrorReview = true
                    errorReviewIndex = 0
                    currentWord = failedWords[0]
                    isRetryWord = true
                    Toast.makeText(context, "📝 Work on Mistakes! ${failedWords.size} word(s) to review", Toast.LENGTH_SHORT).show()
                } else {
                    // No mistakes — reshuffle deck
                    Collections.shuffle(levelWords)
                    wordIndexInLevel = 0
                    currentWord = levelWords[wordIndexInLevel]
                    isRetryWord = false
                }
            } else {
                wordIndexInLevel += 1
                currentWord = levelWords[wordIndexInLevel]
                isRetryWord = false
            }
        }
    }

    fun nextWord() {
        if (showGameOverDialog) return
        if (isLevelTransitioning) return

        if (pendingLevelUp) {
            startLevelUpTransition()
            return
        }

        advanceToNextWordInternal()
    }

    fun requestHint() {
        if (!hasAttemptedCheck) {
            // Show toast if user tries to use hint before attempting
            Toast.makeText(context, "Try to spell it yourself first!", Toast.LENGTH_SHORT).show()
            return
        }
        hintWord = currentWord.word
        showHint = true
    }

    val gameColors = com.app.gectyping.ui.theme.LocalGameColors.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gameColors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        // Don't render game content while loading screen or main menu is showing
        // This prevents the game background from flashing for one frame
        if (!showLoadingScreen && !showMainMenu && !showNameDialog) Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp)
                .padding(bottom = keyboardBottomPadding)
        ) {
            // Back to Menu button — saves current word progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                IconButton(
                    onClick = {
                        // Save progress (wordIndex is already in state)
                        prefs.edit()
                            .putInt("word_index_in_level", wordIndexInLevel)
                            .putInt(KEY_CURRENT_LEVEL, currentLevel)
                            .putInt(KEY_LEVEL_PROGRESS, levelProgressCount)
                            .apply()
                        showMainMenu = true
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Menu",
                        tint = gameColors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Header with Genius logo and scores
            HeaderSection(
                playerName = playerName,
                avatarEmoji = avatarById(selectedAvatarId).emoji,
                selectedAvatarId = selectedAvatarId,
                stars = stars,
                diamonds = diamonds,
                lives = lives,
                maxLives = MAX_LIVES,
                starPulseKey = starPulseKey,
                levelProgress = levelProgressFraction(),
                levelNumber = currentLevel,
                totalStarsEarned = totalStarsEarned,
                nextLevelStars = LEVEL_UNLOCK_STARS[currentLevel + 1] ?: 0,
                comboMultiplier = comboMultiplier
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Overall Time Bar - shows total time remaining
            OverallTimeBar(
                timeLeft = overallTimeLeft,
                maxTime = maxOverallTime,
                isCritical = overallTimeLeft <= 15f
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Wrap the main card so we can scroll it into view on focus
            Column(
                modifier = Modifier
                    .bringIntoViewRequester(bringIntoViewRequester)
            ) {
                MainCard(
                    userInput = userInput,
                    onInputChange = {
                        // Prevent input changes when keyboard is disabled
                        if (!isInputDisabled) {
                            userInput = it
                            isCorrect = null
                            inputColor = Color.Transparent
                        }
                    },
                    onSpeakerClick = { replayWord(currentWord.word) },
                    onEyeClick = { requestHint() },
                    onCheckClick = {
                        checkWord()
                        // Hide keyboard and clear focus after submit
                        isTextFieldFocused = false
                        focusManager.clearFocus(force = true)
                        keyboardController?.hide()
                    },
                    onNextClick = { nextWord() },
                    timerEnabled = false,  // Per-word timer removed - only total time bar
                    timeLeftSeconds = 0,   // Not used anymore
                    comboCount = comboMultiplier,
                    comboPoints = levelRewards(currentLevel).first * comboMultiplier,
                    isCorrect = isCorrect,
                    inputColor = inputColor,
                    showHint = showHint,
                    hintWord = hintWord,
                    showSynonyms = showSynonyms,
                    synonyms = (fetchedSynonyms + currentWord.synonyms)
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(4),
                    isTextFieldFocused = isTextFieldFocused,
                    onFocusChange = { focused ->
                        isTextFieldFocused = focused
                        if (focused) {
                            // When the input gains focus, scroll the card so the
                            // "Check" / "Next" buttons remain visible above the keyboard.
                            coroutineScope.launch {
                                // Slight delay so layout has updated with keyboard visibility
                                kotlinx.coroutines.delay(100)
                                bringIntoViewRequester.bringIntoView()
                            }
                        }
                    },
                    keyboardController = keyboardController,
                    focusManager = focusManager,
                    isEyeButtonEnabled = hasAttemptedCheck,
                    isInputDisabled = isInputDisabled,
                    isCheckEnabled = !hasAwardedForCurrentWord,
                    showRedFlash = showRedInputFlash,
                    showBonusIndicator = showBonusIndicator
                )

                // 📝 CORRECT ANSWER LABEL - Shows when player enters wrong answer
                AnimatedVisibility(
                    visible = showCorrectAnswerLabel,
                    enter = fadeIn(animationSpec = tween(200)) + slideInVertically(initialOffsetY = { -it / 2 }),
                    exit = fadeOut(animationSpec = tween(150))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEF5350).copy(alpha = if (gameColors.isDark) 0.18f else 0.12f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Correct answer:",
                                color = Color(0xFFEF5350),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lastCorrectAnswer,
                                color = gameColors.textPrimary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Custom keyboard at the bottom - show when text field is focused
        AnimatedVisibility(
            visible = isTextFieldFocused,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            enter = fadeIn() + slideInVertically(
                initialOffsetY = { it }
            ),
            exit = fadeOut() + slideOutVertically(
                targetOffsetY = { it }
            )
        ) {
            // Convert navigation bar height from px to dp for bottom padding
            val density = LocalDensity.current
            val navBarBottomPadding = with(density) {
                if (navigationBarHeightPx > 0) navigationBarHeightPx.toDp() else 0.dp
            }

            CustomKeyboard(
                onKeyPress = { key ->
                    // Ignore all keyboard input when disabled
                    if (isInputDisabled) {
                        return@CustomKeyboard
                    }

                    when (key) {
                        "BACKSPACE" -> {
                            if (userInput.isNotEmpty()) {
                                val newValue = userInput.dropLast(1)
                                userInput = newValue
                                isCorrect = null
                                inputColor = Color.Transparent
                            }
                        }
                        "ENTER" -> {
                            // Check word and hide keyboard
                            checkWord()
                            // Hide keyboard and clear focus after submit
                            isTextFieldFocused = false
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                        }
                        "SPACE" -> {
                            val newValue = "$userInput "
                            userInput = newValue
                            isCorrect = null
                            inputColor = Color.Transparent
                        }
                        else -> {
                            val newValue = userInput + key
                            userInput = newValue
                            isCorrect = null
                            inputColor = Color.Transparent
                        }
                    }
                },
                isDisabled = isInputDisabled,
                modifier = Modifier
                    .fillMaxWidth()
                    // Push the keyboard above the system navigation bar so that
                    // SPACE / ENTER remain fully visible and clickable.
                    .padding(bottom = navBarBottomPadding)
            )
        }

        // 🎬 Splash video (shown once per app launch)
        if (showSplash) {
            SplashScreen(onComplete = { showSplash = false })
        }

        // 🌐 No internet overlay — blocks everything until connection restored
        if (!isOnline) {
            NoInternetScreen(onRetry = { checkConnectivity() })
        }

        // Onboarding screen (first-time entry) - BOG style
        if (!showSplash && showNameDialog) {
            OnboardingScreen(
                onComplete = { finalName ->
                    val cleaned = finalName.trim().take(24)
                    if (cleaned.isBlank()) return@OnboardingScreen
                    prefs.edit().putString("player_name", cleaned).apply()
                    playerName = cleaned
                    showNameDialog = false
                    greetedThisSession = true
                }
            )
        }

        // 🏁 Main Menu overlay (center hub — shown after name dialog or on returning visit)
        if (!showSplash && !showNameDialog && showMainMenu) {
            MainMenuScreen(
                context = context,
                playerName = playerName,
                avatarEmoji = avatarById(selectedAvatarId).emoji,
                lives = lives,
                maxLives = MAX_LIVES,
                diamonds = diamonds,
                xp = xp,
                stars = stars,
                dailyStreak = AchievementManager.getDailyStreak(context),
                avatars = AVATARS,
                ownedAvatars = ownedAvatars,
                selectedAvatarId = selectedAvatarId,
                onBuyAvatar = { item -> purchaseOrSelectAvatar(item) },
                onSelectAvatar = { id -> selectedAvatarId = id },
                ownedWorlds = ownedWorlds,
                currentWorld = themeWorld,
                onBuyWorld = { world ->
                    val price = com.app.gectyping.ui.theme.worldPrices[world] ?: 0
                    if (diamonds >= price) {
                        diamonds -= price
                        ownedWorlds = ownedWorlds + world.name
                        prefs.edit().putStringSet("owned_worlds", ownedWorlds.toMutableSet()).apply()
                        AchievementManager.onFirstPurchase(context)
                        if (soundEnabled) SoundManager.shopBuy()
                        Toast.makeText(context, "Purchased ${world.displayName}!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Not enough diamonds!", Toast.LENGTH_SHORT).show()
                    }
                },
                onApplyWorld = { world ->
                    onWorldChange(world)
                    AchievementManager.onThemeSwapped(context)
                    Toast.makeText(context, "${world.displayName} applied!", Toast.LENGTH_SHORT).show()
                },
                navigationBarHeightDp = (navigationBarHeightPx / context.resources.displayMetrics.density).toInt(),
                onBuyLife = {
                    val lifeCost = 100
                    if (diamonds >= lifeCost && lives < MAX_LIVES) {
                        diamonds -= lifeCost
                        lives = (lives + 1).coerceAtMost(MAX_LIVES)
                        AchievementManager.onFirstPurchase(context)
                        if (soundEnabled) SoundManager.lifeBuy()
                        Toast.makeText(context, "+1 Life!", Toast.LENGTH_SHORT).show()
                    } else if (lives >= MAX_LIVES) {
                        Toast.makeText(context, "Already at max lives!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Not enough diamonds!", Toast.LENGTH_SHORT).show()
                    }
                },
                // 🎵 Music Shop callbacks
                ownedMusicTracks = ownedMusicTracks,
                selectedMusicTrackId = selectedMusicTrackId,
                onBuyMusicTrack = { track ->
                    if (diamonds >= track.priceDiamonds) {
                        diamonds -= track.priceDiamonds
                        ownedMusicTracks = ownedMusicTracks + track.id
                        selectedMusicTrackId = track.id
                        AppAudioManager.stopPreview()
                        AppAudioManager.applySelectedTrack(track.id)
                        AchievementManager.onFirstPurchase(context)
                        if (soundEnabled) SoundManager.shopBuy()
                        Toast.makeText(context, "Purchased ${track.title}!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Not enough diamonds!", Toast.LENGTH_SHORT).show()
                    }
                },
                onSelectMusicTrack = { trackId ->
                    AppAudioManager.stopPreview()
                    selectedMusicTrackId = trackId
                    AppAudioManager.applySelectedTrack(trackId)
                    Toast.makeText(context, "Track selected!", Toast.LENGTH_SHORT).show()
                },
                onPreviewMusicTrack = { trackId ->
                    AppAudioManager.playPreview(trackId)
                },
                onStopMusicPreview = {
                    AppAudioManager.stopPreview()
                },
                onStartGame = {
                    showMainMenu = false
                    showLoadingScreen = true
                },
                onOpenSettings = { showSettings = true },
                onNameChange = { newName ->
                    playerName = newName
                    prefs.edit().putString("player_name", newName).apply()
                },
                onChangePhoto = {
                    Toast.makeText(context, "Photo picker — coming soon!", Toast.LENGTH_SHORT).show()
                },
                onDiamondsChange = { newDiamonds -> diamonds = newDiamonds },
                onStarsChange = { newStars -> stars = newStars },
                currentLevel = currentLevel,
                totalStarsEarned = totalStarsEarned,
                nextLevelStars = LEVEL_UNLOCK_STARS[currentLevel + 1] ?: 0
            )
        }

        // 📊 Loading screen overlay (shown after main menu, before game)
        if (!showNameDialog && !showMainMenu && showLoadingScreen) {
            LoadingScreen(
                onLoadingComplete = {
                    showLoadingScreen = false
                    greetedThisSession = true
                }
            )
        }

        // 💫 Revive Dialog (costs 10 stars - unlimited purchases)
        if (showReviveDialog) {
            AlertDialog(
                onDismissRequest = { /* keep open */ },
                title = {
                    Text(
                        text = "💀 Defeated!",
                        color = gameColors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = lastLifeLossReason.ifBlank { "You ran out of lives!" },
                            color = gameColors.textPrimary.copy(alpha = 0.85f),
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Buy an extra life to continue!",
                            color = Color(0xFFFFD54F),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = YellowStar,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Cost: $REVIVE_COST_STARS stars (You have: $stars)",
                                color = gameColors.textPrimary.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { useRevive() },
                        enabled = stars >= REVIVE_COST_STARS,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50),
                            disabledContainerColor = Color(0xFF424242)
                        )
                    ) {
                        Row(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text("✨ Revive ($REVIVE_COST_STARS ", color = White)
                            StarIcon(size = 16.dp)
                            Text(")", color = White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { declineRevive() }) {
                        Text("❌ Give Up", color = Color(0xFFEF5350))
                    }
                },
                containerColor = gameColors.cardBackground,
                textContentColor = gameColors.textPrimary,
                titleContentColor = gameColors.textPrimary
            )
        }

        // ❤️ Final Game Over dialog (no more chances)
        if (showGameOverDialog) {
            AlertDialog(
                onDismissRequest = { /* keep open */ },
                title = {
                    Text(
                        text = "Game Over",
                        color = gameColors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "No more lives remaining.",
                            color = gameColors.textPrimary.copy(alpha = 0.85f),
                            fontSize = 15.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            repeat(MAX_LIVES) { idx ->
                                PixelHeart(
                                    alive = false,
                                    shatterTrigger = 0,
                                    showEmptyWhenDead = true,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "You will restart from Level 1.\nYour stars and diamonds are saved!",
                            color = gameColors.textSecondary,
                            fontSize = 13.sp
                        )
                        // Show stars earned
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = YellowStar,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Total Stars: $totalStarsEarned",
                                color = YellowStar,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { handleRestartAfterGameOver() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = gameColors.accent
                        )
                    ) {
                        Text("🔄 Restart", color = White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        handleRestartAfterGameOver()
                        showMainMenu = true
                    }) {
                        Text("🏠 Back to Menu", color = gameColors.textSecondary)
                    }
                },
                containerColor = gameColors.cardBackground,
                textContentColor = gameColors.textPrimary,
                titleContentColor = gameColors.textPrimary
            )
        }

        // 🔥 Streak Lost Dialog
        if (showStreakLostDialog) {
            StreakLostDialog(
                lostStreak = lostStreakAmount,
                onDismiss = { showStreakLostDialog = false },
                onBuyFreeze = { 
                    showStreakLostDialog = false
                    // Navigate to streak section - user can buy freeze there
                }
            )
        }
        
        // ❄️ Streak Freeze Used Dialog
        if (showStreakFreezeUsedDialog) {
            StreakFreezeUsedDialog(
                streak = streakAfterFreeze,
                freezesLeft = freezesLeftAfterUse,
                onDismiss = { showStreakFreezeUsedDialog = false }
            )
        }

        // ⚙️ Settings bottom sheet
        if (showSettings && !showGameOverDialog) {
            val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                sheetState = settingsSheetState,
                containerColor = gameColors.cardBackground,
                dragHandle = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp, bottom = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 42.dp, height = 4.dp)
                                .clip(RoundedCornerShape(999.dp))
                                .background(gameColors.textSecondary.copy(alpha = 0.22f))
                        )
                    }
                }
            ) {
                SettingsContent(
                    vibrationEnabled = vibrationEnabled,
                    onVibrationChange = { vibrationEnabled = it },
                    soundEnabled = soundEnabled,
                    onSoundChange = { soundEnabled = it },
                    musicEnabled = musicEnabled,
                    onMusicChange = { musicEnabled = it },
                    masterVolume = masterVolume,
                    onMasterVolumeChange = { masterVolume = it },
                    musicVolume = musicVolume,
                    onMusicVolumeChange = { musicVolume = it },
                    sfxVolume = sfxVolume,
                    onSfxVolumeChange = { sfxVolume = it },
                    voiceVolume = voiceVolume,
                    onVoiceVolumeChange = { 
                        voiceVolume = it
                        AppAudioManager.setVoiceVolume(it)
                        prefs.edit().putFloat("voice_volume", it).apply()
                    },
                    ttsPitch = ttsPitch,
                    onTtsPitchChange = { ttsPitch = it },
                    ttsRate = ttsRate,
                    onTtsRateChange = { ttsRate = it }
                )
            }
        }

        // ⚡ Background Flash overlay (green = correct, red = error)
        val animatedFlashAlpha by animateFloatAsState(
            targetValue = bgFlashAlpha,
            animationSpec = tween(durationMillis = 200),
            label = "bgFlash"
        )
        if (animatedFlashAlpha > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgFlashColor.copy(alpha = animatedFlashAlpha))
                    .zIndex(-1f)
            )
        }

        // Level transition overlay (between levels)
        if (isLevelTransitioning) {
            LevelTransitionOverlay(
                fromLevel = levelTransitionFrom,
                toLevel = levelTransitionTo,
                progress = levelTransitionProgress
            )
        }
    }
}


// 🎉 ENHANCED LEVEL TRANSITION - Congratulations screen with celebration
@Composable
private fun LevelTransitionOverlay(
    fromLevel: Int,
    toLevel: Int,
    progress: Float
) {
    // Smooth scale animation with ease-in-out
    val animatedScale by animateFloatAsState(
        targetValue = if (progress > 0.1f) 1f else 0.8f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "levelTransitionScale"
    )

    // Shimmer effect for celebration
    val infiniteTransition = rememberInfiniteTransition(label = "celebrationShimmer")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val tColors = LocalGameColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .zIndex(50f),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = tColors.overlay
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
            modifier = Modifier
                .padding(28.dp)
                .fillMaxWidth()
                .widthIn(max = 380.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                }
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                tColors.cardBackground,
                                tColors.overlay
                            )
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Celebration emoji row
                Row(
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🎉", fontSize = 32.sp)
                    StarIcon(size = 36.dp)
                    Text(text = "🎊", fontSize = 32.sp)
                }

                // Main congratulations text with shimmer
                Text(
                    text = "Congratulations!",
                    color = White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.graphicsLayer {
                        // Subtle glow effect
                        shadowElevation = 8f
                    }
                )

                // Level unlock text
                Text(
                    text = "New Level Unlocked",
                    color = Color(0xFFFFD54F),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(White.copy(alpha = 0.15f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color(0xFF4CAF50),
                                        Color(0xFF8BC34A),
                                        Color(0xFFCDDC39)
                                    )
                                )
                            )
                    )
                }

                // Level transition text
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // From level badge
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = White.copy(alpha = 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Level $fromLevel",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = White.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "→",
                        color = Color(0xFFFFD54F),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // To level badge (highlighted)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Level $toLevel",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            color = White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerNameDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onConfirm: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = { /* force a name on first entry */ },
        title = {
            Text(
                text = "Welcome!",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.imePadding().navigationBarsPadding()) {
                Text(
                    text = "What's your name?",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { onNameChange(it.take(24)) },
                    singleLine = true,
                    label = { Text("Name") },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.trim().isNotEmpty()
            ) {
                Text("Start")
            }
        }
    )
}

@Composable
fun HeaderSection(
    playerName: String,
    avatarEmoji: String,
    selectedAvatarId: String = "space",
    stars: Int,
    diamonds: Int,
    lives: Int,
    maxLives: Int,
    starPulseKey: Int,
    levelProgress: Float,
    levelNumber: Int,
    totalStarsEarned: Int,
    nextLevelStars: Int,
    comboMultiplier: Int
) {
    val hColors = com.app.gectyping.ui.theme.LocalGameColors.current
    var isStarPulsing by remember { mutableStateOf(false) }
    val isOnFire = comboMultiplier > 1
    val fireTransition = rememberInfiniteTransition(label = "fireTransition")
    val firePulseRaw by fireTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "firePulse"
    )
    // Применяем пульс только когда комбо активно — иначе GPU не тратится
    val firePulse = if (isOnFire) firePulseRaw else 1f
    val comboLevel = comboMultiplier.coerceIn(1, 10)
    val fireIntensity = ((comboLevel - 1).toFloat() / 9f).coerceIn(0f, 1f)

    val animatedStars by animateIntAsState(
        targetValue = stars,
        animationSpec = tween(durationMillis = 260),
        label = "animatedStars"
    )
    val animatedDiamonds by animateIntAsState(
        targetValue = diamonds,
        animationSpec = tween(durationMillis = 260),
        label = "animatedDiamonds"
    )
    val animatedLevelProgress by animateFloatAsState(
        targetValue = levelProgress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 420),
        label = "animatedLevelProgress"
    )
    val starScale by animateFloatAsState(
        targetValue = if (isStarPulsing) 1.12f else 1.0f,
        animationSpec = tween(durationMillis = 220),
        label = "starScale"
    )
    val starTint by animateColorAsState(
        targetValue = if (isStarPulsing) YellowStar else YellowStar.copy(alpha = 0.85f),
        animationSpec = tween(durationMillis = 220),
        label = "starTint"
    )

    LaunchedEffect(starPulseKey) {
        if (starPulseKey > 0) {
            isStarPulsing = true
            kotlinx.coroutines.delay(260)
            isStarPulsing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(hColors.textPrimary.copy(alpha = 0.10f))
                        .border(1.dp, hColors.textPrimary.copy(alpha = 0.14f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    AvatarIcon(avatar = avatarById(selectedAvatarId), size = 34.dp, fontSize = 28.sp)
                }

                Text(
                    text = if (playerName.isNotBlank()) "Hi, $playerName!" else "Hi!",
                    color = hColors.textPrimary.copy(alpha = 0.92f),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(hColors.cardBackground.copy(alpha = 0.55f))
                    .border(1.dp, hColors.textPrimary.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    scaleX = starScale
                                    scaleY = starScale
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                starTint.copy(alpha = 0.45f),
                                                starTint.copy(alpha = 0.12f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Stars",
                                tint = starTint,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = animatedStars.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = hColors.textPrimary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DiamondIcon(size = 16.dp)
                        Text(
                            text = animatedDiamonds.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1CB0F6)
                        )
                    }

                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isOnFire) {
                        Box(
                            modifier = Modifier
                                .size((18 + (fireIntensity * 10f)).dp)
                                .graphicsLayer {
                                    scaleX = firePulse
                                    scaleY = firePulse
                                    alpha = 0.95f
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFF6D00).copy(alpha = (0.22f + 0.30f * fireIntensity).coerceIn(0f, 1f)),
                                                Color(0xFFFFD54F).copy(alpha = (0.08f + 0.18f * fireIntensity).coerceIn(0f, 1f)),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                            FireIcon(
                                size = (16 + (fireIntensity * 8f)).dp,
                                modifier = Modifier.graphicsLayer {
                                    // Цвет тинт не применим к Image, но анимация сохраняется через родительский Box
                                }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(hColors.textPrimary.copy(alpha = 0.14f))
                            .border(1.dp, hColors.textPrimary.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                    ) {
                        if (comboLevel >= 10) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFFFFFF).copy(alpha = 0.20f * firePulse),
                                                Color(0xFF40C4FF).copy(alpha = 0.16f * firePulse),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedLevelProgress)
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = if (isOnFire) {
                                            if (comboLevel >= 10) {
                                                listOf(
                                                    Color(0xFF40C4FF),
                                                    Color(0xFFB388FF),
                                                    Color(0xFFFFFFFF)
                                                )
                                            } else {
                                                listOf(
                                                    Color(0xFFFF6D00),
                                                    Color(0xFFFFD54F),
                                                    Color(0xFFFF4081)
                                                )
                                            }
                                        } else {
                                            listOf(
                                                Color(0xFF40C4FF),
                                                Color(0xFF7C4DFF),
                                                Color(0xFF69F0AE)
                                            )
                                        }
                                    )
                                )
                                .shadow(
                                    elevation = if (isOnFire) 10.dp else 6.dp,
                                    shape = RoundedCornerShape(999.dp),
                                    clip = false
                                )
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Level $levelNumber",
                        color = hColors.textPrimary.copy(alpha = 0.88f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (levelNumber >= 5) {
                        Text(
                            text = "∞ Endless",
                            color = hColors.textSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Row(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StarIcon(size = 13.dp)
                            Text(
                                text = "$totalStarsEarned/$nextLevelStars",
                                color = hColors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        text = "${WordList.getWordCountForLevel(levelNumber)} words",
                        color = hColors.textSecondary.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }

        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var previousLives by remember { mutableIntStateOf(lives) }
            var shatterIndex by remember { mutableIntStateOf(-1) }
            var shatterKey by remember { mutableIntStateOf(0) }

            LaunchedEffect(lives) {
                if (lives < previousLives) {
                    shatterIndex = lives
                    shatterKey += 1
                    // After the shatter animation completes, reset the trigger so the empty heart renders.
                    kotlinx.coroutines.delay(360)
                    shatterIndex = -1
                }
                previousLives = lives
            }

            repeat(maxLives) { idx ->
                PixelHeart(
                    alive = idx < lives,
                    shatterTrigger = if (idx == shatterIndex) shatterKey else 0,
                    showEmptyWhenDead = true,
                    modifier = Modifier.size(26.dp)  // Clean minimalist size
                )
            }
        }
    }
}

/**
 * Overall Time Bar - Shows total time remaining with smooth animation
 * Changes color when time is critical (< 15 seconds)
 */
@Composable
fun OverallTimeBar(
    timeLeft: Float,
    maxTime: Float,
    isCritical: Boolean
) {
    val progress = (timeLeft / maxTime).coerceIn(0f, 1f)

    // No animateFloatAsState needed — the timer loop already updates at ~60fps,
    // so the progress value itself is already smooth. Using snap() avoids a
    // redundant animation layer that would otherwise run concurrently with the timer.
    val animatedProgress = progress

    // Color animation for critical state (only fires on state change, not every frame)
    val barColor by animateColorAsState(
        targetValue = if (isCritical) Color(0xFFEF5350) else Color(0xFF4CAF50),
        animationSpec = tween(durationMillis = 300),
        label = "timeBarColor"
    )

    // Pulsing effect when critical — transition всегда существует но значение используется только при isCritical
    val pulseTransition = rememberInfiniteTransition(label = "criticalPulse")
    val pulseAlphaRaw by pulseTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseAlpha = if (isCritical) pulseAlphaRaw else 1f

    val gc = LocalGameColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
    ) {
        // Time display
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isCritical) barColor.copy(alpha = pulseAlpha) else gc.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Total Time",
                    color = gc.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = "${timeLeft.toInt()}s",
                color = if (isCritical) barColor.copy(alpha = pulseAlpha) else gc.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(gc.border.copy(alpha = 0.3f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .then(
                        if (isCritical) Modifier.background(barColor.copy(alpha = pulseAlpha))
                        else Modifier.background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF4CAF50),
                                    Color(0xFF8BC34A)
                                )
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun PixelHeart(
    alive: Boolean,
    shatterTrigger: Int,
    showEmptyWhenDead: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Показываем разбитое сердце во время анимации потери жизни
    var showBroken by remember { mutableStateOf(false) }
    var shatterTarget by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(shatterTrigger) {
        if (shatterTrigger > 0) {
            // Показываем разбитое сердце, затем анимируем исчезновение
            showBroken = true
            shatterTarget = 0f
            kotlinx.coroutines.delay(16)
            shatterTarget = 1f
            kotlinx.coroutines.delay(300) // ждём конца анимации
            showBroken = false
        } else if (alive) {
            shatterTarget = 0f
            showBroken = false
        }
    }

    val shatterProgress by animateFloatAsState(
        targetValue = shatterTarget,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "shatter"
    )

    val effectiveProgress = when {
        alive -> 0f
        shatterTrigger > 0 -> shatterProgress
        else -> 1f
    }

    val alpha = (1f - effectiveProgress).coerceIn(0f, 1f)
    val scale = (1f - 0.3f * effectiveProgress).coerceIn(0f, 1f)

    // Пустое (серое) сердце — когда жизнь уже потеряна и анимация закончилась
    val isEmptySlot = !alive && !showBroken && shatterTrigger == 0

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = if (isEmptySlot) 0.25f else alpha
            scaleX = if (isEmptySlot) 1f else scale
            scaleY = if (isEmptySlot) 1f else scale
        },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(
                id = if (showBroken || isEmptySlot)
                    R.drawable.icons8_broken_heart_96
                else
                    R.drawable.icons8_heart_96
            ),
            contentDescription = if (alive) "Life" else "Lost life",
            modifier = Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainCard(
    userInput: String,
    onInputChange: (String) -> Unit,
    onSpeakerClick: () -> Unit,
    onEyeClick: () -> Unit,
    onCheckClick: () -> Unit,
    onNextClick: () -> Unit,
    timerEnabled: Boolean,
    timeLeftSeconds: Int,
    comboCount: Int,
    comboPoints: Int,
    isCorrect: Boolean?,
    inputColor: Color,
    showHint: Boolean,
    hintWord: String,
    showSynonyms: Boolean,
    synonyms: List<String>,
    isTextFieldFocused: Boolean,
    onFocusChange: (Boolean) -> Unit,
    keyboardController: androidx.compose.ui.platform.SoftwareKeyboardController?,
    focusManager: androidx.compose.ui.focus.FocusManager,
    isEyeButtonEnabled: Boolean,
    isInputDisabled: Boolean,
    isCheckEnabled: Boolean,
    showRedFlash: Boolean = false,  // Red flash on input field for incorrect answer
    showBonusIndicator: Boolean = false  // ×2 bonus indicator near eye icon
) {
    // Animate red flash border
    val redFlashAlpha by animateFloatAsState(
        targetValue = if (showRedFlash) 1f else 0f,
        animationSpec = tween(durationMillis = if (showRedFlash) 150 else 300),
        label = "redFlashAlpha"
    )

    // Animate bonus indicator with scale + fade
    val bonusScale by animateFloatAsState(
        targetValue = if (showBonusIndicator) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bonusScale"
    )
    val bonusAlpha by animateFloatAsState(
        targetValue = if (showBonusIndicator) 1f else 0f,
        animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        label = "bonusAlpha"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        if (comboCount > 1) {
            ComboNotification(
                comboCount = comboCount,
                points = comboPoints,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-10).dp)
                    .zIndex(2f)
            )
        }

        val gc = LocalGameColors.current

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = if (comboCount > 1) 10.dp else 0.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = gc.cardBackground
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (gc.isDark) 10.dp else 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, gc.border.copy(alpha = 0.3f), RoundedCornerShape(28.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                if (timerEnabled) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clip(RoundedCornerShape(999.dp))
                            .background(gc.overlay.copy(alpha = 0.6f))
                            .border(1.dp, gc.border.copy(alpha = 0.2f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = gc.textPrimary.copy(alpha = 0.92f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$timeLeftSeconds s",
                            color = gc.textPrimary.copy(alpha = 0.92f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeumorphicButton(
                        onClick = onSpeakerClick,
                        icon = Icons.Default.VolumeUp,
                        modifier = Modifier.size(62.dp),
                        enabled = true
                    )

                    // Eye button
                    NeumorphicButton(
                        onClick = onEyeClick,
                        icon = Icons.Default.Visibility,
                        modifier = Modifier.size(62.dp),
                        enabled = isEyeButtonEnabled
                    )
                }

                // 🎉 RETRY SUCCESS MESSAGE - Friendly animated message for previously failed words
                if (bonusScale > 0.01f) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .graphicsLayer {
                                scaleX = bonusScale
                                scaleY = bonusScale
                                alpha = bonusAlpha
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF4CAF50).copy(alpha = 0.95f)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎯 Nice Recovery!",
                                color = White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "You got it right this time! +2× ",
                                    color = White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                StarIcon(size = 14.dp)
                                Text(
                                    text = " & ",
                                    color = White.copy(alpha = 0.9f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                DiamondIcon(size = 14.dp)
                            }
                        }
                    }
                }

                // Hint display - only show word when hint is active to prevent flash
                if (showHint) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = YellowStar.copy(alpha = if (gc.isDark) 0.2f else 0.15f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Hint: $hintWord",
                            modifier = Modifier.padding(12.dp),
                            color = gc.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Text input field - using custom keyboard
                // Red flash border color for incorrect answer
                val redFlashBorder = Color.Red.copy(alpha = redFlashAlpha)
                val effectiveBorderColor = when {
                    redFlashAlpha > 0f -> redFlashBorder
                    inputColor != Color.Transparent -> inputColor
                    else -> gc.border.copy(alpha = 0.3f)
                }

                OutlinedTextField(
                    value = userInput,
                    onValueChange = onInputChange,
                    enabled = !isInputDisabled,
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (redFlashAlpha > 0f) {
                                Modifier.border(2.dp, redFlashBorder, RoundedCornerShape(14.dp))
                            } else Modifier
                        )
                        .onFocusChanged { focusState ->
                            onFocusChange(focusState.isFocused)
                            if (focusState.isFocused) { keyboardController?.hide() }
                        },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    placeholder = {
                        Text(text = "Tap here and type...", color = gc.textSecondary)
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = gc.textPrimary,
                        unfocusedTextColor = gc.textPrimary,
                        focusedBorderColor = effectiveBorderColor,
                        unfocusedBorderColor = if (inputColor != Color.Transparent) inputColor else gc.border.copy(alpha = 0.3f),
                        focusedContainerColor = if (gc.isDark) Color.Black.copy(alpha = 0.25f) else gc.surface,
                        unfocusedContainerColor = if (gc.isDark) Color.Black.copy(alpha = 0.18f) else gc.surface
                    ),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true
                )

                // Synonyms display
                AnimatedVisibility(visible = showSynonyms && synonyms.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = GreenSubmit.copy(alpha = if (gc.isDark) 0.2f else 0.12f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Related words:",
                                fontSize = 12.sp,
                                color = gc.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = synonyms.joinToString(", "),
                                fontSize = 14.sp,
                                color = gc.textPrimary,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }

                Text(
                    text = "Tap ✅ to submit",
                    color = gc.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Button(
                        onClick = onCheckClick,
                        enabled = isCheckEnabled && !isInputDisabled && isCorrect == null,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GreenSubmit
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 10.dp,
                            pressedElevation = 4.dp
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Check",
                            tint = White,
                            modifier = Modifier.size(34.dp)
                        )
                    }

                    NeumorphicButton(
                        onClick = onNextClick,
                        icon = Icons.Default.ArrowForward,
                        modifier = Modifier.size(56.dp),
                        enabled = isCorrect != null  // Only enabled after submit
                    )
                }
            }
        }
    }
}

@Composable
fun ComboNotification(comboCount: Int, points: Int, modifier: Modifier = Modifier) {
    val gc = LocalGameColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFFF6D00).copy(alpha = 0.55f),
                        Color(0xFFFFD54F).copy(alpha = 0.42f),
                        Color(0xFFFF4081).copy(alpha = 0.40f)
                    )
                )
            )
            .border(1.dp, gc.border.copy(alpha = 0.25f), RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Whatshot,
            contentDescription = null,
            tint = Color(0xFFFFF3E0),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "x$comboCount",
            fontSize = 14.sp,
            color = White,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "+$points",
            fontSize = 14.sp,
            color = White,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(6.dp))
        Box(
            modifier = Modifier.size(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                drawCircle(
                    color = Color(0xFF1CB0F6),
                    radius = size.minDimension / 2 - 1.2f,
                    center = center
                )
            }
        }
    }
}

@Composable
fun NeumorphicButton(
    onClick: () -> Unit,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val gc = LocalGameColors.current
    val fg = gc.textPrimary
    val bgTop = if (gc.isDark) Color.White.copy(alpha = 0.08f) else gc.cardBackground
    val bgBot = if (gc.isDark) Color.Black.copy(alpha = 0.22f) else gc.border.copy(alpha = 0.25f)

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(colors = listOf(bgTop, bgBot))
            )
            .border(1.dp, gc.border.copy(alpha = 0.4f), CircleShape)
            .shadow(
                elevation = 8.dp,
                shape = CircleShape,
                ambientColor = Color.Black.copy(alpha = if (gc.isDark) 0.65f else 0.15f),
                spotColor = Color.Black.copy(alpha = if (gc.isDark) 0.08f else 0.05f)
            )
            .alpha(if (enabled) 1.0f else 0.5f)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg.copy(alpha = if (enabled) 1.0f else 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun CustomKeyboard(
    onKeyPress: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDisabled: Boolean = false
) {
    // QWERTY layout - standard phone keyboard
    val row1 = listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P")
    val row2 = listOf("A", "S", "D", "F", "G", "H", "J", "K", "L")
    val row3 = listOf("Z", "X", "C", "V", "B", "N", "M")

    val gc = LocalGameColors.current
    val kbShape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)

    Surface(
        modifier = modifier
            .padding(top = 10.dp)
            .alpha(if (isDisabled) 0.5f else 1.0f),
        shape = kbShape,
        color = if (gc.isDark) Color.White.copy(alpha = 0.06f) else gc.cardBackground,
        tonalElevation = 0.dp,
        shadowElevation = if (gc.isDark) 10.dp else 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, gc.border.copy(alpha = 0.25f), kbShape)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // First row: Q-P
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row1.forEach { letter ->
                    KeyboardKey(
                        text = letter,
                        onClick = { onKeyPress(letter.lowercase()) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Second row: A-L
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row2.forEach { letter ->
                    KeyboardKey(
                        text = letter,
                        onClick = { onKeyPress(letter.lowercase()) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Third row: Z-M with Backspace on the right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                row3.forEach { letter ->
                    KeyboardKey(
                        text = letter,
                        onClick = { onKeyPress(letter.lowercase()) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Backspace button in its usual place (top-right)
                KeyboardKey(
                    text = "⌫",
                    onClick = { onKeyPress("BACKSPACE") },
                    modifier = Modifier.weight(1.5f),
                    isSpecialKey = true
                )
            }

            // Bottom row: Spacebar (enlarged) and Enter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Enlarged Spacebar
                KeyboardKey(
                    text = "SPACE",
                    onClick = { onKeyPress("SPACE") },
                    modifier = Modifier.weight(6f),
                    isSpecialKey = true
                )

                // Enter button
                KeyboardKey(
                    text = "ENTER",
                    onClick = { onKeyPress("ENTER") },
                    modifier = Modifier.weight(2f),
                    isSpecialKey = true,
                    icon = Icons.Default.Check
                )
            }
        }
    }
}

@Composable
fun KeyboardKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSpecialKey: Boolean = false,
    icon: ImageVector? = null
) {
    val gc = LocalGameColors.current
    val shape = RoundedCornerShape(10.dp)
    var isPressed by remember { mutableStateOf(false) }

    val backgroundColor by animateColorAsState(
        targetValue = when {
            gc.isDark -> when {
                isPressed -> Color.White.copy(alpha = 0.25f)
                isSpecialKey -> Color.White.copy(alpha = 0.10f)
                else -> Color.White.copy(alpha = 0.07f)
            }
            else -> when {
                isPressed -> gc.border.copy(alpha = 0.5f)
                isSpecialKey -> gc.buttonBackground
                else -> gc.surface
            }
        },
        animationSpec = tween(durationMillis = 100),
        label = "keyBackground"
    )

    val keyTextColor = gc.textPrimary

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "keyScale"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(color = backgroundColor)
            .border(1.dp, gc.border.copy(alpha = if (gc.isDark) 0.10f else 0.25f), shape)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = { onClick() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (icon != null && text == "ENTER") {
            Icon(
                imageVector = icon,
                contentDescription = text.ifEmpty { "Key" },
                tint = keyTextColor,
                modifier = Modifier.size(22.dp)
            )
        } else {
            Text(
                text = text,
                color = keyTextColor,
                fontSize = when {
                    isSpecialKey && text == "SPACE" -> 12.sp
                    isSpecialKey && text == "⌫" -> 20.sp
                    isSpecialKey -> 12.sp
                    else -> 15.sp
                },
                fontWeight = if (isPressed) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}


