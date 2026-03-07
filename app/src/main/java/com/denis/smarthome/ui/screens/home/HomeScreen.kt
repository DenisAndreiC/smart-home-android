package com.denis.smarthome.ui.screens.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
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
import com.denis.smarthome.ui.components.*
import com.denis.smarthome.ui.navigation.NavRoutes
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.HomeViewModel
import com.denis.smarthome.viewmodel.RoomInfo

// Gradient palettes per room slot (cycling)
private val roomGradients = listOf(
    listOf(Color(0xFF0D3545), Background),  // teal-dark (Living)
    listOf(Color(0xFF2A1535), Background),  // purple-dark (Bedroom)
    listOf(Color(0xFF2A1F0D), Background),  // amber-dark (Kitchen)
    listOf(Color(0xFF0D2A1A), Background),  // green-dark
    listOf(Color(0xFF2A0D0D), Background),  // red-dark
)

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val greeting = viewModel.greeting
    val currentDate = viewModel.currentDate
    val userName = user?.name?.split(" ")?.firstOrNull() ?: ""

    var activeChipIndex by remember { mutableStateOf(-1) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // ── Greeting Header ──────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Avatar
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = OnSurface,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (userName.isNotBlank()) "$greeting, $userName"
                                       else greeting,
                                color = OnBackground,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = currentDate,
                                color = OnSurface,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Notification icon with badge
                    Box {
                        IconButton(onClick = { }) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = Primary
                            )
                        }
                        // Badge
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ErrorColor)
                                .align(Alignment.TopEnd)
                                .offset(x = (-10).dp, y = 10.dp)
                        )
                    }
                }
            }

            // ── Stats ────────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionTitle(title = "Statistici")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        StatCard(
                            icon = Icons.Default.Devices,
                            label = "Dispozitive",
                            value = "${stats?.total_devices ?: 12}",
                            change = "+2 adăugate"
                        )
                    }
                    item {
                        StatCard(
                            icon = Icons.Default.Bolt,
                            label = "Consum",
                            value = "4.2 kWh",
                            change = "+0.5% azi"
                        )
                    }
                    item {
                        StatCard(
                            icon = Icons.Default.AutoAwesome,
                            label = "Automatizări",
                            value = "${stats?.total_commands_today ?: 5}",
                            change = "active"
                        )
                    }
                }
            }

            // ── Quick Actions ────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionTitle(title = "Acțiuni Rapide")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        QuickActionChip(
                            label = "All Off",
                            icon = Icons.Default.PowerSettingsNew,
                            isActive = activeChipIndex == 0,
                            onClick = {
                                activeChipIndex = if (activeChipIndex == 0) -1 else 0
                                Log.d("HomeScreen", "All Off triggered")
                            }
                        )
                    }
                    item {
                        QuickActionChip(
                            label = "Movie Mode",
                            icon = Icons.Default.Movie,
                            isActive = activeChipIndex == 1,
                            onClick = {
                                activeChipIndex = if (activeChipIndex == 1) -1 else 1
                                Log.d("HomeScreen", "Movie Mode triggered")
                            }
                        )
                    }
                    item {
                        QuickActionChip(
                            label = "Good Night",
                            icon = Icons.Default.DarkMode,
                            isActive = activeChipIndex == 2,
                            onClick = {
                                activeChipIndex = if (activeChipIndex == 2) -1 else 2
                                Log.d("HomeScreen", "Good Night triggered")
                            }
                        )
                    }
                    item {
                        QuickActionChip(
                            label = "Away",
                            icon = Icons.Default.DirectionsRun,
                            isActive = activeChipIndex == 3,
                            onClick = {
                                activeChipIndex = if (activeChipIndex == 3) -1 else 3
                                Log.d("HomeScreen", "Away triggered")
                            }
                        )
                    }
                }
            }

            // ── Rooms ────────────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionTitle(
                    title = "Camere",
                    action = {
                        TextButton(onClick = {
                            navController.navigate(NavRoutes.Devices.route)
                        }) {
                            Text("Toate", color = Primary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                )

                // Loading indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Primary)
                    }
                } else {
                    RoomsGrid(
                        rooms = rooms,
                        onRoomClick = { navController.navigate(NavRoutes.Devices.route) },
                        onAddRoomClick = { Log.d("HomeScreen", "Add room clicked") }
                    )
                }
            }
        }

        // ── FAB ──────────────────────────────────────────────────────────────
        FloatingActionButton(
            onClick = { Log.d("HomeScreen", "FAB add device clicked") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp),
            containerColor = Primary,
            contentColor = Color.Black,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add device")
        }
    }
}

@Composable
private fun RoomsGrid(
    rooms: List<RoomInfo>,
    onRoomClick: (String) -> Unit,
    onAddRoomClick: () -> Unit
) {
    // Show up to 3 rooms, then "Add" card — always 4 slots in 2×2 grid
    val displayRooms = if (rooms.isEmpty()) {
        listOf(
            RoomInfo("Living Room", 3, 2),
            RoomInfo("Bedroom", 2, 1),
            RoomInfo("Kitchen", 1, 1)
        )
    } else rooms.take(3)

    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (displayRooms.isNotEmpty()) {
                RoomCard(
                    name = displayRooms[0].name,
                    activeCount = displayRooms[0].activeCount,
                    deviceCount = displayRooms[0].deviceCount,
                    gradientColors = roomGradients[0],
                    onClick = { onRoomClick(displayRooms[0].name) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            if (displayRooms.size >= 2) {
                RoomCard(
                    name = displayRooms[1].name,
                    activeCount = displayRooms[1].activeCount,
                    deviceCount = displayRooms[1].deviceCount,
                    gradientColors = roomGradients[1],
                    onClick = { onRoomClick(displayRooms[1].name) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                AddRoomCard(
                    onClick = onAddRoomClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Row 2
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (displayRooms.size >= 3) {
                RoomCard(
                    name = displayRooms[2].name,
                    activeCount = displayRooms[2].activeCount,
                    deviceCount = displayRooms[2].deviceCount,
                    gradientColors = roomGradients[2],
                    onClick = { onRoomClick(displayRooms[2].name) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            AddRoomCard(
                onClick = onAddRoomClick,
                modifier = Modifier.weight(1f)
            )
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
}
