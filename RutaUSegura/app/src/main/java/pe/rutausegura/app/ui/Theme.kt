package pe.rutausegura.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Rojo = Color(0xFFD62828)
val Azul = Color(0xFF1D3557)
val AzulClaro = Color(0xFF2B4C7E)
val Verde = Color(0xFF1B7F3B)
val Naranja = Color(0xFFB26A00)
val ErrorRojo = Color(0xFFB00020)
val Fondo = Color(0xFFF4F6FA)

@Composable
fun RutaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Azul, onPrimary = Color.White, secondary = Rojo,
            background = Fondo, surface = Color.White, error = ErrorRojo
        ),
        content = content
    )
}
