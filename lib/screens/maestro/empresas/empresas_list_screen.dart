import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
// import 'package:cloud_firestore/cloud_firestore.dart'; // TODO: Descomentar para producción
import '../../../utils/colors.dart';
import '../../../models/bioway/empresa_model.dart';
import 'empresa_form_screen.dart';

class EmpresasListScreen extends StatefulWidget {
  const EmpresasListScreen({super.key});

  @override
  State<EmpresasListScreen> createState() => _EmpresasListScreenState();
}

class _EmpresasListScreenState extends State<EmpresasListScreen> {
  final TextEditingController _searchController = TextEditingController();
  String _filtroEstado = 'todos';
  
  // MODO DISEÑO: Datos de prueba con funcionalidad completa
  final List<Map<String, dynamic>> _empresas = [
    {
      'id': '1',
      'nombre': 'Sicit',
      'codigo': 'SICIT2024',
      'tipo': 'Recolección Especializada',
      'descripcion': 'Empresa especializada en recolección de raspa de cuero',
      'materiales': ['Raspa de Cuero'],
      'zonaOperacion': {
        'tipo': 'ilimitado',
        'estados': [],
        'municipios': [],
        'rangoKm': null,
      },
      'configuracion': {
        'recolectoresVenTodo': true,
        'brindadoresRequierenCodigo': true,
        'mostrarEnListaPublica': false,
      },
      'totalRecolectores': 45,
      'totalBrindadores': 120,
      'estado': 'activo',
      'kgRecolectados': 15420,
    },
    {
      'id': '2',
      'nombre': 'EcoPlásticos MX',
      'codigo': 'ECOMX2024',
      'tipo': 'Reciclaje PET',
      'descripcion': 'Reciclaje y procesamiento de plásticos PET',
      'materiales': ['PET Tipo 1', 'PET Tipo 2', 'HDPE'],
      'zonaOperacion': {
        'tipo': 'estados',
        'estados': ['Aguascalientes', 'Jalisco'],
        'municipios': [],
        'rangoKm': null,
      },
      'configuracion': {
        'recolectoresVenTodo': false,
        'brindadoresRequierenCodigo': false,
        'mostrarEnListaPublica': true,
      },
      'totalRecolectores': 28,
      'totalBrindadores': 85,
      'estado': 'activo',
      'kgRecolectados': 8750,
    },
    {
      'id': '3',
      'nombre': 'Papelera del Centro',
      'codigo': 'PAPEL2024',
      'tipo': 'Papel y Cartón',
      'descripcion': 'Recolección y reciclaje de papel y cartón',
      'materiales': ['Papel', 'Cartón', 'Periódico'],
      'zonaOperacion': {
        'tipo': 'municipios',
        'estados': ['Aguascalientes'],
        'municipios': ['Aguascalientes', 'Jesús María'],
        'rangoKm': 10,
      },
      'configuracion': {
        'recolectoresVenTodo': false,
        'brindadoresRequierenCodigo': false,
        'mostrarEnListaPublica': true,
      },
      'totalRecolectores': 15,
      'totalBrindadores': 50,
      'estado': 'pausado',
      'kgRecolectados': 5200,
    },
  ];

