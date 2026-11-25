package pe.rutausegura.app.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rutausegura.app.data.AlertItem
import pe.rutausegura.app.data.Notifier
import pe.rutausegura.app.data.TripState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContactLinkScreen(vm: AppViewModel) {
    var code by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Header("Contacto de confianza", "Vincula al estudiante que vas a acompañar")
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "Pídele al estudiante su código universitario. Con él verás sus alertas y su viaje en tiempo real.",
                fontSize = 16.sp, color = Color(0xFF444444)
            )
            Field(code, { code = it.uppercase(); vm.show(null) }, "Código universitario del estudiante", max = 12)
            MessageBox(vm.message, vm.messageOk)
            BigButton(if (vm.busy) "Buscando…" else "Vincular", enabled = !vm.busy, onClick = { vm.linkContact(code) })
            BigButton("Volver", onClick = { vm.back() }, outlined = true)
        }
    }
}

@Composable
fun ContactDashScreen(vm: AppViewModel) {
    val code = vm.contactCode ?: return
    val ctx = LocalContext.current
    val trip: TripState? by remember(code) { vm.tripFlow(code) }.collectAsState(initial = null)
    val alerts: List<AlertItem>? by remember(code) { vm.alertsFlow(code) }.collectAsState(initial = null)

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    // Aviso local cuando llega una alerta nueva
    var known by remember { mutableStateOf<Set<String>?>(null) }
    LaunchedEffect(alerts) {
        val list = alerts ?: return@LaunchedEffect
        val k = known
        if (k != null) list.filter { it.id !in k }.forEach { Notifier.alert(ctx, it) }
        known = list.map { it.id }.toSet()
    }

    val t = trip
    val latest = alerts?.firstOrNull()
    val mapLat: Double?
    val mapLng: Double?
    val label: String
    when {
        t != null && t.active -> { mapLat = t.lat; mapLng = t.lng; label = "Estudiante en viaje" }
        latest != null -> { mapLat = latest.lat; mapLng = latest.lng; label = "Última alerta" }
        else -> { mapLat = null; mapLng = null; label = "" }
    }
    val studentName = t?.studentName?.ifBlank { null } ?: latest?.studentName ?: "Estudiante $code"

    Column(Modifier.fillMaxSize()) {
        Header(studentName, "Código $code · Eres su contacto de confianza")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LiveMap(mapLat, mapLng, label, Modifier.fillMaxWidth().height(230.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    if (t != null && t.active) {
                        Text("En viaje · placa ${t.plate}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Verde)
                        Text("Ubicación actualizada ${ago(t.updatedAt)}", fontSize = 14.sp, color = Color(0xFF555555))
                    } else {
                        Text("Sin viaje activo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Azul)
                        Text("Verás el mapa en vivo cuando inicie un viaje.", fontSize = 14.sp, color = Color(0xFF555555))
                    }
                }
            }
        }
        Text("Alertas recibidas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Azul, modifier = Modifier.padding(horizontal = 16.dp))
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val list = alerts.orEmpty()
            if (list.isEmpty()) {
                item { Text("Aún no hay alertas. Todo tranquilo.", fontSize = 16.sp, color = Color(0xFF555555)) }
            }
            items(list, key = { it.id }) { a ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Alerta SOS · ${hora(a.createdAt)}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            StatusChip(a.status)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (a.status == "ENVIADA") {
                                TextButton(onClick = { vm.setAlertStatus(a.id, "VISTA") }) { Text("Marcar como vista") }
                            }
                            if (a.status != "ATENDIDA") {
                                TextButton(onClick = { vm.setAlertStatus(a.id, "ATENDIDA") }) { Text("Marcar atendida", color = Verde) }
                            }
                        }
                    }
                }
            }
        }
        TextButton(onClick = { vm.logout() }, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text("Dejar de acompañar a este estudiante", fontSize = 14.sp)
        }
    }
}

private fun hora(ms: Long): String = SimpleDateFormat("dd/MM HH:mm", Locale("es", "PE")).format(Date(ms))

private fun ago(ms: Long): String {
    val s = ((System.currentTimeMillis() - ms) / 1000).coerceAtLeast(0)
    return if (s < 60) "hace $s s" else "hace ${s / 60} min"
}
