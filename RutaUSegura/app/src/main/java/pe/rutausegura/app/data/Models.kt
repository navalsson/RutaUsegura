package pe.rutausegura.app.data

data class Profile(
    val name: String = "",
    val code: String = "",
    val phone: String = "",
    val contactName: String = "",
    val contactPhone: String = "",
    val contactRelation: String = ""
)

data class AlertItem(
    val id: String = "",
    val studentCode: String = "",
    val studentName: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val status: String = "ENVIADA", // ENVIADA -> VISTA -> ATENDIDA
    val createdAt: Long = 0L
)

data class TripState(
    val active: Boolean = false,
    val plate: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val updatedAt: Long = 0L,
    val studentName: String = ""
)
