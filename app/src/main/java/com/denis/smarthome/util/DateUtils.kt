/**
 * DateUtils.kt - Utilitare pentru parsarea timestamp-urilor primite de la backend
 *
 * Backend-ul returneaza timestamp-uri ISO 8601 cu offset UTC explicit
 * (ex: "2025-03-17T18:30:00+00:00"). Aceasta functie le converteste la
 * ora locala a telefonului, in loc sa afiseze bruta ora UTC.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val LOCAL_TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

/**
 * Converteste un timestamp ISO 8601 (cu offset sau "Z") in ora locala a dispozitivului
 * si il formateaza ca "yyyy-MM-dd HH:mm".
 *
 * Daca parsarea esueaza (format neasteptat), revine la trunchierea bruta ca fallback.
 */
fun formatIsoToLocalDateTime(iso: String): String {
    val instant = runCatching { Instant.parse(iso) }
        .recoverCatching { OffsetDateTime.parse(iso).toInstant() }
        .getOrNull() ?: return iso.take(16).replace("T", " ")

    return instant.atZone(ZoneId.systemDefault()).format(LOCAL_TIMESTAMP_FORMAT)
}
