# Guía de Implementación: Sistema de Materiales Reciclables

## Descripción General

Esta guía detalla cómo funciona el sistema de materiales reciclables en BioWay y cómo replicarlo en otras aplicaciones.

---

## 1. Base de Datos

### Firestore Collection
- **Nombre de colección**: `Reciclables`
- **Proyecto Firebase**: `software-4e6b6` (User App)
- **Permisos**: Solo lectura pública, no se puede escribir desde frontend

```javascript
// firestore.main.rules:19-23
match /Reciclables/{document=**} {
  allow read: if true;
  allow write: if false;
}
```

---

## 2. Estructura de Datos

### Documento de Material Reciclable

Cada material en Firestore tiene la siguiente estructura:

```javascript
{
  // ID del documento (ejemplos: "plastico", "papel", "vidrio", "carton", etc.)
  id: "plastico",

  // Información básica (para tarjetas)
  nombre: "Plástico",
  info: "Breve descripción que aparece en la tarjeta del material",
  icon: "<svg xmlns='http://www.w3.org/2000/svg'>...</svg>", // SVG completo como string

  // Información detallada (para modal)
  detailedInfo: {
    // Descripción completa
    descripcion: "Texto descriptivo completo sobre el material, su importancia y características",

    // Categorías de materiales
    categorias: {
      // Materiales SÍ reciclables
      reciclables: {
        icono: "✅",
        lista: [
          { nombre: "Botellas de PET" },
          { nombre: "Envases de plástico limpio" },
          { nombre: "Tapas de plástico" }
        ]
      },

      // Materiales NO reciclables
      no_reciclables: {
        icono: "❌",
        lista: [
          "Plásticos contaminados con comida",
          "Unicel sucio",
          "Bolsas biodegradables"
        ]
      }
    },

    // Consejos de manejo
    consejos: {
      icono: "💡",
      lista: [
        "Enjuagar los envases antes de reciclar",
        "Quitar etiquetas si es posible",
        "Aplastar botellas para ahorrar espacio"
      ]
    }
  }
}
```

### Notas sobre la Estructura
- **`lista` en reciclables**: Array de objetos con propiedad `nombre`
- **`lista` en no_reciclables**: Array de strings directamente
- **`lista` en consejos**: Array de strings directamente
- **`icon`**: SVG completo como string (se convierte a WebP en el frontend)

---

## 3. Categorías de Materiales

Las categorías están hardcodeadas en el frontend (`materiales.js:416-421`):

```javascript
const categoryMaterials = {
  'Plásticos y Derivados': ['plastico', 'unicel'],
  'Papel y Cartón': ['papel', 'carton'],
  'Metales y Vidrio': ['aluminio', 'vidrio'],
  'Residuos Especiales': ['residuoPeligroso', 'aceite', 'raspa']
};
```

### Materiales Existentes
1. **plastico** - Plásticos en general
2. **unicel** - Poliestireno expandido
3. **papel** - Papel y documentos
4. **carton** - Cartón y cajas
5. **aluminio** - Latas y envases de aluminio
6. **vidrio** - Botellas y frascos de vidrio
7. **residuoPeligroso** - Residuos peligrosos
8. **aceite** - Aceites usados
9. **raspa** - Residuos orgánicos especiales

---

## 4. Implementación en el Frontend

### 4.1 Archivo Principal
**Ubicación**: `public_html/js/materiales.js`

### 4.2 Inicialización

```javascript
import { collection, getDocs } from "firebase/firestore";
import { initializeFirebase } from './firebase-config.js';

let userFirestore;

async function initFirestore() {
  const { userFirestore: firestore } = await initializeFirebase();
  userFirestore = firestore;
  return userFirestore;
}
```

### 4.3 Cargar Materiales desde Firestore

```javascript
async function loadRecyclableMaterials() {
  // 1. Inicializar Firestore
  if (!userFirestore) {
    await initFirestore();
  }

  // 2. Obtener la colección
  const materialsRef = collection(userFirestore, "Reciclables");
  const materialsSnapshot = await getDocs(materialsRef);

  // 3. Verificar que hay datos
  if (materialsSnapshot.empty) {
    console.error('No se encontraron materiales');
    return;
  }

  // 4. Procesar documentos
  materialsSnapshot.docs.forEach(doc => {
    const materialData = { id: doc.id, ...doc.data() };
    // Usar materialData...
  });
}
```

