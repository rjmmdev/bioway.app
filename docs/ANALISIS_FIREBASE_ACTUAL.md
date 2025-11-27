# 🔍 ANÁLISIS DE FIREBASE - Base de Datos Actual

**Proyecto:** software-4e6b6
**Fecha de Análisis:** 26 de Noviembre de 2025

---

## 📊 ESTRUCTURA ACTUAL DE FIRESTORE

### 1. Colecciones Principales (BioWay App Original)

#### `UsersInAct/` - Usuarios Activos
**Propósito:** Datos de usuarios (brindadores, recolectores)
**Usado por:** Web BioWay ✅
**Mantener:** ✅ SÍ
**Subcollections:**
- `Historial/` - Historial de reciclaje
- `Residuos/` - Residuos pendientes de recolección

**Reglas:**
- Read: Usuarios autenticados
- Create: Usuarios autenticados
- Update: Propietario o recolector
- Delete: Solo propietario

#### `Recolectores/`
**Propósito:** Datos de recolectores
**Usado por:** Web BioWay ✅
**Mantener:** ✅ SÍ

**Reglas:**
- Read: Usuarios autenticados
- Create: Usuarios autenticados
- Update/Delete: Solo propietario

#### `CentrosDeAcopio/`
**Propósito:** Centros de acopio registrados
**Usado por:** Web BioWay ✅
**Mantener:** ✅ SÍ

**Reglas:**
- Read: Público
- Write: Usuarios autenticados

#### `Reciclables/`
**Propósito:** Catálogo de materiales reciclables
**Usado por:** Web BioWay ✅
**Mantener:** ✅ SÍ (solo lectura)

**Reglas:**
- Read: Público
- Write: Bloqueado (solo admin puede modificar desde consola)

#### `Horarios/`
**Propósito:** Horarios de recolección por zona
**Usado por:** Web BioWay ✅
**Mantener:** ✅ SÍ (solo lectura)

**Reglas:**
- Read: Usuarios autenticados
- Write: Bloqueado (solo admin)

#### `Config/`
**Propósito:** Configuración global de la app
**Usado por:** Web BioWay ✅
**Mantener:** ✅ SÍ

**Reglas:**
- Read: Público
- Write: Bloqueado

#### `companies/`
**Propósito:** Directorio de empresas
**Usado por:** Web BioWay (posible comercio)
**Mantener:** ⚠️ REVISAR

**Reglas:**
- Read: Público
- Create/Update: Propietario
- Delete: Bloqueado

#### `sessions/`
**Propósito:** Sesiones de usuarios
**Usado por:** Web BioWay ✅
**Mantener:** ✅ SÍ

**Reglas:**
- Read/Write: Usuarios autenticados

---

### 2. Colecciones de Trazabilidad (Sistema ECOCE)

#### `trazabilidad_config/`
**Propósito:** Configuración de APK de trazabilidad
**Usado por:** Sistema ECOCE (descarga de APK) ✅
**Mantener:** ✅ SÍ (NO TOCAR - usado por web ECOCE)

**Admins especiales:**
- maestro@bioway.com.mx (full access)
- maestro@ecoce.mx (solo update downloadCount)

#### `trazabilidad_admin/`
**Propósito:** Datos de administradores de trazabilidad
**Usado por:** Sistema ECOCE ✅
**Mantener:** ✅ SÍ (NO TOCAR)

#### `trazabilidad_users/`
**Propósito:** Usuarios del sistema de trazabilidad
**Usado por:** Sistema ECOCE ✅
**Mantener:** ✅ SÍ (NO TOCAR)

**Subcollections:**
- `comments/` - Comentarios de trazabilidad
- `comment_drafts/` - Borradores
- `drafts/` - Borradores legacy

#### `trazabilidad_stats/`
**Propósito:** Estadísticas de trazabilidad
**Usado por:** Sistema ECOCE ✅
**Mantener:** ✅ SÍ (NO TOCAR)

#### `feature_requests/`
**Propósito:** Solicitudes de funcionalidades de ECOCE
**Usado por:** Sistema ECOCE ✅
**Mantener:** ✅ SÍ (NO TOCAR)

---

## 📁 STORAGE ACTUAL

### Carpetas Existentes:

#### `/apk/`
**Propósito:** APKs de trazabilidad para descarga
**Usado por:** Sistema ECOCE ✅
**Mantener:** ✅ SÍ (NO TOCAR)

**Reglas:**
- Read: maestro@bioway.com.mx y maestro@ecoce.mx
- Write: Solo maestro@bioway.com.mx

#### `/trazabilidad/comments/{userId}/`
**Propósito:** Imágenes de comentarios (evidencias)
**Usado por:** Sistema ECOCE ✅
**Mantener:** ✅ SÍ (NO TOCAR)

**Reglas:**
- Read: Admins
- Write: Usuario propietario (ECOCE)
- Validación: Solo imágenes, máx 5MB

---

## 🗄️ REALTIME DATABASE ACTUAL

