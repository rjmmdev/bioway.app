# 📋 Guía de Restauración de Firebase - BioWay App

## 🎨 Estado Actual: MODO DISEÑO
La aplicación está actualmente en **modo diseño** con Firebase deshabilitado para permitir navegación libre y pruebas visuales sin autenticación.

---

## 🔄 Instrucciones para Restaurar la Funcionalidad Completa

### 1. 🔐 **Restaurar Autenticación en Login**

**Archivo:** `lib/screens/auth/bioway_login_screen.dart`

#### Paso 1.1: Restaurar inicialización de Firebase
Buscar y reemplazar:
```dart
// MODO DESARROLLO: Firebase deshabilitado temporalmente
// TODO: Descomentar para producción
/*
[código comentado]
*/

// Crear instancia dummy para evitar errores
_bioWayAuthService = BioWayAuthService();
debugPrint('🎨 MODO DISEÑO: Firebase deshabilitado');
```

Por el código original (descomentarlo):
```dart
try {
  // Inicializar Firebase para BioWay
  await _authService.initializeForPlatform(FirebasePlatform.bioway);
  debugPrint('✅ Firebase inicializado para BioWay');
  
  // Ahora es seguro crear la instancia de BioWayAuthService
  _bioWayAuthService = BioWayAuthService();
} catch (e) {
  debugPrint('❌ Error al inicializar Firebase para BioWay: $e');
  _bioWayAuthService = BioWayAuthService();
}
```

#### Paso 1.2: Restaurar método _handleLogin
En el método `_handleLogin()`, eliminar el código de desarrollo y descomentar el código original que está marcado como:
```dart
/* CÓDIGO ORIGINAL CON FIREBASE - RESTAURAR PARA PRODUCCIÓN
[código comentado con Firebase]
*/
```

### 2. 📝 **Restaurar Registro de Usuarios**

**Archivo:** `lib/screens/auth/bioway_register_screen.dart`

#### Paso 2.1: Restaurar inicialización
Similar al login, buscar y descomentar el código original en `_initializeFirebase()`.

#### Paso 2.2: Restaurar método _register
En el método `_register()`, eliminar el código simulado y descomentar:
```dart
/* CÓDIGO ORIGINAL CON FIREBASE - RESTAURAR PARA PRODUCCIÓN
[código comentado con registro real]
*/
```

### 3. 🗄️ **Verificar Configuración de Firebase**

#### Archivos de configuración necesarios:
- ✅ `lib/firebase_options.dart` - Generado por FlutterFire CLI
- ✅ `android/app/google-services.json` - Configuración Android
- ⚠️ `ios/Runner/GoogleService-Info.plist` - Configuración iOS (falta, descargar de Firebase Console)

### 4. 🔧 **Servicios de Firebase**

Los siguientes servicios ya están implementados y listos:

#### `lib/services/firebase/auth_service.dart`
- ✅ Autenticación con email/password
- ✅ Manejo de errores en español
- ✅ Recuperación de contraseña

#### `lib/services/firebase/firebase_manager.dart`
- ✅ CRUD operations para Firestore
- ✅ Upload/delete de archivos en Storage
- ✅ Queries y streams

#### `lib/services/bioway/bioway_auth_service.dart`
- ✅ Registro diferenciado (Brindador/Recolector)
- ✅ Almacenamiento de datos en Firestore
- ✅ Actualización de perfiles

### 5. 📊 **Estructura de Base de Datos (Firestore)**

#### Colección: `usuarios`
```javascript
{
  uid: "string",
  email: "string",
  nombre: "string",
  tipoUsuario: "brindador" | "recolector",
  
  // Para Brindadores:
  direccion: {
    calle: "string",
    numeroExterior: "string",
    codigoPostal: "string",
    estado: "string",
    municipio: "string",
    colonia: "string"
  },
  puntos: 0,
  nivel: 1,
  
  // Para Recolectores:
  empresa: "string",
  materialesRecolectados: 0,
  calificacion: 5.0,
  verificado: false,
  
  // Comunes:
  activo: true,
  fechaRegistro: timestamp,
  ultimaActualizacion: timestamp,
  ultimoAcceso: timestamp
}
```

### 6. 🚀 **Pasos para Activar en Producción**

1. **Verificar proyecto Firebase:**
   ```bash
   firebase use
   # Debe mostrar: bioway-mexico
   ```

2. **Instalar dependencias:**
   ```bash
   flutter pub get
   ```

3. **Para iOS (si aplica):**
   - Descargar `GoogleService-Info.plist` desde Firebase Console
   - Colocar en `ios/Runner/`
   - Ejecutar:
   ```bash
   cd ios && pod install
   ```

4. **Descomentar código Firebase** en:
   - `lib/screens/auth/bioway_login_screen.dart`
   - `lib/screens/auth/bioway_register_screen.dart`

5. **Verificar reglas de Firestore:**
   ```javascript
   // firestore.rules
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /{document=**} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```

6. **Deploy de reglas:**
   ```bash
   firebase deploy --only firestore:rules
   ```

### 7. 🧪 **Testing de Funcionalidad**

#### Verificar que funciona correctamente:
- [ ] Login con credenciales válidas
- [ ] Registro de nuevo Brindador
- [ ] Registro de nuevo Recolector
- [ ] Navegación según tipo de usuario
- [ ] Persistencia de sesión
- [ ] Cerrar sesión
- [ ] Recuperación de contraseña

### 8. 🔒 **Seguridad**

Antes de producción, actualizar reglas de Firestore para mayor seguridad:
```javascript
match /usuarios/{userId} {
  allow read: if request.auth != null && request.auth.uid == userId;
  allow write: if request.auth != null && request.auth.uid == userId;
  allow create: if request.auth != null;
}
```

### 9. 📱 **Navegación por Tipo de Usuario**

La app navega automáticamente según el tipo de usuario:

- **Email contiene "recolector"** → `RecolectorMainScreen`
- **Cualquier otro email** → `BrindadorMainScreen`
- **Maestro/Admin** → Acceso directo (implementar según necesidad)

### 10. ⚠️ **Notas Importantes**

1. **Variables de entorno:** Considerar mover API keys a variables de entorno para mayor seguridad.

2. **Validación de datos:** El registro actualmente no valida:
   - Formato de código postal
   - Validez de estados/municipios
   - Empresas reales para recolectores

3. **Funcionalidades pendientes:**
   - Email de verificación
   - Login con Google/Facebook
   - 2FA (autenticación de dos factores)
   - Roles y permisos detallados

4. **Performance:** Considerar implementar:
   - Cache de datos de usuario
   - Lazy loading de colecciones grandes
   - Paginación en listas

---

## 🆘 **Solución de Problemas Comunes**

### Error: "Firebase not initialized"
```bash
flutter clean
flutter pub get
# Regenerar firebase_options.dart
flutterfire configure --project=bioway-mexico
```

### Error: "No Firebase App"
Verificar que `main.dart` inicializa Firebase:
```dart
void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await Firebase.initializeApp(
    options: DefaultFirebaseOptions.currentPlatform,
  );
  runApp(const BioWayApp());
}
```

### Error de autenticación persistente
Limpiar caché de autenticación:
```dart
await FirebaseAuth.instance.signOut();
await FirebaseAuth.instance.signInAnonymously(); // Test
```

---

## 📞 **Contacto y Soporte**

Para dudas sobre la implementación de Firebase:
- Documentación oficial: https://firebase.google.com/docs/flutter/setup
- FlutterFire: https://firebase.flutter.dev/

---

**Última actualización:** Diciembre 2024
**Versión del documento:** 1.0
**Proyecto:** BioWay México