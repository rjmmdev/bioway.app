# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**BioWay Android** is a recycling management platform built with Kotlin and Jetpack Compose, migrated from Flutter. It connects citizens (Brindadores), collectors (Recolectores), collection centers (Centros de Acopio), and administrators (Maestros) in a circular economy ecosystem focused on recyclable materials in Mexico.

- **Package:** `com.biowaymexico`
- **Architecture:** Single Activity + Jetpack Compose
- **Min SDK:** 31 (Android 12)
- **Target SDK:** 36
- **Current State:** Firebase backend integrated, active development

## Build Commands

**Prerequisites:**
- JDK 11 required
- Android Gradle Plugin 8.13.1
- Kotlin 2.0.21
- Compose BOM 2024.09.00

### Development

```bash
# Build debug APK
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk

# Build release APK
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk

# Install on connected device/emulator
./gradlew installDebug

# Clean build
./gradlew clean

# Rebuild entire project
./gradlew clean build

# Gradle sync (when build.gradle changes)
./gradlew --refresh-dependencies

# Stop Gradle daemon (if having issues)
./gradlew --stop
```

### Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests com.biowaymexico.ExampleUnitTest
```

### Debugging

```bash
# Build with stacktrace for debugging
./gradlew assembleDebug --stacktrace

# Build with info logging
./gradlew assembleDebug --info

# View logcat for app (replace with your device)
adb logcat | grep -i bioway

# View specific tag logs (e.g., ML classifier)
adb logcat | grep WasteClassifierYOLO
```

### Firebase Cloud Functions

```bash
# Deploy Cloud Functions
cd functions && npm install && firebase deploy --only functions

# View function logs
firebase functions:log

# Run functions locally
cd functions && npm run serve
```

### Android Studio

- **Sync Gradle:** File > Sync Project with Gradle Files
- **Build:** Build > Make Project (⌘F9 / Ctrl+F9)
- **Run:** Run > Run 'app' (⌃R / Shift+F10)
- **Clean:** Build > Clean Project

## Architecture

### Single Activity Pattern

The app uses **Single Activity Architecture** with Jetpack Compose:
- One `MainActivity` serves as the entry point
- All screens are Composables
- Navigation handled by Navigation Compose

### Package Structure

```
com.biowaymexico/
├── MainActivity.kt              # Entry point
├── data/                        # Data layer (Firebase integration)
│   ├── models/                  # Data classes (BrindadorModel, BoteBioWayModel, SesionBoteModel)
│   ├── AuthRepository.kt        # Firebase Auth operations
│   ├── BrindadorRepository.kt   # Brindador CRUD
│   ├── ReciclajeRepository.kt   # Reciclaje transactions
│   ├── MaterialesRepository.kt  # Materials catalog
│   ├── BoteSesionRepository.kt  # BoteBioWay points/rewards system
│   └── FirestoreDebugger.kt     # Debug utilities
├── nfc/                         # NFC services
│   └── BioWayHceService.kt      # HCE service for NFC emulation
├── ui/
│   ├── navigation/              # Navigation system
│   │   ├── BioWayNavigation.kt  # Route definitions & UserType enum
│   │   └── BioWayNavHost.kt     # NavHost implementation
│   ├── theme/                   # Design system
│   │   ├── Color.kt             # BioWayColors object (40+ colors)
│   │   ├── Gradients.kt         # BioWayGradients object (6+ gradients)
│   │   ├── Theme.kt             # BioWayTheme composable
│   │   └── Type.kt              # Typography
│   ├── components/              # Reusable UI components
│   └── screens/                 # Feature screens by module
│       ├── splash/
│       ├── auth/                # Login, Register, PlatformSelector
│       ├── brindador/           # Citizen module (ML classifier, NFC, Nearby)
│       ├── bote_bioway/         # Smart bin module (NFC reader, classifier)
│       ├── recolector/          # Collector module (map, profile)
│       ├── centro_acopio/       # Collection center
│       └── maestro/             # Admin module (botes management)
└── utils/
    ├── WasteClassifierYOLO.kt   # YOLOv8 ML classifier
    ├── CalculadoraImpactoReciclaje.kt  # Environmental impact calculator
    └── BluetoothManager.kt      # Bluetooth/ESP32 communication
