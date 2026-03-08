package com.denis.smarthome.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Singleton care retine starea temei aplicatiei ca Compose state.
 * Orice composable care citeste [isDark] se recompune automat la schimbare.
 */
object ThemeState {
    var isDark by mutableStateOf(true)
}
