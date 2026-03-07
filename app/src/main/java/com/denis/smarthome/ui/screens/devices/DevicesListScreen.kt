package com.denis.smarthome.ui.screens.devices

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.ui.navigation.NavRoutes
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.DevicesViewModel

@Composable
fun DevicesListScreen(
    navController: NavController,
    viewModel: DevicesViewModel = viewModel()
) {
    val devices by viewModel.devices.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var selectedRoom by remember { mutableStateOf("All") }
    val rooms by viewModel.rooms.collectAsState()

    val allRooms = listOf("All") + rooms
    val filtered = if (selectedRoom == "All") devices else devices.filter { it.room == selectedRoom }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Surface)
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("My Devices", style = MaterialTheme.typography.headlineSmall, color = OnBackground, fontWeight = FontWeight.Bold)
                    Text("${devices.size} device${if (devices.size != 1) "s" else ""} total", style = MaterialTheme.typography.bodySmall, color = OnSurface)
                }
                IconButton(onClick = { viewModel.loadDevices() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Primary)
                }
            }
        }

        // Room filter chips
        if (allRooms.size > 1) {
            LazyRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(allRooms) { room ->
                    FilterChip(
                        selected = selectedRoom == room,
                        onClick = { selectedRoom = room },
                        label = { Text(room) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Primary,
                            selectedLabelColor = Color.Black,
                            containerColor = Surface,
                            labelColor = OnSurface
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedRoom == room,
                            borderColor = Outline,
                            selectedBorderColor = Primary
                        )
                    )
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Primary)
            }
        } else if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.DevicesOther, contentDescription = null, tint = OnSurface, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No devices found", color = OnSurface, style = MaterialTheme.typography.bodyLarge)
                    Text("Add a device to get started", color = OnSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { device ->
                    DeviceCard(
                        device = device,
                        onClick = { navController.navigate(NavRoutes.DeviceControl.createRoute(device.id)) },
                        onDelete = { viewModel.deleteDevice(device.id) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceCard(
    device: DeviceResponse,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val deviceIcon = when (device.device_type.lowercase()) {
        "tv" -> Icons.Default.Tv
        "ac", "air_conditioner" -> Icons.Default.AcUnit
        "light", "lamp", "bulb" -> Icons.Default.Lightbulb
        "fan" -> Icons.Default.Air
        else -> Icons.Default.Devices
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (device.is_active) Primary.copy(alpha = 0.4f) else Outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (device.is_active) PrimaryContainer else SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    deviceIcon,
                    contentDescription = null,
                    tint = if (device.is_active) Primary else OnSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(device.name, color = OnBackground, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                Text(device.room, color = OnSurface, style = MaterialTheme.typography.bodySmall)
                Text(device.device_type, color = OnSurface.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (device.is_active) Color(0xFF4CAF50).copy(alpha = 0.15f) else ErrorColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (device.is_active) "Online" else "Offline",
                        color = if (device.is_active) Color(0xFF4CAF50) else ErrorColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ErrorColor, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
