# 🎨 Estándar Visual de BioWay - 2024

## 📝 Tipografía

### Fuentes Oficiales

**1. Hammersmith One** - Para todos los títulos
- **Uso:** Títulos, headings, nombres de secciones
- **Archivo:** `hammersmith_one.ttf`
- **Peso disponible:** Regular (único peso de esta fuente)
- **Fuente:** Google Fonts

**2. Montserrat** - Para todo el texto general
- **Uso:** Texto de cuerpo, labels, botones, descripciones
- **Archivos:**
  - `montserrat_regular.ttf` - FontWeight.Normal (400)
  - `montserrat_medium.ttf` - FontWeight.Medium (500)
  - `montserrat_semibold.ttf` - FontWeight.SemiBold (600)
  - `montserrat_bold.ttf` - FontWeight.Bold (700)
- **Fuente:** Google Fonts

### Implementación en Código

```kotlin
// Usar para títulos
Text(
    text = "Título Principal",
    style = MaterialTheme.typography.headlineLarge  // Hammersmith One
)

// Usar para textos normales
Text(
    text = "Texto descriptivo",
    style = MaterialTheme.typography.bodyMedium  // Montserrat
)

// O directamente
Text(
    text = "Título",
    fontFamily = HammersmithOne
)

Text(
    text = "Texto",
    fontFamily = Montserrat,
    fontWeight = FontWeight.Medium
)
```

### Jerarquía Tipográfica

| Estilo | Fuente | Tamaño | Uso |
|--------|--------|--------|-----|
| displayLarge | Hammersmith One | 57sp | Títulos principales de pantalla |
| headlineLarge | Hammersmith One | 32sp | Títulos de sección |
| headlineMedium | Hammersmith One | 28sp | Subtítulos importantes |
| titleLarge | Hammersmith One | 22sp | Títulos de cards |
| bodyLarge | Montserrat | 16sp | Párrafos principales |
| bodyMedium | Montserrat | 14sp | Texto general |
| labelMedium | Montserrat Medium | 12sp | Labels de campos |

---

## 🎨 Colores Principales

### Paleta del Estándar Visual 2024

**1. Verde Principal**
- **Hex:** `#75EE8A`
- **Variable:** `BioWayColors.BrandGreen`
- **Uso:** Color principal de marca, botones primarios, acentos, bordes activos

**2. Verde Turquesa**
- **Hex:** `#B3FCD4`
- **Variable:** `BioWayColors.BrandTurquoise`
- **Uso:** Degradados, fondos suaves, transiciones

**3. Azul**
- **Hex:** `#00DFFF`
- **Variable:** `BioWayColors.BrandBlue`
- **Uso:** Acentos secundarios, degradados, elementos interactivos

**4. Verde Oscuro** ⭐
- **Hex:** `#007565`
- **Variable:** `BioWayColors.BrandDarkGreen`
- **Uso:** Textos sobre degradados coloridos, texto del logo "BioWay", títulos, campos focused, máximo contraste

**5. Verde del Logo SVG** ⭐
- **Hex:** `#70D162`
- **Uso:** Botones principales (texto), elementos que conectan con el logo
- **Nota:** Este es el color principal del logo SVG de BioWay

**6. Verde Medio Harmonioso**
- **Hex:** `#2E7D6C`
- **Uso:** Textos secundarios en formularios, elementos unfocused, iconos

### Degradado del Estándar

```kotlin
// Degradado oficial con los 3 colores principales
Brush.linearGradient(
    colors = listOf(
        BioWayColors.BrandGreen,      // #75ee8a
        BioWayColors.BrandTurquoise,  // #b3fcd4
        BioWayColors.BrandBlue        // #00dfff
    )
)
```

### Colores Legacy (Mantener por compatibilidad)

- `PrimaryGreen` - #70D997
- `DarkGreen` - #3DB388
- `Turquoise` - #3FD9FF
- `NavGreen` - #74D15F

---

## 📍 Implementación Actual

### Pantallas con Estándar Visual Aplicado

**1. BrindadorDashboardScreen**
- ✅ Degradado aplicado en tarjeta "Sin recolección hoy"
- ✅ Usa los 3 colores principales
- ✅ Texto en blanco para mejor contraste

**2. SplashScreen**
- ✅ Logo BioWay oficial implementado
- ✅ Tipografía ya compatible (se actualizará automáticamente)

**3. LoginScreen**
- ✅ Logo BioWay oficial implementado
- ✅ Tipografía ya compatible

**4. RegisterScreen**
- ✅ Logo BioWay oficial implementado
- ✅ Tipografía ya compatible

