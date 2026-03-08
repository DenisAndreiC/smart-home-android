/**
 * SceneRepository.kt - Repository pentru gestionarea scenelor de automatizare
 *
 * Intermediaza operatiile CRUD pe scene si executia lor catre ApiService.
 * O scena reprezinta o secventa de comenzi pe mai multe dispozitive, executata
 * de server ca o unitate atomica cu suport pentru intarzieri (delay_seconds).
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.data.repository

import com.denis.smarthome.data.api.ApiService
import com.denis.smarthome.data.model.SceneRequest
import com.denis.smarthome.data.model.SceneResponse

/**
 * Repository care expune operatiile pe scene catre stratul ViewModel.
 *
 * Pattern-ul `runCatching { }`: fiecare functie wrapeaza apelul API astfel incat
 * orice exceptie de retea sau HTTP sa fie capturata si returnata ca Result.failure,
 * fara a propaga exceptia si a crasha aplicatia. ViewModel-ul trateaza erorile
 * prin .onFailure { } si le afiseaza utilizatorului ca mesaje Snackbar.
 *
 * @param apiService interfata Retrofit configurata in RetrofitClient
 */
class SceneRepository(private val apiService: ApiService) {

    /**
     * Obtine lista tuturor scenelor utilizatorului curent.
     *
     * Apeleaza GET /scenes — endpoint protejat cu JWT.
     *
     * @return Result.success(lista) cu scenele si actiunile lor,
     *         Result.failure(exceptie) la eroare de retea sau autentificare
     */
    suspend fun getScenes(): Result<List<SceneResponse>> = runCatching {
        apiService.getScenes()
    }

    /**
     * Creeaza o scena noua cu o lista de actiuni.
     *
     * Apeleaza POST /scenes cu datele scenei ca JSON body.
     * Actiunile sunt executate de server in ordinea din lista, cu delay_seconds intre ele.
     *
     * @param request datele scenei noi (name, icon optional, lista de actiuni)
     * @return Result.success(SceneResponse) cu scena creata si id-ul sau,
     *         Result.failure(exceptie) la eroare de validare sau retea
     */
    suspend fun createScene(request: SceneRequest): Result<SceneResponse> = runCatching {
        apiService.createScene(request)
    }

    /**
     * Executa o scena existenta — declanseaza trimiterea tuturor comenzilor din scena.
     *
     * Apeleaza POST /scenes/{id}/execute.
     * Executia este asincron pe server: backend-ul itereaza actiunile si le trimite
     * secvential, respectand delay_seconds definit pentru fiecare actiune.
     * Response-ul de succes vine imediat (nu asteapta finalizarea tuturor comenzilor).
     *
     * @param id identificatorul scenei de executat
     * @return Result.success(Map) cu mesajul de confirmare de la server,
     *         Result.failure(exceptie) daca scena nu exista (404) sau eroare de retea
     */
    suspend fun executeScene(id: Int): Result<Map<String, String>> = runCatching {
        apiService.executeScene(id)
    }

    /**
     * Sterge o scena din sistem.
     *
     * Apeleaza DELETE /scenes/{id}.
     * Returneaza Result<Unit> deoarece DELETE nu are response body.
     *
     * @param id identificatorul unic al scenei de sters
     * @return Result.success(Unit) la stergere reusita,
     *         Result.failure(exceptie) daca scena nu exista sau eroare de retea
     */
    suspend fun deleteScene(id: Int): Result<Unit> = runCatching {
        apiService.deleteScene(id)
    }
}
