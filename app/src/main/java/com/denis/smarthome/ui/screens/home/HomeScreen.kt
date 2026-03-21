/**
 * HomeScreen.kt - Ecranul principal (dashboard) al aplicatiei SmartHome
 *
 * Afiseaza salutul utilizatorului, statistici despre dispozitive, actiuni rapide
 * si un grid 2x2 cu camerele din casa. Suporta pull-to-refresh prin PullToRefreshBox.
 * Grid-ul este implementat cu doua Row-uri manuale (nu LazyVerticalGrid) pentru a
 * evita nested lazy layouts intr-un LazyColumn parinte.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.screens.home

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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

private val roomGradients = listOf(
    listOf(Color(0xFF0D3545), Background),
    listOf(Color(0xFF2A1535), Background),
    listOf(Color(0xFF2A1F0D), Background),
    listOf(Color(0xFF0D2A1A), Background),
    listOf(Color(0xFF2A0D0D), Background),
)

/**
 * Ecranul principal cu dashboard-ul SmartHome.
 *
 * Foloseste [PullToRefreshBox] pentru tragere in jos (refresh). Continutul este un
 * [LazyColumn] cu: header salut, statistici reale din API, actiuni rapide cu efecte
 * (allOff/awayMode) si grid 2x2 camere derivate din dispozitive.
 *
 * @param navController controlerul de navigare Compose
 * @param viewModel ViewModel-ul care furnizeaza datele dashboard-ului
 */
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val rooms by viewModel.rooms.collectAsState()
    val user by viewModel.user.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    val energyKwh by viewModel.energyKwh.collectAsState()
    val scenes by viewModel.scenes.collectAsState()
    val executingSceneId by viewModel.executingSceneId.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val anomalies by viewModel.anomalies.collectAsState()

    val greeting = viewModel.greeting
    val currentDate = viewModel.currentDate
    val userName = user?.username?.split(" ")?.firstOrNull() ?: ""

    var activeChipIndex by remember { mutableStateOf(-1) }
    var showAddRoomDialog by remember { mutableStateOf(false) }

    if (showAddRoomDialog) {
        AlertDialog(
            onDismissRequest = { showAddRoomDialog = false },
            containerColor = Surface,
            title = { Text("Add Room", color = OnBackground, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "To add a room, go to Devices and add a device with the desired room name. " +
                    "Rooms are created automatically based on your devices.",
                    color = OnSurface
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showAddRoomDialog = false
                    navController.navigate(NavRoutes.Devices.route)
                }) { Text("Go to Devices", color = Primary) }
            },
            dismissButton = {
                TextButton(onClick = { showAddRoomDialog = false }) {
                    Text("Cancel", color = OnSurface)
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    Scaffold(
        containerColor = Background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(paddingValues)
        ) {
            PullToRefreshBox(
                isRefreshing = isLoading,
                onRefresh = { viewModel.loadDashboard() },
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 88.dp)
                ) {
                    // ── Header ──
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

                            // Clopotel cu badge: navigheaza la NotificationsScreen
                            // Badge-ul rosu apare doar cand unreadCount > 0
                            Box {
                                IconButton(onClick = {
                                    navController.navigate(NavRoutes.Notifications.route)
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Notifications,
                                        contentDescription = "Notifications",
                                        tint = Primary
                                    )
                                }
                                if (unreadCount > 0) {
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
                    }

                    // ── Statistics ──
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionTitle(title = "Statistics")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                StatCard(
                                    icon = Icons.Default.Devices,
                                    label = "Devices",
                                    value = "${stats?.total_devices ?: 0}",
                                    change = "total"
                                )
                            }
                            item {
                                val kwh = if (energyKwh > 0) String.format("%.1f", energyKwh) else "0.0"
                                StatCard(
                                    icon = Icons.Default.Bolt,
                                    label = "Consumption",
                                    value = "$kwh kWh",
                                    change = "est. today"
                                )
                            }
                            item {
                                StatCard(
                                    icon = Icons.Default.AutoAwesome,
                                    label = "Automations",
                                    value = "${stats?.total_commands_today ?: 0}",
                                    change = "today"
                                )
                            }
                        }
                    }

                    // ── Quick Actions ──
                    // First chip is always "All Off" (hardcoded).
                    // The rest are the user's scenes fetched from GET /api/scenes/,
                    // each calling POST /api/scenes/{id}/execute on tap.
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionTitle(title = "Quick Actions")
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Hardcoded "All Off" chip — always first
                            item {
                                QuickActionChip(
                                    label = "All Off",
                                    icon = Icons.Default.PowerSettingsNew,
                                    isActive = activeChipIndex == 0,
                                    onClick = {
                                        activeChipIndex = if (activeChipIndex == 0) -1 else 0
                                        viewModel.allOff()
                                    }
                                )
                            }
                            // Dynamic chips: one per user scene, icon derived from scene name
                            items(scenes, key = { it.id }) { scene ->
                                val n = scene.name.lowercase()
                                val sceneIcon = when {
                                    n.contains("movie") || n.contains("cinema") -> Icons.Default.Movie
                                    n.contains("morning") || n.contains("wake") -> Icons.Default.WbSunny
                                    n.contains("away") -> Icons.Default.DirectionsRun
                                    n.contains("night") || n.contains("sleep") || n.contains("bed") -> Icons.Default.DarkMode
                                    n.contains("party") -> Icons.Default.MusicNote
                                    n.contains("work") || n.contains("office") -> Icons.Default.Computer
                                    else -> Icons.Default.AutoAwesome
                                }
                                QuickActionChip(
                                    label = scene.name,
                                    icon = sceneIcon,
                                    isActive = executingSceneId == scene.id,
                                    onClick = { viewModel.executeQuickScene(scene.id) }
                                )
                            }
                        }
                    }

                    // ── Anomaly Banner ──
                    // Show at most one banner when the ML engine detects unusual activity
                    if (anomalies.isNotEmpty()) {
                        item {
                            val anomaly = anomalies.first()
                            Spacer(modifier = Modifier.height(16.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1A00)),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Anomaly",
                                        tint = Color(0xFFFF9800),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Text(
                                        text = "Unusual activity: ${anomaly.message}",
                                        color = Color(0xFFFFCC80),
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // ── Suggested Routines ──
                    // Shown only when ML returns non-empty recommendations
                    if (recommendations.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            SectionTitle(title = "Suggested Routines")
                        }
                        items(recommendations, key = { "${it.device_id}:${it.action}" }) { rec ->
                            val actionIcon = when {
                                rec.action.lowercase().contains("off") -> Icons.Default.PowerSettingsNew
                                rec.action.lowercase().contains("on")  -> Icons.Default.Lightbulb
                                rec.device_name.lowercase().contains("tv") -> Icons.Default.Tv
                                rec.device_name.lowercase().contains("ac") || rec.device_name.lowercase().contains("air") -> Icons.Default.AcUnit
                                else -> Icons.Default.AutoAwesome
                            }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2838)),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3F50))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF004D57)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(actionIcon, contentDescription = null, tint = Color(0xFF00BCD4), modifier = Modifier.size(22.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = rec.message,
                                            color = Color(0xFFE8F4F8),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "Detected ${rec.occurrences} times on ${rec.distinct_days} days · ${(rec.confidence * 100).toInt()}% confidence",
                                            color = Color(0xFF7FA8BB),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Create Routine button
                                        TextButton(
                                            onClick = { viewModel.createSceneFromRecommendation(rec) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("Create", color = Color(0xFF00BCD4), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                        // Dismiss button
                                        IconButton(
                                            onClick = { viewModel.dismissRecommendation(rec) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Color(0xFF7FA8BB), modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Rooms ──
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionTitle(
                            title = "Rooms",
                            action = {
                                TextButton(onClick = {
                                    navController.navigate(NavRoutes.Devices.route)
                                }) {
                                    Text("All", color = Primary, style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        )

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
                                onAddRoomClick = { showAddRoomDialog = true }
                            )
                        }
                    }
                }
            }

            // FAB
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
}

/**
 * Grid 2x2 pentru afisarea camerelor din casa.
 *
 * Implementat cu doua [Row]-uri manuale (nu LazyVerticalGrid) pentru a evita
 * nested lazy layouts intr-un LazyColumn parinte. Afiseaza maximum 3 camere
 * reale plus un card de adaugare (AddRoomCard) pe ultimul slot disponibil.
 * Cand nu exista date de la API, foloseste camere de tip placeholder.
 *
 * @param rooms lista camerelor primite din ViewModel
 * @param onRoomClick callback apelat la click pe o camera
 * @param onAddRoomClick callback apelat la click pe cardul de adaugare
 */
@Composable
private fun RoomsGrid(
    rooms: List<RoomInfo>,
    onRoomClick: (String) -> Unit,
    onAddRoomClick: () -> Unit
) {
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