### 4.4 Estructura HTML

#### Contenedor de Materiales
```html
<section id="guia-reciclaje" class="recycling-guide">
  <div class="container">
    <div class="section-header">
      <span class="section-tag">Guía de Materiales</span>
      <h2>¿Qué puedo reciclar?</h2>
      <p class="section-subtitle">Descubre cómo separar y brindar correctamente tus residuos</p>
    </div>

    <!-- Aquí se cargan dinámicamente los materiales -->
    <div class="materials-grid">
      <div class="loading-spinner">
        <div class="spinner"></div>
        <p>Cargando materiales...</p>
      </div>
    </div>
  </div>
</section>
```

#### Estructura Generada Dinámicamente
```html
<div class="materials-grid">
  <!-- Por cada categoría -->
  <div class="material-category">
    <h3>Plásticos y Derivados</h3>
    <div class="material-items">

      <!-- Por cada material -->
      <div class="material-card">
        <div class="material-icon">
          <img src="blob:..." alt="Plástico">
        </div>
        <div>
          <h4>Plástico</h4>
          <p>Breve descripción del material</p>
        </div>
      </div>

    </div>
  </div>
</div>
```

### 4.5 Crear Tarjetas de Materiales

```javascript
// Por cada categoría
for (const [categoryName, materialsList] of Object.entries(categoryMaterials)) {
  const categoryContainer = document.createElement('div');
  categoryContainer.className = 'material-category';
  categoryContainer.innerHTML = `<h3>${categoryName}</h3>`;

  const materialItems = document.createElement('div');
  materialItems.className = 'material-items';

  // Filtrar materiales de esta categoría
  const validMaterials = materialsList
    .map(materialId => {
      const doc = materialsSnapshot.docs.find(d =>
        d.id.toLowerCase() === materialId.toLowerCase()
      );
      return doc ? { id: doc.id, ...doc.data() } : null;
    })
    .filter(Boolean);

  // Crear tarjeta por cada material
  for (const materialData of validMaterials) {
    const materialCard = document.createElement('div');
    materialCard.className = 'material-card';

    // Convertir SVG a WebP y crear imagen
    const iconContainer = document.createElement('div');
    iconContainer.className = 'material-icon';
    const webpUrl = await svgToWebP(materialData.icon);
    iconContainer.innerHTML = `<img src="${webpUrl}" alt="${materialData.nombre}">`;

    // Agregar contenido
    const contentDiv = document.createElement('div');
    contentDiv.innerHTML = `
      <h4>${materialData.nombre}</h4>
      <p>${materialData.info}</p>
    `;

    materialCard.appendChild(iconContainer);
    materialCard.appendChild(contentDiv);

    // Evento click para abrir modal
    materialCard.addEventListener('click', () => showMaterialInfo(materialData));

    materialItems.appendChild(materialCard);
  }

  categoryContainer.appendChild(materialItems);
  materialsContainer.appendChild(categoryContainer);
}
```

### 4.6 Optimización: Convertir SVG a WebP

```javascript
async function svgToWebP(svgString, width = 60, height = 60) {
  return new Promise((resolve, reject) => {
    const canvas = document.createElement('canvas');
    const ctx = canvas.getContext('2d');
    canvas.width = width;
    canvas.height = height;

    const img = new Image();
    img.onload = () => {
      ctx.drawImage(img, 0, 0, width, height);
      canvas.toBlob((blob) => {
        if (blob) {
          resolve(URL.createObjectURL(blob));
        } else {
          reject(new Error('Error converting to WebP'));
        }
      }, 'image/webp');
    };

    img.onerror = () => reject(new Error('Error loading SVG'));

    const svgBlob = new Blob([svgString], {type: 'image/svg+xml'});
    img.src = URL.createObjectURL(svgBlob);
  });
}
```

**Beneficios**:
- Reduce tamaño de archivo
- Mejor rendimiento
- Compatible con todos los navegadores modernos

