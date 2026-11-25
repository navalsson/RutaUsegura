package pe.rutausegura.app.data

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class CodigoDuplicado : Exception("Ese código universitario ya está registrado.")

class Repository(context: Context) {
    private val prefs = context.getSharedPreferences("rutau", Context.MODE_PRIVATE)
    private val db = FirebaseFirestore.getInstance()
    private val noConexion = "No se pudo conectar. Revisa tu internet e intenta de nuevo."

    // ---------- Sesión local ----------
    fun savedProfile(): Profile? {
        val code = prefs.getString("code", null) ?: return null
        return Profile(
            name = prefs.getString("name", "") ?: "",
            code = code,
            phone = prefs.getString("phone", "") ?: "",
            contactName = prefs.getString("cName", "") ?: "",
            contactPhone = prefs.getString("cPhone", "") ?: "",
            contactRelation = prefs.getString("cRel", "") ?: ""
        )
    }

    private fun saveLocal(p: Profile) {
        prefs.edit()
            .putString("name", p.name).putString("code", p.code).putString("phone", p.phone)
            .putString("cName", p.contactName).putString("cPhone", p.contactPhone)
            .putString("cRel", p.contactRelation).apply()
    }

    fun savedContactCode(): String? = prefs.getString("contactCode", null)
    fun saveContactCode(code: String) = prefs.edit().putString("contactCode", code).apply()
    fun logout() = prefs.edit().clear().apply()

    // ---------- MVP 1: registro y alerta ----------
    suspend fun register(p: Profile): Result<Unit> {
        return try {
            val ref = db.collection("students").document(p.code)
            val existe = withTimeoutOrNull(8000) { ref.get().await().exists() }
                ?: return Result.failure(Exception(noConexion))
            if (existe) throw CodigoDuplicado()
            val guardado = withTimeoutOrNull(8000) { ref.set(p).await(); true }
            if (guardado == null) return Result.failure(Exception(noConexion))
            saveLocal(p)
            Result.success(Unit)
        } catch (e: CodigoDuplicado) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("$noConexion\n(${e.message})"))
        }
    }

    /** Ingresar con el código universitario si ya estás registrado (sin volver a registrarte). */
    suspend fun loginStudent(code: String): Result<Profile> {
        return try {
            val doc = withTimeoutOrNull(8000) { db.collection("students").document(code).get().await() }
                ?: return Result.failure(Exception(noConexion))
            val p = doc.toObject(Profile::class.java)
            if (!doc.exists() || p == null) {
                Result.failure(Exception("No encontramos ese código. Regístrate primero."))
            } else {
                saveLocal(p)
                Result.success(p)
            }
        } catch (e: Exception) {
            Result.failure(Exception(noConexion))
        }
    }

    suspend fun sendAlert(p: Profile, lat: Double, lng: Double): Result<String> {
        if (lat.isNaN() || lng.isNaN() || lat !in -90.0..90.0 || lng !in -180.0..180.0) {
            return Result.failure(Exception("La ubicación no es válida. La alerta no fue enviada."))
        }
        return try {
            val ref = db.collection("alerts").document()
            val ok = withTimeoutOrNull(8000) {
                ref.set(
                    mapOf(
                        "studentCode" to p.code, "studentName" to p.name,
                        "lat" to lat, "lng" to lng,
                        "status" to "ENVIADA", "createdAt" to System.currentTimeMillis()
                    )
                ).await()
                true
            }
            if (ok == true) Result.success(ref.id.take(6).uppercase())
            else Result.failure(Exception("No hay conexión. La alerta no fue enviada."))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo enviar la alerta. Intenta de nuevo."))
        }
    }

    // ---------- MVP 2: viaje en tiempo real ----------
    fun updateTrip(p: Profile, plate: String, lat: Double, lng: Double, active: Boolean) {
        db.collection("trips").document(p.code).set(
            mapOf(
                "active" to active, "plate" to plate, "lat" to lat, "lng" to lng,
                "updatedAt" to System.currentTimeMillis(), "studentName" to p.name
            )
        )
    }

    suspend fun studentExists(code: String): Result<Boolean> = try {
        val r = withTimeoutOrNull(8000) { db.collection("students").document(code).get().await().exists() }
        if (r == null) Result.failure(Exception(noConexion)) else Result.success(r)
    } catch (e: Exception) {
        Result.failure(Exception(noConexion))
    }

    fun tripFlow(code: String): Flow<TripState?> = callbackFlow {
        val reg = db.collection("trips").document(code).addSnapshotListener { s, _ ->
            trySend(s?.toObject(TripState::class.java))
        }
        awaitClose { reg.remove() }
    }

    fun alertsFlow(code: String): Flow<List<AlertItem>> = callbackFlow {
        val reg = db.collection("alerts").whereEqualTo("studentCode", code)
            .addSnapshotListener { s, _ ->
                if (s != null) {
                    val list = s.documents.map { d ->
                        (d.toObject(AlertItem::class.java) ?: AlertItem()).copy(id = d.id)
                    }.sortedByDescending { it.createdAt }
                    trySend(list)
                }
            }
        awaitClose { reg.remove() }
    }

    fun setAlertStatus(id: String, status: String) {
        db.collection("alerts").document(id).update("status", status)
    }
}
