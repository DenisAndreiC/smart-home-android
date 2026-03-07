package com.denis.smarthome.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.ui.theme.*

private fun deviceIcon(deviceType: String, name: String): ImageVector {
    val type = deviceType.lowercase()
    val nameLower = name.lowercase()
    return when {
        type == "tv" || nameLower.contains("tv") || nameLower.contains("television") -> Icons.Default.Tv
        type == "ac" || nameLower.contains("air") || nameLower.contains("ac") -> Icons.Default.AcUnit
        nameLower.contains("light") || nameLower.contains("bulb") || nameLower.contains("lamp") -> Icons.Default.Lightbulb
        nameLower.contains("fan") -> Icons.Default.Air
        nameLower.contains("coffee") -> Icons.Default.LocalCafe
        else -> Icons.Default.Devices
    }
}

@Composable
fun DeviceListItem(
    device: DeviceResponse,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Device icon box
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0D2B35)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = deviceIcon(device.device_type, device.name),
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                // Name + status dot
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        color = OnBackground,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                if (device.is_active) Color(0xFF4CAF50) else ErrorColor
                            )
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${device.room} • ${if (device.is_active) "Online" else "Offline"}",
                    color = OnSurface,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "LAST USED: RECENTLY",
                    color = OnSurface.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Toggle switch — consumes its own click, won't propagate to parent
            Switch(
                checked = device.is_active,
                onCheckedChange = onToggle,
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
