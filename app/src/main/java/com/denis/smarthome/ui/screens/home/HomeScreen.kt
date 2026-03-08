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

// Lista de palete gradient pentru cardurile de camera (ciclic, cate una pe slot).
// Fiecare pereche merge de la o culoare inchisa tematica spre culoarea Background,
// creand un efect de adancime vizuala diferit per camera.
private val roomGradients = listOf(
    listOf(Color(0xFF0D3545), Background),  // teal-dark (Living)
    listOf(Color(0xFF2A1535), Background),  // purple-dark (Bedroom)
    listOf(Color(0xFF2A1F0D), Background),  // amber-dark (Kitchen)
    listOf(Color(0xFF0D2A1A), Background),  // green-dark
    listOf(Color(0xFF2A0D0D), Background),  // red-dark
)

/**
 * Ecranul principal cu dashboard-ul SmartHome.
 *
 * Foloseste [PullToRefreshBox] pentru a permite utilizatorului sa reimprospateze
 * datele prin gestul de tragere in jos. Continutul principal este un [LazyColumn]
 * cu sectiunile: header salut, statistici, actiuni rapide si grid camere.
 *
 * @param navController Controlerul de navigare Compose
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        // PullToRefreshBox din M3 experimental: arata indicator de refresh cand isLoading
        // si apeleaza loadDashboard() cand utilizatorul trage in jos
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { viewModel.loadDashboard() },
            modifier = Modifier.fillMaxSize()
        ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            // ── Header salut: avatar circular + salut personalizat + data curenta ──
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

                    // ── Iconita notificari cu badge rosu de activitate ──
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

            // ── Sectiunea statistici: LazyRow cu StatCard-uri pentru dispozitive, consum, automatizari ──
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

            // ── Sectiunea actiuni rapide: LazyRow cu QuickActionChip-uri pentru scenarii comune ──
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

            // ── Sectiunea camere: grid 2x2 cu RoomCard-uri si un card de adaugare ──
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

                // Indicator de incarcare centrat cat timp datele se incarca de la API
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
        } // end PullToRefreshBox

        // ── FAB (Floating Action Button) pentru adaugare rapida dispozitiv ──
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

/**
 * Grid 2x2 pentru afisarea camerelor din casa.
 *
 * Implementat cu doua [Row]-uri manuale (nu LazyVerticalGrid) pentru a evita
 * nested lazy layouts intr-un LazyColumn parinte. Afiseaza maximum 3 camere
 * reale plus un card de adaugare (AddRoomCard) pe ultimul slot disponibil.
 * Cand nu exista date de la API, foloseste camere de tip placeholder.
 *
 * @param rooms Lista camerelor primite din ViewModel
 * @param onRoomClick Callback apelat la click pe o camera
 * @param onAddRoomClick Callback apelat la click pe cardul de adaugare camera
 */
@Composable
private fun RoomsGrid(
    rooms: List<RoomInfo>,
    onRoomClick: (String) -> Unit,
    onAddRoomClick: () -> Unit
) {
    // Afiseaza maximum 3 camere; daca lista e goala, foloseste date placeholder
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
        // Randul 1 al gridului: primele doua sloturi (camera 0 si camera 1 sau AddRoom)
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

        // Randul 2 al gridului: camera 2 si intotdeauna cardul de adaugare camera
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