**5. PlatformSelectorScreen**
- ✅ Logo BioWay oficial implementado
- ✅ Tipografía ya compatible

---

## 🎯 Guía de Uso

### Para Títulos

```kotlin
Text(
    text = "Título de Sección",
    style = MaterialTheme.typography.headlineMedium,  // Hammersmith One automático
    color = BioWayColors.DarkGreen
)
```

### Para Texto Normal

```kotlin
Text(
    text = "Descripción o texto general",
    style = MaterialTheme.typography.bodyMedium,  // Montserrat automático
    color = BioWayColors.TextDark
)
```

### Para Botones

```kotlin
Button(
    onClick = { /* acción */ },
    colors = ButtonDefaults.buttonColors(
        containerColor = BioWayColors.BrandGreen  // Color principal del estándar
    )
) {
    Text("Acción")  // Montserrat Medium por defecto en botones
}
```

### Para Degradados

```kotlin
// Degradado oficial del estándar visual
Box(
    modifier = Modifier.background(
        brush = Brush.linearGradient(
            colors = listOf(
                BioWayColors.BrandGreen,
                BioWayColors.BrandTurquoise,
                BioWayColors.BrandBlue
            )
        )
    )
)
```

---

## 📦 Archivos del Estándar Visual

### Fuentes
- `app/src/main/res/font/hammersmith_one.ttf`
- `app/src/main/res/font/montserrat_regular.ttf`
- `app/src/main/res/font/montserrat_medium.ttf`
- `app/src/main/res/font/montserrat_semibold.ttf`
- `app/src/main/res/font/montserrat_bold.ttf`

### Código
- `ui/theme/Type.kt` - Tipografía configurada (BioWayTypography)
- `ui/theme/Color.kt` - Colores estándar agregados
- `ui/theme/Theme.kt` - Tema que usa la tipografía

---

## ✅ Checklist para Nuevas Pantallas

Al crear una nueva pantalla, asegúrate de:

- [ ] Usar `MaterialTheme.typography.headlineX` para títulos
- [ ] Usar `MaterialTheme.typography.bodyX` para textos
- [ ] Usar `BioWayColors.BrandX` para los colores principales
- [ ] Aplicar degradado del estándar en elementos destacados
- [ ] Texto blanco sobre degradados coloridos

---

## 🎨 Ejemplo Completo

```kotlin
@Composable
fun ExampleScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        BioWayColors.BrandGreen,
                        BioWayColors.BrandTurquoise,
                        BioWayColors.BrandBlue
                    )
                )
            )
            .padding(24.dp)
    ) {
        // Título con Hammersmith One
        Text(
            text = "Título Principal",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Texto con Montserrat
        Text(
            text = "Descripción con texto normal usando Montserrat",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Botón con color principal
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(
                containerColor = BioWayColors.BrandGreen
            )
        ) {
            Text("Acción")  // Montserrat automático
        }
    }
}
```

---

## 📊 Resumen

| Elemento | Especificación |
|----------|----------------|
| **Títulos** | Hammersmith One |
| **Textos** | Montserrat (Regular, Medium, SemiBold, Bold) |
| **Color 1** | Verde #75EE8A (Principal) |
| **Color 2** | Verde Turquesa #B3FCD4 |
| **Color 3** | Azul #00DFFF |
| **Color 4** | Verde Oscuro #007565 (Textos) ⭐ |
| **Degradado** | Verde → Turquesa → Azul (linear) |
| **Texto sobre degradado** | Verde oscuro #007565 (máximo contraste) |
| **Texto sobre blanco** | Verde oscuro #007565 |

## 🎯 Guía de Uso de Colores

### Texto del Logo "BioWay"
```kotlin
Text(
    text = "BioWay",
    color = BioWayColors.BrandDarkGreen  // #007565
)
```

### Textos sobre Degradados Coloridos
```kotlin
Text(
    text = "Título",
    color = BioWayColors.BrandDarkGreen  // #007565 - Máximo contraste
)
```

### Textos sobre Fondos Blancos/Claros
```kotlin
Text(
    text = "Descripción",
    color = BioWayColors.BrandDarkGreen  // #007565 - Legible y coherente
)
```

---

## 🎨 Patrones de Diseño UI

### Patrón 1: Pantalla con Degradado y Glassmorphism

**Cuándo usar:** Pantallas de autenticación, onboarding, splash

**Estructura:**
```kotlin
Box(fillMaxSize + degradado de fondo) {
    Círculos decorativos difuminados
    Column(padding 32dp) {
        Logo (100dp)
        Título (45sp, Hammersmith One, #007565)
        Subtítulo (16sp, Montserrat, #007565)
        Card Glassmorphism (65% opacidad)
        Acciones secundarias
    }
}
```

