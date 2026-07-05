/**
 * TvRemoteScreen.kt - Ecran telecomanda virtuala pentru televizor
 *
 * Ofera o interfata de tip telecomanda cu butoane de volum, canale, navigare
 * directional, meniu si un buton de power cu design rosu. Fiecare buton apeleaza
 * viewModel.sendCommand("...") care face POST /commands/send catre backend.
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.ui.components.*
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.TvRemoteViewModel

/**
 * Ecranul principal al telecomenzii virtuale pentru TV — wrapper subtire peste ViewModel.
 * Colecteaza starea isOn si isMuted din [TvRemoteViewModel] si o paseaza catre
 * [TvRemoteScreenContent], care contine tot UI-ul si nu depinde de ViewModel (poate fi
 * randat direct in @Preview).
 *
 * @param navController pentru butonul de intoarcere din top bar
 * @param device obiectul DeviceResponse cu numele si camera dispozitivului
 * @param deviceId ID-ul dispozitivului folosit la initializarea ViewModel-ului
 */
@Composable
fun TvRemoteScreen(
    navController: NavController,
    device: DeviceResponse,
    deviceId: Int
) {
    val app = LocalContext.current.applicationContext as Application
    val viewModel: TvRemoteViewModel = viewModel(
        factory = TvRemoteViewModel.Factory(app, deviceId)
    )
    val isOn by viewModel.isOn.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()

    TvRemoteScreenContent(
        device = device,
        isOn = isOn,
        isMuted = isMuted,
        onBack = { navController.popBackStack() },
        onSendCommand = { viewModel.sendCommand(it) }
    )
}

/**
 * UI-ul telecomenzii virtuale pentru TV, fara dependinta de ViewModel.
 * Primeste toata starea ca parametri simpli, ceea ce permite randare in @Preview
 * fara apeluri de retea.
 */
@Composable
fun TvRemoteScreenContent(
    device: DeviceResponse,
    isOn: Boolean,
    isMuted: Boolean,
    onBack: () -> Unit,
    onSendCommand: (String) -> Unit
) {
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
            // Bara superioara: buton Back (stanga), nume + camera (centru),
            // indicator status colorat (verde=ON, rosu=OFF) in dreapta
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Surface)
                    .padding(horizontal = 8.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = OnSurface)
                }
                Column(modifier = Modifier.align(Alignment.Center)) {
                    Text(
                        text = device.name,
                        color = OnBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = device.room ?: "",
                        color = OnSurface,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                // Punct colorat: verde cand TV-ul e pornit, rosu cand e oprit
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isOn) Color(0xFF4CAF50) else ErrorColor)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // ── Power Button ─────────────────────────────────────────────
                // Buton circular cu fond rosu inchis (#2A1A1A), border rosu inchis (#8B0000)
                // si iconita rosie aprinsa (#FF3333). Trimite comanda "power" la apasare.
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A1A1A))
                        .border(2.dp, Color(0xFF8B0000), CircleShape)
                        .clickable { onSendCommand("power") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Power",
                        tint = Color(0xFFFF3333),
                        modifier = Modifier.size(36.dp)
                    )
                }

                // ── VOL / CH row ─────────────────────────────────────────────
                // Rand cu trei coloane: VOL (stanga), NavigationPad (centru), CH (dreapta)
                // Fiecare buton apeleaza onSendCommand() cu comanda corespunzatoare
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Coloana volum: buton crestere, buton mute (icon se schimba), buton scadere
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("VOL", color = OnSurface, style = MaterialTheme.typography.labelMedium)
                        RemoteButton(
                            onClick = { onSendCommand("vol_up") },
                            icon = Icons.Default.Add,
                            size = 52.dp
                        )
                        RemoteButton(
                            onClick = { onSendCommand("mute") },
                            icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            iconTint = if (isMuted) Primary else OnBackground,
                            size = 52.dp
                        )
                        RemoteButton(
                            onClick = { onSendCommand("vol_down") },
                            icon = Icons.Default.Remove,
                            size = 52.dp
                        )
                    }

                    // NavigationPad: componentul cu 5 butoane directionale (sus/jos/stanga/dreapta/ok)
                    NavigationPad(
                        onUp = { onSendCommand("up") },
                        onDown = { onSendCommand("down") },
                        onLeft = { onSendCommand("left") },
                        onRight = { onSendCommand("right") },
                        onCenter = { onSendCommand("ok") }
                    )

                    // Coloana canale: buton canal urmator, buton sursa de intrare, buton canal anterior
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("CH", color = OnSurface, style = MaterialTheme.typography.labelMedium)
                        RemoteButton(
                            onClick = { onSendCommand("ch_up") },
                            icon = Icons.Default.KeyboardArrowUp,
                            size = 52.dp
                        )
                        RemoteButton(
                            onClick = { onSendCommand("source") },
                            icon = Icons.Default.Input,
                            size = 52.dp
                        )
                        RemoteButton(
                            onClick = { onSendCommand("ch_down") },
                            icon = Icons.Default.KeyboardArrowDown,
                            size = 52.dp
                        )
                    }
                }

                // ── MENU / BACK / HOME row ────────────────────────────────────
                // Rand cu trei butoane dreptunghiulare pentru navigarea in meniurile TV
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RemoteButton(
                        onClick = { onSendCommand("menu") },
                        label = "MENU",
                        size = 64.dp,
                        cornerRadius = 16.dp
                    )
                    RemoteButton(
                        onClick = { onSendCommand("home") },
                        icon = Icons.Default.Home,
                        size = 64.dp,
                        cornerRadius = 16.dp
                    )
                    RemoteButton(
                        onClick = { onSendCommand("back") },
                        label = "BACK",
                        size = 64.dp,
                        cornerRadius = 16.dp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

private val previewTvDevice = DeviceResponse(
    id = 4,
    name = "Living Room TV",
    device_type = "ir_tv",
    room = "Living Room",
    room_id = 1,
    mqtt_topic = null,
    is_online = true,
    last_status = "on",
    mac_address = null,
    ir_codes = null,
    ir_remote_type = "44-key",
    owner_id = 1,
    created_at = "2026-01-01T00:00:00"
)

@Preview(showBackground = true)
@Composable
fun TvRemoteScreenOnPreview() {
    SmartHomeTheme {
        TvRemoteScreenContent(
            device = previewTvDevice,
            isOn = true,
            isMuted = false,
            onBack = {},
            onSendCommand = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TvRemoteScreenOffPreview() {
    SmartHomeTheme {
        TvRemoteScreenContent(
            device = previewTvDevice.copy(is_online = false, last_status = "off"),
            isOn = false,
            isMuted = true,
            onBack = {},
            onSendCommand = {}
        )
    }
}
