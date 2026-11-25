package pe.rutausegura.app.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pe.rutausegura.app.data.Profile

// ---------------------------------------------------------------- Bienvenida
@Composable
fun WelcomeScreen(vm: AppViewModel) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Header("RutaU Segura", "Movilidad segura para universitarios de Chiclayo")
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("¿Qué quieres hacer?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Azul)
            RoleCard("Crear mi cuenta", "Regístrate como estudiante y elige a tu contacto de confianza.", Rojo) {
                vm.go(Screen.Register)
            }
            RoleCard("Soy contacto de confianza", "Recibe alertas y mira en el mapa dónde está el estudiante.", Azul) {
                vm.go(Screen.ContactLink)
            }
            Text(
                "¿Ya tienes cuenta?",
                fontSize = 16.sp,
                color = Color(0xFF555555),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
            BigButton("Ingresar con mi código", onClick = { vm.go(Screen.Login) }, color = Azul, outlined = true)
        }
    }
}

@Composable
private fun RoleCard(title: String, text: String, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.width(10.dp).height(110.dp).background(color))
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
                Text(text, fontSize = 16.sp, color = Color(0xFF444444))
            }
        }
    }
}

// ------------------------------------------------------------ Ingresar (ya registrado)
@Composable
fun LoginScreen(vm: AppViewModel) {
    var code by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize()) {
        Header("Ingresar", "Entra con tu código universitario")
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Si ya te registraste antes, no necesitas hacerlo otra vez.", fontSize = 16.sp, color = Color(0xFF444444))
            Field(code, { code = it.uppercase(); vm.show(null) }, "Código universitario", max = 12)
            MessageBox(vm.message, vm.messageOk)
            BigButton(if (vm.busy) "Ingresando…" else "Ingresar", enabled = !vm.busy, onClick = { vm.loginStudent(code) })
            BigButton("Quiero registrarme", onClick = { vm.go(Screen.Register) }, outlined = true)
        }
    }
}

// ------------------------------------------------------------------ Registro
@Composable
fun RegisterScreen(vm: AppViewModel) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var name by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var cName by rememberSaveable { mutableStateOf("") }
    var cPhone by rememberSaveable { mutableStateOf("") }
    var cRel by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = step == 2) { step = 1; error = null }

    fun phoneOk(s: String) = s.length == 9 && s.all { it.isDigit() }

    Column(Modifier.fillMaxSize()) {
        Header(
            "Crea tu perfil",
            if (step == 1) "Paso 1 de 2 · Tus datos" else "Paso 2 de 2 · Tu contacto de confianza"
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LinearProgressIndicator(
                progress = { step / 2f },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = Verde
            )
            if (step == 1) {
                Field(name, { name = it; error = null }, "Nombres y apellidos")
                Field(code, { code = it; error = null }, "Código universitario", max = 12)
                Field(phone, { phone = it.filter(Char::isDigit); error = null }, "Tu celular (9 dígitos)", KeyboardType.Phone, 9)
                MessageBox(error ?: vm.message, false)
                BigButton("Continuar", onClick = {
                    error = when {
                        name.isBlank() || code.isBlank() || phone.isBlank() -> "Completa todos los campos obligatorios."
                        !phoneOk(phone) -> "El celular debe tener 9 dígitos."
                        else -> null
                    }
                    if (error == null) step = 2
                })
            } else {
                Text(
                    "Esta persona recibirá tus alertas y podrá ver tu ubicación cuando tú lo indiques. Puede ser un familiar, pareja o amigo.",
                    fontSize = 16.sp, color = Color(0xFF444444)
                )
                Field(cName, { cName = it; error = null }, "Nombre del contacto")
                Field(cPhone, { cPhone = it.filter(Char::isDigit); error = null }, "Celular del contacto (9 dígitos)", KeyboardType.Phone, 9)
                Field(cRel, { cRel = it; error = null }, "Parentesco (madre, hermano, amigo…)", max = 30)
                Text(
                    "Privacidad: guardamos tus datos y tu ubicación solo para avisar a tu contacto de confianza. No se comparten con nadie más.",
                    fontSize = 14.sp, color = Color(0xFF555555)
                )
                MessageBox(error ?: vm.message, false)
                BigButton(
                    if (vm.busy) "Registrando…" else "Registrarme",
                    enabled = !vm.busy,
                    onClick = {
                        error = when {
                            cName.isBlank() || cPhone.isBlank() || cRel.isBlank() -> "Completa todos los campos obligatorios."
                            !phoneOk(cPhone) -> "El celular del contacto debe tener 9 dígitos."
                            else -> null
                        }
                        if (error == null) {
                            vm.register(
                                Profile(
                                    name.trim(), code.trim().uppercase(), phone, cName.trim(), cPhone, cRel.trim()
                                )
                            )
                        }
                    }
                )
            }
        }
    }
}

