package com.app.gectyping

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Иконка молнии (⚡) — PNG версия icons8-high-voltage-48.
 * Используется везде вместо эмодзи Text("⚡").
 */
@Composable
fun LightningIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Image(
        painter = painterResource(id = R.drawable.icons8_high_voltage_48),
        contentDescription = "XP",
        modifier = modifier.requiredSize(size)
    )
}
