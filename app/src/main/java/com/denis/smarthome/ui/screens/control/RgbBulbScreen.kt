package com.denis.smarthome.ui.screens.control

import android.app.Application
import androidx.compose.foundation.background
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
    val error          by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }

    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it); viewModel.clearError() }
    }

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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = OnSurface)
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
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Hero Bulb Circle ─────────────────────────────────────────
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
                            // Glow effect behind
                            if (isOn) {
                                drawCircle(
                                    color = selectedColor.copy(alpha = 0.25f),
                                    radius = size.minDimension * 0.58f
                                )
                            }
                            // Main gradient circle
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
                    // Row 1
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
                    // Row 2
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
