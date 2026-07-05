/**
 * RgbBulbScreen.kt - Ecran de control pentru becul RGB controlat prin IR
 *
 * Afiseaza un cerc hero cu efect de glow care reflecta culoarea activa,
 * doua butoane de luminozitate (step up/down; nu exista buton on/off separat),
 * un grid cu butoanele de culori fixe ale telecomenzii IR si butonul pentru efectul Ice.
 *
 * NOTA: Becul RGB este non-smart, controlat prin telecomanda IR (NEC protocol).
 * Nu exista slider de luminozitate sau color picker continuu — doar comenzi discrete
 * corespunzatoare butoanelor fizice de pe telecomanda 44-key / 24-key.
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.IrColorButton as IrColorButtonModel
import com.denis.smarthome.viewmodel.RgbBulbViewModel
import com.denis.smarthome.viewmodel.rgbIrColors

/**
 * Ecranul principal de control al becului RGB prin IR — wrapper subtire peste ViewModel.
 * Citeste starea din [RgbBulbViewModel] si o paseaza catre [RgbBulbScreenContent], care
 * contine tot UI-ul si nu depinde de ViewModel (poate fi randat direct in @Preview).
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
    val isOn               by viewModel.isOn.collectAsState()
    val selectedColor      by viewModel.selectedColor.collectAsState()
    val activeColorCommand by viewModel.activeColorCommand.collectAsState()
    val isDeleted          by viewModel.isDeleted.collectAsState()

    LaunchedEffect(isDeleted) {
        if (isDeleted) navController.popBackStack()
    }

    RgbBulbScreenContent(
        device = device,
        isOn = isOn,
        selectedColor = selectedColor,
        activeColorCommand = activeColorCommand,
        onBack = { navController.popBackStack() },
        onDeleteConfirm = { viewModel.deleteDevice() },
        onColorSelect = { viewModel.selectColor(it) },
        onBrightnessUp = { viewModel.brightnessUp() },
        onBrightnessDown = { viewModel.brightnessDown() },
        onIce = { viewModel.effectIce() }
    )
}

/**
 * UI-ul ecranului de control al becului RGB, fara dependinta de ViewModel.
 * Primeste toata starea ca parametri simpli, ceea ce permite randare in @Preview
 * fara apeluri de retea.
 *
 * Layout:
 * 1. Hero circle — reflecta culoarea activa cu efect glow
 * 2. Brightness card — doua butoane ▲/▼ (step up/down, nu slider; nu exista on/off)
 * 3. Color grid — butoane pentru culorile fixe ale telecomenzii IR
 * 4. Effects — buton Ice
 */
