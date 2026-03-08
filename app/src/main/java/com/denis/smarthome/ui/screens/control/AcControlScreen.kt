/**
 * AcControlScreen.kt - Ecran de control pentru aparatul de aer conditionat
 *
 * Ofera control complet al AC-ului: temperatura (afisata circular cu butoane +/-),
 * mod de functionare (Cool/Heat/Fan/Auto/Dry), viteza ventilatorului cu bara de progres,
 * oscilatie swing si timer cu increment/decrement. FAB-ul de power isi schimba culoarea
 * in functie de starea isOn (teal=pornit, gri=oprit).
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.screens.control

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import com.denis.smarthome.ui.components.CircularTemperatureDisplay
import com.denis.smarthome.ui.components.ModeSelector
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.AcControlViewModel
import com.denis.smarthome.viewmodel.FanSpeed

/**
 * Ecranul principal de control al aparatului de aer conditionat.
 * Colecteaza toate starile din AcControlViewModel si le afiseaza in carduri dedicate.
 * FAB-ul de power este pozitionat floating la baza ecranului si isi schimba culoarea:
 * teal (Primary) cand AC-ul este pornit, gri (SurfaceVariant) cand este oprit.
 *
 * @param navController pentru butonul de intoarcere
 * @param device datele dispozitivului (nume si camera)
 * @param deviceId ID-ul dispozitivului pentru initializarea ViewModel-ului
 */
@Composable
fun AcControlScreen(
    navController: NavController,
    device: DeviceResponse,
    deviceId: Int
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: AcControlViewModel = viewModel(
        factory = AcControlViewModel.Factory(app, deviceId)
    )
    val isOn by viewModel.isOn.collectAsState()
    val temperature by viewModel.temperature.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val fanSpeed by viewModel.fanSpeed.collectAsState()
    val swingEnabled by viewModel.swingEnabled.collectAsState()
    val timerHours by viewModel.timerHours.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

    // Mapare viteza ventilator -> progres bara liniara (LOW=25%, MED=50%, HIGH=75%, AUTO=100%)
    val fanSpeeds = FanSpeed.values()
    val fanSpeedProgress = mapOf(
        FanSpeed.LOW to 0.25f,
        FanSpeed.MED to 0.5f,
        FanSpeed.HIGH to 0.75f,
        FanSpeed.AUTO to 1f
    )

    Scaffold(
        containerColor = Background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Top Bar ──────────────────────────────────────────────────────
            // Bara superioara simpla: buton Back (stanga) si nume + camera (centru)
            // AC-ul nu are indicator de status in top bar (spre deosebire de TV)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnSurface)
                }
                Column(modifier = Modifier.align(Alignment.Center)) {
                    Text(
                        text = device.name,
                        color = OnBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = device.room,
                        color = OnSurface,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Temperature Display ───────────────────────────────────────
                // CircularTemperatureDisplay afiseaza temperatura curenta intr-un cerc animat.
                // Butoanele -/+ sunt suprapuse (overlaid) la baza cercului si sunt
                // vizibile doar cand AC-ul este pornit (isOn == true).
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularTemperatureDisplay(
                        temperature = temperature,
                        isOn = isOn
                    )
                    // Butoanele de scadere/crestere temperatura, afisate doar cand AC-ul e pornit
                    if (isOn) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .offset(y = (-12).dp),
                            horizontalArrangement = Arrangement.spacedBy(72.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceVariant)
                                    .clickable { viewModel.decreaseTemperature() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = OnBackground, modifier = Modifier.size(20.dp))
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceVariant)
                                    .clickable { viewModel.increaseTemperature() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = OnBackground, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // ── Mode Selector ─────────────────────────────────────────────
                // Card cu componenta ModeSelector pentru alegerea modului AC (Cool/Heat/Fan/Auto/Dry)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Mode", color = OnSurface, style = MaterialTheme.typography.labelMedium)
                        ModeSelector(selectedMode = mode, onModeSelected = viewModel::setMode)
                    }
                }

                // ── Fan Speed ─────────────────────────────────────────────────
                // Card cu LinearProgressIndicator pentru viteza ventilatorului si
                // 4 butoane selectabile (LOW/MED/HIGH/AUTO), cel selectat avand border teal
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Fan Speed", color = OnSurface, style = MaterialTheme.typography.labelMedium)
                            Text(
                                text = fanSpeed.name,
                                color = Primary,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        LinearProgressIndicator(
                            progress = { fanSpeedProgress[fanSpeed] ?: 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Primary,
                            trackColor = SurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fanSpeeds.forEach { speed ->
                                val isSelected = fanSpeed == speed
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) PrimaryContainer else SurfaceVariant)
                                        .border(
                                            1.dp,
                                            if (isSelected) Primary else Color.Transparent,
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { viewModel.setFanSpeed(speed) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = speed.name,
                                        color = if (isSelected) Primary else OnSurface,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Swing + Timer row ─────────────────────────────────────────
                // Doua carduri plasate orizontal: Swing cu Switch si Timer cu butoane +/- ore
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card Swing: Switch care activeaza/dezactiveaza oscilatia lamelor AC
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Swing", color = OnSurface, style = MaterialTheme.typography.labelMedium)
                                Text(
                                    text = if (swingEnabled) "On" else "Off",
                                    color = if (swingEnabled) Primary else OnSurface,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Switch(
                                checked = swingEnabled,
                                onCheckedChange = { viewModel.toggleSwing() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Primary,
                                    uncheckedThumbColor = OnSurface,
                                    uncheckedTrackColor = SurfaceVariant
                                )
                            )
                        }
                    }

                    // Card Timer: afiseaza orele setate si doua IconButton-uri (sus=+1h, jos=-1h)
                    // Valoarea 0 inseamna timer dezactivat si afiseaza textul "Off"
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Timer", color = OnSurface, style = MaterialTheme.typography.labelMedium)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (timerHours > 0) "${timerHours}h" else "Off",
                                    color = if (timerHours > 0) Primary else OnSurface,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                Column {
                                    IconButton(
                                        onClick = { viewModel.setTimer(timerHours + 1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "+1h", tint = Primary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(
                                        onClick = { if (timerHours > 0) viewModel.setTimer(timerHours - 1) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "-1h", tint = OnSurface, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // ── Power FAB ────────────────────────────────────────────────────────
        // Buton flotant circular pozitionat la baza ecranului (BottomCenter).
        // Culoarea se schimba dinamic: Primary (teal) cand AC-ul e pornit,
        // SurfaceVariant (gri) cand AC-ul e oprit. Apeleaza viewModel.togglePower().
        Box(modifier = Modifier.fillMaxSize()) {
            FloatingActionButton(
                onClick = { viewModel.togglePower() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = innerPadding.calculateBottomPadding() + 24.dp),
                containerColor = if (isOn) Primary else SurfaceVariant,
                contentColor = if (isOn) Color.Black else OnSurface,
                shape = CircleShape
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = "Power", modifier = Modifier.size(28.dp))
            }
        }
    }
}
