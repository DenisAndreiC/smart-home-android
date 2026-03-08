/**
 * RemoteButton.kt - Butoane reutilizabile pentru interfata de telecomanda
 *
 * Defineste doua componente Composable: RemoteButton (buton patrat cu icona sau eticheta)
 * si RemoteTextButton (buton dreptunghiular cu text), folosite in ecranele de control
 * al dispozitivelor smart (TV, AC etc.).
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.denis.smarthome.ui.theme.OnBackground
import com.denis.smarthome.ui.theme.OnSurface
import com.denis.smarthome.ui.theme.SurfaceVariant

/**
 * Buton patrat pentru telecomanda, cu suport pentru icona sau text scurt.
 *
 * Afiseaza fie o icona vectoriala, fie o eticheta text in centrul unui Box
 * cu colturi rotunjite si culoare de fundal configurabila.
 *
 * @param onClick Callback apelat la apasarea butonului.
 * @param modifier Modifier optional pentru personalizare externa.
 * @param icon Icona vectoriala afisata in centru (prioritara fata de label).
 * @param label Text afisat daca nu este furnizata o icona.
 * @param backgroundColor Culoarea de fundal a butonului.
 * @param iconTint Culoarea aplicata iconei sau textului.
 * @param size Dimensiunea laturii butonului patrat.
 * @param cornerRadius Raza colturilor rotunjite.
 */
@Composable
fun RemoteButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    label: String? = null,
    backgroundColor: Color = SurfaceVariant,
    iconTint: Color = OnBackground,
    size: Dp = 52.dp,
    cornerRadius: Dp = 14.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Daca exista icona, o afisam; altfel afisam eticheta text
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        } else if (label != null) {
            Text(
                text = label,
                color = iconTint,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Buton dreptunghiular cu eticheta text, folosit in randurile de control ale telecomenzii.
 *
 * Are o inaltime fixa de 44dp si colturi rotunjite, potrivit pentru butoane
 * functionale precum "Sursa", "Meniu" sau alte comenzi textuale.
 *
 * @param label Textul afisat pe buton.
 * @param onClick Callback apelat la apasarea butonului.
 * @param modifier Modifier optional pentru personalizare externa.
 * @param backgroundColor Culoarea de fundal.
 * @param textColor Culoarea textului.
 */
@Composable
fun RemoteTextButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = SurfaceVariant,
    textColor: Color = OnSurface
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}
