/**
 * Shape.kt - Formele (raza colturilor) componentelor UI SmartHome
 *
 * Defineste sistemul de forme Material 3 cu colturi rotunjite progresiv.
 * Formele mai mari (large, extraLarge) sunt folosite pentru carduri si
 * panouri principale, cele mici pentru chip-uri si butoane mici.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Sistemul de forme al aplicatiei SmartHome.
 * Toate componentele Material 3 utilizeaza automat aceste forme
 * prin tema aplicatiei — nu este necesara specificarea manuala.
 */
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),   // chip-uri mici, badge-uri
    small = RoundedCornerShape(12.dp),        // butoane, campuri de input
    medium = RoundedCornerShape(16.dp),       // carduri de dispositiv si scena
    large = RoundedCornerShape(24.dp),        // panouri si foi de jos (bottom sheets)
    extraLarge = RoundedCornerShape(32.dp)    // elemente proeminente, dialoguri
)
