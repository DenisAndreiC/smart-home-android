/**
 * SmartFilterChip.kt - Chip de filtrare personalizat pentru listele aplicatiei
 *
 * Componenta chip cu stare selectata/neselectata, folosita pentru filtrarea
 * dispozitivelor dupa camera. Stilul vizual (fundal, bordura, culoare text)
 * se adapteaza dinamic in functie de parametrul [selected].
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.denis.smarthome.ui.theme.Primary

/**
 * SmartFilterChip - Chip de filtrare cu doua stari: selectat si neselectat.
 *
 * Cand [selected] este true, chipul este umplut cu culoarea Primary si textul
 * devine negru. Cand este false, fundalul este transparent si textul apare in
 * Primary. Suporta un element optional [trailingContent] (ex: un numar badge).
 *
 * @param label Textul afisat pe chip
 * @param selected Starea curenta a chipului (selectat / neselectat)
 * @param onClick Callback invocat la apasarea chipului
 * @param modifier Modifier optional pentru stilizare externa
 * @param trailingContent Composable optional afisat dupa text (ex: contor)
 */
@Composable
fun SmartFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(20.dp)
    // Fundalul este plin (Primary) cand selectat, transparent cand nu este
    val bgColor = if (selected) Primary else Color.Transparent
    // Textul negru pe fundal teal selectat, teal pe fundal transparent
    val textColor = if (selected) Color.Black else Primary
    // Bordura completa cand selectat, semitransparenta cand neselectat
    val borderColor = if (selected) Primary else Primary.copy(alpha = 0.5f)

    Row(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Fontul devine SemiBold cand chipul este selectat
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
        // Continutul trailing (ex: badge cu numar) este optional
        trailingContent?.invoke()
    }
}
