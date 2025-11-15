# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**BioWay Android** is a recycling management platform built with Kotlin and Jetpack Compose, migrated from Flutter. It connects citizens (Brindadores), collectors (Recolectores), collection centers (Centros de Acopio), and administrators (Maestros) in a circular economy ecosystem focused on recyclable materials in Mexico.

- **Package:** `com.biowaymexico`
- **Architecture:** Single Activity + Jetpack Compose
- **Min SDK:** 33 (Android 13)
- **Target SDK:** 36
- **Current State:** UI-complete, backend integration pending

## Build Commands

### Development

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device/emulator
./gradlew installDebug

# Clean build
./gradlew clean

# Rebuild entire project
./gradlew clean build
```

### Testing

```bash
# Run unit tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests com.biowaymexico.ExampleUnitTest
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
│   │   ├── Gradients.kt         # BioWayGradients object
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
│       ├── brindador/           # Citizen module (4 screens)
│       ├── recolector/          # Collector module (4 screens)
│       ├── centro_acopio/       # Collection center (1 screen)
│       └── maestro/             # Admin module (1 screen)
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

1. **Brindador (Citizen):** Uses `HorizontalPager` for 4 tabs
   - Dashboard, Comercio Local, Competencias, Perfil

2. **Recolector (Collector):** Uses `HorizontalPager` for 3 tabs
   - Mapa (Leaflet WebView), Historial, Perfil

3. **Centro de Acopio:** Single dashboard with modules
   - Recepción, Inventario, Prepago, Reportes

4. **Maestro (Admin):** Single dashboard with admin modules
   - Empresas, Usuarios, Materiales, Horarios, Zonas, Configuración

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

### Map Integration

The map functionality uses **Leaflet via WebView** (no external dependencies):
- Main: `RecolectorMapaScreenLeaflet.kt` (WebView + Leaflet.js)
- Alternative: `RecolectorMapaScreenSimple.kt` (placeholder)
- OSMDroid was removed to avoid dependency issues

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

This project was migrated from a Flutter codebase. Documentation files track the migration:
- `MIGRACION_RESUMEN.md` - Complete migration summary
- `INSTRUCCIONES.md` - Setup and execution instructions
- `PROGRESO_MIGRACION.md` - Migration progress tracking

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

### Core Dependencies

```kotlin
// Jetpack Compose (BOM managed)
implementation(platform("androidx.compose:compose-bom:2024.xx.xx"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended:1.6.4")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.7")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
```

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
```

### Compose Preview Issues

- Ensure `@Preview` functions are top-level or in PreviewParameterProvider
- Wrap previews in `BioWayTheme { }` to see correct colors
- Use `showBackground = true` parameter

### Navigation Issues

- Always use route constants from `BioWayDestinations`
- Ensure NavHost includes all routes
- Check for route typos (compile-time safety with sealed classes)

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
- **WebView maps:** Leaflet via WebView, not Google Maps (to avoid API keys during development)
- **Design mode:** Quick access buttons bypass authentication for UI development
- **Material3:** Use Material3 components, not Material2
- **Edge-to-edge:** UI uses edge-to-edge display (adjust for system bars)
- **Spanish language:** UI text is in Spanish (target audience: Mexico)

## Resources

- **Migration docs:** See `MIGRACION_RESUMEN.md` for complete migration details
- **Setup instructions:** See `INSTRUCCIONES.md` for build/run instructions
- **Jetpack Compose:** https://developer.android.com/jetpack/compose
- **Navigation Compose:** https://developer.android.com/jetpack/compose/navigation
- **Material3:** https://m3.material.io/