**Reglas:**
```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

⚠️ **ALERTA DE SEGURIDAD:** Completamente abierto (público)

**Estado:** 🔴 INSEGURO
**Acción Recomendada:**
- Si se usa: Agregar autenticación
- Si NO se usa: Dejar como está o cerrar

---

## ✅ COLECCIONES PARA BIOWAY ANDROID

### Colecciones que DEBE usar la app Android:

1. ✅ **UsersInAct/** - COMPARTIDA con web
   - Leer/escribir usuarios existentes
   - Agregar nuevos usuarios de Android

2. ✅ **Recolectores/** - COMPARTIDA con web
   - Leer recolectores disponibles

3. ✅ **CentrosDeAcopio/** - COMPARTIDA con web
   - Leer centros disponibles

4. ✅ **Reciclables/** - COMPARTIDA con web (solo lectura)
   - Leer catálogo de materiales

5. ✅ **Horarios/** - COMPARTIDA con web (solo lectura)
   - Leer horarios de recolección

6. ⚠️ **companies/** - REVISAR SI SE USA
   - Si no se usa en web, podemos reutilizar para comercio local Android

7. ⚠️ **Config/** - COMPARTIDA (solo lectura)
   - Configuraciones globales

8. ⚠️ **sessions/** - COMPARTIDA
   - Sesiones activas

### Colecciones NUEVAS que puede crear Android (sin afectar web):

1. ✅ **productos/** - Para comercio local (nuevo)
2. ✅ **logros/** - Sistema de gamificación (nuevo)
3. ✅ **biocoins_transacciones/** - Historial de BioCoins (nuevo)
4. ✅ **ranking/** - Ranking de usuarios (nuevo)

---

## ⚠️ ÁREAS DE RIESGO (NO TOCAR)

### 🔴 NO MODIFICAR - Sistema ECOCE:
- `trazabilidad_*` (todas las colecciones)
- `/apk/` (Storage)
- `/trazabilidad/` (Storage)
- feature_requests

### 🟡 COMPARTIDAS - Modificar con Cuidado:
- `UsersInAct/`
- `Recolectores/`
- `CentrosDeAcopio/`
- `Horarios/`
- `Config/`
- `sessions/`

### 🟢 SEGURAS PARA ANDROID:
- Nuevas colecciones que creemos
- Subcollections de usuarios específicos de Android

---

## 🔧 RECOMENDACIONES PARA LIMPIEZA

### 1. Realtime Database
**Problema:** Completamente abierto (inseguro)

**Opción A:** Si NO se usa
```json
{
  "rules": {
    ".read": false,
    ".write": false
  }
}
```

**Opción B:** Si SÍ se usa
```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null"
  }
}
```

### 2. Colecciones a Revisar

**companies/**
- ¿Se usa en la web actual?
- Si NO: Podemos eliminar o reutilizar para comercio local
- Si SÍ: Compartir con web

**sessions/**
- ¿Qué guarda? ¿Sesiones de qué?
- Posiblemente legacy, revisar si se puede eliminar

### 3. Estructura Sugerida para Android

**Opción A: Compartir base de datos con web**
- Pro: Datos sincronizados
- Contra: Más complejo, puede romper web

**Opción B: Colecciones separadas para Android**
- Pro: No afecta web existente
- Contra: Datos duplicados si son los mismos usuarios

**Recomendación:** Usar **UsersInAct/** compartida pero agregar campo `platform: "android"` para distinguir

---

## 📋 PRÓXIMOS PASOS

### Fase 1: Investigación (Ahora)
- [ ] Ver datos en Firestore Console
- [ ] Identificar colecciones que usa la web actualmente
- [ ] Verificar si Realtime Database se está usando
- [ ] Revisar qué hay en companies/

### Fase 2: Plan de Limpieza
- [ ] Decidir qué colecciones eliminar
- [ ] Decidir qué colecciones compartir
- [ ] Planificar estructura nueva para Android

### Fase 3: Ejecución
- [ ] Backup de la base de datos
- [ ] Limpiar colecciones no usadas
- [ ] Actualizar reglas si es necesario
- [ ] Crear colecciones nuevas para Android

---

## 🎯 ACCIONES INMEDIATAS

**Para revisar en Firebase Console:**

1. **Firestore Database:**
   ```
   https://console.firebase.google.com/project/software-4e6b6/firestore/databases/-default-/data
   ```
   Verificar:
   - ¿Cuántos documentos hay en cada colección?
   - ¿Cuáles tienen datos recientes?
   - ¿companies/ se está usando?

2. **Realtime Database:**
   ```
   https://console.firebase.google.com/project/software-4e6b6/database
   ```
   Verificar:
   - ¿Hay datos?
   - ¿Se está usando?

3. **Storage:**
   ```
   https://console.firebase.google.com/project/software-4e6b6/storage
   ```
   Verificar:
   - Tamaño de /apk/
   - Tamaño de /trazabilidad/

---

**Archivos de reglas creados:**
- ✅ `firestore.rules` - Reglas de Firestore
- ✅ `storage.rules` - Reglas de Storage
- ✅ `database.rules.json` - Reglas de Realtime Database

**Listos para modificar localmente y desplegar con:**
```bash
firebase deploy --only firestore:rules
firebase deploy --only storage:rules
firebase deploy --only database:rules
```

