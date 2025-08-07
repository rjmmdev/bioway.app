class Comercio {
  final String id;
  final String nombre;
  final String categoria;
  final String estado;
  final String municipio;
  final String direccion;
  final double latitud;
  final double longitud;
  final String telefono;
  final String horario;
  final String descripcion;
  final String imagen;
  final Map<String, dynamic> descuentos;
  final bool activo;

  Comercio({
    required this.id,
    required this.nombre,
    required this.categoria,
    required this.estado,
    required this.municipio,
    required this.direccion,
    required this.latitud,
    required this.longitud,
    required this.telefono,
    required this.horario,
    required this.descripcion,
    required this.imagen,
    required this.descuentos,
    required this.activo,
  });

  factory Comercio.fromMap(Map<String, dynamic> map) {
    return Comercio(
      id: map['id'] ?? '',
      nombre: map['nombre'] ?? '',
      categoria: map['categoria'] ?? '',
      estado: map['estado'] ?? '',
      municipio: map['municipio'] ?? '',
      direccion: map['direccion'] ?? '',
      latitud: map['latitud']?.toDouble() ?? 0.0,
      longitud: map['longitud']?.toDouble() ?? 0.0,
      telefono: map['telefono'] ?? '',
      horario: map['horario'] ?? '',
      descripcion: map['descripcion'] ?? '',
      imagen: map['imagen'] ?? '',
      descuentos: map['descuentos'] ?? {},
      activo: map['activo'] ?? true,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'nombre': nombre,
      'categoria': categoria,
      'estado': estado,
      'municipio': municipio,
      'direccion': direccion,
      'latitud': latitud,
      'longitud': longitud,
      'telefono': telefono,
      'horario': horario,
      'descripcion': descripcion,
      'imagen': imagen,
      'descuentos': descuentos,
      'activo': activo,
    };
  }
  
  static List<Comercio> getMockComercios() {
    return [
      Comercio(
        id: '1',
        nombre: 'Café Verde',
        categoria: 'Cafetería',
        estado: 'CDMX',
        municipio: 'Coyoacán',
        direccion: 'Av. Universidad 123',
        latitud: 19.3506,
        longitud: -99.1616,
        telefono: '555-1234',
        horario: '8:00 - 20:00',
        descripcion: 'Café orgánico y sustentable',
        imagen: 'https://via.placeholder.com/200',
        descuentos: {'cafe': 20, 'postres': 15},
        activo: true,
      ),
      Comercio(
        id: '2',
        nombre: 'EcoMarket',
        categoria: 'Supermercado',
        estado: 'CDMX',
        municipio: 'Benito Juárez',
        direccion: 'Insurgentes Sur 456',
        latitud: 19.3856,
        longitud: -99.1776,
        telefono: '555-5678',
        horario: '7:00 - 22:00',
        descripcion: 'Productos orgánicos y locales',
        imagen: 'https://via.placeholder.com/200',
        descuentos: {'vegetales': 10, 'frutas': 15},
        activo: true,
      ),
    ];
  }
}