```

### Firebase Cloud Functions

```
functions/                       # Node.js Cloud Functions (Node 20)
├── index.js                     # Function definitions
├── package.json                 # Dependencies (firebase-admin, firebase-functions)
└── node_modules/                # Dependencies
```

**Current Functions:**
- `deleteUnverifiedAccounts` - Daily cleanup of unverified accounts after 10 days

### State Management

**Current:** Local composition state with `remember { mutableStateOf() }`
- No ViewModel layer implemented yet
- Callback-based event handling
- Suitable for UI-only implementation

**Ready for:** MVVM with ViewModels + StateFlow when backend integration begins

### Navigation System

Routes are defined using sealed classes in `BioWayNavigation.kt`:

```kotlin
sealed class BioWayDestinations(val route: String) {
    object Splash : BioWayDestinations("splash")
    object Login : BioWayDestinations("login")
    // ... more routes
}
```

User types determine which home screen to show:

```kotlin
enum class UserType {
    BRINDADOR,      // Citizen
    RECOLECTOR,     // Collector
    CENTRO_ACOPIO,  // Collection Center
    EMPRESA,        // Company
    MAESTRO,        // Administrator
    GUEST
}
```

Navigation flow: **Splash → PlatformSelector → Login → User-specific module**

### Module Organization

Each user type has its own module with a main screen container:

1. **Brindador (Citizen):** Uses `HorizontalPager` for 3 tabs in `BrindadorMainScreen.kt`
   - Dashboard (`BrindadorDashboardScreen.kt`) - BioCoins, schedules, tips, quick actions
   - Comercio Local (`BrindadorComercioLocalScreen.kt`) - marketplace, product listings
   - Perfil/Competencias (`BrindadorPerfilCompetenciasScreen.kt`) - profile, rankings, achievements
   - Additional screens: `ClasificadorScreenYOLO.kt` (ML classifier with camera)

2. **Recolector (Collector):** Uses `HorizontalPager` for 2 tabs in `RecolectorMainScreen.kt`
   - Mapa (`RecolectorMapaScreenSimple.kt`) - OSMDroid map with collection points
   - Perfil (`RecolectorPerfilScreen.kt`) - statistics, environmental impact metrics
   - Note: `RecolectorHistorialScreen.kt` exists but not currently in tab navigation

3. **Centro de Acopio:** Single dashboard in `CentroAcopioHomeScreen.kt`
   - Modules: Recepción, Inventario, Prepago, Reportes (UI cards ready, screens not fully implemented)

4. **Maestro (Admin):** Single dashboard in `MaestroHomeScreen.kt`
   - Modules: Empresas, Usuarios, Materiales, Horarios, Disponibilidad, Configuración
   - Botes management: `MaestroBotesScreen.kt`, `CrearBoteScreen.kt`, `MapaSelectorBoteScreen.kt`

5. **Bote BioWay (Smart Bin):** Uses tabs in `BoteBioWayMainScreen.kt`
   - NFC Reader (`CelularEnBoteNFCScreen.kt`) - Reads user IDs via HCE
   - Nearby Discovery (`CelularEnBoteNearbyScreen.kt`) - Discovers nearby users (1-10m)
   - Classifier (`ClasificadorBoteScreen.kt`) - ML waste classification for the bin

## Design System

### Typography (Estándar Visual 2024)

The app uses custom typography defined in `ui/theme/Type.kt`:

**Fonts:**
- **Titles/Headings:** Hammersmith One (`HammersmithOne`)
- **Body text/Labels:** Montserrat (`Montserrat` - Regular, Medium, SemiBold, Bold)

**Usage:**
```kotlin
// Titles (uses Hammersmith One automatically)
Text(text = "Title", style = MaterialTheme.typography.headlineLarge)

