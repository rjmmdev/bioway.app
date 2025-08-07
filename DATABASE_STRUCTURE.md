# 📊 Estructura de Base de Datos - BioWay v2.0

## 🏢 Colección: `empresas`
```javascript
{
  id: "empresa_id",
  codigo: "SICIT2024", // Código único para registro
  nombre: "Sicit",
  tipo: "recoleccion_especializada",
  estado: "activo",
  
  // Configuración de materiales
  materiales: [
    {
      materialId: "raspa_cuero",
      nombre: "Raspa de Cuero",
      activo: true,
      requiereAutorizacion: true
    }
  ],
  
  // Zonas de operación
  zonasOperacion: {
    tipo: "ilimitado" | "estados" | "municipios" | "codigos_postales",
    estados: ["Aguascalientes", "Jalisco"],
    municipios: ["Aguascalientes", "Zapopan"],
    codigosPostales: ["20000", "45000"],
    rangoKm: null // null = sin límite, número = km de radio
  },
  
  // Configuración de visibilidad
  configuracion: {
    recolectoresVenTodo: true, // true para Sicit
    brindadoresRequierenCodigo: true,
    mostrarEnListaPublica: false
  },
  
  // Estadísticas
  estadisticas: {
    totalRecolectores: 0,
    totalBrindadores: 0,
    totalRecolecciones: 0,
    kgRecolectados: 0
  },
  
  fechaCreacion: timestamp,
  fechaActualizacion: timestamp
}
```

## 👤 Colección: `usuarios` (Actualizada)
```javascript
{
  uid: "user_id",
  email: "usuario@email.com",
  nombre: "Juan Pérez",
  tipoUsuario: "brindador" | "recolector",
  
  // Asociación empresarial
  empresa: {
    id: "empresa_id" | null, // null = usuario BioWay general
    codigo: "SICIT2024" | null,
    nombre: "Sicit" | null
  },
  
  // Para Brindadores
  brindador: {
    direccion: {
      calle: "string",
      numeroExterior: "string",
      codigoPostal: "20000",
      estado: "Aguascalientes",
      municipio: "Aguascalientes",
      colonia: "Centro",
      coordenadas: {
        lat: 21.8853,
        lng: -102.2916
      }
    },
    
    // Materiales habilitados para brindar
    materialesHabilitados: [
      {
        materialId: "pet_tipo1",
        habilitado: true,
        origen: "general" // "general" | "empresa"
      },
      {
        materialId: "raspa_cuero",
        habilitado: true,
        origen: "empresa" // Habilitado por código de empresa
      }
    ],
    
    puntos: 0,
    nivel: 1
  },
  
  // Para Recolectores
  recolector: {
    codigoPostalBase: "20000", // CP donde opera principalmente
    
    // Configuración de visibilidad
    rangoVisibilidad: {
      tipo: "codigo_postal" | "kilometros" | "ilimitado",
      valorKm: 5, // Si tipo = "kilometros"
      codigosPostalesPermitidos: ["20000", "20010"], // Si tipo = "codigo_postal"
      estadosPermitidos: [], // Para empresas con permisos especiales
      municipiosPermitidos: []
    },
    
    // Materiales que puede recolectar
    materialesRecoleccion: [
      {
        materialId: "pet_tipo1",
        habilitado: true,
        origen: "general"
      },
      {
        materialId: "raspa_cuero",
        habilitado: true,
        origen: "empresa"
      }
    ],
    
    calificacion: 5.0,
    totalRecolecciones: 0
  },
  
  // Control de actividad
  actividad: {
    ultimoAcceso: timestamp,
    fechaRegistro: timestamp,
    diasConsecutivos: 0,
    advertenciaInactividad: false,
    fechaAdvertencia: null
  },
  
  activo: true,
  verificado: false,
  bloqueado: false,
  razonBloqueo: null
}
```

## ♻️ Colección: `materiales`
```javascript
{
  id: "material_id",
  codigo: "PET_TIPO1",
  nombre: "Plástico PET Tipo 1",
  categoria: "plasticos",
  
  // Configuración de disponibilidad
  disponibilidad: {
    tipo: "publico" | "empresarial" | "mixto",
    empresasAutorizadas: ["empresa_id"], // Si es empresarial o mixto
    requiereAutorizacion: false,
    habilitadoPorDefecto: true
  },
  
  // Información visual
  visual: {
    icono: "url_icono",
    imagen: "url_imagen",
    color: "#4CAF50",
    descripcion: "Botellas de agua y refrescos",
    instrucciones: "Lavar y quitar etiquetas"
  },
  
  // Valor y puntos
  economia: {
    puntosBase: 10,
    valorKg: 5.50,
    unidadMedida: "kg"
  },
  
  activo: true,
  fechaCreacion: timestamp
}
```

