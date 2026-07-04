/**
 * RoutineRepository.kt - Repository pentru gestionarea rutinelor automate
 *
 * Intermediaza operatiile CRUD pe rutine (manuale si sugerate de ML) catre ApiService.
 * O rutina executa o singura actiune pe un dispozitiv la o ora si zile specificate,
 * planificata de APScheduler pe backend.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.data.repository

import com.denis.smarthome.data.api.ApiService
import com.denis.smarthome.data.model.RoutineCreate
import com.denis.smarthome.data.model.RoutineDetectResponse
import com.denis.smarthome.data.model.RoutineResponse
import com.denis.smarthome.data.model.RoutineToggle

class RoutineRepository(private val apiService: ApiService) {

    /** Obtine lista tuturor rutinelor utilizatorului curent (manuale + ML). */
    suspend fun getRoutines(): Result<List<RoutineResponse>> = runCatching {
        apiService.getRoutines()
    }

    /** Creeaza o rutina manuala noua. */
    suspend fun createRoutine(request: RoutineCreate): Result<RoutineResponse> = runCatching {
        apiService.createRoutine(request)
    }

    /** Activeaza/dezactiveaza o rutina existenta. */
    suspend fun toggleRoutine(id: Int, isActive: Boolean): Result<RoutineResponse> = runCatching {
        apiService.toggleRoutine(id, RoutineToggle(isActive))
    }

    /** Sterge o rutina. */
    suspend fun deleteRoutine(id: Int): Result<Unit> = runCatching {
        apiService.deleteRoutine(id)
    }

    /** Ruleaza detectia ML (DBSCAN) pe istoricul de comenzi si salveaza tiparele noi ca rutine inactive. */
    suspend fun detectRoutines(): Result<RoutineDetectResponse> = runCatching {
        apiService.detectRoutines()
    }
}
