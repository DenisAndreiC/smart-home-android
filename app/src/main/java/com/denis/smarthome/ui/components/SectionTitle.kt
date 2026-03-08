/**
 * SectionTitle.kt - Componenta titlu de sectiune pentru ecranele aplicatiei
 *
 * Afiseaza un titlu formatat cu majuscule si spatiere extinsa intre litere,
 * aliniat la stanga. Suporta optional un element actiune (ex: buton "Vezi tot")
 * afisat la dreapta, prin intermediul unui slot composable.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.denis.smarthome.ui.theme.OnSurface

/**
 * SectionTitle - Titlu stilizat pentru o sectiune de continut.
 *
 * Randeaza textul cu majuscule si letter-spacing marit pentru aspect vizual
 * consistent cu designul dashboardului. Daca parametrul [action] este furnizat,
 * acesta este afisat la capatul drept al randului (ex: link "Vezi tot").
 *
 * @param title Textul titlului care va fi transformat automat in majuscule
 * @param modifier Modifier optional pentru personalizare externa
 * @param action Composable optional afisat in dreapta titlului (ex: TextButton)
 */
@Composable
fun SectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Titlul este afisat cu majuscule si spatiere extinsa intre litere
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurface,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp
        )
        // Slotul de actiune este invocat doar daca a fost furnizat
        action?.invoke()
    }
}