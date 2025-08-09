# 🌱 BioWay - Plataforma de Economía Circular

<p align="center">
  <strong>Transformando el reciclaje en México mediante tecnología y dignificación laboral</strong>
</p>

<p align="center">
  <a href="#características">Características</a> •
  <a href="#arquitectura">Arquitectura</a> •
  <a href="#instalación">Instalación</a> •
  <a href="#uso">Uso</a> •
  <a href="#desarrollo">Desarrollo</a>
</p>

---

## 🎯 Descripción

**BioWay** es una plataforma digital innovadora que conecta y organiza toda la cadena de reciclaje en México, desde ciudadanos conscientes hasta centros de acopio, dignificando el trabajo de miles de recolectores informales.

### 🚀 Misión
Crear un ecosistema de reciclaje eficiente, transparente y justo que beneficie a todos los participantes mientras protege el medio ambiente.

### 💡 Problema que Resuelve
- **Desorganización** del reciclaje urbano
- **Invisibilización** de recolectores informales
- **Ineficiencia** en la separación de materiales
- **Falta de trazabilidad** del impacto ambiental

## ✨ Características

### 🤖 Visión Artificial Integrada
- **Escáner Inteligente** con IA para identificación automática de materiales
- Detección de **18+ categorías** de residuos reciclables
- Procesamiento en tiempo real con Google ML Kit
- Funcionamiento offline después de descarga inicial

### 👥 Tipos de Usuarios

#### 🏠 **Brindadores** (Ciudadanos)
- Guía visual para separar residuos correctamente
- Sistema de puntos y recompensas
- Conexión directa con recolectores
- Seguimiento del impacto ambiental personal

#### ♻️ **Recolectores** 
- Mapa interactivo con materiales disponibles
- Optimización de rutas de recolección
- Materiales pre-separados (mayor valor)
- Dignificación y profesionalización del trabajo

#### 🏢 **Empresas Asociadas**
- Sistema B2B para empresas especializadas (ej: Sicit)
- Gestión de materiales específicos
- Códigos únicos de registro
- Control de zonas de operación

#### 🏭 **Centros de Acopio**
- Recepción digital de materiales
- Control de inventarios
- Sistema de prepago
- Reportes automatizados

#### 👨‍💼 **Administradores**
- Panel de control maestro
- Gestión de empresas y usuarios
- Configuración de horarios y zonas
- Analytics en tiempo real

## 🏗 Arquitectura

### 📱 Tecnologías
- **Frontend:** Flutter (iOS & Android)
- **Backend:** Firebase
  - Authentication
  - Cloud Firestore
  - Cloud Storage
  - Cloud Functions
- **Visión Artificial:** Google ML Kit
  - Image Labeling
  - On-device Processing
  - Custom Model Support
- **Mapas:** Google Maps API
- **Pagos:** Integración con pasarelas locales

### 📊 Estructura de Datos

```
BioWay/
├── 👥 Usuarios/
│   ├── Brindadores
│   ├── Recolectores
│   └── Administradores
├── 🏢 Empresas/
│   └── [Sicit, EcoMX, etc.]
├── ♻️ Materiales/
│   └── [PET, Cartón, Raspa, etc.]
├── 📍 Zonas Habilitadas/
│   └── [Estados, Municipios, CPs]
└── 📦 Solicitudes de Recolección/
```

## 🛠 Instalación

### Requisitos Previos
- Flutter SDK ≥ 3.0.0
- Dart SDK ≥ 3.0.0
- Android Studio / Xcode
- Cuenta de Firebase

### Pasos

1. **Clonar el repositorio**
```bash
git clone https://github.com/tuusuario/bioway.git
cd bioway_nueva_version
```

2. **Instalar dependencias**
```bash
flutter pub get
```

3. **Configurar Firebase**
```bash
flutterfire configure --project=bioway-mexico
```

4. **Ejecutar la aplicación**
```bash
flutter run
```

## 🎨 Modo Desarrollo

La app está actualmente en **MODO DISEÑO** para facilitar la navegación y pruebas visuales:

### Accesos Rápidos (Login)
- **Email:** cualquier texto → Brindador
- **Email:** contiene "recolector" → Recolector
- **Botones directos** para todos los módulos

### Características del Modo Diseño
- ✅ Navegación libre sin autenticación
- ✅ Datos de prueba precargados
- ✅ Firebase temporalmente deshabilitado
- ✅ Todos los módulos accesibles

Para restaurar la funcionalidad completa, consulta [`RESTAURAR_FIREBASE.md`](RESTAURAR_FIREBASE.md)

## 📂 Estructura del Proyecto

```
lib/
├── screens/
│   ├── auth/           # Login y Registro
│   ├── brindador/      # Módulo ciudadanos
│   ├── recolector/     # Módulo recolectores
│   ├── maestro/        # Panel administrativo
│   ├── centro_acopio/  # Gestión de centros
│   └── ai/             # Escáner inteligente con IA
├── services/
│   ├── firebase/       # Servicios Firebase
│   ├── bioway/         # Lógica de negocio
│   └── ai/             # Servicios de visión artificial
├── widgets/            # Componentes reutilizables
└── utils/              # Utilidades y constantes
```

## 🚀 Estado del Desarrollo

### ✅ Completado
- Sistema de autenticación multi-rol
- Gestión de empresas asociadas
- Sistema de códigos únicos
- Navegación y UI/UX consistente
- Estructura de base de datos
- **Escáner inteligente con IA** para identificación de materiales
- Sistema de detección multi-nivel con ML Kit

### 🔧 En Desarrollo
- Integración de pagos
- Sistema de notificaciones push
- Optimización de rutas con IA
- Gamificación avanzada
- Mejora continua del modelo de IA con feedback de usuarios

### 📋 Próximas Características
- Blockchain para trazabilidad
- IoT con contenedores inteligentes
- Marketplace de productos reciclados
- Programa de certificaciones

## 🤝 Contribuir

Este es un proyecto privado en desarrollo. Para contribuir o reportar problemas, contacta al equipo de desarrollo.

## 📄 Documentación Adicional

- [`AI_SCANNER_ARCHITECTURE.md`](docs/AI_SCANNER_ARCHITECTURE.md) - **Arquitectura completa del sistema de visión artificial**
- [`DATABASE_STRUCTURE.md`](DATABASE_STRUCTURE.md) - Estructura completa de la base de datos
- [`RESTAURAR_FIREBASE.md`](RESTAURAR_FIREBASE.md) - Guía para activar Firebase en producción

## 👥 Equipo

Desarrollado por **RJMM Dev** para **BioWay México**

## 📞 Contacto

- **Email:** contacto@bioway.com.mx
- **Web:** [www.bioway.com.mx](https://bioway.com.mx)

## ⚖️ Licencia

© 2024 BioWay México. Todos los derechos reservados.

---

<p align="center">
  <strong>🌱 Juntos construimos un México más limpio y justo 🇲🇽</strong>
</p>
