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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
 * Snackbar-ul este conectat la [HomeViewModel.snackbarMessage].
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
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val unreadCount by viewModel.unreadNotificationCount.collectAsState()
    val energyKwh by viewModel.energyKwh.collectAsState()

    val greeting = viewModel.greeting
    val currentDate = viewModel.currentDate
    val userName = user?.username?.split(" ")?.firstOrNull() ?: ""

    var activeChipIndex by remember { mutableStateOf(-1) }
    var showAddRoomDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

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
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
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
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        SectionTitle(title = "Quick Actions")
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
                                        viewModel.allOff()
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
                                        viewModel.awayMode()
                                    }
                                )
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
