# RutaU Segura – App Android (MVP 1 y MVP 2)

Kotlin + Jetpack Compose + Firebase Firestore + mapa OpenStreetMap.
Paquete: `pe.rutausegura.app`

## 1. Instalar lo necesario (solo una vez)
1. Descarga **Android Studio** (versión estable más reciente) desde https://developer.android.com/studio e instálalo con las opciones por defecto.
   Ya incluye Java (JDK) y el SDK de Android: no necesitas instalar nada más.
   Requiere unos 8 GB de RAM y varios GB libres de disco.
2. Abre Android Studio y deja que termine de descargar los componentes iniciales.
3. VS Code puedes seguir usándolo para el informe; para la app se usa Android Studio.

## 2. Crear el proyecto en Firebase (gratis)
1. Entra a https://console.firebase.google.com con una cuenta Google y pulsa **Crear proyecto** (nombre: RutaUSegura; puedes desactivar Google Analytics).
2. Dentro del proyecto: **Agregar app → Android**.
   - Nombre del paquete: `pe.rutausegura.app`
   - Descarga el archivo **google-services.json**.
3. Copia `google-services.json` dentro de la carpeta `app/` de este proyecto (junto a `build.gradle.kts`).
4. En el menú **Compilación → Firestore Database → Crear base de datos**, elige modo de prueba (o pega estas reglas, válidas solo para la demo):
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}
```

## 3. Abrir y ejecutar
1. Android Studio → **Open** → selecciona la carpeta `RutaUSegura`. Espera a que termine "Gradle sync" (la primera vez tarda varios minutos y necesita internet).
2. **Device Manager → Create device** → Pixel 6, imagen del sistema **API 34** (descargarla). Crea dos emuladores o usa un celular real con "Depuración USB" para el segundo rol.
3. En el emulador: ⋮ (Extended controls) → **Location** para fijar una ubicación (por ejemplo, Chiclayo: -6.7714, -79.8409) y pulsar *Set location*.
4. Pulsa **Run ▶**.

## 4. Guion de demo
1. Dispositivo A: **Soy estudiante** → registro en 2 pasos → pantalla SOS.
2. Dispositivo B: **Soy contacto de confianza** → escribir el código universitario del estudiante → panel.
3. En A: mantener SOS 2 s (o "confirmar con un toque") → en B llega la notificación y la alerta aparece como ENVIADA. En B: *Marcar como vista* / *Marcar atendida*.
4. En A: **Iniciar viaje acompañado** → placa → B ve el mapa en vivo y la placa.
5. En A: botón **Llamar a la Policía (105)** abre el marcador del teléfono.

## 5. Qué trae cada MVP
- **MVP 1:** registro en 2 pasos con validaciones (campos obligatorios, celular de 9 dígitos, código duplicado), texto de privacidad, SOS con pulsación de 2 s y anillo, alternativa de confirmación con un toque (M-11), validación de rango de ubicación, mensajes claros ("Alerta enviada…", errores de permiso y conexión).
- **MVP 2:** envío real de la alerta al contacto, estados ENVIADA → VISTA → ATENDIDA, viaje acompañado con placa y ubicación en tiempo real, vista del contacto con mapa y notificación.

## 6. Limitaciones conocidas (para la demo)
- El aviso al contacto funciona con la app abierta (notificación local al recibir un cambio en Firestore). Notificaciones push con la app cerrada requieren Cloud Functions (plan de pago).
- El seguimiento del viaje funciona con la app en primer plano.
- Reglas de Firestore abiertas y sin inicio de sesión: solo para demo.
- El respaldo por SMS sin conexión y el MVP 3 (IA) quedan pendientes.
- Ícono de la app: temporal (del sistema). Puedes cambiarlo con *New → Image Asset*.
