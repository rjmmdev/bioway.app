import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:latlong2/latlong.dart';
import '../../../utils/colors.dart';
import '../../../models/bioway/material_reciclable.dart' as bioway_material;

class RecolectorMapaScreen extends StatefulWidget {
  const RecolectorMapaScreen({super.key});

  @override
  State<RecolectorMapaScreen> createState() => _RecolectorMapaScreenState();
}

class _RecolectorMapaScreenState extends State<RecolectorMapaScreen> {
  final List<bioway_material.MaterialReciclable> materiales = bioway_material.MaterialReciclable.materiales;
  final Set<String> selectedFilters = {};
  
  final MapController _mapController = MapController();
  
  // Posición inicial (Ciudad de México)
  final LatLng _initialPosition = const LatLng(19.4326, -99.1332);
  final double _initialZoom = 13.0;
  
  // Lista de puntos de recolección
  List<Map<String, dynamic>> puntosRecoleccion = [];
  
  @override
  void initState() {
    super.initState();
    _loadPuntosRecoleccion();
  }
  
  void _loadPuntosRecoleccion() {
    // Puntos de recolección simulados con IDs de materiales válidos - 25 puntos
    setState(() {
      puntosRecoleccion = [
        {
          'id': '1',
          'position': const LatLng(19.4326, -99.1332),
          'nombre': 'Centro de Acopio Norte',
          'direccion': 'Av. Insurgentes Norte 123',
          'materiales': ['pet_tipo1', 'papel', 'vidrio'],
          'cantidad': 45.5,
        },
        {
          'id': '2',
          'position': const LatLng(19.4280, -99.1380),
          'nombre': 'Punto Verde Polanco',
          'direccion': 'Horacio 234, Polanco',
          'materiales': ['hdpe', 'metal', 'carton'],
          'cantidad': 32.0,
        },
        {
          'id': '3',
          'position': const LatLng(19.4360, -99.1290),
          'nombre': 'Reciclaje Condesa',
          'direccion': 'Amsterdam 567, Condesa',
          'materiales': ['vidrio', 'papel', 'pet_tipo1'],
          'cantidad': 28.5,
        },
        {
          'id': '4',
          'position': const LatLng(19.4250, -99.1350),
          'nombre': 'EcoPunto Roma',
          'direccion': 'Álvaro Obregón 890, Roma Norte',
          'materiales': ['pet_tipo1', 'hdpe', 'metal'],
          'cantidad': 52.0,
        },
        {
          'id': '5',
          'position': const LatLng(19.4390, -99.1310),
          'nombre': 'Centro Comunitario Juárez',
          'direccion': 'Bucareli 345, Juárez',
          'materiales': ['carton', 'papel', 'vidrio'],
          'cantidad': 18.5,
        },
        {
          'id': '6',
          'position': const LatLng(19.4410, -99.1450),
          'nombre': 'Punto Limpio Anzures',
          'direccion': 'Mariano Escobedo 456, Anzures',
          'materiales': ['pet_tipo1', 'hdpe', 'papel'],
          'cantidad': 37.2,
        },
        {
          'id': '7',
          'position': const LatLng(19.4195, -99.1420),
          'nombre': 'Centro Verde Nápoles',
          'direccion': 'Dakota 789, Nápoles',
          'materiales': ['vidrio', 'metal', 'carton'],
          'cantidad': 41.8,
        },
        {
          'id': '8',
          'position': const LatLng(19.4450, -99.1280),
          'nombre': 'Recicladora San Rafael',
          'direccion': 'Ribera de San Cosme 234, San Rafael',
          'materiales': ['papel', 'carton', 'pet_tipo1'],
          'cantidad': 29.3,
        },
        {
          'id': '9',
          'position': const LatLng(19.4150, -99.1380),
          'nombre': 'Punto Ecológico Del Valle',
          'direccion': 'Félix Cuevas 567, Del Valle',
          'materiales': ['hdpe', 'vidrio', 'metal'],
          'cantidad': 55.7,
        },
        {
          'id': '10',
          'position': const LatLng(19.4380, -99.1520),
          'nombre': 'Centro de Acopio Tacubaya',
          'direccion': 'Parque Lira 890, Tacubaya',
          'materiales': ['pet_tipo1', 'papel', 'metal'],
          'cantidad': 33.9,
        },
        {
          'id': '11',
          'position': const LatLng(19.4220, -99.1250),
          'nombre': 'Reciclaje Doctores',
          'direccion': 'Dr. Lavista 123, Doctores',
          'materiales': ['carton', 'vidrio', 'hdpe'],
          'cantidad': 24.6,
        },
        {
          'id': '12',
          'position': const LatLng(19.4480, -99.1400),
          'nombre': 'Punto Verde Santa María',
          'direccion': 'Sabino 456, Santa María la Ribera',
          'materiales': ['papel', 'pet_tipo1', 'metal'],
          'cantidad': 47.3,
        },
        {
          'id': '13',
          'position': const LatLng(19.4100, -99.1450),
          'nombre': 'Centro Ecológico Coyoacán',
          'direccion': 'Miguel Ángel de Quevedo 789, Coyoacán',
          'materiales': ['vidrio', 'carton', 'papel'],
          'cantidad': 39.8,
        },
        {
          'id': '14',
          'position': const LatLng(19.4340, -99.1180),
          'nombre': 'Recicladora Obrera',
          'direccion': 'José T. Cuéllar 234, Obrera',
          'materiales': ['hdpe', 'pet_tipo1', 'metal'],
          'cantidad': 26.1,
        },
        {
          'id': '15',
          'position': const LatLng(19.4290, -99.1550),
          'nombre': 'Punto Limpio San Miguel',
          'direccion': 'Gobernador Melchor 567, San Miguel Chapultepec',
          'materiales': ['papel', 'vidrio', 'carton'],
          'cantidad': 51.4,
        },
        {
          'id': '16',
          'position': const LatLng(19.4420, -99.1350),
          'nombre': 'Centro Verde Guerrero',
          'direccion': 'Zarco 890, Guerrero',
          'materiales': ['metal', 'hdpe', 'pet_tipo1'],
          'cantidad': 35.7,
        },
        {
          'id': '17',
          'position': const LatLng(19.4170, -99.1480),
          'nombre': 'Reciclaje Escandón',
          'direccion': 'Patriotismo 123, Escandón',
          'materiales': ['carton', 'papel', 'vidrio'],
          'cantidad': 42.9,
        },
        {
          'id': '18',
          'position': const LatLng(19.4510, -99.1320),
          'nombre': 'Punto Ecológico Peralvillo',
          'direccion': 'Calzada de Guadalupe 456, Peralvillo',
          'materiales': ['pet_tipo1', 'metal', 'hdpe'],
          'cantidad': 28.3,
        },
        {
          'id': '19',
          'position': const LatLng(19.4050, -99.1400),
          'nombre': 'Centro de Acopio Portales',
          'direccion': 'Municipio Libre 789, Portales',
          'materiales': ['vidrio', 'papel', 'carton'],
          'cantidad': 36.5,
        },
        {
          'id': '20',
          'position': const LatLng(19.4370, -99.1600),
          'nombre': 'Recicladora Bosques',
          'direccion': 'Bosques de Duraznos 234, Bosques de las Lomas',
          'materiales': ['hdpe', 'pet_tipo1', 'papel'],
          'cantidad': 58.2,
        },
        {
          'id': '21',
          'position': const LatLng(19.4260, -99.1200),
          'nombre': 'Punto Verde Centro',
          'direccion': '5 de Mayo 567, Centro Histórico',
          'materiales': ['metal', 'vidrio', 'carton'],
          'cantidad': 44.7,
        },
        {
          'id': '22',
          'position': const LatLng(19.4460, -99.1480),
          'nombre': 'Centro Ecológico Cuauhtémoc',
          'direccion': 'Río Lerma 890, Cuauhtémoc',
          'materiales': ['papel', 'pet_tipo1', 'hdpe'],
          'cantidad': 31.9,
        },
        {
          'id': '23',
          'position': const LatLng(19.4120, -99.1320),
          'nombre': 'Reciclaje Benito Juárez',
          'direccion': 'División del Norte 123, Benito Juárez',
          'materiales': ['vidrio', 'metal', 'papel'],
          'cantidad': 40.3,
        },
        {
          'id': '24',
          'position': const LatLng(19.4400, -99.1230),
          'nombre': 'Punto Limpio Tabacalera',
          'direccion': 'Paseo de la Reforma 456, Tabacalera',
          'materiales': ['carton', 'hdpe', 'pet_tipo1'],
          'cantidad': 49.6,
        },
        {
          'id': '25',
          'position': const LatLng(19.4200, -99.1500),
          'nombre': 'Centro Verde Mixcoac',
          'direccion': 'Revolución 789, Mixcoac',
          'materiales': ['papel', 'vidrio', 'metal'],
          'cantidad': 34.8,
        },
      ];
    });
  }
  