// ------------------------------------------------------------- Inicio (SOS)
@Composable
fun StudentHomeScreen(vm: AppViewModel) {
    val p = vm.profile ?: return
    val ctx = LocalContext.current
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    val gate = rememberLocationGate(
        onGranted = { vm.sendSos() },
        onDenied = { vm.show("No se pudo obtener la ubicación. Activa el permiso e intenta de nuevo.") }
    )

    Column(Modifier.fillMaxSize()) {
        Header("Hola, ${p.name.substringBefore(' ')}", "Tu contacto de confianza: ${p.contactName} (${p.contactRelation})")
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                if (vm.busy) "Obteniendo ubicación…" else "Mantén presionado el botón 2 segundos si estás en peligro",
                fontSize = 18.sp, fontWeight = FontWeight.Medium, color = Azul
            )
            SosButton(enabled = !vm.busy, onTriggered = { gate() })
            MessageBox(vm.message, vm.messageOk)
            TextButton(onClick = { showConfirm = true }) {
                Text("No puedo mantener presionado: confirmar con un toque", fontSize = 15.sp, color = Azul)
            }
            BigButton("Iniciar viaje acompañado", onClick = { vm.go(Screen.StartTrip) }, color = Azul)
            BigButton("Llamar a la Policía (105)", onClick = {
                try { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:105"))) } catch (e: Exception) {}
            }, color = Rojo, outlined = true)
            TextButton(onClick = { vm.logout() }) { Text("Cerrar sesión / registrar otra persona", fontSize = 14.sp) }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text("¿Enviar alerta SOS?") },
            text = { Text("Se avisará a ${p.contactName} con tu ubicación actual.") },
            confirmButton = {
                TextButton(onClick = { showConfirm = false; gate() }) { Text("Enviar alerta", color = Rojo, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showConfirm = false }) { Text("Cancelar") } }
        )
    }
}

// ----------------------------------------------------------- Viaje acompañado
@Composable
fun StartTripScreen(vm: AppViewModel) {
    val p = vm.profile ?: return
    var plate by rememberSaveable { mutableStateOf("") }
    val gate = rememberLocationGate(
        onGranted = { vm.startTrip(plate) },
        onDenied = { vm.show("Para acompañarte necesitamos el permiso de ubicación.") }
    )
    Column(Modifier.fillMaxSize()) {
        Header("Viaje acompañado", "${p.contactName} verá tu recorrido en tiempo real")
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                "Escribe la placa del vehículo en el que vas a viajar. Se enviará automáticamente a tu contacto de confianza.",
                fontSize = 16.sp, color = Color(0xFF444444)
            )
            Field(plate, { plate = it.uppercase(); vm.show(null) }, "Placa del vehículo", max = 8)
            MessageBox(vm.message, vm.messageOk)
            BigButton("Iniciar viaje", onClick = { gate() }, color = Verde)
            BigButton("Volver", onClick = { vm.back() }, outlined = true)
        }
    }
}

@Composable
fun ActiveTripScreen(vm: AppViewModel) {
    val p = vm.profile ?: return
    val gate = rememberLocationGate(
        onGranted = { vm.sendSos() },
        onDenied = { vm.show("No se pudo obtener la ubicación. Activa el permiso e intenta de nuevo.") }
    )
    Column(Modifier.fillMaxSize()) {
        Header("Viaje en curso", "Compartiendo tu ubicación con ${p.contactName}")
        Column(Modifier.weight(1f).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LiveMap(vm.myLat, vm.myLng, "Tú", Modifier.weight(1f).fillMaxWidth())
            if (vm.myLat == null) Text("Obteniendo tu ubicación…", fontSize = 15.sp, color = Color(0xFF555555))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Row(
                    Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SosButton(enabled = !vm.busy, onTriggered = { gate() }, size = 120.dp)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Placa: ${vm.plate}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Azul)
                        Text("Mantén SOS 2 segundos si necesitas ayuda", fontSize = 14.sp, color = Color(0xFF555555))
                    }
                }
            }
            MessageBox(vm.message, vm.messageOk)
            BigButton("Finalizar viaje", onClick = { vm.endTrip() }, color = Verde)
        }
    }
}
