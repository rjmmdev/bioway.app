class MaterialTipo {
  final String id;
  final String nombre;
  final String categoria;
  final String color;
  final String icono;
  final double precioPorKg;
  final String descripcion;
  final bool activo;

  MaterialTipo({
    required this.id,
    required this.nombre,
    required this.categoria,
    required this.color,
    required this.icono,
    required this.precioPorKg,
    required this.descripcion,
    required this.activo,
  });

  factory MaterialTipo.fromMap(Map<String, dynamic> map) {
    return MaterialTipo(
      id: map['id'] ?? '',
      nombre: map['nombre'] ?? '',
      categoria: map['categoria'] ?? '',
      color: map['color'] ?? '#000000',
      icono: map['icono'] ?? '',
      precioPorKg: map['precioPorKg']?.toDouble() ?? 0.0,
      descripcion: map['descripcion'] ?? '',
      activo: map['activo'] ?? true,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'nombre': nombre,
      'categoria': categoria,
      'color': color,
      'icono': icono,
      'precioPorKg': precioPorKg,
      'descripcion': descripcion,
      'activo': activo,
    };
  }
}