## 📅 Colección: `horarios_recoleccion`
```javascript
{
  id: "horario_id",
  zonaId: "zona_id", // Puede ser estado, municipio o CP
  tipoZona: "estado" | "municipio" | "codigo_postal",
  
  // Configuración por día
  calendario: [
    {
      dia: "lunes",
      activo: true,
      materiales: ["pet_tipo1", "carton", "papel"],
      horarios: [
        {
          inicio: "08:00",
          fin: "12:00",
          tipo: "manana"
        },
        {
          inicio: "14:00",
          fin: "18:00",
          tipo: "tarde"
        }
      ]
    },
    {
      dia: "martes",
      activo: true,
      materiales: ["vidrio", "metal"],
      horarios: [
        {
          inicio: "08:00",
          fin: "12:00",
          tipo: "manana"
        }
      ]
    }
    // ... resto de días
  ],
  
  // Excepciones y días especiales
  excepciones: [
    {
      fecha: "2024-12-25",
      razon: "Navidad",
      activo: false
    }
  ],
  
  empresaId: null, // null = horario general, string = horario de empresa
  activo: true,
  fechaCreacion: timestamp,
  fechaActualizacion: timestamp
}
```

## 🗺️ Colección: `zonas_habilitadas`
```javascript
{
  id: "zona_id",
  tipo: "estado" | "municipio" | "codigo_postal",
  nombre: "Aguascalientes",
  
  ubicacion: {
    estado: "Aguascalientes",
    municipio: "Aguascalientes", // Si aplica
    codigoPostal: "20000", // Si aplica
    coordenadasCentro: {
      lat: 21.8853,
      lng: -102.2916
    }
  },
  
  configuracion: {
    bioWayActivo: true,
    fechaActivacion: timestamp,
    fasePiloto: false,
    maxUsuarios: null // null = sin límite
  },
  
  estadisticas: {
    usuariosActivos: 0,
    brindadores: 0,
    recolectores: 0,
    kgReciclados: 0
  }
}
```

## 🎟️ Colección: `codigos_registro`
```javascript
{
  id: "codigo_id",
  codigo: "SICIT2024",
  empresaId: "empresa_id",
  
  configuracion: {
    tipo: "empresa" | "evento" | "promocion",
    usoMaximo: 100, // null = ilimitado
    usoActual: 5,
    fechaExpiracion: timestamp | null
  },
  
  beneficios: {
    materialesHabilitados: ["raspa_cuero"],
    puntosBonus: 100,
    nivelInicial: 2
  },
  
  activo: true,
  fechaCreacion: timestamp
}
```

## 📦 Colección: `solicitudes_recoleccion`
```javascript
{
  id: "solicitud_id",
  
  brindador: {
    uid: "user_id",
    nombre: "María García",
    direccion: {...},
    empresaId: "empresa_id" | null
  },
  
  recolector: {
    uid: "user_id" | null, // null = no asignado
    nombre: "Pedro López",
    empresaId: "empresa_id" | null
  },
  
  materiales: [
    {
      materialId: "pet_tipo1",
      cantidad: 5,
      unidad: "kg",
      descripcion: "Botellas limpias"
    }
  ],
  
  estado: "pendiente" | "aceptada" | "en_camino" | "completada" | "cancelada",
  
  ubicacion: {
    direccion: "Calle 123",
    coordenadas: {
      lat: 21.8853,
      lng: -102.2916
    },
    referencias: "Casa verde con portón negro"
  },
  
  horario: {
    fecha: "2024-12-15",
    ventana: "08:00-12:00"
  },
  
  tracking: {
    fechaCreacion: timestamp,
    fechaAceptacion: timestamp,
    fechaRecoleccion: timestamp,
    fechaCompletado: timestamp
  },
  
  calificacion: {
    brindadorCalifica: 5,
    recolectorCalifica: 5,
    comentarios: "Excelente servicio"
  }
}
```

## 🚫 Colección: `palabras_prohibidas`
```javascript
{
  id: "palabra_id",
  palabra: "obscenidad",
  tipo: "obscena" | "ofensiva" | "spam",
  severidad: "alta" | "media" | "baja",
  activo: true
}
```

