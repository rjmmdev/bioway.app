class EmpresaModel {
  final String? id;
  final String nombre;
  final String codigo;
  final String tipo;
  final String descripcion;
  final List<String> materiales;
  final Map<String, dynamic> zonaOperacion;
  final Map<String, dynamic> configuracion;
  final int totalRecolectores;
  final int totalBrindadores;
  final String estado;
  final double kgRecolectados;
  final DateTime? fechaCreacion;
  final List<String> materialesRecolectan;
  final List<String> estadosDisponibles;
  final List<String> municipiosDisponibles;
  final bool rangoRestringido;
  final double? rangoMaximoKm;

  EmpresaModel({
    this.id,
    required this.nombre,
    required this.codigo,
    required this.tipo,
    required this.descripcion,
    required this.materiales,
    required this.zonaOperacion,
    required this.configuracion,
    this.totalRecolectores = 0,
    this.totalBrindadores = 0,
    this.estado = 'activo',
    this.kgRecolectados = 0,
    this.fechaCreacion,
    List<String>? materialesRecolectan,
    List<String>? estadosDisponibles,
    List<String>? municipiosDisponibles,
    this.rangoRestringido = false,
    this.rangoMaximoKm,
  }) : materialesRecolectan = materialesRecolectan ?? materiales,
       estadosDisponibles = estadosDisponibles ?? [],
       municipiosDisponibles = municipiosDisponibles ?? [];

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'nombre': nombre,
      'codigo': codigo,
      'tipo': tipo,
      'descripcion': descripcion,
      'materiales': materiales,
      'zonaOperacion': zonaOperacion,
      'configuracion': configuracion,
      'totalRecolectores': totalRecolectores,
      'totalBrindadores': totalBrindadores,
      'estado': estado,
      'kgRecolectados': kgRecolectados,
      'fechaCreacion': fechaCreacion?.toIso8601String(),
      'materialesRecolectan': materialesRecolectan,
      'estadosDisponibles': estadosDisponibles,
      'municipiosDisponibles': municipiosDisponibles,
      'rangoRestringido': rangoRestringido,
      'rangoMaximoKm': rangoMaximoKm,
    };
  }

  factory EmpresaModel.fromMap(Map<String, dynamic> map) {
    return EmpresaModel(
      id: map['id'],
      nombre: map['nombre'] ?? '',
      codigo: map['codigo'] ?? '',
      tipo: map['tipo'] ?? '',
      descripcion: map['descripcion'] ?? '',
      materiales: List<String>.from(map['materiales'] ?? []),
      zonaOperacion: Map<String, dynamic>.from(map['zonaOperacion'] ?? {}),
      configuracion: Map<String, dynamic>.from(map['configuracion'] ?? {}),
      totalRecolectores: map['totalRecolectores'] ?? 0,
      totalBrindadores: map['totalBrindadores'] ?? 0,
      estado: map['estado'] ?? 'activo',
      kgRecolectados: (map['kgRecolectados'] ?? 0).toDouble(),
      fechaCreacion: map['fechaCreacion'] != null 
          ? DateTime.parse(map['fechaCreacion']) 
          : null,
      materialesRecolectan: List<String>.from(map['materialesRecolectan'] ?? map['materiales'] ?? []),
      estadosDisponibles: List<String>.from(map['estadosDisponibles'] ?? []),
      municipiosDisponibles: List<String>.from(map['municipiosDisponibles'] ?? []),
      rangoRestringido: map['rangoRestringido'] ?? false,
      rangoMaximoKm: map['rangoMaximoKm']?.toDouble(),
    );
  }
}