// Body text (uses Montserrat automatically)
Text(text = "Description", style = MaterialTheme.typography.bodyMedium)
```

**Font files:** `app/src/main/res/font/hammersmith_one.ttf`, `montserrat_*.ttf`

See `ESTANDAR_VISUAL_BIOWAY.md` for complete typography guidelines.

### Colors

All colors are centralized in `BioWayColors` object (`ui/theme/Color.kt`):

**Brand Colors (Estándar Visual 2024):**
- **Verde Principal:** `BrandGreen` (#75EE8A)
- **Verde Turquesa:** `BrandTurquoise` (#B3FCD4)
- **Azul:** `BrandBlue` (#00DFFF)
- **Verde Oscuro:** `BrandDarkGreen` (#007565) - For text on gradients/light backgrounds
- **Gradient:** Use all three main colors for brand gradient

**Legacy Colors:**
- **Primary:** `PrimaryGreen` (#70D997)
- **Dark:** `DarkGreen` (#3DB388)
- **Accent:** `Turquoise` (#3FD9FF)
- **ECOCE:** Separate green palette for alternative platform

Always use `BioWayColors.BrandGreen` for new features or `BioWayColors.PrimaryGreen` for existing components.

### Gradients

Defined in `BioWayGradients` object (`ui/theme/Gradients.kt`):
- `BackgroundGradient` - Main background gradient
- `MainGradient` - Primary button gradient
- `AquaGradient`, `WarmGradient`, etc.

Apply with: `background = BioWayGradients.MainGradient`

### Components

Use predefined BioWay components for consistency:

```kotlin
// Buttons
BioWayPrimaryButton(text = "Login", onClick = { })
BioWaySecondaryButton(text = "Cancel", onClick = { })

// Text fields
BioWayTextField(value = email, onValueChange = { }, label = "Email")
BioWayPasswordTextField(value = password, onValueChange = { })

// Cards
BioWayCard { /* content */ }
BioWayGradientCard(gradient = BioWayGradients.AquaGradient) { }
```

## Key Implementation Details

### Android Configuration

**Permissions required:**
- `CAMERA` - For ML waste classification
- `INTERNET` - For OSMDroid map tiles
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` - For map location (not yet implemented)
- `WRITE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE` - For OSMDroid tile cache
- `NFC` - For NFC communication (Usuario Normal and Celular en Bote NFC mode)
- `VIBRATE` - For haptic feedback on NFC/Nearby events
- `BLUETOOTH_ADVERTISE` / `BLUETOOTH_CONNECT` / `BLUETOOTH_SCAN` - For Nearby Connections (Android 12+)
- `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE` - For Nearby Connections WiFi mode

**Build configuration highlights:**
- Uses Compose BOM for version management
- `mlModelBinding = true` for TensorFlow Lite support
- `aaptOptions { noCompress("tflite", "lite") }` to prevent model compression
- NDK filters for TFLite: armeabi-v7a, arm64-v8a, x86, x86_64

### Machine Learning Integration (YOLOv8)

The app includes real-time waste classification using YOLOv8:
- **Model:** `best.tflite` (3MB, 12 waste categories)
- **Location:** `app/src/main/assets/models/best.tflite`
- **Labels:** `app/src/main/assets/labels/labels.txt`
- **Implementation:** `WasteClassifierYOLO.kt` (utils package) and `ClasificadorScreenYOLO.kt` (UI)
- **Legacy classifier:** `ClasificadorResiduos.kt` and old model `modelo_residuos.tflite` (43MB) - kept for reference
- **Input size:** 320x320 (not 640x640)
- **Thresholds:** Confidence 0.45f, IOU 0.50f
- **Performance:** 40-72ms CPU, 20-40ms GPU with TensorFlow Lite GPU delegate
- **Categories:** Plástico, Cartón, Papel, Vidrio, Metal, Orgánico, Basura, etc.

**Usage in code:**
```kotlin
val classifier = WasteClassifierYOLO(context)
classifier.initialize()
val result = classifier.classifyImage(bitmap)
result.detections.forEach { detection ->
    // detection.className, detection.confidence, detection.boundingBox
}
```

### NFC Communication (Host Card Emulation)

