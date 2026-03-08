/**
 * DashboardRepository.kt - Repository pentru datele afisate pe ecranul principal
 *
 * Furnizeaza statisticile agregate si feed-ul de activitate recenta pentru HomeScreen.
 * Datele sunt calculate si agregate de server la fiecare apel — nu exista cache local.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.data.repository

import com.denis.smarthome.data.api.ApiService
import com.denis.smarthome.data.model.ActivityResponse
import com.denis.smarthome.data.model.DashboardStats

/**
 * Repository care expune datele de dashboard catre stratul ViewModel.
 *
 * Pattern-ul `runCatching { }`: fiecare functie wrapeaza apelul API astfel incat
 * exceptiile de retea sau HTTP sa fie capturate si returnate ca Result.failure,
 * permitand ViewModel-ului sa afiseze stari de eroare in UI fara crash.
 *
 * @param apiService interfata Retrofit configurata in RetrofitClient
 */
class DashboardRepository(private val apiService: ApiService) {

    /**
     * Obtine statisticile agregate pentru dashboard-ul principal.
     *
     * Apeleaza GET /dashboard/stats — endpoint protejat cu JWT.
     * Server-ul calculeaza in timp real: total dispozitive, dispozitive active,
     * comenzi trimise azi si cel mai utilizat dispozitiv.
     *
     * @return Result.success(DashboardStats) cu metricile curente,
     *         Result.failure(exceptie) la eroare de retea sau autentificare
     */
    suspend fun getStats(): Result<DashboardStats> = runCatching {
        apiService.getDashboardStats()
    }

    /**
     * Obtine lista de activitati recente pentru feed-ul din HomeScreen.
     *
     * Apeleaza GET /dashboard/activity?limit={limit}.
     * Activitatile sunt returnate in ordine descrescatoare a timpului (cele mai noi primele).
     * Limita implicita de 10 este suficienta pentru feed-ul vizibil in UI fara scroll excesiv.
     *
     * @param limit numarul maxim de activitati de returnat (implicit 10)
     * @return Result.success(lista) cu activitatile recente,
     *         Result.failure(exceptie) la eroare de retea sau autentificare
     */
    suspend fun getActivity(limit: Int = 10): Result<List<ActivityResponse>> = runCatching {
        apiService.getDashboardActivity(limit)
    }
}
