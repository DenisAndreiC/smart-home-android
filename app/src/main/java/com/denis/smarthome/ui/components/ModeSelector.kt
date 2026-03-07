package com.denis.smarthome.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.AcMode

private data class ModeItem(val mode: AcMode, val label: String, val icon: ImageVector)

private val modeItems = listOf(
    ModeItem(AcMode.COOL, "Cool", Icons.Default.AcUnit),
    ModeItem(AcMode.HEAT, "Heat", Icons.Default.LocalFireDepartment),
    ModeItem(AcMode.FAN, "Fan", Icons.Default.Air),
    ModeItem(AcMode.DRY, "Dry", Icons.Default.WaterDrop),
    ModeItem(AcMode.AUTO, "Auto", Icons.Default.AutoMode)
)

@Composable
fun ModeSelector(
    selectedMode: AcMode,
    onModeSelected: (AcMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modeItems.forEach { item ->
            val isSelected = selectedMode == item.mode
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) PrimaryContainer else SurfaceVariant)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) Primary else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable { onModeSelected(item.mode) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (isSelected) Primary else OnSurface,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = item.label,
                    color = if (isSelected) Primary else OnSurface,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}
