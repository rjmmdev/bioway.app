class EmpresaModel {
  final String id;
  final String nombre;
  final String rfc;
  final String direccion;
  final String telefono;
  final String email;
  final String contacto;
  final String tipoEmpresa;
  final bool activa;
  final DateTime fechaRegistro;
  final DateTime fechaCreacion;
  final Map<String, dynamic> configuracion;
  final String municipio;
  final String estado;
  final String cp;
  final bool rangoRestringido;
  final double? rangoMaximoKm;
  final String descripcion;
  final List<String> materialesRecolectan;
  final List<String> estadosDisponibles;
  final List<String> municipiosDisponibles;
  
  EmpresaModel({
    required this.id,
    required this.nombre,
    required this.rfc,
    required this.direccion,
    required this.telefono,
    required this.email,
    required this.contacto,
    required this.tipoEmpresa,
    required this.activa,
    required this.fechaRegistro,
    DateTime? fechaCreacion,
    required this.configuracion,
    String? municipio,
    String? estado,
    String? cp,
    bool? rangoRestringido,
    this.rangoMaximoKm,
    String? descripcion,
    List<String>? materialesRecolectan,
    List<String>? estadosDisponibles,
    List<String>? municipiosDisponibles,
  }) : fechaCreacion = fechaCreacion ?? fechaRegistro,
       municipio = municipio ?? '',
       estado = estado ?? '',
       cp = cp ?? '',
       rangoRestringido = rangoRestringido ?? false,
       descripcion = descripcion ?? '',
       materialesRecolectan = materialesRecolectan ?? [],
       estadosDisponibles = estadosDisponibles ?? [],
       municipiosDisponibles = municipiosDisponibles ?? [];
  
  factory EmpresaModel.fromMap(Map<String, dynamic> map) {
    return EmpresaModel(
      id: map['id'] ?? '',
      nombre: map['nombre'] ?? '',
      rfc: map['rfc'] ?? '',
      direccion: map['direccion'] ?? '',
      telefono: map['telefono'] ?? '',
      email: map['email'] ?? '',
      contacto: map['contacto'] ?? '',
      tipoEmpresa: map['tipoEmpresa'] ?? map['tipo'] ?? 'regular',
      activa: map['activa'] ?? map['estado'] == 'activo',
      fechaRegistro: map['fechaRegistro'] != null
          ? DateTime.parse(map['fechaRegistro'])
          : DateTime.now(),
      fechaCreacion: map['fechaCreacion'] != null
          ? DateTime.parse(map['fechaCreacion'])
          : null,
      configuracion: map['configuracion'] ?? {},
      municipio: map['municipio'],
      estado: map['estado'],
      cp: map['cp'],
      rangoRestringido: map['rangoRestringido'],
      rangoMaximoKm: map['rangoMaximoKm']?.toDouble(),
      descripcion: map['descripcion'] ?? '',
      materialesRecolectan: map['materialesRecolectan'] != null 
          ? List<String>.from(map['materialesRecolectan'])
          : (map['materiales'] != null ? List<String>.from(map['materiales']) : []),
      estadosDisponibles: map['estadosDisponibles'] != null 
          ? List<String>.from(map['estadosDisponibles']) 
          : [],
      municipiosDisponibles: map['municipiosDisponibles'] != null 
          ? List<String>.from(map['municipiosDisponibles']) 
          : [],
    );
  }
  
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'nombre': nombre,
      'rfc': rfc,
      'direccion': direccion,
      'telefono': telefono,
      'email': email,
      'contacto': contacto,
      'tipoEmpresa': tipoEmpresa,
      'activa': activa,
      'fechaRegistro': fechaRegistro.toIso8601String(),
      'fechaCreacion': fechaCreacion.toIso8601String(),
      'configuracion': configuracion,
      'municipio': municipio,
      'estado': estado,
      'cp': cp,
      'rangoRestringido': rangoRestringido,
      'rangoMaximoKm': rangoMaximoKm,
      'descripcion': descripcion,
      'materialesRecolectan': materialesRecolectan,
      'estadosDisponibles': estadosDisponibles,
      'municipiosDisponibles': municipiosDisponibles,
    };
  }
}