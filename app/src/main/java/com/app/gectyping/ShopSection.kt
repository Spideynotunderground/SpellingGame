package com.app.gectyping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

/**
 * ============================================================================
 * SHOP SECTION — Avatars, Themes, Lives, Music
 * ============================================================================
 */

enum class ShopCategory { AVATARS, THEMES, LIVES, MUSIC }

@Composable
fun ShopSection(
    diamonds: Int,
    lives: Int,
    maxLives: Int,
    avatars: List<AvatarItem>,
    ownedAvatars: Set<String>,
    selectedAvatarId: String,
    onBuyAvatar: (AvatarItem) -> Unit,
    onSelectAvatar: (String) -> Unit,
    // Theme world shop
    ownedWorlds: Set<String>,
    currentWorld: ThemeWorld,
    onBuyWorld: (ThemeWorld) -> Unit,
    onApplyWorld: (ThemeWorld) -> Unit,
    onBuyLife: () -> Unit,
    musicTracks: List<MusicTrackItem> = MUSIC_TRACKS,
    ownedMusicTracks: Set<String> = emptySet(),
    selectedMusicTrackId: String = "default",
    onBuyMusicTrack: (MusicTrackItem) -> Unit = {},
    onSelectMusicTrack: (String) -> Unit = {},
    onPreviewMusicTrack: (String) -> Unit = {},
    onStopPreview: () -> Unit = {}
) {
    val colors = LocalGameColors.current
    var selectedCategory by remember { mutableStateOf(ShopCategory.AVATARS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // ---------- Balance bar ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shop",
                color = colors.textPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                DiamondIcon(size = 20.dp)
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = diamonds.toString(),
                    color = Color(0xFF1CB0F6),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---------- Category tabs ----------
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ShopTab("Avatars", Icons.Default.Person, ShopCategory.AVATARS, selectedCategory) { selectedCategory = it }
            ShopTab("Themes", Icons.Default.Palette, ShopCategory.THEMES, selectedCategory) { selectedCategory = it }
            ShopTab("Lives", Icons.Default.Favorite, ShopCategory.LIVES, selectedCategory) { selectedCategory = it }
            ShopTab("Music", Icons.Default.MusicNote, ShopCategory.MUSIC, selectedCategory) { selectedCategory = it }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ---------- Content ----------
        when (selectedCategory) {
            ShopCategory.AVATARS -> AvatarGrid(avatars, ownedAvatars, selectedAvatarId, diamonds, onBuyAvatar, onSelectAvatar)
            ShopCategory.THEMES -> ThemeWorldGrid(ownedWorlds, currentWorld, diamonds, onBuyWorld, onApplyWorld)
            ShopCategory.LIVES -> LivesShop(lives, maxLives, diamonds, onBuyLife)
            ShopCategory.MUSIC -> MusicGrid(
                tracks = musicTracks,
                owned = ownedMusicTracks,
                selectedId = selectedMusicTrackId,
                diamonds = diamonds,
                onBuy = onBuyMusicTrack,
                onSelect = onSelectMusicTrack,
                onPreview = onPreviewMusicTrack,
                onStopPreview = onStopPreview
            )
        }
    }
}

@Composable
private fun RowScope.ShopTab(
    label: String,
    icon: ImageVector,
    tab: ShopCategory,
    selected: ShopCategory,
    onSelect: (ShopCategory) -> Unit
) {
    val colors = LocalGameColors.current
    val isSelected = tab == selected
    val accent = colors.accent

    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) accent.copy(alpha = 0.15f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = if (isSelected) accent else colors.textSecondary.copy(alpha = 0.2f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect(tab) }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, tint = if (isSelected) accent else colors.textSecondary, modifier = Modifier.size(16.dp))
            Text(
                text = label,
                color = if (isSelected) accent else colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun AvatarGrid(
    avatars: List<AvatarItem>,
    owned: Set<String>,
    selectedId: String,
    diamonds: Int,
    onBuy: (AvatarItem) -> Unit,
    onSelect: (String) -> Unit
) {
    val colors = LocalGameColors.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(avatars) { item ->
            val isOwned = owned.contains(item.id)
            val isSelected = item.id == selectedId
            val accent = colors.accent

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardBackground)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accent else colors.textSecondary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable {
                        SoundManager.click()
                        if (isOwned) onSelect(item.id)
                        else if (diamonds >= item.priceDiamonds) onBuy(item)
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AvatarIcon(avatar = item, size = 44.dp, fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = item.title, color = colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                    if (isOwned) {
                        if (isSelected) Text("Equipped", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        else Text("Owned", color = colors.textSecondary, fontSize = 10.sp)
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DiamondIcon(size = 11.dp)
                            Text("${item.priceDiamonds}", color = Color(0xFF1CB0F6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeWorldGrid(
    owned: Set<String>,
    currentWorld: ThemeWorld,
    diamonds: Int,
    onBuy: (ThemeWorld) -> Unit,
    onApply: (ThemeWorld) -> Unit
) {
    val colors = LocalGameColors.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(ThemeWorld.entries.toList()) { world ->
            val palette = worldPalettes[world]!!
            val price = worldPrices[world] ?: 0
            val isOwned = owned.contains(world.name)
            val isActive = world == currentWorld
            val accent = colors.accent

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.background)
                    .border(
                        width = if (isActive) 2.dp else 1.dp,
                        color = if (isActive) palette.accent else palette.textSecondary.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (world.iconRes != null) {
                        Image(
                            painter = painterResource(id = world.iconRes),
                            contentDescription = world.displayName,
                            modifier = Modifier.size(32.dp)
                        )
                    } else {
                        Text(text = world.emoji, fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = world.displayName,
                        color = palette.textPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    when {
                        isActive -> Text("Active", color = palette.accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        isOwned -> {
                            Button(
                                onClick = { onApply(world) },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                            ) {
                                Text("Apply", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            Button(
                                onClick = { if (diamonds >= price) onBuy(world) },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = accent)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DiamondIcon(size = 11.dp)
                                    Text("$price", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LivesShop(lives: Int, maxLives: Int, diamonds: Int, onBuyLife: () -> Unit) {
    val colors = LocalGameColors.current
    val lifeCost = 100

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Image(
            painter = painterResource(id = R.drawable.icons8_heart_96),
            contentDescription = "Lives",
            modifier = Modifier.size(72.dp)
        )

        Text(
            text = "Lives: $lives / $maxLives",
            color = colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Buy extra lives to keep playing!",
            color = colors.textSecondary,
            fontSize = 14.sp
        )

        Button(
            onClick = onBuyLife,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(52.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Buy +1 Life  (", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                DiamondIcon(size = 16.dp)
                Text("$lifeCost)", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (lives >= maxLives) {
            Text(
                text = "You already have max lives!",
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun MusicGrid(
    tracks: List<MusicTrackItem>,
    owned: Set<String>,
    selectedId: String,
    diamonds: Int,
    onBuy: (MusicTrackItem) -> Unit,
    onSelect: (String) -> Unit,
    onPreview: (String) -> Unit,
    onStopPreview: () -> Unit
) {
    val colors = LocalGameColors.current

    if (tracks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("🎵", fontSize = 48.sp)
                Text(
                    text = "Coming Soon…",
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "New tracks will be added soon.",
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    var previewingId by remember { mutableStateOf<String?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(tracks) { track ->
            val isOwned = owned.contains(track.id)
            val isSelected = track.id == selectedId
            val isPreviewing = previewingId == track.id
            val accent = colors.accent

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.cardBackground)
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) accent else colors.textSecondary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable {
                        SoundManager.click()
                        if (isOwned) onSelect(track.id)
                        else if (diamonds >= track.priceDiamonds) onBuy(track)
                    }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (track.iconRes != null) {
                        Image(
                            painter = painterResource(id = track.iconRes),
                            contentDescription = track.title,
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        Text(text = track.emoji, fontSize = 32.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = track.title,
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = track.description,
                        color = colors.textSecondary,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Preview button
                    IconButton(
                        onClick = {
                            if (isPreviewing) {
                                onStopPreview()
                                previewingId = null
                            } else {
                                onStopPreview()
                                onPreview(track.id)
                                previewingId = track.id
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isPreviewing) "Stop" else "Listen",
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    if (isOwned) {
                        if (isSelected) Text("Active", color = accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        else Text("Owned", color = colors.textSecondary, fontSize = 10.sp)
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DiamondIcon(size = 11.dp)
                            Text("${track.priceDiamonds}", color = Color(0xFF1CB0F6), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
