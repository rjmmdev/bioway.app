# 📡 Comunicación NFC entre Teléfonos Android con HCE

## ✅ Implementación Completada

La app ahora usa **Host Card Emulation (HCE)** para comunicación real entre dos teléfonos Android.

## 🔧 Arquitectura Implementada

### Dispositivo 1: Usuario Normal (HCE - Emisor)
- **Modo:** Host Card Emulation (Emula una tarjeta NFC virtual)
- **Tecnología:** `HostApduService`
- **Función:** Responde a comandos APDU del lector con el User ID
- **AID:** F0010203040506

### Dispositivo 2: Celular en Bote (Reader - Receptor)
- **Modo:** Reader Mode con IsoDep
- **Tecnología:** `NfcAdapter.enableReaderMode()` + `IsoDep`
- **Función:** Lee la tarjeta emulada mediante comandos APDU
- **Comandos:** SELECT AID + GET_USER_ID

## 📋 Cómo Usar

### Paso 1: Preparar los Dispositivos

**Dispositivo 1 (Usuario Normal):**
1. Instalar la app
2. Habilitar NFC en: Configuración → Conexiones → NFC
3. Abrir la app → Brindador → Dashboard
4. Tocar "Usuario Normal"
5. Verificar que aparezca un ID de 8 dígitos
6. Verificar en "Estado del Sistema":
   - NFC Soportado: Sí
   - NFC Habilitado: Sí
   - HCE Soportado: Sí
   - Modo: Card Emulation (HCE)

**Dispositivo 2 (Celular en Bote):**
1. Instalar la app
2. Habilitar NFC en: Configuración → Conexiones → NFC
3. Abrir la app → Brindador → Dashboard
4. Tocar "Celular en Bote"
5. Verificar en "Estado del Sistema":
   - NFC Soportado: Sí
   - NFC Habilitado: Sí
   - Escuchando: Sí
   - Modo: IsoDep Reader

### Paso 2: Realizar la Comunicación

1. **Mantener ambas pantallas activas** (no ir a home ni bloquear)
2. **Acercar las partes traseras** de ambos dispositivos
3. **Mantener cerca** por 1-2 segundos
4. **Observar:**
   - Ambos dispositivos vibrarán si la comunicación es exitosa
   - El "Celular en Bote" mostrará el ID detectado
   - Aparecerá animación de "¡Detectado!" en verde

## 🔍 Debugging con Logcat

### Ver todos los logs NFC en ambos dispositivos:

**Terminal 1 - Usuario Normal:**
```bash
adb -s DEVICE_1_ID logcat | grep "UsuarioNormalNFC\|BioWayHceService"
```

**Terminal 2 - Celular en Bote:**
```bash
adb -s DEVICE_2_ID logcat | grep "CelularEnBoteNFC"
```

### Ver solo conexiones exitosas:
```bash
adb logcat | grep -E "(✅|DETECCIÓN EXITOSA|Mensaje escrito exitosamente)"
```

### Ver solo errores:
```bash
adb logcat | grep -E "(❌|ERROR)" | grep -E "(NFC|HCE)"
```

## 📊 Logs Esperados en Comunicación Exitosa

### Dispositivo 1 - Usuario Normal (Logs del HCE Service):

```
D/UsuarioNormalNFC: === Inicializando Usuario Normal NFC (HCE Mode) ===
D/UsuarioNormalNFC: ✅ NFC soportado. Estado: Habilitado
D/UsuarioNormalNFC: HCE soportado: true
D/UsuarioNormalNFC: User ID actualizado en servicio HCE: 12345678

[Cuando se acerca el otro dispositivo]
D/BioWayHceService: === processCommandApdu llamado ===
D/BioWayHceService: Comando recibido: 00 A4 04 00 07 F0 01 02 03 04 05 06 00
D/BioWayHceService: ✅ Comando SELECT AID recibido
D/BioWayHceService: === processCommandApdu llamado ===
D/BioWayHceService: Comando recibido: 00 CA 00 00 00
D/BioWayHceService: ✅ Comando GET_USER_ID recibido
D/BioWayHceService: Enviando User ID: 12345678
D/BioWayHceService: ✅ Respuesta enviada: 31 32 33 34 35 36 37 38 90 00
D/BioWayHceService: 🔴 Sesión NFC desactivada. Razón: Pérdida de enlace
D/UsuarioNormalNFC: Sesión finalizada. Total: 1
```

### Dispositivo 2 - Celular en Bote (Logs del Reader):

```
D/CelularEnBoteNFC: === Inicializando Celular en Bote NFC (Reader Mode) ===
D/CelularEnBoteNFC: ✅ NFC soportado
D/CelularEnBoteNFC: Estado: Habilitado ✅
D/CelularEnBoteNFC: ✅ Reader mode habilitado con FLAG_READER_SKIP_NDEF_CHECK

[Cuando detecta el otro dispositivo]
D/CelularEnBoteNFC: ========================================
D/CelularEnBoteNFC: 🔵 TAG DETECTADO
D/CelularEnBoteNFC: ========================================
D/CelularEnBoteNFC: Tag ID: 04:68:9E:B2:5C:5C:80
D/CelularEnBoteNFC: Tecnologías soportadas: android.nfc.tech.IsoDep, android.nfc.tech.NfcA
D/CelularEnBoteNFC: === INICIO readUserIdFromHce ===
D/CelularEnBoteNFC: ✅ IsoDep disponible, conectando...
D/CelularEnBoteNFC: ✅ Conectado!
D/CelularEnBoteNFC: 📤 Enviando SELECT APDU: 00 A4 04 00 07 F0 01 02 03 04 05 06 00
D/CelularEnBoteNFC: 📥 Respuesta SELECT: 90 00
D/CelularEnBoteNFC: ✅ SELECT exitoso!
D/CelularEnBoteNFC: 📤 Enviando GET_USER_ID APDU: 00 CA 00 00 00
D/CelularEnBoteNFC: 📥 Respuesta GET_USER_ID: 31 32 33 34 35 36 37 38 90 00
D/CelularEnBoteNFC: ✅ User ID extraído: '12345678'
D/CelularEnBoteNFC: ✅ Formato de User ID válido!
D/CelularEnBoteNFC: ========================================
D/CelularEnBoteNFC: ✅ DETECCIÓN EXITOSA
D/CelularEnBoteNFC: User ID: 12345678
D/CelularEnBoteNFC: Total detecciones: 1
D/CelularEnBoteNFC: ========================================
```