The app implements **phone-to-phone NFC communication** using HCE:

**Architecture:**
- **Usuario Normal (Emitter):** Uses `HostApduService` to emulate an NFC card
- **Celular en Bote (Reader):** Uses `IsoDep` reader mode with APDU commands
- **Protocol:** APDU-based communication (SELECT AID → GET_USER_ID)
- **AID:** F0010203040506 (BioWay custom Application ID)

**Files:**
- `nfc/BioWayHceService.kt` - HCE service that emulates NFC card
- `brindador/UsuarioNormalNFCScreen.kt` - Emitter UI (generates random 8-digit ID)
- `bote_bioway/CelularEnBoteNFCScreen.kt` - Reader UI (detects and reads IDs)
- `res/xml/apduservice.xml` - HCE service configuration

**Key features:**
- Real-time device detection (both devices must have screens active)
- Automatic lifecycle management (pause/resume detection)
- Optimized for maximum detection range (FLAG_READER_SKIP_NDEF_CHECK)
- Extensive logging for debugging (`BioWayHceService`, `UsuarioNormalNFC`, `CelularEnBoteNFC`)
- Visual feedback (animations) and haptic feedback (vibration)

**Usage:**
- Device 1: Open "Usuario Normal" → Shows generated ID
- Device 2: Open "Celular en Bote" → Listens for emitters
- Bring devices close (back-to-back, <5cm) → Automatic ID transfer

Use logcat tags `BioWayHceService`, `UsuarioNormalNFC`, `CelularEnBoteNFC` for debugging.

### NFC Reader Implementation in ClasificadorBoteScreen (Patrón Singleton)

La implementación NFC que **funciona correctamente** en `ClasificadorBoteScreen.kt` utiliza un **patrón Singleton** para manejar el NFC Reader Mode completamente fuera del ciclo de vida de Jetpack Compose. Esta arquitectura resuelve los problemas típicos de closures y recomposiciones.

**Problema resuelto:**
El NFC Reader Mode de Android requiere un callback estable que no cambie durante la vida de la Activity. Jetpack Compose crea nuevas instancias de lambdas en cada recomposición, lo que causa que el NFC deje de funcionar o tenga comportamiento errático.

**Solución: `BoteNfcReaderSingleton`**

```kotlin
// Ubicación: ClasificadorBoteScreen.kt (líneas 72-237)
object BoteNfcReaderSingleton {
    // Constantes APDU (protocolo de comunicación)
    private val SELECT_APDU = byteArrayOf(
        0x00, 0xA4, 0x04, 0x00,  // Header: SELECT AID
        0x07,                     // Longitud AID
        0xF0, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06,  // AID BioWay
        0x00                      // Le (expected response length)
    )

    private val GET_USER_ID_APDU = byteArrayOf(
        0x00, 0xCA, 0x00, 0x00, 0x00  // GET DATA command
    )

    // Callbacks dinámicos (actualizados desde Compose)
    var onUserIdDetected: ((String) -> Unit)? = null
    var onVibrate: (() -> Unit)? = null

    // Callback NFC estático (nunca cambia)
    private val readerCallback = NfcAdapter.ReaderCallback { tag ->
        // Procesar en hilo IO
        Thread {
            val userId = readUserIdFromTag(tag)
            if (userId != null) {
                // Notificar en hilo principal
                Handler(Looper.getMainLooper()).post {
                    onUserIdDetected?.invoke(userId)
                    onVibrate?.invoke()
                }
            }
        }.start()
    }
}
```

**Flujo de comunicación APDU:**

```
┌─────────────────────┐                    ┌─────────────────────┐
│   ClasificadorBote  │                    │   UsuarioNormal     │
│   (Reader Mode)     │                    │   (HCE Emitter)     │
└─────────────────────┘                    └─────────────────────┘
         │                                          │
         │  1. SELECT AID (F0010203040506)          │
         │─────────────────────────────────────────>│
         │                                          │
         │  2. Response: 90 00 (Success)            │
         │<─────────────────────────────────────────│
         │                                          │
         │  3. GET_USER_ID (00 CA 00 00 00)         │
         │─────────────────────────────────────────>│
         │                                          │
         │  4. Response: [UserID bytes] 90 00       │
         │<─────────────────────────────────────────│
```

