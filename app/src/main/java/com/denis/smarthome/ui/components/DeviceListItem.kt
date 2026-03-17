/**
 * DeviceListItem.kt - Componenta card pentru un dispozitiv in lista
 *
 * Afiseaza informatii despre un dispozitiv (nume, camera, status online/offline)
 * impreuna cu o iconita selectata dinamic si un Switch M3 pentru pornire/oprire.
 * Dot-ul colorat (verde/rosu) indica rapid starea activa sau inactiva.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denis.smarthome.data.model.DeviceResponse
import com.denis.smarthome.ui.theme.*

/**
 * Selecteaza iconita potrivita pentru un dispozitiv pe baza tipului si numelui.
 *
 * Verificarea se face in ordine: tipul exact (tv, ac), apoi cuvinte cheie din
 * numele dispozitivului (light, bulb, lamp, fan, coffee). Daca nimic nu se
 * potriveste, returneaza iconita generica [Icons.Default.Devices].
 *
 * @param deviceType Tipul dispozitivului din backend (ex: "relay", "ir")
 * @param name Numele dispozitivului introdus de utilizator
 * @return Iconita [ImageVector] corespunzatoare
 */
private fun deviceIcon(deviceType: String, name: String): ImageVector {
    val type = deviceType.lowercase()
    val nameLower = name.lowercase()
    return when {
        type == "ir_tv" || type == "tv" || nameLower.contains("tv") || nameLower.contains("television") -> Icons.Default.Tv
        type == "ir_ac" || type == "ac" || nameLower.contains("air") || nameLower.contains("ac") -> Icons.Default.AcUnit
        type == "ir_rgb" || nameLower.contains("rgb") || nameLower.contains("light") || nameLower.contains("bulb") || nameLower.contains("lamp") -> Icons.Default.Lightbulb
        nameLower.contains("fan") -> Icons.Default.Air
        nameLower.contains("coffee") -> Icons.Default.LocalCafe
        else -> Icons.Default.Devices
    }
}

/**
 * Card pentru un dispozitiv din lista cu iconita, info si switch de control.
 *
 * Click-ul pe card navigheaza la ecranul de control al dispozitivului.
 * Switch-ul M3 isi consuma propriul eveniment de click si nu il propaga la card.
 *
 * @param device Datele dispozitivului primite din API
 * @param onToggle Callback apelat cand utilizatorul schimba starea switch-ului
 * @param onClick Callback apelat la click pe intreaga linie (navigare la control)
 * @param modifier Modifier optional pentru stilizare externa
 */
@Composable
fun DeviceListItem(
    device: DeviceResponse,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Derive toggle state from last_status when available, fallback to is_online
    val isOn = when (device.last_status?.lowercase()) {
        "on" -> true
        "off" -> false
        else -> device.is_online
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(start = 14.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Iconita dispozitivului selectata dinamic de deviceIcon()
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0D2B35)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = deviceIcon(device.device_type, device.name),
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                // Dot colorat: verde (#4CAF50) daca activ, rosu (ErrorColor) daca inactiv
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = device.name,
                        color = OnBackground,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isOn) Color(0xFF4CAF50) else ErrorColor)
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${device.room ?: ""} • ${if (isOn) "Online" else "Offline"}",
                    color = OnSurface,
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "LAST USED: RECENTLY",
                    color = OnSurface.copy(alpha = 0.55f),
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 0.5.sp
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Switch M3 pentru pornire/oprire; isi consuma propriul click, nu propaga la card
            Switch(
                checked = isOn,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = Primary,
                    uncheckedThumbColor = OnSurface,
                    uncheckedTrackColor = SurfaceVariant
                )
            )

            // Buton de stergere
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = ErrorColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
