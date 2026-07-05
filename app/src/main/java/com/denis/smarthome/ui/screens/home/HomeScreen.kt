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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.data.model.AnomalyItem
import com.denis.smarthome.data.model.DashboardStats
import com.denis.smarthome.data.model.RoutineCandidate
import com.denis.smarthome.data.model.SceneAction
import com.denis.smarthome.data.model.SceneResponse
import com.denis.smarthome.data.model.UserResponse
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
 * Ecranul principal cu dashboard-ul SmartHome — wrapper subtire peste ViewModel.
 * Citeste starea din [HomeViewModel] si o paseaza catre [HomeScreenContent], care
 * contine tot UI-ul si nu depinde de ViewModel (poate fi randat direct in @Preview).
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
    val scenes by viewModel.scenes.collectAsState()
    val executingSceneId by viewModel.executingSceneId.collectAsState()
    val recommendations by viewModel.recommendations.collectAsState()
    val anomalies by viewModel.anomalies.collectAsState()

    val greeting = viewModel.greeting
    val currentDate = viewModel.currentDate
    val userName = user?.username?.split(" ")?.firstOrNull() ?: ""

    HomeScreenContent(
        greeting = greeting,
        currentDate = currentDate,
        userName = userName,
        unreadCount = unreadCount,
        stats = stats,
        scenes = scenes,
        executingSceneId = executingSceneId,
        anomalies = anomalies,
        recommendations = recommendations,
        rooms = rooms,
        isLoading = isLoading,
        onRefresh = { viewModel.loadDashboard() },
        onNotificationsClick = { navController.navigate(NavRoutes.Notifications.route) },
        onAllOff = { viewModel.allOff() },
        onExecuteScene = { viewModel.executeQuickScene(it) },
        onCreateRoutine = { viewModel.createRoutineFromRecommendation(it) },
        onDismissRecommendation = { viewModel.dismissRecommendation(it) },
        onDevicesClick = { navController.navigate(NavRoutes.Devices.route) }
    )
}

/**
 * UI-ul dashboard-ului SmartHome, fara dependinta de ViewModel.
 * Primeste toata starea ca parametri simpli, ceea ce permite randare in @Preview
 * fara apeluri de retea.
 *
 * Foloseste [PullToRefreshBox] pentru tragere in jos (refresh). Continutul este un
 * [LazyColumn] cu: header salut, statistici reale din API, actiuni rapide (allOff +
 * scene), banner anomalii, rutine sugerate si grid 2x2 camere derivate din dispozitive.
 *
 * @param greeting salutul dinamic (Good morning/afternoon/evening)
 * @param currentDate data curenta formatata
 * @param userName prenumele userului (poate fi gol)
 * @param unreadCount numarul de notificari necitite (afisat ca badge pe clopotel)
 * @param stats statisticile dashboard-ului (poate fi null cat timp se incarca)
 * @param scenes scenele userului, afisate ca Quick Action chips
 * @param executingSceneId ID-ul scenei in curs de executie (pentru loading state pe chip)
 * @param anomalies lista de anomalii ML (se afiseaza doar prima, daca exista)
 * @param recommendations lista de rutine sugerate de ML
 * @param rooms lista camerelor derivate din dispozitive
 * @param isLoading true cat timp se incarca/reincarca dashboard-ul
 * @param onRefresh callback pentru pull-to-refresh
 * @param onNotificationsClick callback la click pe clopotel
 * @param onAllOff callback pentru chip-ul "All Off"
 * @param onExecuteScene callback pentru executarea unei scene (Quick Action chip)
 * @param onCreateRoutine callback pentru crearea unei rutine dintr-o recomandare
 * @param onDismissRecommendation callback pentru respingerea unei recomandari
 * @param onDevicesClick callback pentru navigare catre ecranul de dispozitive
 */