**Integración con Compose (3 componentes clave):**

1. **LaunchedEffect inicial** - Configura callbacks y arranca el reader:
```kotlin
LaunchedEffect(Unit) {
    // Conectar callbacks dinámicos al singleton
    BoteNfcReaderSingleton.onUserIdDetected = { userId ->
        onNfcUserDetected(userId)  // Función local de Compose
    }
    BoteNfcReaderSingleton.onVibrate = {
        vibrator?.vibrate(VibrationEffect.createOneShot(200, DEFAULT_AMPLITUDE))
    }

    // Inicializar y arrancar
    BoteNfcReaderSingleton.initialize(context)
    activity?.let { BoteNfcReaderSingleton.startReaderMode(it) }
}
```

2. **Heartbeat cada 3 segundos** - Mantiene el reader mode activo:
```kotlin
LaunchedEffect(Unit) {
    while (true) {
        delay(3000)
        activity?.let {
            BoteNfcReaderSingleton.startReaderMode(it)  // Re-registrar
        }
    }
}
```

3. **LifecycleEventObserver** - Maneja pause/resume de Activity:
```kotlin
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        when (event) {
            ON_RESUME -> BoteNfcReaderSingleton.startReaderMode(activity)
            ON_STOP -> BoteNfcReaderSingleton.stopReaderMode(activity)
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
        BoteNfcReaderSingleton.stopReaderMode(activity)
        // IMPORTANTE: Limpiar callbacks para evitar memory leaks
        BoteNfcReaderSingleton.onUserIdDetected = null
        BoteNfcReaderSingleton.onVibrate = null
    }
}
```

**Flags de Reader Mode optimizados:**
```kotlin
nfcAdapter.enableReaderMode(
    activity,
    readerCallback,
    NfcAdapter.FLAG_READER_NFC_A or      // ISO 14443-3A
    NfcAdapter.FLAG_READER_NFC_B or      // ISO 14443-3B
    NfcAdapter.FLAG_READER_NFC_F or      // JIS 6319-4
    NfcAdapter.FLAG_READER_NFC_V or      // ISO 15693
    NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or  // ← Crítico: mejora rango
    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
    Bundle().apply {
        putInt(EXTRA_READER_PRESENCE_CHECK_DELAY, 5000)  // 5s timeout
    }
)
```

**Por qué funciona:**
1. El `readerCallback` es una propiedad del object, no una lambda - **nunca se recrea**
2. Los callbacks dinámicos (`onUserIdDetected`, `onVibrate`) se actualizan por referencia
3. El heartbeat garantiza que el reader mode siempre esté activo
4. El lifecycle observer maneja correctamente pause/resume sin perder estado

**Logging tags para debug:**
- `BoteNfcSingleton` - Operaciones del singleton (detección, APDU, errores)
- `ClasificadorBote` - Procesamiento de usuario detectado, puntos otorgados

### Google Nearby Connections (Proximity Communication)

The app also implements **Google Nearby Connections API** as an alternative with longer range:

**Architecture:**
- **Usuario Normal (Nearby - Advertiser):** Broadcasts User ID using Nearby API
- **Celular en Bote (Nearby - Discoverer):** Discovers and connects to nearby advertisers
- **Strategy:** P2P_CLUSTER (optimized for 1-10 meters proximity)
- **Protocols:** Automatically uses WiFi + BLE + ultrasonic

**Files:**
- `brindador/UsuarioNormalNearbyScreen.kt` - Advertiser UI (range: 1-10m)
- `bote_bioway/CelularEnBoteNearbyScreen.kt` - Discovery UI (range: 1-10m)

**Key features:**
- Automatic discovery and connection (1-3 seconds)
- **10-100x longer range than NFC** (1-10 meters vs 6-10cm)
- Automatic disconnection when devices move apart
- Multi-protocol (uses best available: WiFi Direct, BLE, or ultrasonic)
- No location permissions required (Android 12+ with Nearby 19.0.0+)
- Encrypted communication by default

