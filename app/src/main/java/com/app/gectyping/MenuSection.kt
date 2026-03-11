package com.app.gectyping

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors

/**
 * ============================================================================
 * MENU SECTION — Profile + Settings access
 *
 * - Name editing
 * - Avatar / photo change
 * - Opens Settings sub-menu (reuses SettingsContent)
 * ============================================================================
 */
@Composable
fun MenuSection(
    playerName: String,
    avatarEmoji: String,
    selectedAvatarId: String = "space",
    onNameChange: (String) -> Unit,
    onChangePhoto: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = LocalGameColors.current
    var isEditing by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf(playerName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Profile",
            color = colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ---------- Avatar ----------
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(colors.textPrimary.copy(alpha = 0.08f))
                    .border(2.dp, colors.accent.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AvatarIcon(avatar = avatarById(selectedAvatarId), size = 60.dp, fontSize = 50.sp)
            }

            // Camera button overlay
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(colors.accent)
                    .clickable { onChangePhoto() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = com.app.gectyping.R.drawable.icons8_star_96),
                    contentDescription = "Change photo",
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // ---------- Name ----------
        if (isEditing) {
            OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it.take(24) },
                singleLine = true,
                label = { Text("Your name") },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = { isEditing = false; nameInput = playerName }) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val cleaned = nameInput.trim()
                        if (cleaned.isNotBlank()) {
                            onNameChange(cleaned)
                            isEditing = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent)
                ) {
                    Text("Save", color = Color.White)
                }
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = playerName.ifBlank { "No name" },
                    color = colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(onClick = { isEditing = true; nameInput = playerName }) {
                    Icon(
                        painter = painterResource(id = com.app.gectyping.R.drawable.icons8_hand_with_pen_writing_96),
                        contentDescription = "Edit name",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(color = colors.textSecondary.copy(alpha = 0.15f))

        Spacer(modifier = Modifier.height(8.dp))

        // ---------- Settings button ----------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.cardBackground)
                .clickable { onOpenSettings() }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                painter = painterResource(id = com.app.gectyping.R.drawable.icons8_settings_96),
                contentDescription = "Settings",
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "Settings",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // ---------- Branding ----------
        Text(
            text = "Powered by Genius English Courses",
            color = colors.textSecondary.copy(alpha = 0.55f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
