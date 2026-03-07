package com.denis.smarthome.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.denis.smarthome.ui.theme.OnSurface
import com.denis.smarthome.ui.theme.Primary
import com.denis.smarthome.ui.theme.SurfaceVariant

private val SWITCH_WIDTH  = 120.dp
private val SWITCH_HEIGHT = 56.dp
private val THUMB_SIZE    = 44.dp
private val PADDING       = 6.dp

@Composable
fun LargeToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val trackColor by animateColorAsState(
        targetValue = if (checked) Primary else SurfaceVariant,
        animationSpec = tween(300),
        label = "trackColor"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) Color.White else OnSurface,
        animationSpec = tween(300),
        label = "thumbColor"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) SWITCH_WIDTH - THUMB_SIZE - PADDING else PADDING,
        animationSpec = tween(300),
        label = "thumbOffset"
    )

    Box(
        modifier = modifier
            .size(width = SWITCH_WIDTH, height = SWITCH_HEIGHT)
            .clip(RoundedCornerShape(SWITCH_HEIGHT / 2))
            .background(trackColor)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset)
                .size(THUMB_SIZE)
                .clip(CircleShape)
                .background(thumbColor)
        )
    }
}