**Comparison:**
- **NFC:** Ultra-secure, very short range (6-10cm), requires near-contact
- **Nearby:** Convenient, medium range (1-10m), automatic disconnect

### Map Integration

The app uses **OSMDroid** (OpenStreetMap) for maps:
- **Implementation:** `RecolectorMapaScreenSimple.kt`
- **Dependencies:** `org.osmdroid:osmdroid-android:6.1.18`
- **No API keys required** (uses OpenStreetMap tiles)
- Displays collection points in Mexico City area

### Platform Selector

BioWay supports two platforms:
1. **BioWay** - Main platform (implemented)
2. **ECOCE** - Partner platform (placeholder)

Platform selection happens in `PlatformSelectorScreen.kt` before login.

### Firestore Collections Structure

User data is separated by type in different collections:
- `Brindador/` - Citizens who recycle
- `Recolector/` - Collectors
- `CentroAcopio/` - Collection centers
- `Maestro/` - Administrators
- `BoteBioWay/` - Smart recycling bins

**Login flow:** Firebase Auth → AuthRepository searches collections → determines UserType → navigates to correct module.

### BioCoins Points System

The `BoteSesionRepository` manages the rewards system:

```kotlin
// Points per material type
val PUNTOS_POR_MATERIAL = mapOf(
    "plastico" to 10,
    "metal" to 12,
    "vidrio" to 8,
    "carton" to 5,
    "papel" to 3,
    "organico" to 2,
    "basura" to 1
)

// User levels based on BioCoins
// Bronce < 500 < Plata < 2000 < Oro < 5000 < Platino < 10000 < Diamante
```

### Mock Login / Design Mode

The login screen includes quick access buttons for testing each module without authentication:
- "Acceso Brindador" → Citizen dashboard
- "Acceso Recolector" → Collector map
- "Acceso Centro" → Collection center
- "Acceso Admin" → Admin panel

### Migration Context

This project was migrated from Flutter (biowayreferencia/bioway.app-main) to Kotlin/Compose.

**Map evolution:** Leaflet WebView → OSMDroid (final, no API keys required)

**ML model evolution:** Original 43MB model → optimized 3MB YOLOv8 (`best.tflite`, 12 categories)

## Adding New Features

### New Screen

1. Create screen file in appropriate module:
   ```kotlin
   // ui/screens/brindador/NuevaPantallaScreen.kt
   @Composable
   fun NuevaPantallaScreen(onNavigateBack: () -> Unit) {
       // Implementation
   }
   ```

2. Add route in `BioWayNavigation.kt`:
   ```kotlin
   object NuevaPantalla : BioWayDestinations("nueva_pantalla")
   ```

3. Add to NavHost in `BioWayNavHost.kt`:
   ```kotlin
   composable(BioWayDestinations.NuevaPantalla.route) {
       NuevaPantallaScreen(onNavigateBack = { navController.popBackStack() })
   }
   ```

### New Component

1. Add to appropriate file in `ui/components/` or create new file
2. Follow naming convention: `BioWay[ComponentName]`
3. Use BioWayColors and BioWayGradients for styling
4. Accept modifier parameter for flexibility

### New User Module

1. Create directory under `ui/screens/[module_name]/`
2. Create main screen with HorizontalPager or tabs
3. Add UserType to enum in `BioWayNavigation.kt`
4. Update `getUserHomeDestination()` function
5. Add navigation routes and composables

## Dependencies

The project uses **Version Catalog** (`gradle/libs.versions.toml`) for core dependencies and direct implementation for others.

### Core Dependencies (from libs.versions.toml)

```kotlin
// Managed by Compose BOM (2024.09.00)
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.ui)
implementation(libs.androidx.ui.graphics)
implementation(libs.androidx.material3)

// Core Android
implementation(libs.androidx.core.ktx)              // 1.10.1
implementation(libs.androidx.lifecycle.runtime.ktx)  // 2.6.1
implementation(libs.androidx.activity.compose)       // 1.8.0
```

