/**
 * LampControlScreen.kt - Ecran de control pentru lampa/relay inteligent
 *
 * Afiseaza un cerc hero cu border animat (teal cand pornita, gri cand oprita),
 * text de status si un switch mare pentru pornire/oprire.
 * Folosit atat pentru dispozitive de tip relay cat si WoL (Wake-on-LAN).
 *
 * Scheduled automation is handled by Routines (in the Scenes screen), not here —
 * there is no per-device schedules feature/endpoint on the backend.
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.ui.components.LargeToggleSwitch
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.LampControlViewModel

/**
 * Ecranul principal de control al lampii/relay-ului.
 * Afiseaza starea curenta (ON/OFF) si permite pornirea/oprirea prin LargeToggleSwitch.
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
        factory = LampControlViewModel.Factory(app, deviceId, device.last_status)
    )
    val isOn       by viewModel.isOn.collectAsState()
    val isDeleted  by viewModel.isDeleted.collectAsState()

    var showMenu         by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(isDeleted) {
        if (isDeleted) navController.popBackStack()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Surface,
            titleContentColor = OnBackground,
            textContentColor = OnSurface,
            title = { Text("Delete Device", fontWeight = FontWeight.Bold) },
            text = { Text("Delete \"${device.name}\"? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { showDeleteDialog = false; viewModel.deleteDevice() },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorColor)
                ) { Text("Delete", color = Color.White, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = OnSurface)
                }
            }
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
                            onClick = { showMenu = false; showDeleteDialog = true }
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

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
