# 🧹 PLAN DE LIMPIEZA DE FIREBASE

**Proyecto:** software-4e6b6
**Fecha:** 26 de Noviembre de 2025

---

## ⚠️ USUARIOS A MANTENER (NO ELIMINAR)

**Emails críticos que NO se deben tocar:**
- `maestro@bioway.com.mx`
- `maestro@ecoce.mx`

---

## 📋 COLECCIONES A LIMPIAR

### 1. UsersInAct/

**Objetivo:** Mantener solo usuarios relevantes

**MANTENER:**
- Documentos con email: maestro@bioway.com.mx
- Documentos con email: maestro@ecoce.mx
- (Opcional) Últimos 10 usuarios más recientes para testing

**ELIMINAR:**
- Usuarios de prueba antiguos
- Usuarios inactivos sin reciclaje

**Comando sugerido (EJEMPLO - NO EJECUTAR AÚN):**
```bash
# Listar todos los usuarios primero
firebase firestore:get UsersInAct --limit 100

# Eliminar un usuario específico (ejemplo)
# firebase firestore:delete "UsersInAct/userId123" --force
```

---

### 2. Recolectores/

**Objetivo:** Mantener solo recolectores activos

**MANTENER:**
- Recolectores con email maestro@*
- Recolectores con recolecciones recientes
- Recolectores verificados

**ELIMINAR:**
- Recolectores de prueba
- Recolectores sin datos completos

---

### 3. CentrosDeAcopio/

**Objetivo:** Mantener solo centros reales

**MANTENER:**
- Centros con ubicación válida
- Centros con datos completos
- Centros activos

**ELIMINAR:**
- Centros de prueba
- Centros duplicados
- Centros sin ubicación

---

### 4. Horarios/

**Objetivo:** Mantener solo horarios actuales

**MANTENER:**
- Horarios vigentes
- Horarios de zonas activas

**ELIMINAR:**
- Horarios antiguos
- Horarios de prueba
- Horarios de zonas no cubiertas

---

## ⚠️ IMPORTANTE - NO TOCAR

### Colecciones Protegidas:

**🔴 Sistema ECOCE - NO ELIMINAR NADA:**
- `trazabilidad_config/`
- `trazabilidad_admin/`
- `trazabilidad_users/` (y sus subcollections)
- `trazabilidad_stats/`
- `feature_requests/`

**🔴 Storage ECOCE - NO ELIMINAR:**
- `/apk/`
- `/trazabilidad/comments/`

**🟡 Configuración - REVISAR ANTES:**
- `Config/` - Solo lectura, verificar antes
- `Reciclables/` - Catálogo, solo lectura

---

## 🛠️ SCRIPT DE LIMPIEZA SEGURA

### Opción A: Limpieza Manual (Recomendada)

**Pasos:**
1. Ve a Firebase Console
2. Abre Firestore Database
3. Filtra manualmente por colección
4. Revisa cada documento antes de eliminar
5. Elimina solo documentos de prueba obvios

**URL:**
```
https://console.firebase.google.com/project/software-4e6b6/firestore/databases/-default-/data
```

### Opción B: Limpieza con Firebase CLI

**Ver documentos de una colección:**
```bash
firebase firestore:get UsersInAct --limit 20
```

**Eliminar documento específico:**
```bash
firebase firestore:delete "UsersInAct/documentId" --force
```

**⚠️ PRECAUCIÓN:** CLI de Firebase no permite listar fácilmente. Mejor usar Console.

### Opción C: Script de Python con Firebase Admin SDK

Puedo crear un script de Python que:
1. Lista todas las colecciones
2. Filtra documentos por criterios
3. Muestra qué se eliminaría
4. Pide confirmación antes de eliminar

**Requiere:**
- Credenciales de servicio de Firebase (JSON)
- Python con firebase-admin

---

## 📊 ESTRATEGIA RECOMENDADA

### Fase 1: Análisis (Manual en Console)

**Para cada colección:**
1. Abrir en Firebase Console
2. Ordenar por fecha de creación/actualización
3. Identificar documentos obsoletos
4. Documentar qué mantener

### Fase 2: Backup

```bash
# Exportar Firestore completo (por seguridad)
# gcloud firestore export gs://software-4e6b6.appspot.com/backups/$(date +%Y%m%d)
```

### Fase 3: Limpieza Progresiva

**Orden sugerido:**
1. Eliminar 1-2 documentos de prueba obvios
2. Verificar que web sigue funcionando
3. Continuar con limpieza gradual
4. Monitorear errores

### Fase 4: Optimización

Después de limpiar:
1. Actualizar índices si es necesario
2. Optimizar reglas
3. Agregar reglas para app Android

---

## 🎯 CRITERIOS DE ELIMINACIÓN

### UsersInAct/

**ELIMINAR si:**
- Email contiene "test", "prueba", "demo"
- Sin reciclaje en últimos 6 meses
- Datos incompletos (sin nombre, sin email)
- Creado hace más de 1 año sin actividad

**MANTENER si:**
- Email: maestro@bioway.com.mx o maestro@ecoce.mx
- Reciclaje reciente (últimos 3 meses)
- Datos completos y válidos

### Recolectores/

**ELIMINAR si:**
- isRecolector != "1"
- Sin recolecciones registradas
- Datos de prueba

**MANTENER si:**
- Email maestro@*
- Recolecciones activas
- Datos verificados

---

## 📝 CHECKLIST ANTES DE ELIMINAR

- [ ] Backup de Firestore realizado
- [ ] Verificado en console qué se va a eliminar
- [ ] Web sigue funcionando después de cada eliminación
- [ ] NO tocar nada de trazabilidad_*
- [ ] NO tocar storage de ECOCE
- [ ] Documentar qué se eliminó

---

## ⚡ COMANDOS ÚTILES

**Ver proyecto actual:**
```bash
firebase use
```

**Listar colecciones (requiere emulador):**
```bash
firebase emulators:start --only firestore
```

**Eliminar con recursión (PELIGROSO):**
```bash
firebase firestore:delete "UsersInAct/userId" --recursive --force
```

**Ver reglas actuales:**
```bash
cat firestore.rules
```

**Desplegar reglas:**
```bash
firebase deploy --only firestore:rules
```

---

**Recomendación Final:**

Para limpieza segura de una base en producción con web activa:
1. ✅ Hacer limpieza manual desde Firebase Console
2. ✅ Eliminar de 5-10 documentos a la vez
3. ✅ Verificar que web funciona después de cada batch
4. ✅ NO usar comandos masivos de eliminación
5. ✅ Mantener backups

¿Quieres que cree un script de Python para ayudarte a analizar qué hay en cada colección antes de eliminar?
