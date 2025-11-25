# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**BioWay Android** is a recycling management platform built with Kotlin and Jetpack Compose, migrated from Flutter. It connects citizens (Brindadores), collectors (Recolectores), collection centers (Centros de Acopio), and administrators (Maestros) in a circular economy ecosystem focused on recyclable materials in Mexico.

- **Package:** `com.biowaymexico`
- **Architecture:** Single Activity + Jetpack Compose
- **Min SDK:** 31 (Android 12)
- **Target SDK:** 36
- **Current State:** UI-complete, backend integration pending

## Build Commands

**Prerequisites:**
- JDK 11 required
- Android Gradle Plugin 8.12.3
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
│   │   ├── BioWayButtons.kt
│   │   ├── BioWayTextFields.kt
│   │   ├── BioWayCards.kt
│   │   └── BioWayBottomNavigationBar.kt
│   └── screens/                 # Feature screens by module
│       ├── splash/
│       ├── auth/                # Login, Register, PlatformSelector
│       ├── brindador/           # Citizen module (6 screens including ML)
│       ├── recolector/          # Collector module (4 screens)
│       ├── centro_acopio/       # Collection center (1 screen)
│       └── maestro/             # Admin module (1 screen)
└── utils/
    ├── ClasificadorResiduos.kt  # Legacy waste classifier
    └── WasteClassifierYOLO.kt   # YOLOv8 ML classifier
```

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
   - Modules: Empresas, Usuarios, Materiales, Horarios, Disponibilidad, Configuración (UI ready, detailed screens pending)

## Design System

### Colors

All colors are centralized in `BioWayColors` object (`ui/theme/Color.kt`):
- **Primary:** `PrimaryGreen` (#70D997)
- **Dark:** `DarkGreen` (#3DB388)
- **Accent:** `Turquoise` (#3FD9FF)
- **ECOCE:** Separate green palette for alternative platform

Always use `BioWayColors.PrimaryGreen` instead of hardcoded colors.

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
- `NFC` - For NFC communication (Usuario Normal and Celular en Bote)
- `VIBRATE` - For haptic feedback on NFC events

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
- `BioWayHceService.kt` - HCE service that emulates NFC card
- `UsuarioNormalNFCScreen.kt` - Emitter UI (generates random 8-digit ID)
- `CelularEnBoteNFCScreen.kt` - Reader UI (detects and reads IDs)
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

See `NFC_COMUNICACION_HCE.md` for detailed debugging instructions.

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

### Mock Login / Design Mode

The login screen includes quick access buttons for testing each module without authentication:
- "Acceso Brindador" → Citizen dashboard
- "Acceso Recolector" → Collector map
- "Acceso Centro" → Collection center
- "Acceso Admin" → Admin panel

### Migration Context

This project was migrated from Flutter (biowayreferencia/bioway.app-main) to Kotlin/Compose. Key migration docs:
- `MIGRACION_RESUMEN.md` - Complete migration summary (15+ screens migrated, 40+ colors, 6+ gradients)
- `INSTRUCCIONES.md` - Setup and execution instructions
- `PROGRESO_MIGRACION.md` - Migration progress tracking

**Map evolution:** The project went through multiple map implementations - Leaflet WebView → OSMDroid. See `DIAGNOSTICO_MAPA.md`, `SOLUCION_MAPA.md`, `LIMPIEZA_OSMDROID.md` for debugging history.

**ML model evolution:** Original 43MB model (`modelo_residuos.tflite`) → optimized 3MB YOLOv8 (`best.tflite`). See `MODELO_DEBUG.md`, `INTEGRACION_YOLOV8.md`, `MODELOS_PREENTRENADOS.md` for details.

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
```

**To add new core dependencies:**
1. Add version to `gradle/libs.versions.toml` under `[versions]`
2. Add library definition under `[libraries]`
3. Reference in build.gradle.kts with `libs.library.name`

### Not Yet Implemented

- Firebase (Auth, Firestore, Storage) - Planned for Phase 2
- Retrofit/Ktor - No network layer yet
- Room - No local database yet
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

### Phase 2: Backend Integration

When implementing backend:
1. Add ViewModel layer with StateFlow
2. Create data models in `data/models/`
3. Implement repositories in `data/repositories/`
4. Add use cases in `domain/usecases/`
5. Configure Firebase (auth, firestore, storage)
6. Add dependency injection (Hilt recommended)

### MVVM Structure

```
com.biowaymexico/
├── data/
│   ├── models/           # Data classes
│   ├── repositories/     # Repository implementations
│   ├── remote/          # API/Firebase
│   └── local/           # Room database
├── domain/
│   ├── models/          # Domain models
│   ├── repositories/    # Repository interfaces
│   └── usecases/        # Business logic
└── ui/                  # Existing structure + ViewModels
```

## Important Notes

- **No backend yet:** All data is mock/placeholder
- **Maps:** OSMDroid with OpenStreetMap tiles (no API keys required)
- **ML Classification:** YOLOv8 model for waste classification (3MB, 12 categories)
- **Design mode:** Quick access buttons bypass authentication for UI development
- **Material3:** Use Material3 components, not Material2
- **Edge-to-edge:** UI uses edge-to-edge display (adjust for system bars)
- **Spanish language:** UI text is in Spanish (target audience: Mexico)
- **Gradle Configuration:** JDK 11 required, min SDK 31 (Android 12)
- **Platform Selector:** Currently disabled in navigation (line commented out)

### Logging Tags

Key logging tags used in the codebase (for filtering logcat):
- `WasteClassifierYOLO` - ML classifier initialization, inference, errors
- `ClasificadorScreenYOLO` - Camera setup, permission handling
- `RecolectorMapaScreen` - Map initialization and marker setup
- `BioWayNavHost` - Navigation events
- `BioWayHceService` - HCE service APDU commands and responses
- `UsuarioNormalNFC` - Usuario Normal screen (HCE emitter)
- `CelularEnBoteNFC` - Celular en Bote screen (IsoDep reader)

## Resources

- **Migration docs:** See `MIGRACION_RESUMEN.md` for complete migration details
- **Setup instructions:** See `INSTRUCCIONES.md` for build/run instructions
- **Jetpack Compose:** https://developer.android.com/jetpack/compose
- **Navigation Compose:** https://developer.android.com/jetpack/compose/navigation
- **Material3:** https://m3.material.io/
