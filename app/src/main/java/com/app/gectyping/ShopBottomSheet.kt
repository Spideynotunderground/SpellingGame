package com.app.gectyping

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.gectyping.ui.theme.LocalGameColors
import com.app.gectyping.ui.theme.ThemeWorld

/**
 * ============================================================================
 * SHOP BOTTOM SHEET — Shop as a modal bottom sheet
 * ============================================================================
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopBottomSheet(
    diamonds: Int,
    lives: Int,
    maxLives: Int,
    avatars: List<AvatarItem>,
    ownedAvatars: Set<String>,
    selectedAvatarId: String,
    onBuyAvatar: (AvatarItem) -> Unit,
    onSelectAvatar: (String) -> Unit,
    ownedWorlds: Set<String>,
    currentWorld: ThemeWorld,
    onBuyWorld: (ThemeWorld) -> Unit,
    onApplyWorld: (ThemeWorld) -> Unit,
    onBuyLife: () -> Unit,
    ownedMusicTracks: Set<String>,
    selectedMusicTrackId: String,
    onBuyMusicTrack: (MusicTrackItem) -> Unit,
    onSelectMusicTrack: (String) -> Unit,
    onPreviewMusicTrack: (String) -> Unit,
    onStopPreview: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalGameColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.cardBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        ShopSection(
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
            onStopPreview = onStopPreview
        )
    }
}
