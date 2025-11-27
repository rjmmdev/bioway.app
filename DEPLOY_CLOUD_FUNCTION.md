# Desplegar Cloud Function - Eliminar Cuentas No Verificadas

## Instalación

```bash
cd functions
npm install
```

## Deploy

```bash
firebase deploy --only functions
```

## Función

**deleteUnverifiedAccounts** - Se ejecuta cada 24 horas y:
1. Busca usuarios con `emailVerificado: false`
2. Filtra los que tienen más de 10 días desde `fechaRegistro`
3. Elimina de Firestore y Firebase Auth

## Logs

```bash
firebase functions:log
```
