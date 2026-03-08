/**
 * StatCard.kt - Card de statistici pentru dashboard-ul SmartHome
 *
 * Afiseaza o statistica simpla cu iconita, eticheta, valoare numerica si
 * un text de modificare/trend. Folosit in LazyRow pe ecranul HomeScreen.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denis.smarthome.ui.theme.*

/**
 * Card de statistici cu iconita si valoare numerica.
 *
 * @param icon Iconita [ImageVector] afisata in cercul teal din colt
 * @param label Eticheta descriptiva a statisticii (ex: "Dispozitive")
 * @param value Valoarea principala afisata mare (ex: "12")
 * @param change Text secundar care indica trendul (ex: "+2 adaugate")
 * @param modifier Modifier optional pentru stilizare externa
 */
@Composable
fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    change: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(160.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Cerc colorat PrimaryContainer cu iconita teal in centru
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PrimaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Eticheta descriptiva in culoare secundara (OnSurface)
            Text(
                text = label,
                color = OnSurface,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Valoarea principala afisata cu font mare si bold
            Text(
                text = value,
                color = OnBackground,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Textul de trend/schimbare afisat in culoarea Primary (teal)
            Text(
                text = change,
                color = Primary,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}