---

## 5. Modal de Detalles

### 5.1 Estructura del Modal

```html
<div class="material-modal">
  <div class="modal-content">
    <button class="modal-close">&times;</button>

    <div class="modal-header">
      <div class="modal-icon">
        <!-- Imagen del material -->
      </div>
      <h3>Nombre del Material</h3>
    </div>

    <div class="modal-body">
      <!-- Descripción -->
      <div class="modal-section description-section">
        <p class="description"></p>
      </div>

      <!-- Materiales Reciclables -->
      <div class="modal-section reciclables-section">
        <h4>Materiales Reciclables ✅</h4>
        <ul class="info-list reciclables-list"></ul>
      </div>

      <!-- Materiales NO Reciclables -->
      <div class="modal-section no-reciclables-section">
        <h4>Materiales No Reciclables ❌</h4>
        <ul class="info-list no-reciclables-list"></ul>
      </div>

      <!-- Consejos -->
      <div class="modal-section consejos-section">
        <h4>Consejos 💡</h4>
        <ul class="info-list consejos-list"></ul>
      </div>
    </div>
  </div>
</div>
```

### 5.2 Función para Mostrar Modal

```javascript
function showMaterialInfo(material) {
  const modalEl = document.querySelector('.material-modal');

  // 1. Encabezado
  modalEl.querySelector('.modal-header h3').textContent = material.nombre;
  modalEl.querySelector('.modal-icon').innerHTML =
    `<img src="${materialWebPMap.get(material.id)}" alt="${material.nombre}">`;

  // 2. Descripción
  const description = modalEl.querySelector('.description');
  description.textContent = material.detailedInfo?.descripcion || '';
  description.closest('.modal-section').style.display =
    description.textContent ? 'block' : 'none';

  // 3. Materiales Reciclables
  const reciclablesList = modalEl.querySelector('.reciclables-list');
  reciclablesList.innerHTML = '';

  if (material.detailedInfo?.categorias?.reciclables?.lista?.length) {
    const icon = material.detailedInfo.categorias.reciclables.icono || '';
    modalEl.querySelector('.reciclables-section h4').textContent =
      `Materiales Reciclables ${icon}`;

    material.detailedInfo.categorias.reciclables.lista.forEach(item => {
      const li = document.createElement('li');
      li.textContent = item.nombre; // Nota: es un objeto con propiedad 'nombre'
      reciclablesList.appendChild(li);
    });
  }

  // 4. Materiales NO Reciclables
  const noReciclablesList = modalEl.querySelector('.no-reciclables-list');
  noReciclablesList.innerHTML = '';

  if (material.detailedInfo?.categorias?.no_reciclables?.lista?.length) {
    const icon = material.detailedInfo.categorias.no_reciclables.icono || '';
    modalEl.querySelector('.no-reciclables-section h4').textContent =
      `Materiales No Reciclables ${icon}`;

    material.detailedInfo.categorias.no_reciclables.lista.forEach(item => {
      const li = document.createElement('li');
      li.textContent = item; // Nota: es string directo
      noReciclablesList.appendChild(li);
    });
  }

  // 5. Consejos
  const consejosList = modalEl.querySelector('.consejos-list');
  consejosList.innerHTML = '';

  if (material.detailedInfo?.consejos?.lista?.length) {
    const icon = material.detailedInfo.consejos.icono || '';
    modalEl.querySelector('.consejos-section h4').textContent =
      `Consejos ${icon}`;

    material.detailedInfo.consejos.lista.forEach(tip => {
      const li = document.createElement('li');
      li.textContent = tip; // Nota: es string directo
      consejosList.appendChild(li);
    });
  }

  // 6. Mostrar modal
  modalEl.classList.add('active');
  document.body.style.overflow = 'hidden';
}
```

### 5.3 Event Listeners del Modal

```javascript
const modal = document.querySelector('.material-modal');

// Cerrar con botón X
modal.querySelector('.modal-close').addEventListener('click', () => {
  modal.classList.remove('active');
  document.body.style.overflow = '';
});

// Cerrar al hacer click fuera del contenido
modal.addEventListener('click', (e) => {
  if (e.target === modal) {
    modal.classList.remove('active');
    document.body.style.overflow = '';
  }
});
```

