/**
 * RgbBulbScreen.kt - Ecran de control pentru becul RGB inteligent
 *
 * Afiseaza un cerc hero cu efect de glow realizat prin drawBehind pe Canvas,
 * un card pentru power si luminozitate (Slider dezactivat cand becul e oprit),
 * o roata de culori (ColorWheel) pentru selectia culorii si un grid 2x3 cu presetari rapide.
 * Cand becul este oprit, cercul hero devine gri inchis (#2A2A2A) iar slider-ul e disabled.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.screens.control

import android.app.Application
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.ui.components.*
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.RgbBulbViewModel
import com.denis.smarthome.viewmodel.rgbPresets

/**
 * Ecranul principal de control al becului RGB.
 * Colecteaza starea completa din RgbBulbViewModel (isOn, brightness, selectedColor,
 * selectedAngle, selectedPreset) si le afiseaza intr-o interfata scrollabila.
 *
 * @param navController pentru navigare inapoi
 * @param device datele dispozitivului (nume si camera)
 * @param deviceId ID-ul dispozitivului pentru ViewModel
 */
@Composable
fun RgbBulbScreen(
    navController: NavController,
    device: DeviceResponse,
    deviceId: Int
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: RgbBulbViewModel = viewModel(
        factory = RgbBulbViewModel.Factory(app, deviceId)
    )
    val isOn           by viewModel.isOn.collectAsState()
    val brightness     by viewModel.brightness.collectAsState()
    val selectedColor  by viewModel.selectedColor.collectAsState()
    val selectedAngle  by viewModel.selectedAngle.collectAsState()
    val selectedPreset by viewModel.selectedPreset.collectAsState()
    val isDeleted      by viewModel.isDeleted.collectAsState()

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
            // Bara superioara cu buton Back (stanga), titlu (centru) si meniu MoreVert (dreapta).
            // Meniul MoreVert contine optiunile Rename si Delete pentru dispozitiv.
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
                // Dropdown cu optiuni suplimentare per dispozitiv
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Hero Bulb Circle ─────────────────────────────────────────
                // Cercul principal care reflecta culoarea curenta a becului.
                // Cand becul e oprit, culoarea devine gri inchis (#2A2A2A) fara glow.
                val heroColor = if (isOn) selectedColor else Color(0xFF2A2A2A)
                val lighterColor = if (isOn) {
                    Color(
                        red = minOf(selectedColor.red + 0.35f, 1f),
                        green = minOf(selectedColor.green + 0.35f, 1f),
                        blue = minOf(selectedColor.blue + 0.35f, 1f)
                    )
                } else Color(0xFF3A3A3A)

                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(CircleShape)
                        .drawBehind {
                            // Efect glow: un cerc mare semitransparent (alpha=0.25) desenat in spate,
                            // cu raza mai mare decat cercul principal, vizibil doar cand becul e pornit.
                            if (isOn) {
                                drawCircle(
                                    color = selectedColor.copy(alpha = 0.25f),
                                    radius = size.minDimension * 0.58f
                                )
                            }
                            // Cercul principal cu gradient radial: de la culoarea mai deschisa (centru)
                            // la culoarea selectata (margine), centrul gradientului e offset spre stanga-sus
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(lighterColor, heroColor),
                                    center = Offset(size.width * 0.4f, size.height * 0.35f),
                                    radius = size.minDimension * 0.7f
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = if (isOn) Color.White else OnSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = device.name,
                        color = OnBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isOn) "Status: Powered On" else "Status: Powered Off",
                        color = if (isOn) Primary else OnSurface,
                        fontSize = 14.sp
                    )
                }

                // ── Power + Brightness Card ───────────────────────────────────
                // Card cu doua randuri: Power (Switch) si Brightness (Slider).
                // Slider-ul are enabled=false cand becul e oprit, prevenind modificarea
                // luminozitatii fara ca becul sa fie activ.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Power row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = Primary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Power", color = OnBackground, fontSize = 16.sp, modifier = Modifier.weight(1f))
                            Switch(
                                checked = isOn,
                                onCheckedChange = { viewModel.togglePower() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Primary,
                                    uncheckedThumbColor = OnSurface,
                                    uncheckedTrackColor = SurfaceVariant
                                )
                            )
                        }

                        HorizontalDivider(color = Outline)

                        // Brightness row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Brightness6, contentDescription = null, tint = if (isOn) Primary else OnSurface, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Brightness", color = if (isOn) OnBackground else OnSurface, modifier = Modifier.weight(1f))
                            Text(
                                text = "${brightness}%",
                                color = OnSurface,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Slider(
                            value = brightness.toFloat(),
                            onValueChange = { if (isOn) viewModel.setBrightness(it.toInt()) },
                            valueRange = 0f..100f,
                            steps = 19,
                            enabled = isOn,
                            colors = SliderDefaults.colors(
                                activeTrackColor = Primary,
                                thumbColor = Color.White,
                                inactiveTrackColor = SurfaceVariant,
                                disabledActiveTrackColor = OnSurface.copy(alpha = 0.3f),
                                disabledThumbColor = OnSurface.copy(alpha = 0.3f),
                                disabledInactiveTrackColor = SurfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ── Color Spectrum ────────────────────────────────────────────
                // Componenta ColorWheel pentru selectia culorii prin atingere.
                // Culoarea si unghiul selectat sunt trimise catre ViewModel doar cand becul e pornit.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Color Spectrum",
                        color = OnBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        ColorWheel(
                            selectedAngle = selectedAngle,
                            onColorChanged = { color, angle ->
                                if (isOn) viewModel.setColorFromWheel(color, angle)
                            }
                        )
                    }
                }

                // ── Quick Presets ─────────────────────────────────────────────
                // Grid 2x3 cu presetarile de culori predefinite (de ex. Warm White, Red, Blue etc.)
                // Presetarile sunt definite in RgbBulbViewModel ca lista rgbPresets.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Quick Presets",
                        color = OnBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    // Primul rand: primele 3 presetari din lista rgbPresets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rgbPresets.take(3).forEach { preset ->
                            ColorPresetButton(
                                name = preset.name,
                                color = preset.color,
                                isSelected = selectedPreset == preset.name,
                                onClick = { viewModel.selectPreset(preset) }
                            )
                        }
                    }
                    // Al doilea rand: urmatoarele 3 presetari (pozitiile 3, 4, 5)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rgbPresets.drop(3).forEach { preset ->
                            ColorPresetButton(
                                name = preset.name,
                                color = preset.color,
                                isSelected = selectedPreset == preset.name,
                                onClick = { viewModel.selectPreset(preset) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
