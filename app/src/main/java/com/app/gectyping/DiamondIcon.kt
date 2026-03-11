package com.app.gectyping

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Иконка алмаза — PNG версия icons8-diamond-96.
 * Используется везде вместо эмодзи 💎.
 */
@Composable
fun DiamondIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Image(
        painter = painterResource(id = R.drawable.icons8_diamond_96),
        contentDescription = "Diamonds",
        modifier = modifier.requiredSize(size)
    )
}
