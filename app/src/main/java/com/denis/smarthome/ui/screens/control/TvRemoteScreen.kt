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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.ui.components.*
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.viewmodel.TvRemoteViewModel

/**
 * Ecranul principal al telecomenzii virtuale pentru TV.
 * Colecteaza starea isOn si isMuted din TvRemoteViewModel si afiseaza
 * o interfata scrollabila cu toate controalele telecomenzii.
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
                    onClick = { navController.popBackStack() },
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
                        .clickable { viewModel.sendCommand("power") },
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
                // Fiecare buton apeleaza viewModel.sendCommand() cu comanda corespunzatoare
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
                            onClick = { viewModel.sendCommand("vol_up") },
                            icon = Icons.Default.Add,
                            size = 52.dp
                        )
                        RemoteButton(
                            onClick = { viewModel.sendCommand("mute") },
                            icon = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            iconTint = if (isMuted) Primary else OnBackground,
                            size = 52.dp
                        )
                        RemoteButton(
                            onClick = { viewModel.sendCommand("vol_down") },
                            icon = Icons.Default.Remove,
                            size = 52.dp
                        )
                    }

                    // NavigationPad: componentul cu 5 butoane directionale (sus/jos/stanga/dreapta/ok)
                    NavigationPad(
                        onUp = { viewModel.sendCommand("up") },
                        onDown = { viewModel.sendCommand("down") },
                        onLeft = { viewModel.sendCommand("left") },
                        onRight = { viewModel.sendCommand("right") },
                        onCenter = { viewModel.sendCommand("ok") }
                    )

                    // Coloana canale: buton canal urmator, buton sursa de intrare, buton canal anterior
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("CH", color = OnSurface, style = MaterialTheme.typography.labelMedium)
                        RemoteButton(
                            onClick = { viewModel.sendCommand("ch_up") },
                            icon = Icons.Default.KeyboardArrowUp,
                            size = 52.dp
                        )
                        RemoteButton(
                            onClick = { viewModel.sendCommand("source") },
                            icon = Icons.Default.Input,
                            size = 52.dp
                        )
                        RemoteButton(
                            onClick = { viewModel.sendCommand("ch_down") },
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
                        onClick = { viewModel.sendCommand("menu") },
                        label = "MENU",
                        size = 64.dp,
                        cornerRadius = 16.dp
                    )
                    RemoteButton(
                        onClick = { viewModel.sendCommand("home") },
                        icon = Icons.Default.Home,
                        size = 64.dp,
                        cornerRadius = 16.dp
                    )
                    RemoteButton(
                        onClick = { viewModel.sendCommand("back") },
                        label = "BACK",
                        size = 64.dp,
                        cornerRadius = 16.dp
                    )
                }

                // ── Learn New Button ──────────────────────────────────────────
                // Buton pentru invatarea de noi comenzi IR (functionalitate viitoare)
                HorizontalDivider(color = Outline, modifier = Modifier.padding(vertical = 4.dp))
                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariant)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Learn New Command", color = Primary, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
