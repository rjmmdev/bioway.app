class SolicitudRecoleccion {
  final String id;
  final String brindadorId;
  final String brindadorNombre;
  final String brindadorDireccion;
  final double latitud;
  final double longitud;
  final String? recolectorId;
  final String? recolectorNombre;
  final String estado;
  final Map<String, dynamic> materiales;
  final DateTime fechaSolicitud;
  final DateTime? fechaRecoleccion;
  final String? comentarios;
  final double? peso;
  final String? imagenEvidencia;

  SolicitudRecoleccion({
    required this.id,
    required this.brindadorId,
    required this.brindadorNombre,
    required this.brindadorDireccion,
    required this.latitud,
    required this.longitud,
    this.recolectorId,
    this.recolectorNombre,
    required this.estado,
    required this.materiales,
    required this.fechaSolicitud,
    this.fechaRecoleccion,
    this.comentarios,
    this.peso,
    this.imagenEvidencia,
  });

  factory SolicitudRecoleccion.fromMap(Map<String, dynamic> map) {
    return SolicitudRecoleccion(
      id: map['id'] ?? '',
      brindadorId: map['brindadorId'] ?? '',
      brindadorNombre: map['brindadorNombre'] ?? '',
      brindadorDireccion: map['brindadorDireccion'] ?? '',
      latitud: map['latitud']?.toDouble() ?? 0.0,
      longitud: map['longitud']?.toDouble() ?? 0.0,
      recolectorId: map['recolectorId'],
      recolectorNombre: map['recolectorNombre'],
      estado: map['estado'] ?? 'pendiente',
      materiales: map['materiales'] ?? {},
      fechaSolicitud: map['fechaSolicitud'] != null
          ? DateTime.parse(map['fechaSolicitud'])
          : DateTime.now(),
      fechaRecoleccion: map['fechaRecoleccion'] != null
          ? DateTime.parse(map['fechaRecoleccion'])
          : null,
      comentarios: map['comentarios'],
      peso: map['peso']?.toDouble(),
      imagenEvidencia: map['imagenEvidencia'],
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'brindadorId': brindadorId,
      'brindadorNombre': brindadorNombre,
      'brindadorDireccion': brindadorDireccion,
      'latitud': latitud,
      'longitud': longitud,
      'recolectorId': recolectorId,
      'recolectorNombre': recolectorNombre,
      'estado': estado,
      'materiales': materiales,
      'fechaSolicitud': fechaSolicitud.toIso8601String(),
      'fechaRecoleccion': fechaRecoleccion?.toIso8601String(),
      'comentarios': comentarios,
      'peso': peso,
      'imagenEvidencia': imagenEvidencia,
    };
  }
}