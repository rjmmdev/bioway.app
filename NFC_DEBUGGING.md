# 📡 Guía de Debugging NFC

## 🔍 Verificar Logs en Tiempo Real

### Ver todos los logs NFC:
```bash
adb logcat | grep -E "(UsuarioNormalNFC|CelularEnBoteNFC)"
```

### Ver solo logs de Usuario Normal (Emisor):
```bash
adb logcat | grep UsuarioNormalNFC
```

### Ver solo logs de Celular en Bote (Receptor):
```bash
adb logcat | grep CelularEnBoteNFC
```

### Ver logs de errores:
```bash
adb logcat | grep -E "(UsuarioNormalNFC|CelularEnBoteNFC)" | grep -E "(ERROR|❌|⚠️)"
```

### Limpiar y ver logs desde cero:
```bash
adb logcat -c && adb logcat | grep -E "(UsuarioNormalNFC|CelularEnBoteNFC)"
```

## 📋 Checklist de Verificación

### Antes de probar:

1. **Verificar que NFC esté habilitado en ambos dispositivos:**
   - Configuración → Conexiones → NFC y pagos → Activar NFC
   - La app muestra el estado en pantalla en la tarjeta "Estado del Sistema"

2. **Instalar la app en ambos dispositivos:**
   ```bash
   adb devices  # Ver dispositivos conectados
   adb -s DEVICE_ID install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Verificar permisos:**
   - La app debe tener permisos de NFC (se otorgan automáticamente)
   - Verificar en: Configuración → Aplicaciones → BioWay → Permisos

### Durante la prueba:

1. **Dispositivo 1:** Abrir "Usuario Normal"
   - Debe mostrar un ID de 8 dígitos
   - Estado debe mostrar: "NFC Habilitado: Sí"
   - Ver logs: Debe aparecer "Reader mode habilitado"

2. **Dispositivo 2:** Abrir "Celular en Bote"
   - Debe mostrar "Escuchando..."
   - Estado debe mostrar: "NFC Habilitado: Sí", "Escuchando: Sí"
   - Ver logs: Debe aparecer "Reader mode habilitado"

3. **Acercar los dispositivos:**
   - Poner las partes traseras juntas (donde está la antena NFC)
   - Mantener cerca por 1-2 segundos
   - Ambos deben vibrar si la transmisión es exitosa

## 🐛 Logs Esperados

### Usuario Normal (Emisor) - Flujo Exitoso:
```
D/UsuarioNormalNFC: Inicializando NFC...
D/UsuarioNormalNFC: NFC soportado. Estado: Habilitado
D/UsuarioNormalNFC: Reader mode habilitado
D/UsuarioNormalNFC: Tag detectado: [ID del tag]
D/UsuarioNormalNFC: === INICIO writeNdefMessageToTag ===
D/UsuarioNormalNFC: User ID a escribir: 12345678
D/UsuarioNormalNFC: Tecnologías del tag: [lista de tecnologías]
D/UsuarioNormalNFC: Ndef instance: disponible
D/UsuarioNormalNFC: Tag es NDEF, intentando conectar...
D/UsuarioNormalNFC: Conectado. Tipo: [tipo], Max: [tamaño] bytes, Writable: true
D/UsuarioNormalNFC: Escribiendo mensaje NDEF...
D/UsuarioNormalNFC: ✅ Mensaje escrito exitosamente!
D/UsuarioNormalNFC: Transmisión exitosa. ID: 12345678, Total: 1
D/UsuarioNormalNFC: === FIN writeNdefMessageToTag ===
```

### Celular en Bote (Receptor) - Flujo Exitoso:
```
D/CelularEnBoteNFC: Inicializando NFC...
D/CelularEnBoteNFC: NFC soportado. Estado: Habilitado
D/CelularEnBoteNFC: Reader mode habilitado
D/CelularEnBoteNFC: Tag detectado: [ID del tag]
D/CelularEnBoteNFC: === INICIO extractNdefMessages ===
D/CelularEnBoteNFC: Tecnologías del tag: [lista]
D/CelularEnBoteNFC: Ndef instance: disponible
D/CelularEnBoteNFC: Intentando conectar al tag...
D/CelularEnBoteNFC: ✅ Conectado. Tipo: [tipo], Max: [tamaño] bytes
D/CelularEnBoteNFC: Mensaje NDEF: disponible con 2 records
D/CelularEnBoteNFC: === INICIO extractUserIdFromNdef ===
D/CelularEnBoteNFC: Total de records: 2
D/CelularEnBoteNFC: --- Record 0 ---
D/CelularEnBoteNFC: TNF: 1
D/CelularEnBoteNFC: ✅ Es un record WELL_KNOWN
D/CelularEnBoteNFC: Texto extraído: '12345678'
D/CelularEnBoteNFC: ✅ ID válido encontrado: 12345678
D/CelularEnBoteNFC: ID detectado: 12345678, Total: 1
```

## ⚠️ Problemas Comunes

### 1. "NFC no soportado"
- **Causa:** El dispositivo no tiene hardware NFC
- **Solución:** Usar un dispositivo con NFC (mayoría de smartphones modernos)

### 2. "NFC Desactivado"
- **Causa:** NFC está apagado en configuración
- **Solución:** Activar NFC en Configuración → Conexiones → NFC

### 3. "Tag detectado pero no se escribe/lee"
- **Logs a revisar:**
  ```bash
  adb logcat | grep "Tag no es escribible\|Tag no soporta NDEF"
  ```
- **Causa:** Los smartphones no son tags NFC escribibles tradicionales
- **Solución:** Esto es NORMAL - los teléfonos en modo HCE (Host Card Emulation) actúan diferente

### 4. "No detecta el otro dispositivo"
- **Verificar:**
  - Ambos dispositivos tienen NFC habilitado
  - Ambas apps están en las pantallas NFC correspondientes
  - Los dispositivos están muy cerca (< 5cm)
  - Las antenas NFC están alineadas (usualmente parte trasera central)

## 💡 Importante: Comunicación Teléfono a Teléfono

**NOTA CRÍTICA:** La implementación actual usa `enableReaderMode` en ambos dispositivos, lo que funciona para leer tags NFC pasivos pero **NO** para comunicación directa teléfono-a-teléfono.

Para comunicación entre dos smartphones Android, se requiere:

### Opción 1: Host Card Emulation (HCE)
- Un dispositivo actúa como "tarjeta" (HCE)
- Otro dispositivo lee como "lector"
- Requiere implementar `HostApduService`

### Opción 2: Android Beam (DEPRECATED en API 29+)
- No disponible en minSdk 31
- Ya no se recomienda usar

### Opción 3: Wi-Fi Direct o Bluetooth
- Para transferencia de datos entre dispositivos
- Más confiable que NFC

## 🔧 Próximos Pasos Sugeridos

Para que funcione la comunicación teléfono-a-teléfono con NFC, se debe:

1. **Usuario Normal:** Implementar HCE (Host Card Emulation)
2. **Celular en Bote:** Mantener Reader Mode actual
3. Agregar servicio HCE en AndroidManifest
4. Implementar clase que extienda `HostApduService`

¿Quieres que implemente HCE para comunicación real entre teléfonos?
