package com.app.gectyping

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.gectyping.ui.theme.LocalGameColors

enum class NavTab { HOME, SPELLING, STREAK, SHOP, ACCOUNT, MENU }

@Composable
fun BottomNavBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalGameColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(colors.cardBackground)
            .padding(horizontal = 4.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        NavItemPng(R.drawable.icons8_homepage_96,  "Home",     NavTab.HOME,     selectedTab, onTabSelected)
        NavItemPng(R.drawable.icons8_dyslexia_96,  "Spelling", NavTab.SPELLING, selectedTab, onTabSelected)
        NavItemPng(R.drawable.icons8_fire_96,      "Streak",   NavTab.STREAK,   selectedTab, onTabSelected)
        NavItemPng(R.drawable.icons8_shop_96,      "Shop",     NavTab.SHOP,     selectedTab, onTabSelected)
        NavItemPng(R.drawable.icons8_avatar_96,    "Account",  NavTab.ACCOUNT,  selectedTab, onTabSelected)
        NavItemPng(R.drawable.icons8_menu_96,      "Menu",     NavTab.MENU,     selectedTab, onTabSelected)
    }
}

@Composable
private fun NavItemPng(
    iconRes: Int,
    label: String,
    tab: NavTab,
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    showBadge: Boolean = false
) {
    val colors = LocalGameColors.current
    val isSelected = tab == selectedTab

    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.12f else 1f,
        animationSpec = tween(200, easing = FastOutSlowInEasing),
        label = "navScale"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.6f,
        animationSpec = tween(200),
        label = "navAlpha"
    )

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onTabSelected(tab) }
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .graphicsLayer { 
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            androidx.compose.foundation.Image(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(28.dp)
            )
            if (showBadge) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.TopEnd)
                        .background(Color(0xFFEF4444), shape = RoundedCornerShape(5.dp))
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = if (isSelected) colors.textPrimary else colors.textSecondary,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
