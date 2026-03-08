package com.denis.smarthome.ui.theme

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

// ── Culori fixe (identice in ambele teme) ─────────────────────────────────────
val Primary         = Color(0xFF00BCD4)
val Secondary       = Color(0xFF26C6DA)
val PrimaryContainer   = Color(0xFF004D57)
val SecondaryContainer = Color(0xFF004A52)
val OnPrimary       = Color(0xFF000000)
val ErrorColor      = Color(0xFFFF5252)

// ── Culori dark (valorile originale) ──────────────────────────────────────────
private val DarkBackground    = Color(0xFF0D1B2A)
private val DarkSurface       = Color(0xFF1B2838)
private val DarkSurfaceVariant= Color(0xFF1E3040)
private val DarkOnBackground  = Color(0xFFE0E0E0)
private val DarkOnSurface     = Color(0xFFB0BEC5)
private val DarkOutline       = Color(0xFF2A3F50)
private val DarkOutlineVariant= Color(0xFF1A2D3D)

// ── Culori light ───────────────────────────────────────────────────────────────
private val LightBackground    = Color(0xFFF5F5F5)
private val LightSurface       = Color(0xFFFFFFFF)
private val LightSurfaceVariant= Color(0xFFECECEC)
private val LightOnBackground  = Color(0xFF1B1B1B)
private val LightOnSurface     = Color(0xFF555555)
private val LightOutline       = Color(0xFFE0E0E0)
private val LightOutlineVariant= Color(0xFFCCCCCC)

// ── Valori reactive — se schimba instant cand ThemeState.isDark se schimba ───
val Background     by derivedStateOf { if (ThemeState.isDark) DarkBackground     else LightBackground }
val Surface        by derivedStateOf { if (ThemeState.isDark) DarkSurface        else LightSurface }
val SurfaceVariant by derivedStateOf { if (ThemeState.isDark) DarkSurfaceVariant else LightSurfaceVariant }
val OnBackground   by derivedStateOf { if (ThemeState.isDark) DarkOnBackground   else LightOnBackground }
val OnSurface      by derivedStateOf { if (ThemeState.isDark) DarkOnSurface      else LightOnSurface }
val Outline        by derivedStateOf { if (ThemeState.isDark) DarkOutline        else LightOutline }
val OutlineVariant by derivedStateOf { if (ThemeState.isDark) DarkOutlineVariant else LightOutlineVariant }
