/**
 * RoomCard.kt - Card de camera si card de adaugare camera pentru grid-ul HomeScreen
 *
 * [RoomCard] afiseaza numele camerei si numarul de dispozitive active cu un gradient
 * de fundal personalizat. [AddRoomCard] este un card cu border punctat (dashedBorder)
 * care permite utilizatorului sa adauge o camera noua.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denis.smarthome.ui.theme.OnBackground
import com.denis.smarthome.ui.theme.OnSurface
import com.denis.smarthome.ui.theme.Primary

/**
 * Card de camera cu gradient de fundal si informatii despre dispozitive.
 *
 * @param name Numele camerei (ex: "Living Room")
 * @param activeCount Numarul de dispozitive active in camera
 * @param deviceCount Numarul total de dispozitive in camera
 * @param gradientColors Lista de culori pentru gradientul vertical de fundal
 * @param onClick Callback apelat la click pe card
 * @param modifier Modifier optional pentru stilizare externa
 */
@Composable
fun RoomCard(
    name: String,
    activeCount: Int,
    deviceCount: Int,
    gradientColors: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(165.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(gradientColors))
            .clickable(onClick = onClick)
    ) {
        // Buton sageata in coltul din dreapta sus pentru navigare rapida
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Primary.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(14.dp)
            )
        }

        // Informatii camera aliniate in partea de jos a cardului
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp)
        ) {
            Text(
                text = name,
                color = OnBackground,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$activeCount/$deviceCount dispozitive active",
                color = OnSurface,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

/**
 * Card de adaugare camera cu border punctat (dashedBorder Modifier extension).
 *
 * @param onClick Callback apelat la click pe card
 * @param modifier Modifier optional pentru stilizare externa
 */
@Composable
fun AddRoomCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(165.dp)
            .clip(RoundedCornerShape(16.dp))
            .dashedBorder(Primary, cornerRadius = 16.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Adaugă cameră",
                color = Primary,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}