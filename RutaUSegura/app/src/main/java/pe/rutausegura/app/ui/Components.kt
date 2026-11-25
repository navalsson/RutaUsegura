package pe.rutausegura.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import pe.rutausegura.app.data.LocationHelper

@Composable
fun Header(title: String, subtitle: String) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(Brush.verticalGradient(listOf(Azul, AzulClaro)))
            .padding(horizontal = 24.dp, vertical = 28.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = Color(0xFFDCE6F5), fontSize = 16.sp)
        }
    }
}

@Composable
fun MessageBox(message: String?, ok: Boolean) {
    if (message == null) return
    Surface(
        color = if (ok) Color(0xFFE6F4EA) else Color(0xFFFDECEE),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Text(
            message,
            color = if (ok) Verde else ErrorRojo,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(14.dp)
        )
    }
}

@Composable
fun BigButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Azul,
    enabled: Boolean = true,
    outlined: Boolean = false
) {
    val m = modifier.fillMaxWidth().heightIn(min = 56.dp)
    if (outlined) {
        OutlinedButton(
            onClick = onClick, enabled = enabled, modifier = m, shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
        ) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
    } else {
        Button(
            onClick = onClick, enabled = enabled, modifier = m, shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
        ) { Text(text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    keyboard: KeyboardType = KeyboardType.Text,
    max: Int = 60
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= max) onChange(it) },
        label = { Text(label, fontSize = 16.sp) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun StatusChip(status: String) {
    val color = when (status) {
        "ATENDIDA" -> Verde
        "VISTA" -> Naranja
        else -> Rojo
    }
    Surface(color = color, shape = RoundedCornerShape(50)) {
        Text(
            status,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

/** Botón SOS: hay que mantenerlo presionado 2 segundos; un anillo muestra el avance. */
@Composable
fun SosButton(enabled: Boolean, onTriggered: () -> Unit, size: Dp = 240.dp) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress.value },
            modifier = Modifier.fillMaxSize(),
            color = Verde,
            trackColor = Color(0xFFE3E3E3),
            strokeWidth = size / 20
        )
        Box(
            Modifier
                .size(size * 0.82f)
                .clip(CircleShape)
                .background(if (enabled) Rojo else Color.Gray)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures(onPress = {
                        val job = scope.launch {
                            progress.animateTo(1f, tween(2000, easing = LinearEasing))
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTriggered()
                            progress.snapTo(0f)
                        }
                        tryAwaitRelease()
                        job.cancel()
                        scope.launch { progress.snapTo(0f) }
                    })
                }
                .semantics {
                    contentDescription = "Botón SOS. Mantén presionado dos segundos para enviar una alerta"
                    role = Role.Button
                },
            contentAlignment = Alignment.Center
        ) {
            Text("SOS", color = Color.White, fontSize = (size.value * 0.2f).sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

/** Mapa OpenStreetMap con un marcador (no requiere clave de API). */
@Composable
fun LiveMap(lat: Double?, lng: Double?, label: String, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val mapView = remember {
        Configuration.getInstance().load(ctx, ctx.getSharedPreferences("osm", 0))
        Configuration.getInstance().userAgentValue = ctx.packageName
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            controller.setCenter(GeoPoint(-6.7714, -79.8409)) // Chiclayo
        }
    }
    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause(); mapView.onDetach() }
    }
    AndroidView(
        factory = { mapView },
        modifier = modifier.clip(RoundedCornerShape(18.dp)),
        update = { mv ->
            if (lat != null && lng != null) {
                val pt = GeoPoint(lat, lng)
                mv.overlays.clear()
                mv.overlays.add(Marker(mv).apply {
                    position = pt
                    title = label
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                })
                mv.controller.animateTo(pt)
                mv.invalidate()
            }
        }
    )
}

/** Pide el permiso de ubicación y llama a onGranted / onDenied. Devuelve la función a invocar. */
@Composable
fun rememberLocationGate(onGranted: () -> Unit, onDenied: () -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val granted by rememberUpdatedState(onGranted)
    val denied by rememberUpdatedState(onDenied)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r.values.any { it }) granted() else denied()
    }
    return remember {
        {
            if (LocationHelper.hasPermission(ctx)) granted()
            else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
}
