/**
 * DeviceRepository.kt - Repository pentru gestionarea dispozitivelor si a comenzilor
 *
 * Intermediaza toate operatiile CRUD pe dispozitive si trimiterea comenzilor catre acestea.
 * Fiecare operatie returneaza Result<T> prin pattern-ul runCatching, permitand
 * ViewModel-ului sa gestioneze erorile de retea fara try-catch explicite.
 *
 * Proiect: SmartHome IoT - Licenta CSIE-ASE 2025
 * Autor: Denis Andrei C.
 */
package com.denis.smarthome.data.repository

import com.denis.smarthome.data.api.ApiService
import com.denis.smarthome.data.model.CommandRequest
import com.denis.smarthome.data.model.CommandResponse
import com.denis.smarthome.data.model.DeviceRequest
import com.denis.smarthome.data.model.DeviceResponse

/**
 * Repository care expune operatiile pe dispozitive si comenzi catre stratul ViewModel.
 *
 * Grupeaza in acelasi repository atat operatiile pe dispozitive cat si pe comenzi,
 * deoarece comenzile sunt strans legate de dispozitive (o comanda apartine unui dispozitiv).
 *
 * Pattern-ul `runCatching { }`: fiecare functie wrapeaza apelul API intr-un bloc
 * runCatching care captureaza orice exceptie (IOException pentru retea, HttpException
 * pentru erori HTTP 4xx/5xx) si o returneaza ca Result.failure, in loc sa propage
 * exceptia si sa creze crash.
 *
 * @param apiService interfata Retrofit configurata in RetrofitClient
 */
class DeviceRepository(private val apiService: ApiService) {

    /**
     * Obtine lista tuturor dispozitivelor utilizatorului curent.
     *
     * Apeleaza GET /devices — endpoint protejat cu JWT.
     *
     * @return Result.success(lista) cu toate dispozitivele,
     *         Result.failure(exceptie) la eroare de retea sau autentificare
     */
    suspend fun getDevices(): Result<List<DeviceResponse>> = runCatching {
        apiService.getDevices()
    }

    /**
     * Creeaza un dispozitiv nou in sistem.
     *
     * Apeleaza POST /devices cu datele dispozitivului ca JSON body.
     *
     * @param request datele noului dispozitiv (name, device_type, room, mqtt_topic etc.)
     * @return Result.success(DeviceResponse) cu dispozitivul creat si id-ul sau,
     *         Result.failure(exceptie) daca datele sunt invalide sau eroare de retea
     */
    suspend fun createDevice(request: DeviceRequest): Result<DeviceResponse> = runCatching {
        apiService.createDevice(request)
    }

    /**
     * Sterge un dispozitiv din sistem dupa id.
     *
     * Apeleaza DELETE /devices/{id}.
     * Returneaza Result<Unit> deoarece DELETE nu are response body (204 No Content).
     *
     * @param id identificatorul unic al dispozitivului de sters
     * @return Result.success(Unit) la stergere reusita,
     *         Result.failure(exceptie) daca dispozitivul nu exista (404) sau eroare de retea
     */
    suspend fun deleteDevice(id: Int): Result<Unit> = runCatching {
        apiService.deleteDevice(id)
    }

    /**
     * Obtine lista distincta a camerelor cu dispozitive.
     *
     * Apeleaza GET /devices/rooms.
     * Folosita in DevicesListScreen pentru generarea chip-urilor de filtrare.
     *
     * @return Result.success(lista) cu numele camerelor (ex: ["Living", "Dormitor"]),
     *         Result.failure(exceptie) la eroare de retea
     */
    suspend fun getRooms(): Result<List<String>> = runCatching {
        apiService.getRooms()
    }

    /**
     * Trimite o comanda catre un dispozitiv specificat.
     *
     * Apeleaza POST /commands/send cu comanda ca JSON body.
     * Backend-ul determina canalul de livrare (MQTT sau IR) pe baza tipului dispozitivului
     * si publica comanda pe infrastructura corespunzatoare.
     *
     * @param request comanda de trimis (device_id, command_type, command_data optional)
     * @return Result.success(CommandResponse) cu confirmarea primirii comenzii de catre server,
     *         Result.failure(exceptie) daca dispozitivul nu exista sau eroare de retea
     */
    suspend fun sendCommand(request: CommandRequest): Result<CommandResponse> = runCatching {
        apiService.sendCommand(request)
    }
}
