package com.app.gectyping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors

/**
 * ============================================================================
 * SETTINGS BOTTOM SHEET
 *
 * Order (as requested):
 *  1. Voice Volume (TTS/Google Chirp 3HD)
 *  2. Music Volume
 *  3. Toggles: Music On/Off, Vibration On/Off
 *
 * All state is hoisted; caller persists values.
 * ============================================================================
 */
@Composable
fun SettingsContent(
    vibrationEnabled: Boolean,
    onVibrationChange: (Boolean) -> Unit,
    soundEnabled: Boolean,
    onSoundChange: (Boolean) -> Unit,
    musicEnabled: Boolean,
    onMusicChange: (Boolean) -> Unit,

    // Mixer sliders (0..1)
    masterVolume: Float,
    onMasterVolumeChange: (Float) -> Unit,
    musicVolume: Float,
    onMusicVolumeChange: (Float) -> Unit,
    sfxVolume: Float,
    onSfxVolumeChange: (Float) -> Unit,
    
    // Voice volume (TTS)
    voiceVolume: Float = 1f,
    onVoiceVolumeChange: (Float) -> Unit = {},

    // Optional / kept for compatibility
    isDarkTheme: Boolean = true,
    onThemeChange: (Boolean) -> Unit = {},

    // TTS voice settings — wired to Google Cloud TTS API
    ttsPitch: Float = -1.0f,
    onTtsPitchChange: (Float) -> Unit = {},
    ttsRate: Float = 1.0f,
    onTtsRateChange: (Float) -> Unit = {}
) {
    val colors = LocalGameColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ---------- Title ----------
        Text(
            text = "Settings",
            color = colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // ---------- 1. Voice Volume (TTS / Google Chirp 3HD) ----------
        MixerSliderRow(
            icon = com.app.gectyping.R.drawable.icons8_music_96,
            label = "Voice Volume",
            value = voiceVolume,
            onValueChange = onVoiceVolumeChange,
            enabled = soundEnabled,
            accentColor = colors.accent
        )

        SettingsDivider()

        // ---------- 2. Music Volume ----------
        MixerSliderRow(
            icon = com.app.gectyping.R.drawable.icons8_music_96,
            label = "Music Volume",
            value = musicVolume,
            onValueChange = onMusicVolumeChange,
            enabled = musicEnabled,
            accentColor = colors.accent
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ---------- 3. TTS Voice Speed ----------
        // Range: 0.50 (very slow, every syllable clear) to 1.20 (slightly faster than normal)
        // Default: 0.80 — slower than natural but not robotic, ideal for spelling games
        val rateLabel = when {
            ttsRate < 0.65f -> "Very Slow"
            ttsRate < 0.85f -> "Slow"
            ttsRate < 1.00f -> "Normal"
            ttsRate < 1.10f -> "Fast"
            else            -> "Very Fast"
        }
        TtsSliderRow(
            label = "Voice Speed",
            value = ttsRate,
            onValueChange = onTtsRateChange,
            valueRange = 0.50f..1.20f,
            displayValue = rateLabel,
            accentColor = colors.accent
        )

        SettingsDivider()

        // ---------- 4. TTS Voice Pitch ----------
        // Range: -4.0 (deeper/clearer on phone speakers) to +4.0 (higher)
        // Default: -1.0 — slightly lower than natural, cuts through better on earphones
        val pitchLabel = when {
            ttsPitch < -2.5f -> "Very Low"
            ttsPitch < -0.5f -> "Low"
            ttsPitch <  0.5f -> "Normal"
            ttsPitch <  2.5f -> "High"
            else             -> "Very High"
        }
        TtsSliderRow(
            label = "Voice Pitch",
            value = ttsPitch,
            onValueChange = onTtsPitchChange,
            valueRange = -4.0f..4.0f,
            displayValue = pitchLabel,
            accentColor = colors.accent
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ---------- 5. Toggles ----------
        // Music toggle
        SettingsToggleRow(
            icon = com.app.gectyping.R.drawable.icons8_music_96,
            label = "Music",
            description = if (musicEnabled) "On" else "Off",
            checked = musicEnabled,
            onCheckedChange = onMusicChange,
            accentColor = colors.accent
        )

        SettingsDivider()

        // Vibration toggle
        SettingsToggleRow(
            icon = com.app.gectyping.R.drawable.icons8_star_96,
            label = "Vibration",
            description = if (vibrationEnabled) "On" else "Off",
            checked = vibrationEnabled,
            onCheckedChange = onVibrationChange,
            accentColor = colors.accent
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingsDivider() {
    val colors = LocalGameColors.current
    HorizontalDivider(
        color = colors.textSecondary.copy(alpha = 0.15f),
        thickness = 1.dp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

// ============================================================
// Mixer slider row (Master / Music / SFX) + dB display
// ============================================================
@Composable
private fun MixerSliderRow(
    icon: Int,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean,
    accentColor: Color
) {
    val colors = LocalGameColors.current

    val pct = (value.coerceIn(0f, 1f) * 100).toInt()
    val db = AppAudioMixer.sliderToDb(value)
    val dbText = if (pct <= 0) "−∞ dB" else "%.1f dB".format(db)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )

            Column {
                Text(
                    text = label,
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$pct% • $dbText",
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = value.coerceIn(0f, 1f),
            onValueChange = { onValueChange(it.coerceIn(0f, 1f)) },
            enabled = enabled,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = colors.textSecondary.copy(alpha = 0.2f),
                disabledThumbColor = colors.textSecondary.copy(alpha = 0.4f),
                disabledActiveTrackColor = colors.textSecondary.copy(alpha = 0.3f),
                disabledInactiveTrackColor = colors.textSecondary.copy(alpha = 0.1f)
            )
        )
    }
}

// ============================================================
// Reusable toggle row
// ============================================================
@Composable
private fun SettingsToggleRow(
    icon: Int,
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    accentColor: Color
) {
    val colors = LocalGameColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground.copy(alpha = 0.5f))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = label,
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = description,
                    color = colors.textSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentColor,
                uncheckedThumbColor = colors.textSecondary,
                uncheckedTrackColor = colors.cardBackground
            )
        )
    }
}

// ============================================================
// TTS pitch / rate slider row
// ============================================================
@Composable
private fun TtsSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    displayValue: String,
    accentColor: Color
) {
    val colors = LocalGameColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.cardBackground.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                painter = painterResource(id = com.app.gectyping.R.drawable.icons8_music_96),
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = displayValue,
                color = colors.textSecondary,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = colors.textSecondary.copy(alpha = 0.2f)
            )
        )
    }
}