---

## 6. Integración en Página HTML

### 6.1 Imports en Script Module

```html
<script type="module">
import { initializeFirebase } from './js/firebase-config.js';
import { loadRecyclableMaterials } from './js/materiales.js';

async function init() {
  try {
    // Inicializar Firebase
    await initializeFirebase();

    // Cargar materiales
    await loadRecyclableMaterials();
  } catch (error) {
    console.error("Error en la inicialización:", error);
  }
}

// Ejecutar cuando el DOM esté listo
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  init();
}
</script>
```

---

## 7. Estilos CSS Importantes

### 7.1 Grid de Materiales

```css
.materials-grid {
  display: flex;
  flex-direction: column;
  gap: 3rem;
}

.material-category h3 {
  font-size: 1.5rem;
  margin-bottom: 1.5rem;
  color: var(--primary-green);
}

.material-items {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 1.5rem;
}
```

### 7.2 Tarjetas de Materiales

```css
.material-card {
  background: white;
  border-radius: 12px;
  padding: 1.5rem;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.material-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.material-icon {
  width: 60px;
  height: 60px;
  margin: 0 auto 1rem;
  border-radius: 8px;
  overflow: hidden;
}

.material-icon img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
```

### 7.3 Modal

```css
.material-modal {
  display: none;
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 1000;
  overflow-y: scroll;
}

.material-modal.active {
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 2rem 0;
}

.modal-content {
  background-color: white;
  padding: 2rem;
  border-radius: 12px;
  max-width: 500px;
  width: 90%;
  position: relative;
}

.reciclables-section {
  background: rgba(46, 204, 113, 0.1);
  border: 1px solid rgba(46, 204, 113, 0.2);
  border-radius: 12px;
  padding: 1.5rem;
}

.no-reciclables-section {
  background: rgba(231, 76, 60, 0.1);
  border: 1px solid rgba(231, 76, 60, 0.2);
  border-radius: 12px;
  padding: 1.5rem;
}
```

---

## 8. Flujo Completo de Datos

```
1. Usuario carga la página
   ↓
2. initializeFirebase() conecta a Firebase
   ↓
3. loadRecyclableMaterials() consulta Firestore
   ↓
4. getDocs(collection(userFirestore, "Reciclables"))
   ↓
5. Se organizan materiales por categorías
   ↓
6. Se convierten íconos SVG a WebP
   ↓
7. Se crean tarjetas HTML dinámicamente
   ↓
8. Usuario hace click en tarjeta
   ↓
9. showMaterialInfo(material) abre modal
   ↓
10. Modal muestra:
    - Descripción
    - Materiales reciclables (verde)
    - Materiales NO reciclables (rojo)
    - Consejos
```

---

## 9. Puntos Importantes a Considerar

### 9.1 Diferencias en Estructura de Listas

⚠️ **IMPORTANTE**: Las listas tienen estructuras diferentes:

```javascript
// reciclables.lista → Array de OBJETOS
categorias.reciclables.lista = [
  { nombre: "Botellas de PET" },
  { nombre: "Envases" }
]

// no_reciclables.lista → Array de STRINGS
categorias.no_reciclables.lista = [
  "Plásticos sucios",
  "Unicel contaminado"
]

// consejos.lista → Array de STRINGS
consejos.lista = [
  "Enjuagar antes de reciclar",
  "Quitar etiquetas"
]
```

### 9.2 Manejo de Duplicados

```javascript
// Se usa un tracker para evitar procesar el mismo material dos veces
const processedMaterialsTracker = {
  processedIds: new Set(),
  hasBeenProcessed(id) {
    return this.processedIds.has(id.toLowerCase().trim());
  },
  markAsProcessed(id) {
    this.processedIds.add(id.toLowerCase().trim());
  }
};
```

### 9.3 Secciones Condicionales

Las secciones del modal solo se muestran si tienen contenido:

```javascript
// Si no hay descripción, ocultar sección
description.closest('.modal-section').style.display =
  description.textContent ? 'block' : 'none';
```

### 9.4 Performance