  List<Map<String, dynamic>> get _empresasFiltradas {
    return _empresas.where((empresa) {
      final matchesSearch = empresa['nombre']
          .toString()
          .toLowerCase()
          .contains(_searchController.text.toLowerCase()) ||
          empresa['codigo']
          .toString()
          .toLowerCase()
          .contains(_searchController.text.toLowerCase());
      final matchesEstado = _filtroEstado == 'todos' || 
          empresa['estado'] == _filtroEstado;
      return matchesSearch && matchesEstado;
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: BioWayColors.backgroundGrey,
      appBar: AppBar(
        title: const Text(
          'Gestión de Empresas',
          style: TextStyle(
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
        backgroundColor: BioWayColors.navGreen,
        foregroundColor: Colors.white,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.analytics_outlined),
            onPressed: _mostrarEstadisticas,
            tooltip: 'Estadísticas',
          ),
          IconButton(
            icon: const Icon(Icons.help_outline),
            onPressed: _mostrarAyuda,
            tooltip: 'Ayuda',
          ),
        ],
      ),
      body: Column(
        children: [
          // Header con búsqueda y filtros
          Container(
            decoration: BoxDecoration(
              gradient: LinearGradient(
                colors: BioWayColors.backgroundGradientSoft,
                begin: Alignment.topLeft,
                end: Alignment.bottomRight,
              ),
              borderRadius: const BorderRadius.only(
                bottomLeft: Radius.circular(24),
                bottomRight: Radius.circular(24),
              ),
            ),
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 20),
            child: Column(
              children: [
                // Barra de búsqueda
                Container(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(12),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.1),
                        blurRadius: 10,
                        offset: const Offset(0, 2),
                      ),
                    ],
                  ),
                  child: TextField(
                    controller: _searchController,
                    onChanged: (_) => setState(() {}),
                    decoration: InputDecoration(
                      hintText: 'Buscar por nombre o código...',
                      prefixIcon: Icon(
                        Icons.search,
                        color: BioWayColors.navGreen,
                      ),
                      suffixIcon: _searchController.text.isNotEmpty
                          ? IconButton(
                              icon: const Icon(Icons.clear),
                              onPressed: () {
                                _searchController.clear();
                                setState(() {});
                              },
                            )
                          : null,
                      border: InputBorder.none,
                      contentPadding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 14,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 12),
                
                // Filtros
                SingleChildScrollView(
                  scrollDirection: Axis.horizontal,
                  child: Row(
                    children: [
                      _buildFilterChip('Todas', 'todos'),
                      const SizedBox(width: 8),
                      _buildFilterChip('Activas', 'activo'),
                      const SizedBox(width: 8),
                      _buildFilterChip('Pausadas', 'pausado'),
                      const SizedBox(width: 8),
                      _buildFilterChip('Inactivas', 'inactivo'),
                    ],
                  ),
                ),
              ],
            ),
          ),
          
          // Lista de empresas
          Expanded(
            child: _empresasFiltradas.isEmpty
                ? Center(
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Icon(
                          Icons.business_outlined,
                          size: 80,
                          color: Colors.grey.shade300,
                        ),
                        const SizedBox(height: 16),
                        Text(
                          'No se encontraron empresas',
                          style: TextStyle(
                            fontSize: 16,
                            color: Colors.grey.shade600,
                          ),
                        ),
                      ],
                    ),
                  )
                : ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: _empresasFiltradas.length,
                    itemBuilder: (context, index) {
                      return _buildEmpresaCard(_empresasFiltradas[index]);
                    },
                  ),
          ),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _agregarEmpresa,
        backgroundColor: BioWayColors.navGreen,
        icon: const Icon(Icons.add, color: Colors.white),
        label: const Text(
          'Nueva Empresa',
          style: TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
        ),
      ),
    );
  }

  Widget _buildFilterChip(String label, String value) {
    final isSelected = _filtroEstado == value;
    return GestureDetector(
      onTap: () {
        HapticFeedback.lightImpact();
        setState(() {
          _filtroEstado = value;
        });
      },
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: isSelected ? Colors.white : Colors.white.withOpacity(0.2),
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: isSelected ? Colors.white : Colors.white.withOpacity(0.3),
            width: 1,
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            color: isSelected ? BioWayColors.navGreen : const Color(0xFF00553F),
            fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
            fontSize: 14,
          ),
        ),
      ),
    );
  }

  Widget _buildEmpresaCard(Map<String, dynamic> empresa) {
    final estado = empresa['estado'];
    final estadoColor = estado == 'activo' 
        ? BioWayColors.success 
        : estado == 'pausado' 
            ? BioWayColors.warning 
            : BioWayColors.error;

    final zonaOperacion = empresa['zonaOperacion'];
    String zonaTexto = '';
    if (zonaOperacion['tipo'] == 'ilimitado') {
      zonaTexto = 'Cobertura Nacional';
    } else if (zonaOperacion['tipo'] == 'estados') {
      zonaTexto = (zonaOperacion['estados'] as List).join(', ');
    } else if (zonaOperacion['tipo'] == 'municipios') {
      zonaTexto = '${(zonaOperacion['municipios'] as List).length} municipios';
    }

    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      elevation: 2,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
      ),
      child: InkWell(
        onTap: () => _verDetalleEmpresa(empresa),
        borderRadius: BorderRadius.circular(16),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header
              Row(
                children: [
                  Container(
                    width: 48,
                    height: 48,
                    decoration: BoxDecoration(
                      color: BioWayColors.navGreen.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Icon(
                      Icons.business,
                      color: BioWayColors.navGreen,
                      size: 24,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Expanded(
                              child: Text(
                                empresa['nombre'],
                                style: const TextStyle(
                                  fontSize: 18,
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                            ),
                            Container(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 8,
                                vertical: 4,
                              ),
                              decoration: BoxDecoration(
                                color: estadoColor.withOpacity(0.1),
                                borderRadius: BorderRadius.circular(8),
                              ),
                              child: Text(
                                estado.toString().toUpperCase(),
                                style: TextStyle(
                                  fontSize: 11,
                                  fontWeight: FontWeight.bold,
                                  color: estadoColor,
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        Row(
                          children: [
                            Icon(
                              Icons.qr_code,
                              size: 14,
                              color: Colors.grey.shade600,
                            ),
                            const SizedBox(width: 4),
                            Text(
                              empresa['codigo'],
                              style: TextStyle(
                                fontSize: 12,
                                color: Colors.grey.shade600,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                            if (empresa['configuracion']['brindadoresRequierenCodigo'] == true) ...[
                              const SizedBox(width: 8),
                              Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 6,
                                  vertical: 2,
                                ),
                                decoration: BoxDecoration(
                                  color: Colors.blue.withOpacity(0.1),
                                  borderRadius: BorderRadius.circular(4),
                                ),
                                child: const Text(
                                  'Requiere Código',
                                  style: TextStyle(
                                    fontSize: 10,
                                    color: Colors.blue,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ),
                            ],
                          ],
                        ),
                      ],
                    ),
                  ),
                  PopupMenuButton<String>(
                    onSelected: (value) => _handleMenuAction(value, empresa),
                    itemBuilder: (context) => [
                      const PopupMenuItem(
                        value: 'edit',
                        child: Row(
                          children: [
                            Icon(Icons.edit, size: 20),
                            SizedBox(width: 8),
                            Text('Editar'),
                          ],
                        ),
                      ),
                      PopupMenuItem(
                        value: 'toggle',
                        child: Row(
                          children: [
                            Icon(
                              empresa['estado'] == 'activo'
                                  ? Icons.pause
                                  : Icons.play_arrow,
                              size: 20,
                            ),
                            const SizedBox(width: 8),
                            Text(empresa['estado'] == 'activo'
                                ? 'Pausar'
                                : 'Activar'),
                          ],
                        ),
                      ),
                      const PopupMenuItem(
                        value: 'copy_code',
                        child: Row(
                          children: [
                            Icon(Icons.copy, size: 20),
                            SizedBox(width: 8),
                            Text('Copiar Código'),
                          ],
                        ),
                      ),
                      const PopupMenuDivider(),
                      const PopupMenuItem(
                        value: 'delete',
                        child: Row(
                          children: [
                            Icon(Icons.delete, size: 20, color: Colors.red),
                            SizedBox(width: 8),
                            Text('Eliminar', style: TextStyle(color: Colors.red)),
                          ],
                        ),
                      ),
                    ],
                  ),
                ],
              ),
              
              const SizedBox(height: 12),
              
              // Tipo y zona
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: Colors.grey.shade50,
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Column(
                  children: [
                    Row(
                      children: [
                        Icon(
                          Icons.category_outlined,
                          size: 16,
                          color: Colors.grey.shade600,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            empresa['tipo'],
                            style: const TextStyle(fontSize: 13),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Row(
                      children: [
                        Icon(
                          Icons.location_on_outlined,
                          size: 16,
                          color: Colors.grey.shade600,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            zonaTexto,
                            style: const TextStyle(fontSize: 13),
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                        if (empresa['configuracion']['recolectoresVenTodo'] == true)
                          Container(
                            padding: const EdgeInsets.symmetric(
                              horizontal: 6,
                              vertical: 2,
                            ),
                            decoration: BoxDecoration(
                              color: Colors.green.withOpacity(0.1),
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: const Text(
                              'Visibilidad Total',
                              style: TextStyle(
                                fontSize: 10,
                                color: Colors.green,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                      ],
                    ),
                  ],
                ),
              ),
              
              const SizedBox(height: 12),
              
              // Materiales
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: (empresa['materiales'] as List).map((material) {
                  return Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 8,
                      vertical: 4,
                    ),
                    decoration: BoxDecoration(
                      color: BioWayColors.mediumGreen.withOpacity(0.1),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: BioWayColors.mediumGreen.withOpacity(0.3),
                      ),
                    ),
                    child: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Icon(
                          Icons.recycling,
                          size: 14,
                          color: BioWayColors.darkGreen,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          material,
                          style: TextStyle(
                            fontSize: 11,
                            color: BioWayColors.darkGreen,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ],
                    ),
                  );
                }).toList(),
              ),
              
              const SizedBox(height: 12),
              const Divider(),
              const SizedBox(height: 8),
              
              // Estadísticas
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                children: [
                  _buildStat(
                    Icons.person_outline,
                    empresa['totalRecolectores'].toString(),
                    'Recolectores',
                  ),
                  _buildStat(
                    Icons.volunteer_activism,
                    empresa['totalBrindadores'].toString(),
                    'Brindadores',
                  ),
                  _buildStat(
                    Icons.recycling,
                    '${(empresa['kgRecolectados'] / 1000).toStringAsFixed(1)}t',
                    'Recolectado',
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStat(IconData icon, String value, String label) {
    return Column(
      children: [
        Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(icon, size: 16, color: BioWayColors.navGreen),
            const SizedBox(width: 4),
            Text(
              value,
              style: const TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
              ),
            ),
          ],
        ),
        const SizedBox(height: 2),
        Text(
          label,
          style: TextStyle(
            fontSize: 11,
            color: Colors.grey.shade600,
          ),
        ),
      ],
    );
  }

  void _handleMenuAction(String action, Map<String, dynamic> empresa) {
    switch (action) {
      case 'edit':
        _verDetalleEmpresa(empresa);
        break;
      case 'toggle':
        _toggleEmpresaStatus(empresa);
        break;
      case 'copy_code':
        Clipboard.setData(ClipboardData(text: empresa['codigo']));
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Código ${empresa['codigo']} copiado'),
            backgroundColor: BioWayColors.success,
          ),
        );
        break;
      case 'delete':
        _showDeleteConfirmation(empresa);
        break;
    }
  }

  void _toggleEmpresaStatus(Map<String, dynamic> empresa) {
    setState(() {
      final index = _empresas.indexOf(empresa);
      if (empresa['estado'] == 'activo') {
        _empresas[index]['estado'] = 'pausado';
      } else {
        _empresas[index]['estado'] = 'activo';
      }
    });
    
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          empresa['estado'] == 'activo'
              ? 'Empresa activada'
              : 'Empresa pausada',
        ),
        backgroundColor: BioWayColors.success,
      ),
    );
  }

  void _showDeleteConfirmation(Map<String, dynamic> empresa) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Eliminar Empresa'),
        content: Text(
          '¿Está seguro de eliminar la empresa "${empresa['nombre']}"?\n\n'
          'Se eliminarán también:\n'
          '• ${empresa['totalRecolectores']} recolectores asociados\n'
          '• ${empresa['totalBrindadores']} brindadores asociados\n'
          '• Historial de recolecciones\n\n'
          'Esta acción no se puede deshacer.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            onPressed: () {
              Navigator.pop(context);
              setState(() {
                _empresas.remove(empresa);
              });
              ScaffoldMessenger.of(context).showSnackBar(
                const SnackBar(
                  content: Text('Empresa eliminada correctamente'),
                  backgroundColor: Colors.green,
                ),
              );
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red,
            ),
            child: const Text(
              'Eliminar',
              style: TextStyle(color: Colors.white),
            ),
          ),
        ],
      ),
    );
  }

  void _agregarEmpresa() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => const EmpresaFormScreen(),
      ),
    );
  }

  void _verDetalleEmpresa(Map<String, dynamic> empresa) {
    final empresaModel = EmpresaModel.fromMap(empresa);
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => EmpresaFormScreen(empresa: empresaModel),
      ),
    );
  }

  void _mostrarEstadisticas() {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => Container(
        height: MediaQuery.of(context).size.height * 0.7,
        decoration: const BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.only(
            topLeft: Radius.circular(24),
            topRight: Radius.circular(24),
          ),
        ),
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: Colors.grey.shade300,
                borderRadius: BorderRadius.circular(2),
              ),
            ),
            const SizedBox(height: 20),
            const Text(
              'Estadísticas Generales',
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 20),
            Expanded(
              child: ListView(
                children: [
                  _buildStatCard(
                    'Total de Empresas',
                    _empresas.length.toString(),
                    Icons.business,
                    BioWayColors.navGreen,
                  ),
                  _buildStatCard(
                    'Empresas Activas',
                    _empresas.where((e) => e['estado'] == 'activo').length.toString(),
                    Icons.check_circle,
                    BioWayColors.success,
                  ),
                  _buildStatCard(
                    'Total Recolectores',
                    _empresas.fold<int>(0, (sum, e) => sum + (e['totalRecolectores'] as int)).toString(),
                    Icons.person,
                    BioWayColors.info,
                  ),
                  _buildStatCard(
                    'Total Brindadores',
                    _empresas.fold<int>(0, (sum, e) => sum + (e['totalBrindadores'] as int)).toString(),
                    Icons.volunteer_activism,
                    BioWayColors.warning,
                  ),
                  _buildStatCard(
                    'Material Recolectado',
                    '${(_empresas.fold<int>(0, (sum, e) => sum + (e['kgRecolectados'] as int)) / 1000).toStringAsFixed(1)} toneladas',
                    Icons.recycling,
                    BioWayColors.success,
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildStatCard(String title, String value, IconData icon, Color color) {
    return Card(
      margin: const EdgeInsets.only(bottom: 12),
      child: ListTile(
        leading: Container(
          padding: const EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: color.withOpacity(0.1),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Icon(icon, color: color),
        ),
        title: Text(title),
        trailing: Text(
          value,
          style: const TextStyle(
            fontSize: 18,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
    );
  }

  void _mostrarAyuda() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Gestión de Empresas'),
        content: const SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                'Sistema de Empresas Asociadas',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 8),
              Text(
                '• Cada empresa tiene un código único para registro\n'
                '• Los materiales son específicos por empresa\n'
                '• Control de zonas de operación\n'
                '• Gestión de permisos y visibilidad\n',
              ),
              SizedBox(height: 12),
              Text(
                'Configuraciones Especiales:',
                style: TextStyle(fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 8),
              Text(
                '• Visibilidad Total: Recolectores ven todo el país\n'
                '• Requiere Código: Brindadores necesitan código para registrarse\n'
                '• Zonas: Estados, municipios o ilimitado\n',
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Entendido'),
          ),
        ],
      ),
    );
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }
}