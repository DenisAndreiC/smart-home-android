/**
 * NotificationsScreen.kt - Ecranul de notificari al aplicatiei SmartHome
 *
 * Afiseaza lista de notificari primite de la backend, ordonate cronologic.
 * Notificarile necitite sunt evidentiate vizual (bold, punct albastru, fundal teal).
 * Timestamp-ul ISO 8601 (cu offset UTC) este convertit la ora locala a telefonului.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.denis.smarthome.data.model.NotificationResponse
import com.denis.smarthome.ui.theme.*
import com.denis.smarthome.util.formatIsoToLocalDateTime
import com.denis.smarthome.viewmodel.NotificationsViewModel

/**
 * Ecranul principal al sectiunii Notificari.
 *
 * Afiseaza 3 stari posibile:
 * - incarcare: [CircularProgressIndicator] centrat
 * - lista goala: mesaj informativ cu iconita
 * - lista populata: [LazyColumn] cu [NotificationItem]-uri
 *
 * Butonul Refresh din TopAppBar reapeleaza [NotificationsViewModel.loadNotifications].
 *
 * @param navController controlerul de navigare Compose
 * @param viewModel NotificationsViewModel furnizat prin injectare Compose
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavController,
    viewModel: NotificationsViewModel = viewModel()
) {
    val notifications by viewModel.notifications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("Notifications", color = OnBackground, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    // Buton inapoi: popBackStack() revine la ecranul anterior (Dashboard)
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OnBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadNotifications() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        }
    ) { innerPadding ->
        // Tranzitie intre cele 3 stari: loading / gol / populat
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Primary) }
            }
            notifications.isEmpty() -> {
                // Stare goala: iconita + mesaj descriptiv
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = OnSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Text("No notifications", color = OnSurface, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            else -> {
                // Lista de notificari cu key = id pentru recompunere eficienta
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(notifications, key = { it.id }) { notification ->
                        NotificationItem(notification = notification)
                    }
                }
            }
        }
    }
}

/**
 * Card compozabil pentru o singura notificare.
 *
 * Diferentiaza vizual notificarile necitite (is_read == false) de cele citite:
 * - Necitita: fundal PrimaryContainer, text bold, punct albastru pe dreapta
 * - Citita: fundal SurfaceVariant, text normal, fara punct
 *
 * Timestamp-ul ISO 8601 (ex: "2025-03-17T18:30:00+00:00") este convertit la
 * ora locala a telefonului prin [formatIsoToLocalDateTime].
 *
 * @param notification datele notificarii primite din API
 */
@Composable
private fun NotificationItem(notification: NotificationResponse) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Outline)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Iconita cu fundal colorat: PrimaryContainer daca necitita, SurfaceVariant daca citita
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (notification.is_read) SurfaceVariant else PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = if (notification.is_read) OnSurface else Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notification.message,
                    // Notificarile necitite au text OnBackground + bold; cele citite OnSurface + normal
                    color = if (notification.is_read) OnSurface else OnBackground,
                    fontSize = 14.sp,
                    fontWeight = if (notification.is_read) FontWeight.Normal else FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                // Timestamp ISO 8601 cu offset UTC, convertit la ora locala a telefonului
                Text(
                    text = formatIsoToLocalDateTime(notification.created_at),
                    color = OnSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
            // Punct albastru indicator "necitit" pe partea dreapta a cardului
            if (!notification.is_read) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Primary)
                )
            }
        }
    }
}
