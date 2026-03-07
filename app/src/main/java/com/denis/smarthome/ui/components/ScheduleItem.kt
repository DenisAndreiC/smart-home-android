package com.denis.smarthome.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denis.smarthome.viewmodel.Schedule
import com.denis.smarthome.ui.theme.*

@Composable
fun ScheduleItem(
    schedule: Schedule,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val iconTint = when (schedule.icon) {
        "sunset" -> Color(0xFFFF9800)
        "night"  -> Color(0xFF90CAF9)
        else     -> Primary
    }
    val icon = when (schedule.icon) {
        "sunset" -> Icons.Default.WbSunny
        "night"  -> Icons.Default.DarkMode
        else     -> Icons.Default.WbSunny
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.name,
                    color = OnBackground,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${schedule.repeat} • ${schedule.time}",
                    color = OnSurface,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(
                checked = schedule.isOn,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = OnSurface,
                    uncheckedTrackColor = SurfaceVariant
                )
            )
        }
    }
}