## ⚙️ Configuración para Máxima Distancia

La implementación incluye optimizaciones para maximizar el rango de detección:

1. **Polling agresivo:** `EXTRA_READER_PRESENCE_CHECK_DELAY = 50ms`
2. **Todos los protocolos NFC:** NFC-A, NFC-B, NFC-F, NFC-V
3. **Skip NDEF check:** `FLAG_READER_SKIP_NDEF_CHECK` para comunicación directa con HCE
4. **Timeout extendido:** 3000ms para mejorar estabilidad
5. **Sin sonidos del sistema:** `FLAG_READER_NO_PLATFORM_SOUNDS`

## ⚠️ Limitaciones del NFC

**Distancia máxima:** ~4-5cm (limitación del hardware NFC, no de software)
- El NFC es una tecnología de corto alcance por diseño
- La distancia real depende del hardware de cada dispositivo
- La antena NFC suele estar en la parte trasera central del dispositivo

## 🐛 Solución de Problemas

### 1. "HCE No Soportado"
- Verifica que el dispositivo tenga Android 4.4+ (API 19+)
- La mayoría de dispositivos modernos soportan HCE

### 2. No detecta el otro dispositivo
- **Verifica que ambos tengan NFC habilitado**
- **Verifica que las pantallas correctas estén abiertas:**
  - Dispositivo 1: "Usuario Normal"
  - Dispositivo 2: "Celular en Bote"
- **Acerca los dispositivos muy cerca** (<2cm inicialmente)
- **Busca el punto óptimo:** La antena NFC varía por dispositivo
- **Mantén contacto por 1-2 segundos**
- **No bloquees las pantallas:** Ambas deben estar activas

### 3. Se detecta pero no se lee el ID
- Verifica los logs con `adb logcat | grep BioWayHceService`
- Si no aparecen logs del servicio, verifica el AndroidManifest
- Asegúrate de que el servicio esté registrado correctamente

### 4. Verificar que el servicio HCE esté activo:
```bash
adb shell dumpsys nfc | grep -A 20 "HCE"
```

## 📱 Archivos Creados/Modificados

### Nuevos archivos:
1. ✅ `BioWayHceService.kt` - Servicio HCE que emula tarjeta NFC
2. ✅ `res/xml/apduservice.xml` - Configuración del servicio HCE
3. ✅ `NFC_COMUNICACION_HCE.md` - Esta documentación

### Archivos modificados:
1. ✅ `UsuarioNormalNFCScreen.kt` - Ahora usa HCE en lugar de Reader Mode
2. ✅ `CelularEnBoteNFCScreen.kt` - Ahora usa IsoDep con comandos APDU
3. ✅ `AndroidManifest.xml` - Registrado servicio HCE
4. ✅ `strings.xml` - Agregadas descripciones del servicio
5. ✅ `libs.versions.toml` - AGP actualizado a 8.13.1

## 🎯 Protocolo de Comunicación

### Flujo APDU:

1. **Lector → Emisor:** SELECT AID (00 A4 04 00 07 F0010203040506 00)
2. **Emisor → Lector:** SUCCESS (90 00)
3. **Lector → Emisor:** GET_USER_ID (00 CA 00 00 00)
4. **Emisor → Lector:** USER_ID + SUCCESS (ej: 31323334353637 38 90 00)

### Interpretación de bytes:
- `31 32 33 34 35 36 37 38` = "12345678" en UTF-8
- `90 00` = Status Word (Success)

## 📚 Referencias Técnicas

- [Host-based Card Emulation Overview - Android Developers](https://developer.android.com/develop/connectivity/nfc/hce)
- [How to use Host-based Card Emulation (HCE) in Android](https://medium.com/@androidcrypto/how-to-use-host-based-card-emulation-hce-in-android-a-beginner-tutorial-java-32974dd89529)
- [GitHub - Android_HCE_Beginner_App](https://github.com/AndroidCrypto/Android_HCE_Beginner_App)
- [How to build a simple smart card emulator & reader for Android](https://medium.com/the-almanac/how-to-build-a-simple-smart-card-emulator-reader-for-android-7975fae4040f)

## ✅ Próximos Pasos

1. **Instalar en dos dispositivos físicos** (el emulador no soporta NFC real)
2. **Habilitar NFC en ambos**
3. **Abrir las pantallas correspondientes**
4. **Acercar y esperar la detección**
5. **Monitorear logs** para debugging si es necesario

La implementación está lista para usar en dispositivos reales con NFC! 🎉