### Direct Dependencies (in build.gradle.kts)

```kotlin
// Navigation
implementation("androidx.navigation:navigation-compose:2.7.7")

// Material Icons Extended
implementation("androidx.compose.material:material-icons-extended:1.6.4")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

// Machine Learning (YOLOv8)
implementation("org.tensorflow:tensorflow-lite:2.14.0")
implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
implementation("org.tensorflow:tensorflow-lite-gpu-api:2.14.0")

// Camera (for ML classifier)
implementation("androidx.camera:camera-core:1.3.1")
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")

// Maps - OSMDroid (OpenStreetMap)
implementation("org.osmdroid:osmdroid-android:6.1.18")
implementation("tech.utsmankece:osm-androd-compose:0.0.3")  // Note: typo in package name is intentional

// Accompanist
implementation("com.google.accompanist:accompanist-permissions:0.32.0")
implementation("com.google.accompanist:accompanist-pager:0.34.0")
implementation("com.google.accompanist:accompanist-pager-indicators:0.34.0")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Google Nearby Connections - Proximity communication (1-10m range)
implementation("com.google.android.gms:play-services-nearby:19.3.0")

// Firebase - Backend and authentication
implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
implementation("com.google.firebase:firebase-auth-ktx")
implementation("com.google.firebase:firebase-firestore-ktx")
implementation("com.google.firebase:firebase-storage-ktx")
implementation("com.google.firebase:firebase-analytics-ktx")

// Google Fonts
implementation("androidx.compose.ui:ui-text-google-fonts:1.6.8")
```

**To add new core dependencies:**
1. Add version to `gradle/libs.versions.toml` under `[versions]`
2. Add library definition under `[libraries]`
3. Reference in build.gradle.kts with `libs.library.name`

### Not Yet Implemented

- Room - No local database/offline cache yet
- Hilt/Koin - No dependency injection yet
- Coil/Glide - No image loading library yet

## Common Patterns

### Navigation with Arguments

```kotlin
// Define route with argument
object Detail : BioWayDestinations("detail/{id}") {
    fun createRoute(id: String) = "detail/$id"
}

// Navigate with argument
navController.navigate(BioWayDestinations.Detail.createRoute("123"))

// Receive argument
composable(
    route = BioWayDestinations.Detail.route,
    arguments = listOf(navArgument("id") { type = NavType.StringType })
) { backStackEntry ->
    val id = backStackEntry.arguments?.getString("id")
    DetailScreen(id = id)
}
```

### Bottom Navigation Pattern

The app uses custom bottom navigation in module screens:

```kotlin
BioWayBottomNavigationBar(
    items = listOf("Dashboard", "Historial", "Perfil"),
    selectedIndex = pagerState.currentPage,
    onItemSelected = { index ->
        scope.launch { pagerState.animateScrollToPage(index) }
    }
)
```

### HorizontalPager for Tabs

Multi-screen modules use HorizontalPager:

```kotlin
val pagerState = rememberPagerState(pageCount = { 3 })

HorizontalPager(state = pagerState) { page ->
    when (page) {
        0 -> Screen1()
        1 -> Screen2()
        2 -> Screen3()
    }
}
```

## Testing

### Current Status

- Unit tests: Minimal (example test only)
- UI tests: Framework configured, no tests written
- Test directory: `app/src/test/java/com/biowaymexico/`

### Testing Compose UI

```kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun myTest() {
    composeTestRule.setContent {
        BioWayTheme {
            MyScreen()
        }
    }
    composeTestRule.onNodeWithText("Login").assertExists()
}
```

## Troubleshooting

### Gradle Sync Issues

```bash
# Invalidate caches and restart Android Studio
# File > Invalidate Caches > Invalidate and Restart

# Or clean from terminal
./gradlew clean
rm -rf .gradle

# Force dependency refresh
./gradlew build --refresh-dependencies
```

### Compose Preview Issues