- Los íconos SVG se convierten a WebP una sola vez
- Se almacenan en un Map para reutilizarlos
- El modal se crea una sola vez en el DOM
- Los estilos se agregan una sola vez al `<head>`

---

## 10. Ejemplo de Uso en React Native / Mobile

### 10.1 Consulta desde Firebase

```javascript
import firestore from '@react-native-firebase/firestore';

async function getMaterials() {
  try {
    const materialsSnapshot = await firestore()
      .collection('Reciclables')
      .get();

    const materials = materialsSnapshot.docs.map(doc => ({
      id: doc.id,
      ...doc.data()
    }));

    return materials;
  } catch (error) {
    console.error('Error fetching materials:', error);
    return [];
  }
}
```

### 10.2 Organizar por Categorías

```javascript
const categoryMaterials = {
  'Plásticos y Derivados': ['plastico', 'unicel'],
  'Papel y Cartón': ['papel', 'carton'],
  'Metales y Vidrio': ['aluminio', 'vidrio'],
  'Residuos Especiales': ['residuoPeligroso', 'aceite', 'raspa']
};

function organizeMaterialsByCategory(materials) {
  const organized = {};

  for (const [categoryName, materialIds] of Object.entries(categoryMaterials)) {
    organized[categoryName] = materialIds
      .map(id => materials.find(m => m.id.toLowerCase() === id.toLowerCase()))
      .filter(Boolean);
  }

  return organized;
}
```

### 10.3 Renderizar SVG en React Native

```javascript
import { SvgXml } from 'react-native-svg';

function MaterialIcon({ svgString }) {
  return (
    <SvgXml
      xml={svgString}
      width={60}
      height={60}
    />
  );
}
```

---

## 11. Resumen de Archivos Involucrados

| Archivo | Ubicación | Propósito |
|---------|-----------|-----------|
| `biowayapp.html` | `public_html/` | Página principal con estructura HTML |
| `materiales.js` | `public_html/js/` | Lógica para cargar y mostrar materiales |
| `firebase-config.js` | `public_html/js/` | Configuración de Firebase |
| `firestore.main.rules` | Raíz del proyecto | Reglas de seguridad de Firestore |
| `style-biowayapp.css` | `public_html/src/` | Estilos (asumido) |

---

## 12. Checklist de Implementación

- [ ] Configurar acceso a Firebase
- [ ] Crear función para inicializar Firestore
- [ ] Consultar colección `Reciclables`
- [ ] Organizar materiales por categorías
- [ ] Crear estructura HTML para grid de materiales
- [ ] Implementar conversión SVG a WebP (opcional, para web)
- [ ] Crear tarjetas de materiales dinámicamente
- [ ] Implementar modal de detalles
- [ ] Manejar diferentes estructuras de listas (objetos vs strings)
- [ ] Agregar event listeners para abrir/cerrar modal
- [ ] Aplicar estilos CSS
- [ ] Manejar estados de carga y errores
- [ ] Prevenir procesamiento duplicado de materiales
- [ ] Optimizar rendimiento (cache, lazy loading, etc.)

---

## 13. Troubleshooting Común

### Error: "No se encontraron materiales"
- Verificar que la colección `Reciclables` existe en Firestore
- Verificar permisos de lectura en `firestore.rules`
- Verificar conexión a Firebase

### Los íconos no se muestran
- Verificar que el campo `icon` contiene SVG válido
- Revisar consola por errores de conversión
- Usar fallback: `<i class="fas fa-recycle"></i>`

### Modal no se cierra
- Verificar que los event listeners están correctamente asignados
- Revisar que la clase `active` se remueve
- Verificar que `document.body.style.overflow` se restaura

### Listas vacías en modal
- Verificar estructura de `detailedInfo`
- Para `reciclables.lista`: acceder a `item.nombre`
- Para `no_reciclables.lista` y `consejos.lista`: usar `item` directamente

---

## Conclusión

Este sistema proporciona una forma escalable y mantenible de mostrar información sobre materiales reciclables. La separación entre datos (Firestore) y presentación (frontend) permite actualizar el contenido sin modificar código, y la estructura modular facilita la reutilización en diferentes plataformas (web, mobile, etc.).