**Especificaciones:**
- Degradado: Verde → Turquesa → Azul (vertical)
- Blur decorativo: 100dp en círculos grandes
- Card principal: Blanco @ **75% opacidad** ⭐ (mejorado para legibilidad)
- Padding screen: **24dp horizontal** ⭐ (cards más anchas)
- Padding card: **28dp** ⭐ (balance óptimo)

### Patrón 2: Card Glassmorphism

**Uso:** Contenedores principales sobre fondos coloridos

```kotlin
Surface(
    shape = RoundedCornerShape(32.dp),
    color = Color.White.copy(alpha = 0.75f),  // ⭐ Mejorado
    shadowElevation = 0.dp
) {
    Column(padding = 28dp) {  // ⭐ Ajustado
        // Contenido
    }
}
```

### Patrón 3: Campo de Texto Glassmorphism

**Uso:** Formularios sobre fondos coloridos

**Colores:**
- Focused: Borde #007565, texto #007565, fondo blanco @ **75%** ⭐
- Unfocused: Borde #007565 @ 50%, texto #2E7D6C, fondo blanco @ **55%** ⭐

```kotlin
OutlinedTextField(
    shape = RoundedCornerShape(16.dp),
    colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = BioWayColors.BrandDarkGreen,
        focusedTextColor = BioWayColors.BrandDarkGreen,
        unfocusedTextColor = Color(0xFF2E7D6C)
        // ... más colores
    )
)
```

### Patrón 4: Botón Principal Inverso

**Uso:** CTA principal sobre fondos coloridos

```kotlin
Button(
    modifier = Modifier.fillMaxWidth().height(56.dp),
    shape = RoundedCornerShape(16.dp)
) {
    Box(
        modifier = Modifier.background(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.9f),
                    Color.White.copy(alpha = 0.8f)
                )
            )
        )
    ) {
        Text(
            text = "Acción",
            color = Color(0xFF70D162)  // Verde del logo
        )
    }
}
```

### Patrón 5: Botones Secundarios Glassmorphism

**Uso:** Acciones secundarias, navegación

```kotlin
Surface(
    shape = RoundedCornerShape(12.dp),
    color = Color.White.copy(alpha = 0.25f)
) {
    Box(padding = 12dp vertical, 20dp horizontal) {
        Text(
            text = "Acción",
            color = Color.White
        )
    }
}
```

---

## 📏 Sistema de Espaciado

### Base: 4dp

**Escala recomendada:**
- **XS:** 8dp, 12dp, 16dp - Entre elementos muy relacionados
- **S:** 20dp, 24dp, 28dp - Entre elementos cercanos
- **M:** 32dp, 40dp - Entre elementos relacionados
- **L:** 60dp - Entre secciones
- **XL:** 100dp - Padding inicial de pantalla

### Padding Estándar

- **Pantalla:** **24dp horizontal** ⭐ (optimizado para cards más anchas)
- **Card:** **28dp todos los lados** ⭐ (balance entre espacio y legibilidad)
- **Botón interno:** 12-20dp vertical, 20-32dp horizontal
- **Campo de texto:** Determinado por Material 3

### Alturas Estándar

- **Botón principal:** 56dp
- **Campo de texto:** ~56dp (Material 3)
- **Botón secundario:** ~48dp

---

## 🎨 Opacidades y Transparencias

### Glassmorphism

| Elemento | Opacidad | Uso |
|----------|----------|-----|
| **Card principal** | **75%** ⭐ | Contenedor sobre degradado (mejorado) |
| **Campo focused** | **75%** ⭐ | Cuando usuario escribe (más legible) |
| **Campo unfocused** | **55%** ⭐ | Estado inicial (más visible) |
| **Botón secundario** | 25% | Acciones menos importantes |
| **Blur circles** | 20-30% | Decoración de fondo |

### Texto sobre Fondos

- Texto sobre degradado: #007565 (100% opacidad)
- Texto sobre blanco: #007565 o #2E7D6C (100% opacidad)
- Texto blanco sobre degradado: #FFFFFF @ 80-100%

---

## 🎯 Bordes Redondeados

| Elemento | Radio | Razón |
|----------|-------|-------|
| **Card principal** | 32dp | Suave, acogedor |
| **Botón/Campo** | 16dp | Moderno, balanceado |
| **Botón secundario** | 12dp | Sutil, compacto |
| **Círculos blur** | 50% | Totalmente redondeado |

---

Este estándar visual está implementado y listo para usar en toda la aplicación! 🎉

**Ver también:** `REVISION_DISENO_LOGIN.md` para análisis detallado de composición.