- Ensure `@Preview` functions are top-level or in PreviewParameterProvider
- Wrap previews in `BioWayTheme { }` to see correct colors
- Use `showBackground = true` parameter

### Navigation Issues

- Always use route constants from `BioWayDestinations`
- Ensure NavHost includes all routes
- Check for route typos (compile-time safety with sealed classes)

### Build Failures

```bash
# Common fixes for build issues
./gradlew clean
./gradlew --stop  # Stop Gradle daemon
./gradlew assembleDebug --stacktrace  # Debug with stack trace

# JDK version issues (requires JDK 11)
java -version  # Should show version 11
```

### ML Model Loading Issues

- Ensure `best.tflite` exists in `app/src/main/assets/models/`
- Ensure `labels.txt` exists in `app/src/main/assets/labels/`
- Check camera permissions are granted (`Manifest.permission.CAMERA`)
- Verify TensorFlow Lite dependencies are included in build.gradle.kts
- Model requires minimum 150MB free memory
- Model input size is 320x320, not 640x640
- GPU delegate may not work on all devices (falls back to CPU automatically)

### OSMDroid/Map Issues

- Ensure `osmdroid.tilesource` is configured in `osmdroid_config.xml`
- OSMDroid caches tiles in app storage (can grow large over time)
- No API keys required for OpenStreetMap tiles
- Map center defaults to Mexico City (19.4326, -99.1332)
- If map doesn't load, check network connectivity

## Future Architecture Plans

### Next Steps

1. Add ViewModel layer with StateFlow for complex screens
2. Add use cases in `domain/usecases/` for business logic
3. Add Room database for offline caching
4. Add dependency injection (Hilt recommended)
5. Implement remaining user modules (Recolector backend, Centro de Acopio)

### Target MVVM Structure

```
com.biowaymexico/
├── data/                # ✅ Exists (Firebase repositories)
│   ├── models/          # ✅ Data classes
│   ├── repositories/    # ✅ Firebase implementations
│   └── local/           # 🔲 Room database (planned)
├── domain/              # 🔲 Planned
│   ├── models/          # Domain models
│   └── usecases/        # Business logic
└── ui/                  # ✅ Exists + ViewModels (in progress)
```

## Important Notes

- **Firebase backend:** Firebase Auth, Firestore, Storage integrated (Project: `software-4e6b6`)
- **Maps:** OSMDroid with OpenStreetMap tiles (no API keys required)
- **ML Classification:** YOLOv8 model for waste classification (3MB, 12 categories)
- **Design mode:** Quick access buttons bypass authentication for UI development
- **Material3:** Use Material3 components, not Material2
- **Edge-to-edge:** UI uses edge-to-edge display (adjust for system bars)
- **Spanish language:** UI text is in Spanish (target audience: Mexico)
- **Gradle Configuration:** JDK 11 required, AGP 8.13.1, min SDK 31 (Android 12)
- **Firebase config:** Requires `app/google-services.json` (not committed to git)

### Logging Tags

Key logging tags used in the codebase (for filtering logcat):
- `WasteClassifierYOLO` - ML classifier initialization, inference, errors
- `ClasificadorScreenYOLO` - Camera setup, permission handling
- `RecolectorMapaScreen` - Map initialization and marker setup
- `BioWayNavHost` - Navigation events
- `BioWayHceService` - HCE service APDU commands and responses (NFC)
- `UsuarioNormalNFC` - Usuario Normal screen HCE emitter (NFC mode)
- `CelularEnBoteNFC` - Celular en Bote screen IsoDep reader (NFC mode)
- `UsuarioNormalNearby` - Usuario Normal screen advertiser (Nearby mode, 1-10m)
- `CelularEnBoteNearby` - Celular en Bote screen discoverer (Nearby mode, 1-10m)

## Resources

- **Jetpack Compose:** https://developer.android.com/jetpack/compose
- **Navigation Compose:** https://developer.android.com/jetpack/compose/navigation
- **Material3:** https://m3.material.io/
- **Firebase Android:** https://firebase.google.com/docs/android/setup
- **OSMDroid:** https://github.com/osmdroid/osmdroid
