package pe.rutausegura.app.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import pe.rutausegura.app.data.AlertItem
import pe.rutausegura.app.data.LocationHelper
import pe.rutausegura.app.data.Profile
import pe.rutausegura.app.data.Repository
import pe.rutausegura.app.data.TripState

enum class Screen { Welcome, Login, Register, StudentHome, StartTrip, ActiveTrip, ContactLink, ContactDash }

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository(app)

    var profile by mutableStateOf(repo.savedProfile())
        private set
    var contactCode by mutableStateOf(repo.savedContactCode())
        private set
    var screen by mutableStateOf(
        when {
            repo.savedProfile() != null -> Screen.StudentHome
            repo.savedContactCode() != null -> Screen.ContactDash
            else -> Screen.Welcome
        }
    )
        private set
    var message by mutableStateOf<String?>(null)
        private set
    var messageOk by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var plate by mutableStateOf("")
        private set
    var tripActive by mutableStateOf(false)
        private set
    var myLat by mutableStateOf<Double?>(null)
        private set
    var myLng by mutableStateOf<Double?>(null)
        private set

    private var tripJob: Job? = null

    fun go(s: Screen) { message = null; screen = s }
    fun show(msg: String?, ok: Boolean = false) { message = msg; messageOk = ok }

    fun back() {
        when (screen) {
            Screen.Register, Screen.ContactLink, Screen.Login -> go(Screen.Welcome)
            Screen.StartTrip -> go(Screen.StudentHome)
            else -> Unit
        }
    }

    // ---------- Registro (HU-01) ----------
    fun register(p: Profile) {
        viewModelScope.launch {
            busy = true
            repo.register(p)
                .onSuccess { profile = p; go(Screen.StudentHome) }
                .onFailure { show(it.message) }
            busy = false
        }
    }

    fun loginStudent(rawCode: String) {
        val code = rawCode.trim().uppercase()
        if (code.isBlank()) { show("Escribe tu código universitario."); return }
        viewModelScope.launch {
            busy = true
            repo.loginStudent(code)
                .onSuccess { profile = it; go(Screen.StudentHome) }
                .onFailure { show(it.message) }
            busy = false
        }
    }

    fun logout() {
        endTrip()
        repo.logout()
        profile = null
        contactCode = null
        go(Screen.Welcome)
    }

    // ---------- SOS (HU-02) ----------
    fun sendSos() {
        val p = profile ?: run { show("Primero debes registrarte."); return }
        if (busy) return
        viewModelScope.launch {
            busy = true
            val ctx = getApplication<Application>()
            val loc = if (LocationHelper.hasPermission(ctx)) {
                try { LocationHelper.current(ctx) } catch (e: Exception) { null }
            } else null
            if (loc == null) {
                show("No se pudo obtener la ubicación. Activa el permiso e intenta de nuevo.")
            } else {
                repo.sendAlert(p, loc.latitude, loc.longitude)
                    .onSuccess { show("Alerta enviada a ${p.contactName}. Código: $it", true) }
                    .onFailure { show(it.message) }
            }
            busy = false
        }
    }

    // ---------- Viaje acompañado (MVP 2) ----------
    fun startTrip(rawPlate: String) {
        val p = profile ?: return
        if (rawPlate.isBlank()) { show("Escribe la placa del vehículo."); return }
        plate = rawPlate.trim().uppercase()
        tripActive = true
        myLat = null; myLng = null
        go(Screen.ActiveTrip)
        tripJob?.cancel()
        tripJob = viewModelScope.launch {
            try {
                LocationHelper.updates(getApplication()).collect { loc ->
                    myLat = loc.latitude; myLng = loc.longitude
                    repo.updateTrip(p, plate, loc.latitude, loc.longitude, true)
                }
            } catch (e: Exception) {
                show("Se perdió la ubicación. Revisa el GPS.")
            }
        }
    }

    fun endTrip() {
        tripJob?.cancel()
        if (tripActive) profile?.let { repo.updateTrip(it, plate, myLat ?: 0.0, myLng ?: 0.0, false) }
        tripActive = false
        myLat = null; myLng = null
        if (screen == Screen.ActiveTrip) go(Screen.StudentHome)
    }

    // ---------- Contacto de confianza (HU-03) ----------
    fun linkContact(rawCode: String) {
        val code = rawCode.trim().uppercase()
        if (code.isBlank()) { show("Escribe el código universitario del estudiante."); return }
        viewModelScope.launch {
            busy = true
            repo.studentExists(code)
                .onSuccess { existe ->
                    if (existe) { repo.saveContactCode(code); contactCode = code; go(Screen.ContactDash) }
                    else show("No encontramos ese código universitario.")
                }
                .onFailure { show(it.message) }
            busy = false
        }
    }

    fun tripFlow(code: String): Flow<TripState?> = repo.tripFlow(code)
    fun alertsFlow(code: String): Flow<List<AlertItem>> = repo.alertsFlow(code)
    fun setAlertStatus(id: String, status: String) = repo.setAlertStatus(id, status)
}
