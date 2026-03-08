/**
 * SmartHomeCard.kt - Card generic reutilizabil pentru suprafete de continut
 *
 * Defineste componenta [SmartHomeCard] cu stil unitar (culoare Surface, colturi
 * rotunjite, bordura Outline) folosita in toata aplicatia. Include si extensia
 * [dashedBorder] pe Modifier pentru a desena o bordura punctata cu PathEffect.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denis.smarthome.ui.theme.Outline
import com.denis.smarthome.ui.theme.Surface

/**
 * dashedBorder - Extensie Modifier care deseneaza o bordura punctata in jurul componentei.
 *
 * Foloseste [PathEffect.dashPathEffect] pentru a crea modelul linie-spatiu al bordurii.
 * Colturi rotunjite sunt controlate prin [cornerRadius], iar raportul linie/spatiu
 * prin [dashWidth] si [gapWidth].
 *
 * @param color Culoarea bordurii punctate
 * @param cornerRadius Raza colturilor dreptunghiului rotunjit
 * @param dashWidth Lungimea fiecarei liniute din bordura
 * @param gapWidth Spatiul dintre liniute
 */
fun Modifier.dashedBorder(
    color: Color,
    cornerRadius: Dp = 16.dp,
    dashWidth: Float = 12f,
    gapWidth: Float = 8f
): Modifier = this.drawBehind {
    // Deseneaza un dreptunghi rotunjit cu stil punctat folosind PathEffect
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(cornerRadius.toPx()),
        style = Stroke(
            width = 1.5.dp.toPx(),
            // Alterneaza intre liniuta de dashWidth si spatiu de gapWidth
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(dashWidth, gapWidth), 0f)
        )
    )
}

/**
 * SmartHomeCard - Card generic cu stil vizual unitar al aplicatiei SmartHome.
 *
 * Ofera o suprafata cu fundal Surface, colturi rotunjite la 16dp si bordura
 * subtire Outline. Continutul este injectat prin slotul [content] si infasurat
 * intr-un [Box] cu padding configurabil prin [contentPadding].
 *
 * @param modifier Modifier optional aplicat pe Card
 * @param contentPadding Padding intern aplicat in jurul continutului (implicit 16dp pe toate partile)
 * @param content Slotul composable care reprezinta continutul cardului
 */
@Composable
fun SmartHomeCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        // Culoarea de fundal este Surface din paleta temei aplicatiei
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(16.dp),
        // Bordura subtire pentru a delimita cardul de fundal
        border = BorderStroke(1.dp, Outline)
    ) {
        // Box aplica padding-ul intern configurat si gazduieste continutul
        Box(modifier = Modifier.padding(contentPadding)) {
            content()
        }
    }
}