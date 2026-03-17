/**
 * LampControlScreen.kt - Ecran de control pentru lampa/relay inteligent
 *
 * Afiseaza un cerc hero cu border animat (teal cand pornita, gri cand oprita),
 * text de status, un switch mare pentru pornire/oprire, carduri de statistici
 * si o sectiune de programari (schedules) cu posibilitatea de a adauga noi intrari.
 * Folosit atat pentru dispozitive de tip relay cat si WoL (Wake-on-LAN).
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.screens.control

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.ui.components.LargeToggleSwitch
import com.denis.smarthome.ui.components.ScheduleItem
import com.denis.smarthome.ui.components.StatCard
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.LampControlViewModel
import com.denis.smarthome.viewmodel.Schedule

/**
 * Ecranul principal de control al lampii/relay-ului.
 * Afiseaza starea curenta (ON/OFF), permite pornirea/oprirea prin LargeToggleSwitch
 * si gestioneaza programarile (schedules) salvate local in ViewModel.
 *
 * @param navController pentru navigare inapoi
 * @param device datele dispozitivului (nume si camera)
 * @param deviceId ID-ul dispozitivului pentru ViewModel
 */
@Composable
fun LampControlScreen(
    navController: NavController,
    device: DeviceResponse,
    deviceId: Int
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: LampControlViewModel = viewModel(
        factory = LampControlViewModel.Factory(app, deviceId)
    )
    val isOn      by viewModel.isOn.collectAsState()
    val schedules by viewModel.schedules.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showAddScheduleDialog by remember { mutableStateOf(false) }

    if (showAddScheduleDialog) {
        AddScheduleDialog(
            onConfirm = { schedule ->
                viewModel.addSchedule(schedule)
                showAddScheduleDialog = false
            },
            onDismiss = { showAddScheduleDialog = false }
        )
    }

    Scaffold(
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top Bar ──────────────────────────────────────────────────────
            // Bara superioara cu buton Back (stanga), titlu (centru) si meniu MoreVert (dreapta)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(horizontal = 4.dp, vertical = 8.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurface)
                }
                Text(
                    text = device.name,
                    color = OnBackground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.Center)
                )
                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = OnSurface)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename", color = OnBackground) },
                            onClick = { showMenu = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = ErrorColor) },
                            onClick = { showMenu = false }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // ── Hero Bulb Circle ─────────────────────────────────────────
                // Cerc mare (220dp) cu border colorat: teal (Primary) cand lampa e pornita,
                // gri (Outline) cand e oprita. Iconita din interior isi schimba si ea culoarea.
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(Surface)
                        .border(
                            width = 4.dp,
                            color = if (isOn) Primary else Outline,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = if (isOn) Primary else OnSurface,
                        modifier = Modifier.size(64.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "STATUS",
                        color = Primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = if (isOn) "Currently ON" else "Currently OFF",
                        color = OnBackground,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // ── Large Toggle ──────────────────────────────────────────────
                // Switch supradimensionat pentru pornire/oprire facila a lampii
                LargeToggleSwitch(
                    checked = isOn,
                    onCheckedChange = { viewModel.togglePower() }
                )

                // ── Stats Row ─────────────────────────────────────────────────
                // Doua carduri de statistici (StatCard refolosit din componente comune):
                // puterea consumata in W si orele de utilizare astazi
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        icon = Icons.Default.Bolt,
                        label = "POWER",
                        value = "${viewModel.powerWatts} W",
                        change = "Active",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        icon = Icons.Default.Schedule,
                        label = "USAGE TODAY",
                        value = "${viewModel.usageHours} h",
                        change = "Today",
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── Schedules ─────────────────────────────────────────────────
                // Lista de programari salvate local in ViewModel.
                // Butonul "Add New" deschide AddScheduleDialog pentru adaugarea unei noi programari.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Schedules",
                            color = OnBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { showAddScheduleDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Primary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add New", color = Primary, style = MaterialTheme.typography.labelLarge)
                        }
                    }

                    schedules.forEachIndexed { index, schedule ->
                        ScheduleItem(
                            schedule = schedule,
                            onToggle = { viewModel.toggleSchedule(index) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Dialog pentru adaugarea unei noi programari a lampii.
 * Contine un formular cu un singur pas cu campurile:
 * - Nume programare (ex: "Sunset On")
 * - Ora (ex: "6:45 PM")
 * - Actiune: Turn ON / Turn OFF (Switch)
 * - Repetare: dropdown cu optiunile Daily / Weekdays / Once
 *
 * La confirmare construieste un obiect Schedule si il trimite prin callback onConfirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddScheduleDialog(
    onConfirm: (Schedule) -> Unit,
    onDismiss: () -> Unit
) {
    var scheduleName by remember { mutableStateOf("") }
    var timeText     by remember { mutableStateOf("") }
    var isOnAction   by remember { mutableStateOf(true) }
    var repeatExpanded by remember { mutableStateOf(false) }
    var repeat       by remember { mutableStateOf("Daily") }
    val repeatOptions = listOf("Daily", "Weekdays", "Once")

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Primary,
        focusedLabelColor = Primary,
        unfocusedBorderColor = Outline,
        unfocusedLabelColor = OnSurface,
        cursorColor = Primary,
        focusedTextColor = OnBackground,
        unfocusedTextColor = OnBackground
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface,
        titleContentColor = OnBackground,
        textContentColor = OnSurface,
        title = { Text("Add Schedule", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = scheduleName,
                    onValueChange = { scheduleName = it },
                    label = { Text("Name (e.g. Sunset On)") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = timeText,
                    onValueChange = { timeText = it },
                    label = { Text("Time (e.g. 6:45 PM)") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Action", color = OnSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isOnAction) "Turn ON" else "Turn OFF", color = Primary, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = isOnAction,
                            onCheckedChange = { isOnAction = it },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Primary,
                                checkedThumbColor = Color.White,
                                uncheckedTrackColor = SurfaceVariant,
                                uncheckedThumbColor = OnSurface
                            )
                        )
                    }
                }
                ExposedDropdownMenuBox(expanded = repeatExpanded, onExpandedChange = { repeatExpanded = it }) {
                    OutlinedTextField(
                        value = repeat,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Repeat") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = repeatExpanded) },
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = repeatExpanded,
                        onDismissRequest = { repeatExpanded = false },
                        modifier = Modifier.background(Surface)
                    ) {
                        repeatOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option, color = OnBackground) },
                                onClick = { repeat = option; repeatExpanded = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = scheduleName.ifBlank { "New Schedule" }
                    val time = timeText.ifBlank { "--:-- --" }
                    val icon = if (isOnAction) "sunset" else "night"
                    onConfirm(Schedule(name, time, repeat, isOnAction, icon))
                },
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Add", color = Color.Black, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = OnSurface) }
        }
    )
}
