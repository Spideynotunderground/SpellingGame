package com.app.gectyping

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.gectyping.ui.theme.LocalGameColors
import com.app.gectyping.ui.theme.ThemeWorld

/**
 * ============================================================================
 * MAIN MENU SCREEN — Container with Bottom Nav Bar + 4 Sections
 *
 * Sections: Home, Shop, Achievements, Menu
 * The nav bar is always visible (hidden during gameplay).
 * ============================================================================
 */
@Composable
fun MainMenuScreen(
    context: Context,
    playerName: String,
    avatarEmoji: String,
    lives: Int,
    maxLives: Int,
    diamonds: Int,
    xp: Int,
    stars: Int,
    dailyStreak: Int,
    navigationBarHeightDp: Int = 0,
    // Shop data
    avatars: List<AvatarItem>,
    ownedAvatars: Set<String>,
    selectedAvatarId: String,
    onBuyAvatar: (AvatarItem) -> Unit,
    onSelectAvatar: (String) -> Unit,
    // Theme world shop
    ownedWorlds: Set<String> = setOf(ThemeWorld.SPACE.name),
    currentWorld: ThemeWorld = ThemeWorld.SPACE,
    onBuyWorld: (ThemeWorld) -> Unit = {},
    onApplyWorld: (ThemeWorld) -> Unit = {},
    onBuyLife: () -> Unit,
    // Music shop data
    ownedMusicTracks: Set<String> = emptySet(),
    selectedMusicTrackId: String = "default",
    onBuyMusicTrack: (MusicTrackItem) -> Unit = {},
    onSelectMusicTrack: (String) -> Unit = {},
    onPreviewMusicTrack: (String) -> Unit = {},
    onStopMusicPreview: () -> Unit = {},
    // Callbacks
    onStartGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onNameChange: (String) -> Unit,
    onChangePhoto: () -> Unit,
    onDiamondsChange: (Int) -> Unit,
    onStarsChange: (Int) -> Unit = {},
    onStartLesson: (LearningUnit, Lesson) -> Unit = { _, _ -> },
    // Spelling level progress
    currentLevel: Int = 1,
    totalStarsEarned: Int = 0,
    nextLevelStars: Int = 35
) {
    val colors = LocalGameColors.current
    var selectedTab by remember { mutableStateOf(NavTab.HOME) }
    
    LaunchedEffect(selectedTab) {
        if (selectedTab != NavTab.SHOP) {
            onStopMusicPreview()
        }
    }

    // Rewards screen state
    var showRewardsScreen by remember { mutableStateOf(false) }
    
    // Check for unclaimed quest rewards
    val hasUnclaimedRewards = remember(diamonds) {
        try {
            val state = DailyQuestsManager.getQuestsState(context)
            state.quests.any { it.isCompleted && !it.isClaimed } ||
                (state.allCompleted && !state.bonusChestClaimed)
        } catch (e: Exception) { false }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Content area — takes remaining space above nav bar
        Box(modifier = Modifier.weight(1f)) {
            Crossfade(
                targetState = selectedTab,
                animationSpec = tween(300),
                label = "tabCrossfade"
            ) { tab ->
                when (tab) {
                    // HOME = Learning Path (изучение слов)
                    NavTab.HOME -> {
                        if (showRewardsScreen) {
                            RewardsScreen(
                                context = context,
                                diamonds = diamonds,
                                onDiamondsChange = onDiamondsChange,
                                onBack = { showRewardsScreen = false }
                            )
                        } else {
                            HomeSection(
                                playerName = playerName,
                                avatarEmoji = avatarEmoji,
                                lives = lives,
                                maxLives = maxLives,
                                diamonds = diamonds,
                                xp = xp,
                                stars = stars,
                                dailyStreak = dailyStreak,
                                onStartGame = onStartGame,
                                context = context,
                                onOpenRewards = { showRewardsScreen = true },
                                hasUnclaimedRewards = hasUnclaimedRewards
                            )
                        }
                    }

                    // SPELLING = Spelling Game (текущая игра)
                    NavTab.SPELLING -> SpellingGameSection(
                        playerName = playerName,
                        avatarEmoji = avatarEmoji,
                        lives = lives,
                        maxLives = maxLives,
                        diamonds = diamonds,
                        stars = stars,
                        dailyStreak = dailyStreak,
                        currentLevel = currentLevel,
                        totalStarsEarned = totalStarsEarned,
                        nextLevelStars = nextLevelStars,
                        onStartGame = onStartGame
                    )

                    NavTab.SHOP -> ShopSection(
                        diamonds = diamonds,
                        lives = lives,
                        maxLives = maxLives,
                        avatars = avatars,
                        ownedAvatars = ownedAvatars,
                        selectedAvatarId = selectedAvatarId,
                        onBuyAvatar = onBuyAvatar,
                        onSelectAvatar = onSelectAvatar,
                        ownedWorlds = ownedWorlds,
                        currentWorld = currentWorld,
                        onBuyWorld = onBuyWorld,
                        onApplyWorld = onApplyWorld,
                        onBuyLife = onBuyLife,
                        ownedMusicTracks = ownedMusicTracks,
                        selectedMusicTrackId = selectedMusicTrackId,
                        onBuyMusicTrack = onBuyMusicTrack,
                        onSelectMusicTrack = onSelectMusicTrack,
                        onPreviewMusicTrack = onPreviewMusicTrack,
                        onStopPreview = onStopMusicPreview
                    )

                    NavTab.STREAK -> StreakSection(
                        context = context,
                        diamonds = diamonds,
                        stars = stars,
                        onDiamondsChange = onDiamondsChange,
                        onStarsChange = onStarsChange
                    )

                    NavTab.ACCOUNT -> AccountScreen(
                        context = context,
                        playerName = playerName,
                        avatarEmoji = avatarEmoji,
                        selectedAvatarId = selectedAvatarId,
                        diamonds = diamonds,
                        xp = xp
                    )

                    NavTab.MENU -> MenuSection(
                        playerName = playerName,
                        avatarEmoji = avatarEmoji,
                        selectedAvatarId = selectedAvatarId,
                        onNameChange = onNameChange,
                        onChangePhoto = onChangePhoto,
                        onOpenSettings = onOpenSettings
                    )
                }
            }
        }

        // Bottom Nav Bar — always visible, above system navigation
        BottomNavBar(
            selectedTab = selectedTab,
            onTabSelected = { tab ->
                if (tab != NavTab.HOME) showRewardsScreen = false
                selectedTab = tab
            },
            modifier = Modifier.padding(bottom = navigationBarHeightDp.dp)
        )
    }
}