@Composable
fun HomeScreenContent(
    greeting: String,
    currentDate: String,
    userName: String,
    unreadCount: Int,
    stats: DashboardStats?,
    scenes: List<SceneResponse>,
    executingSceneId: Int?,
    anomalies: List<AnomalyItem>,
    recommendations: List<RoutineCandidate>,
    rooms: List<RoomInfo>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onNotificationsClick: () -> Unit,
    onAllOff: () -> Unit,
    onExecuteScene: (Int) -> Unit,
    onCreateRoutine: (RoutineCandidate) -> Unit,
    onDismissRecommendation: (RoutineCandidate) -> Unit,
    onDevicesClick: () -> Unit
) {
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
                    onDevicesClick()
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
                onRefresh = onRefresh,
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
                                IconButton(onClick = onNotificationsClick) {
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
                    // Toate valorile vin din GET /api/dashboard/stats (date reale calculate
                    // de backend), nu mai sunt numere inventate client-side.
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
                                StatCard(
                                    icon = Icons.Default.AutoAwesome,
                                    label = "Commands",
                                    value = "${stats?.total_commands_today ?: 0}",
                                    change = "today"
                                )
                            }
                            item {
                                StatCard(
                                    icon = Icons.Default.Schedule,
                                    label = "Routines",
                                    value = "${stats?.total_routines_active ?: 0}",
                                    change = "active"
                                )
                            }
                        }

                        // Cel mai folosit dispozitiv si ora de varf — afisate doar cand
                        // backend-ul are suficiente date pentru a le calcula
                        val mostUsed = stats?.most_used_device
                        val peakHour = stats?.peak_hour
                        if (mostUsed != null || peakHour != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (mostUsed != null) {
                                    InfoPill(
                                        icon = Icons.Default.Star,
                                        label = "Most used",
                                        value = mostUsed,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (peakHour != null) {
                                    InfoPill(
                                        icon = Icons.Default.AccessTime,
                                        label = "Peak hour",
                                        value = "${peakHour.toString().padStart(2, '0')}:00",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
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
                                        onAllOff()
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
                                    onClick = { onExecuteScene(scene.id) }
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
                        items(recommendations, key = { "${it.device_id}:${it.action}:${it.value}:${it.trigger_time}" }) { rec ->
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
                                            text = rec.name,
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
                                            onClick = { onCreateRoutine(rec) },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("Create", color = Color(0xFF00BCD4), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                        // Dismiss button
                                        IconButton(
                                            onClick = { onDismissRecommendation(rec) },
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
                                TextButton(onClick = onDevicesClick) {
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
                                onRoomClick = { onDevicesClick() },
                                onAddRoomClick = { showAddRoomDialog = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card mic pentru afisarea unui fapt real din DashboardStats (ex: cel mai folosit
 * dispozitiv, ora de varf), sub forma de "pill" cu iconita si eticheta.
 */
@Composable
private fun InfoPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, color = OnSurface, style = MaterialTheme.typography.labelSmall)
            Text(value, color = OnBackground, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1)
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

// ── Preview fake data ────────────────────────────────────────────────────────────

private val previewHomeUser = UserResponse(
    id = 1,
    username = "Denis",
    email = "denis@example.com",
    display_name = "Denis",
    avatar_url = null,
    created_at = "2026-01-01T00:00:00",
    is_verified = true
)

private val previewHomeStats = DashboardStats(
    total_devices = 12,
    total_commands_today = 34,
    total_routines_active = 3,
    total_scenes = 4,
    most_used_device = "Living Room Bulb",
    peak_hour = 20,
    commands_by_day = null,
    commands_by_device = null,
    device_type_distribution = null
)

private val previewHomeRooms = listOf(
    RoomInfo("Living Room", 5, 3),
    RoomInfo("Bedroom", 3, 1),
    RoomInfo("Kitchen", 4, 4)
)

private val previewHomeScenes = listOf(
    SceneResponse(
        id = 1,
        name = "Movie Night",
        icon = null,
        actions = listOf(SceneAction(device_id = 1, command_type = "off", command_data = null)),
        is_active = true
    ),
    SceneResponse(
        id = 2,
        name = "Good Morning",
        icon = null,
        actions = listOf(SceneAction(device_id = 2, command_type = "on", command_data = null)),
        is_active = true
    )
)

private val previewHomeRecommendations = listOf(
    RoutineCandidate(
        device_id = 3,
        device_name = "Living Room TV",
        action = "off",
        value = null,
        trigger_time = "23:00",
        days_of_week = "Mon,Tue,Wed,Thu,Fri",
        occurrences = 14,
        distinct_days = 5,
        confidence = 0.87f,
        name = "Turn off Living Room TV at 23:00",
        candidate_index = 0
    ),
    RoutineCandidate(
        device_id = 4,
        device_name = "Bedroom Lamp",
        action = "on",
        value = null,
        trigger_time = "07:00",
        days_of_week = "Mon,Tue,Wed,Thu,Fri",
        occurrences = 10,
        distinct_days = 5,
        confidence = 0.72f,
        name = "Turn on Bedroom Lamp at 07:00",
        candidate_index = 1
    )
)

private val previewHomeAnomalies = listOf(
    AnomalyItem(
        device_id = 5,
        device_name = "Kitchen Plug",
        action = "on",
        time = "03:14",
        z_score = 3.2f,
        message = "Kitchen Plug turned on at an unusual hour (03:14)"
    )
)

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    SmartHomeTheme {
        HomeScreenContent(
            greeting = "Good morning",
            currentDate = "Monday, March 17",
            userName = previewHomeUser.username,
            unreadCount = 2,
            stats = previewHomeStats,
            scenes = previewHomeScenes,
            executingSceneId = null,
            anomalies = previewHomeAnomalies,
            recommendations = previewHomeRecommendations,
            rooms = previewHomeRooms,
            isLoading = false,
            onRefresh = {},
            onNotificationsClick = {},
            onAllOff = {},
            onExecuteScene = {},
            onCreateRoutine = {},
            onDismissRecommendation = {},
            onDevicesClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenLoadingPreview() {
    SmartHomeTheme {
        HomeScreenContent(
            greeting = "Good evening",
            currentDate = "Monday, March 17",
            userName = "",
            unreadCount = 0,
            stats = null,
            scenes = emptyList(),
            executingSceneId = null,
            anomalies = emptyList(),
            recommendations = emptyList(),
            rooms = emptyList(),
            isLoading = true,
            onRefresh = {},
            onNotificationsClick = {},
            onAllOff = {},
            onExecuteScene = {},
            onCreateRoutine = {},
            onDismissRecommendation = {},
            onDevicesClick = {}
        )
    }
}