## 📊 Colección: `analytics`
```javascript
{
  id: "analytics_id",
  tipo: "diario" | "semanal" | "mensual",
  fecha: "2024-12-01",
  
  metricas: {
    usuarios: {
      nuevosRegistros: 50,
      activosHoy: 200,
      inactivos30Dias: 10,
      eliminadosPorInactividad: 2
    },
    
    recolecciones: {
      total: 150,
      completadas: 140,
      canceladas: 10,
      kgTotales: 500
    },
    
    porMaterial: [
      {
        materialId: "pet_tipo1",
        cantidad: 200,
        unidad: "kg"
      }
    ],
    
    porEmpresa: [
      {
        empresaId: "sicit_id",
        recolecciones: 50,
        kgTotales: 100
      }
    ],
    
    porZona: [
      {
        zonaId: "aguascalientes",
        usuarios: 100,
        recolecciones: 75
      }
    ]
  }
}
```

## 🔄 Triggers y Funciones Cloud

### 1. **Limpieza automática de usuarios inactivos**
```javascript
// Cloud Function - Ejecutar diariamente
exports.limpiezaUsuariosInactivos = functions.pubsub
  .schedule('every 24 hours')
  .onRun(async (context) => {
    const tresMesesAtras = Date.now() - (90 * 24 * 60 * 60 * 1000);
    
    // Buscar usuarios inactivos
    const usuariosInactivos = await db.collection('usuarios')
      .where('actividad.ultimoAcceso', '<', tresMesesAtras)
      .where('activo', '==', true)
      .get();
    
    // Enviar advertencia a 60 días
    // Eliminar a 90 días
  });
```

### 2. **Validación de nombres**
```javascript
// Cloud Function - onCreate/onUpdate
exports.validarNombreUsuario = functions.firestore
  .document('usuarios/{userId}')
  .onCreate(async (snap, context) => {
    const userData = snap.data();
    const nombre = userData.nombre.toLowerCase();
    
    // Verificar contra palabras prohibidas
    const palabrasProhibidas = await db.collection('palabras_prohibidas')
      .where('activo', '==', true)
      .get();
    
    // Si contiene palabras prohibidas, bloquear cuenta
  });
```

## 🔐 Reglas de Seguridad Firestore

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Funciones auxiliares
    function isAuthenticated() {
      return request.auth != null;
    }
    
    function isMaestro() {
      return isAuthenticated() && 
        get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data.rol == 'maestro';
    }
    
    function isEmpresaAdmin(empresaId) {
      return isAuthenticated() && 
        get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data.empresa.id == empresaId &&
        get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data.rol == 'admin_empresa';
    }
    
    function belongsToEmpresa(empresaId) {
      return isAuthenticated() && 
        get(/databases/$(database)/documents/usuarios/$(request.auth.uid)).data.empresa.id == empresaId;
    }
    
    // Reglas para empresas
    match /empresas/{empresaId} {
      allow read: if isAuthenticated();
      allow write: if isMaestro();
    }
    
    // Reglas para usuarios
    match /usuarios/{userId} {
      allow read: if isAuthenticated() && 
        (request.auth.uid == userId || isMaestro() || 
         (resource.data.empresa.id != null && 
          belongsToEmpresa(resource.data.empresa.id)));
      allow create: if isAuthenticated();
      allow update: if request.auth.uid == userId || isMaestro();
      allow delete: if isMaestro();
    }
    
    // Reglas para materiales
    match /materiales/{materialId} {
      allow read: if isAuthenticated();
      allow write: if isMaestro();
    }
    
    // Reglas para horarios
    match /horarios_recoleccion/{horarioId} {
      allow read: if isAuthenticated();
      allow write: if isMaestro() || 
        (resource.data.empresaId != null && 
         isEmpresaAdmin(resource.data.empresaId));
    }
    
    // Reglas para solicitudes
    match /solicitudes_recoleccion/{solicitudId} {
      allow read: if isAuthenticated() && 
        (resource.data.brindador.uid == request.auth.uid ||
         resource.data.recolector.uid == request.auth.uid ||
         isMaestro());
      allow create: if isAuthenticated();
      allow update: if isAuthenticated() && 
        (resource.data.brindador.uid == request.auth.uid ||
         resource.data.recolector.uid == request.auth.uid);
    }
  }
}
```