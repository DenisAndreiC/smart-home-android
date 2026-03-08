/**
 * NavigationPad.kt - Pad directional tip D-Pad pentru telecomanda smart
 *
 * Implementeaza un control cruciform cu cinci butoane (Sus, Jos, Stanga, Dreapta, OK)
 * dispuse intr-un Box de 220dp, util pentru navigarea in meniurile TV sau ale altor
 * dispozitive controlate prin aplicatia SmartHome.
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.denis.smarthome.ui.theme.OnBackground
import com.denis.smarthome.ui.theme.Primary
import com.denis.smarthome.ui.theme.SurfaceVariant

/**
 * Pad directional cruciform cu butoane de navigare si buton central de confirmare.
 *
 * Cele patru directii (Sus, Jos, Stanga, Dreapta) sunt amplasate simetric in jurul
 * butonului circular central "OK". Fiecare buton apeleaza callback-ul corespunzator
 * la apasare, fara stare interna.
 *
 * @param onUp Callback apelat la apasarea directiei Sus.
 * @param onDown Callback apelat la apasarea directiei Jos.
 * @param onLeft Callback apelat la apasarea directiei Stanga.
 * @param onRight Callback apelat la apasarea directiei Dreapta.
 * @param onCenter Callback apelat la apasarea butonului central OK.
 * @param modifier Modifier optional pentru personalizare externa.
 */
@Composable
fun NavigationPad(
    onUp: () -> Unit,
    onDown: () -> Unit,
    onLeft: () -> Unit,
    onRight: () -> Unit,
    onCenter: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        // Butonul circular central "OK" — confirmare/selectie in meniu
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Primary)
                .clickable(onClick = onCenter),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "OK",
                tint = Color.Black,
                modifier = Modifier.size(26.dp)
            )
        }

        // Butonul Sus — colturi rotunjite doar in partea de sus pentru aspect cruciform
        Box(
            modifier = Modifier
                .size(52.dp)
                .align(Alignment.TopCenter)
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
                .background(SurfaceVariant)
                .clickable(onClick = onUp),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = OnBackground)
        }

        // Butonul Jos — colturi rotunjite doar in partea de jos
        Box(
            modifier = Modifier
                .size(52.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
                .background(SurfaceVariant)
                .clickable(onClick = onDown),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = OnBackground)
        }

        // Butonul Stanga — colturi rotunjite pe latura stanga
        Box(
            modifier = Modifier
                .size(52.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(topStart = 14.dp, bottomStart = 14.dp))
                .background(SurfaceVariant)
                .clickable(onClick = onLeft),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = OnBackground)
        }

        // Butonul Dreapta — colturi rotunjite pe latura dreapta
        Box(
            modifier = Modifier
                .size(52.dp)
                .align(Alignment.CenterEnd)
                .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp))
                .background(SurfaceVariant)
                .clickable(onClick = onRight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = OnBackground)
        }
    }
}
