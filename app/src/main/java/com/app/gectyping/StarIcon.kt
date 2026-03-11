package com.app.gectyping

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Иконка звезды — PNG версия icons8-star-96.
 * Используется везде вместо эмодзи ⭐ и 🌟.
 */
@Composable
fun StarIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp
) {
    Image(
        painter = painterResource(id = R.drawable.icons8_star_96),
        contentDescription = "Star",
        modifier = modifier.requiredSize(size)
    )
}