@Composable
fun RgbBulbScreenContent(
    device: DeviceResponse,
    isOn: Boolean,
    selectedColor: Color,
    activeColorCommand: String,
    onBack: () -> Unit,
    onDeleteConfirm: () -> Unit,
    onColorSelect: (IrColorButtonModel) -> Unit,
    onBrightnessUp: () -> Unit,
    onBrightnessDown: () -> Unit,
    onIce: () -> Unit
) {
    var showMenu         by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

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
                    onClick = { showDeleteDialog = false; onDeleteConfirm() },
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
                    onClick = onBack,
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

                // ── Brightness Card ───────────────────────────────────────────
                // Doua butoane mari pentru brightness up/down (step-based, nu slider).
                // Becul IR nu suporta procente — doar increment/decrement pe butonul fizic.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Brightness6,
                                contentDescription = null,
                                tint = if (isOn) Primary else OnSurface,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Brightness",
                                color = if (isOn) OnBackground else OnSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Brightness Down button
                            Button(
                                onClick = onBrightnessDown,
                                enabled = isOn,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SurfaceVariant,
                                    contentColor = OnBackground,
                                    disabledContainerColor = SurfaceVariant.copy(alpha = 0.5f),
                                    disabledContentColor = OnSurface.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.RemoveCircleOutline,
                                    contentDescription = "Brightness Down",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lumina mai slaba", fontWeight = FontWeight.Medium)
                            }

                            // Brightness Up button
                            Button(
                                onClick = onBrightnessUp,
                                enabled = isOn,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Primary,
                                    contentColor = Color.Black,
                                    disabledContainerColor = Primary.copy(alpha = 0.3f),
                                    disabledContentColor = OnSurface.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.AddCircleOutline,
                                    contentDescription = "Brightness Up",
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lumina mai tare", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                // ── Color Selection ───────────────────────────────────────────
                // Butoane de culori fixe corespunzatoare telecomenzii IR.
                // Fiecare buton trimite comanda IR directa catre ESP32 (ex: "red", "blue").
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Colors",
                        color = OnBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Grid de culori — afisam toate culorile IR disponibile
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rgbIrColors.forEach { irColor ->
                            IrColorButton(
                                irColor = irColor,
                                isSelected = activeColorCommand == irColor.irCommand,
                                isEnabled = isOn,
                                onClick = { onColorSelect(irColor) }
                            )
                        }
                    }
                }

                // ── Effects ───────────────────────────────────────────────────
                // Buton pentru modul Ice (efect alb-albastru pulsat de pe telecomanda IR).
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Effects",
                        color = OnBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Ice effect button
                    OutlinedButton(
                        onClick = onIce,
                        enabled = isOn,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isOn) Primary else Outline
                        )
                    ) {
                        Icon(
                            Icons.Default.AcUnit,
                            contentDescription = null,
                            tint = if (isOn) Primary else OnSurface.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Ice",
                            color = if (isOn) Primary else OnSurface.copy(alpha = 0.3f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private val previewRgbBulbDevice = DeviceResponse(
    id = 1,
    name = "Living Room Bulb",
    device_type = "ir_rgb",
    room = "Living Room",
    room_id = 1,
    mqtt_topic = null,
    is_online = true,
    last_status = "on",
    mac_address = null,
    ir_codes = null,
    ir_remote_type = "24-key",
    owner_id = 1,
    created_at = "2026-01-01T00:00:00"
)

@Preview(showBackground = true)
@Composable
fun RgbBulbScreenOnPreview() {
    SmartHomeTheme {
        RgbBulbScreenContent(
            device = previewRgbBulbDevice,
            isOn = true,
            selectedColor = Color(0xFFFF0000),
            activeColorCommand = "red",
            onBack = {},
            onDeleteConfirm = {},
            onColorSelect = {},
            onBrightnessUp = {},
            onBrightnessDown = {},
            onIce = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RgbBulbScreenOffPreview() {
    SmartHomeTheme {
        RgbBulbScreenContent(
            device = previewRgbBulbDevice.copy(is_online = false, last_status = "off"),
            isOn = false,
            selectedColor = Color(0xFFFF0000),
            activeColorCommand = "red",
            onBack = {},
            onDeleteConfirm = {},
            onColorSelect = {},
            onBrightnessUp = {},
            onBrightnessDown = {},
            onIce = {}
        )
    }
}

/**
 * Buton individual de culoare IR cu preview vizual si indicator de selectie.
 *
 * Afiseaza un cerc colorat cu border highlight cand este selectat activ.
 * La click trimite comanda IR corespunzatoare catre ViewModel.
 *
 * @param irColor modelul de date al culorii IR (nume, culoare, comanda)
 * @param isSelected true daca aceasta culoare este activa pe bec
 * @param isEnabled true daca becul este pornit (butoanele sunt dezactivate cand e oprit)
 * @param onClick callback apelat la selectarea culorii
 */
@Composable
private fun IrColorButton(
    irColor: com.denis.smarthome.viewmodel.IrColorButton,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(8.dp)
    ) {
        // Cercul de culoare cu border de selectie
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isEnabled) irColor.color else irColor.color.copy(alpha = 0.3f)
                )
                .then(
                    if (isSelected && isEnabled) {
                        Modifier.border(3.dp, Color.White, CircleShape)
                    } else {
                        Modifier.border(1.dp, Outline, CircleShape)
                    }
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = irColor.name,
            color = if (isSelected && isEnabled) OnBackground else OnSurface,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