  List<Map<String, dynamic>> get filteredPuntos {
    if (selectedFilters.isEmpty) return puntosRecoleccion;
    
    return puntosRecoleccion.where((punto) {
      final materialesPunto = List<String>.from(punto['materiales']);
      return materialesPunto.any((mat) => selectedFilters.contains(mat));
    }).toList();
  }
  
  void _showPuntoDetails(Map<String, dynamic> punto) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return DraggableScrollableSheet(
          initialChildSize: 0.75,
          minChildSize: 0.5,
          maxChildSize: 0.95,
          expand: false,
          builder: (context, scrollController) {
            return Container(
              padding: const EdgeInsets.all(24),
              child: SingleChildScrollView(
                controller: scrollController,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
              // Header con icono
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: BioWayColors.primaryGreen.withValues(alpha: 0.1),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      Icons.recycling,
                      color: BioWayColors.primaryGreen,
                      size: 28,
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          punto['nombre'],
                          style: const TextStyle(
                            fontSize: 22,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Row(
                          children: [
                            Icon(Icons.location_on, size: 18, color: Colors.grey[600]),
                            const SizedBox(width: 4),
                            Expanded(
                              child: Text(
                                punto['direccion'],
                                style: TextStyle(
                                  color: Colors.grey[600],
                                  fontSize: 16,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 24),
              
              // Cantidad disponible destacada
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: [
                      BioWayColors.success,
                      BioWayColors.success.withValues(alpha: 0.8),
                    ],
                  ),
                  borderRadius: BorderRadius.circular(16),
                ),
                child: Column(
                  children: [
                    Icon(
                      Icons.scale,
                      color: Colors.white,
                      size: 32,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '${punto['cantidad']} kg',
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 32,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    Text(
                      'Disponibles para recolectar',
                      style: TextStyle(
                        color: Colors.white.withValues(alpha: 0.9),
                        fontSize: 16,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              
              Text(
                'Materiales en este punto:',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w600,
                  color: BioWayColors.textDark,
                ),
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: (punto['materiales'] as List).map((matId) {
                  // Buscar el material de forma segura
                  final materialIndex = materiales.indexWhere((m) => m.id == matId);
                  
                  // Si no se encuentra el material, crear uno por defecto
                  if (materialIndex == -1) {
                    return Container(
                      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                      decoration: BoxDecoration(
                        color: BioWayColors.primaryGreen.withValues(alpha: 0.2),
                        borderRadius: BorderRadius.circular(20),
                        border: Border.all(
                          color: BioWayColors.primaryGreen.withValues(alpha: 0.5),
                        ),
                      ),
                      child: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          Icon(
                            _getMaterialIcon(matId),
                            color: BioWayColors.primaryGreen,
                            size: 20,
                          ),
                          const SizedBox(width: 6),
                          Text(
                            matId.toString().toUpperCase(),
                            style: TextStyle(
                              color: BioWayColors.primaryGreen,
                              fontWeight: FontWeight.w600,
                              fontSize: 16,
                            ),
                          ),
                        ],
                      ),
                    );
                  }
                  
                  final material = materiales[materialIndex];
                  return Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    decoration: BoxDecoration(
                      color: material.color.withValues(alpha: 0.2),
                      borderRadius: BorderRadius.circular(20),
                      border: Border.all(
                        color: material.color.withValues(alpha: 0.5),
                      ),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          _getMaterialIcon(matId),
                          color: material.color,
                          size: 20,
                        ),
                        const SizedBox(width: 6),
                        Text(
                          material.nombre,
                          style: TextStyle(
                            color: material.color,
                            fontWeight: FontWeight.w600,
                            fontSize: 16,
                          ),
                        ),
                      ],
                    ),
                  );
                }).toList(),
              ),
              const SizedBox(height: 24),
              
              // Botón de acción grande
              SizedBox(
                width: double.infinity,
                child: ElevatedButton(
                  onPressed: () {
                    Navigator.pop(context);
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text('Navegando a ${punto['nombre']}'),
                        backgroundColor: BioWayColors.success,
                        behavior: SnackBarBehavior.floating,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10),
                        ),
                      ),
                    );
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: BioWayColors.primaryGreen,
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  child: Text(
                    'Ir a Recolectar',
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
              ),
                    const SizedBox(height: 8),
                  ],
                ),
              ),
            );
          },
        );
      },
    );
  }
  
  IconData _getMaterialIcon(String material) {
    switch (material) {
      case 'pet_tipo1':
        return Icons.local_drink;
      case 'hdpe':
        return Icons.cleaning_services;
      case 'vidrio':
        return Icons.wine_bar;
      case 'papel':
        return Icons.description;
      case 'carton':
        return Icons.inventory_2;
      case 'metal':
        return Icons.hardware;
      case 'raspa_cuero':
        return Icons.cut;
      default:
        return Icons.recycling;
    }
  }
  
  Widget _buildCustomMarker(Map<String, dynamic> punto) {
    return GestureDetector(
      onTap: () => _showPuntoDetails(punto),
      child: Container(
        width: 50,
        height: 50,
        decoration: BoxDecoration(
          color: BioWayColors.primaryGreen,
          shape: BoxShape.circle,
          border: Border.all(
            color: Colors.white,
            width: 3,
          ),
          boxShadow: [
            BoxShadow(
              color: Colors.black.withValues(alpha: 0.3),
              blurRadius: 6,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Center(
          child: SvgPicture.asset(
            'assets/logos/bioway_logo.svg',
            width: 24,
            height: 24,
            colorFilter: const ColorFilter.mode(Colors.white, BlendMode.srcIn),
          ),
        ),
      ),
    );
  }
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: BioWayColors.backgroundGrey,
      body: SafeArea(
        child: Stack(
          children: [
            // Mapa de OpenStreetMap
            FlutterMap(
              mapController: _mapController,
              options: MapOptions(
                initialCenter: _initialPosition,
                initialZoom: _initialZoom,
                minZoom: 10,
                maxZoom: 18,
                onTap: (tapPosition, point) {
                  // Opcional: cerrar cualquier popup abierto
                },
              ),
              children: [
                // Capa de tiles - Múltiples opciones de diseño
                TileLayer(
                  // OPCIÓN 1: CartoDB Positron - Minimalista y limpio (RECOMENDADO)
                  urlTemplate: 'https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}.png',
                  subdomains: const ['a', 'b', 'c', 'd'],
                  
                  // OPCIÓN 2: CartoDB Dark Matter - Modo oscuro elegante
                  // urlTemplate: 'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png',
                  // subdomains: const ['a', 'b', 'c', 'd'],
                  
                  // OPCIÓN 3: CartoDB Voyager - Colores suaves y modernos
                  // urlTemplate: 'https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}.png',
                  // subdomains: const ['a', 'b', 'c', 'd'],
                  
                  // OPCIÓN 4: Stamen Toner Lite - Ultra minimalista en blanco y negro
                  // urlTemplate: 'https://stamen-tiles.a.ssl.fastly.net/toner-lite/{z}/{x}/{y}.png',
                  
                  // OPCIÓN 5: Esri World Gray Canvas - Profesional en escala de grises
                  // urlTemplate: 'https://server.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile/{z}/{y}/{x}',
                  
                  // OPCIÓN 6: OpenStreetMap Estándar (el actual)
                  // urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                  // subdomains: const ['a', 'b', 'c'],
                  
                  userAgentPackageName: 'com.bioway.app',
                  maxZoom: 19,
                ),
                
                // Capa de marcadores
                MarkerLayer(
                  markers: filteredPuntos.map((punto) {
                    return Marker(
                      point: punto['position'],
                      width: 50,
                      height: 50,
                      child: _buildCustomMarker(punto),
                    );
                  }).toList(),
                ),
                
                // Atribución del mapa (requerida por licencia)
                const SimpleAttributionWidget(
                  source: Text('© CARTO'),
                  backgroundColor: Colors.white,
                ),
              ],
            ),
            
            // Header con filtros
            Positioned(
              top: 0,
              left: 0,
              right: 0,
              child: Container(
                decoration: BoxDecoration(
                  color: Colors.white,
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.1),
                      blurRadius: 10,
                      offset: const Offset(0, 2),
                    ),
                  ],
                ),
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(8),
                          decoration: BoxDecoration(
                            color: BioWayColors.primaryGreen.withValues(alpha: 0.1),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Icon(
                            Icons.location_on,
                            color: BioWayColors.primaryGreen,
                            size: 24,
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Puntos de Recolección',
                                style: TextStyle(
                                  fontSize: 22,
                                  fontWeight: FontWeight.bold,
                                  color: BioWayColors.textDark,
                                ),
                              ),
                              Text(
                                '${filteredPuntos.length} puntos disponibles',
                                style: TextStyle(
                                  fontSize: 14,
                                  color: BioWayColors.textGrey,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    // Botón para abrir el diálogo de filtros
                    GestureDetector(
                      onTap: _showFilterDialog,
                      child: Container(
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: BioWayColors.primaryGreen.withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(
                            color: BioWayColors.primaryGreen.withValues(alpha: 0.3),
                            width: 2,
                          ),
                        ),
                        child: Row(
                          children: [
                            Icon(
                              Icons.filter_alt,
                              color: BioWayColors.primaryGreen,
                              size: 24,
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    selectedFilters.isEmpty 
                                        ? 'Mostrando todos los materiales'
                                        : 'Filtrado: ${_getSelectedMaterialsText()}',
                                    style: TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.w600,
                                      color: BioWayColors.textDark,
                                    ),
                                  ),
                                  const SizedBox(height: 2),
                                  Text(
                                    'Toca para cambiar filtros',
                                    style: TextStyle(
                                      fontSize: 14,
                                      color: BioWayColors.textGrey,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            Icon(
                              Icons.arrow_forward_ios,
                              color: BioWayColors.primaryGreen,
                              size: 20,
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            
            // Botones de control del mapa
            Positioned(
              bottom: 20,
              right: 20,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Botón de zoom in
                  FloatingActionButton.small(
                    heroTag: 'zoom_in',
                    onPressed: () {
                      HapticFeedback.lightImpact();
                      final currentZoom = _mapController.camera.zoom;
                      _mapController.move(
                        _mapController.camera.center,
                        currentZoom + 1,
                      );
                    },
                    backgroundColor: Colors.white,
                    child: Icon(
                      Icons.add,
                      color: BioWayColors.primaryGreen,
                    ),
                  ),
                  const SizedBox(height: 8),
                  // Botón de zoom out
                  FloatingActionButton.small(
                    heroTag: 'zoom_out',
                    onPressed: () {
                      HapticFeedback.lightImpact();
                      final currentZoom = _mapController.camera.zoom;
                      _mapController.move(
                        _mapController.camera.center,
                        currentZoom - 1,
                      );
                    },
                    backgroundColor: Colors.white,
                    child: Icon(
                      Icons.remove,
                      color: BioWayColors.primaryGreen,
                    ),
                  ),
                  const SizedBox(height: 16),
                  // Botón de ubicación actual
                  FloatingActionButton(
                    heroTag: 'my_location',
                    onPressed: () async {
                      HapticFeedback.lightImpact();
                      // Por ahora solo centra en la posición inicial
                      // En producción aquí obtendrías la ubicación real del usuario
                      _mapController.move(_initialPosition, 15);
                      
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: const Text('Centrando en tu ubicación'),
                          backgroundColor: BioWayColors.info,
                          behavior: SnackBarBehavior.floating,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(10),
                          ),
                          duration: const Duration(seconds: 1),
                        ),
                      );
                    },
                    backgroundColor: BioWayColors.primaryGreen,
                    child: const Icon(
                      Icons.my_location,
                      color: Colors.white,
                      size: 24,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
  
  String _getSelectedMaterialsText() {
    if (selectedFilters.isEmpty) return 'Todos';
    
    final selectedMaterials = materiales
        .where((m) => selectedFilters.contains(m.id))
        .map((m) => m.nombre)
        .toList();
    
    if (selectedMaterials.length > 2) {
      return '${selectedMaterials.take(2).join(', ')} y ${selectedMaterials.length - 2} más';
    }
    return selectedMaterials.join(', ');
  }
  
  void _showFilterDialog() {
    final tempFilters = Set<String>.from(selectedFilters);
    
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext dialogContext) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            return AlertDialog(
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(20),
              ),
              title: Column(
                children: [
                  Icon(
                    Icons.filter_alt,
                    color: BioWayColors.primaryGreen,
                    size: 40,
                  ),
                  const SizedBox(height: 12),
                  const Text(
                    'Filtrar Materiales',
                    style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Selecciona qué materiales quieres ver en el mapa',
                    style: TextStyle(
                      fontSize: 16,
                      color: Colors.grey[600],
                      fontWeight: FontWeight.normal,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ],
              ),
              content: Container(
                width: double.maxFinite,
                child: SingleChildScrollView(
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      // Botón de seleccionar todos/ninguno
                      GestureDetector(
                        onTap: () {
                          setDialogState(() {
                            if (tempFilters.length == materiales.length) {
                              tempFilters.clear();
                            } else {
                              tempFilters.clear();
                              tempFilters.addAll(materiales.map((m) => m.id));
                            }
                          });
                        },
                        child: Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: tempFilters.length == materiales.length
                                ? BioWayColors.primaryGreen.withValues(alpha: 0.1)
                                : Colors.grey.withValues(alpha: 0.1),
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(
                              color: tempFilters.length == materiales.length
                                  ? BioWayColors.primaryGreen
                                  : Colors.grey,
                              width: 2,
                            ),
                          ),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Icon(
                                tempFilters.length == materiales.length
                                    ? Icons.check_box
                                    : Icons.check_box_outline_blank,
                                color: tempFilters.length == materiales.length
                                    ? BioWayColors.primaryGreen
                                    : Colors.grey,
                              ),
                              const SizedBox(width: 8),
                              Text(
                                tempFilters.length == materiales.length
                                    ? 'Deseleccionar todos'
                                    : 'Seleccionar todos',
                                style: TextStyle(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w600,
                                  color: tempFilters.length == materiales.length
                                      ? BioWayColors.primaryGreen
                                      : Colors.grey[700],
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 16),
                      const Divider(),
                      const SizedBox(height: 16),
                      // Lista de materiales
                      ...materiales.map((material) {
                        final isSelected = tempFilters.contains(material.id);
                        return Padding(
                          padding: const EdgeInsets.only(bottom: 12),
                          child: GestureDetector(
                            onTap: () {
                              setDialogState(() {
                                if (isSelected) {
                                  tempFilters.remove(material.id);
                                } else {
                                  tempFilters.add(material.id);
                                }
                              });
                            },
                            child: Container(
                              padding: const EdgeInsets.all(16),
                              decoration: BoxDecoration(
                                color: isSelected
                                    ? material.color.withValues(alpha: 0.1)
                                    : Colors.grey.withValues(alpha: 0.05),
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(
                                  color: isSelected
                                      ? material.color
                                      : Colors.grey.withValues(alpha: 0.3),
                                  width: 2,
                                ),
                              ),
                              child: Row(
                                children: [
                                  Icon(
                                    isSelected
                                        ? Icons.check_circle
                                        : Icons.circle_outlined,
                                    color: isSelected
                                        ? material.color
                                        : Colors.grey,
                                    size: 28,
                                  ),
                                  const SizedBox(width: 12),
                                  Icon(
                                    _getMaterialIcon(material.id),
                                    color: isSelected
                                        ? material.color
                                        : Colors.grey,
                                    size: 24,
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: Text(
                                      material.nombre,
                                      style: TextStyle(
                                        fontSize: 18,
                                        fontWeight: FontWeight.w600,
                                        color: isSelected
                                            ? material.color
                                            : Colors.grey[700],
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        );
                      }).toList(),
                    ],
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: () {
                    Navigator.of(dialogContext).pop();
                  },
                  child: Text(
                    'Cancelar',
                    style: TextStyle(
                      fontSize: 16,
                      color: Colors.grey[600],
                    ),
                  ),
                ),
                ElevatedButton(
                  onPressed: () {
                    setState(() {
                      selectedFilters.clear();
                      selectedFilters.addAll(tempFilters);
                    });
                    Navigator.of(dialogContext).pop();
                  },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: BioWayColors.primaryGreen,
                    padding: const EdgeInsets.symmetric(
                      horizontal: 24,
                      vertical: 12,
                    ),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  child: const Text(
                    'Aplicar filtros',
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.w600,
                      color: Colors.white,
                    ),
                  ),
                ),
              ],
            );
          },
        );
      },
    );
  }
}