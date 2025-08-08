class Comercio {
  final String id;
  final String nombre;
  final String descripcion;
  final String categoria;
  final String direccion;
  final String municipio;
  final String estado;
  final String telefono;
  final String horario;
  final double latitud;
  final double longitud;
  final String? imagen;
  final double calificacion;
  final int totalReviews;
  final bool aceptaPuntos;
  final int descuentoMax;

  Comercio({
    required this.id,
    required this.nombre,
    required this.descripcion,
    required this.categoria,
    required this.direccion,
    required this.municipio,
    required this.estado,
    required this.telefono,
    required this.horario,
    required this.latitud,
    required this.longitud,
    this.imagen,
    this.calificacion = 0.0,
    this.totalReviews = 0,
    this.aceptaPuntos = true,
    this.descuentoMax = 20,
  });

  static List<Comercio> getMockComercios() {
    return [
      Comercio(
        id: '1',
        nombre: 'Café Verde',
        descripcion: 'Cafetería ecológica con productos orgánicos',
        categoria: 'Alimentos',
        direccion: 'Av. López Mateos 123',
        municipio: 'Aguascalientes',
        estado: 'Aguascalientes',
        telefono: '449-123-4567',
        horario: '8:00 AM - 10:00 PM',
        latitud: 21.8853,
        longitud: -102.2916,
        calificacion: 4.8,
        totalReviews: 245,
        aceptaPuntos: true,
        descuentoMax: 25,
      ),
      Comercio(
        id: '2',
        nombre: 'EcoMart',
        descripcion: 'Supermercado con productos sustentables',
        categoria: 'Supermercado',
        direccion: 'Blvd. Zacatecas 456',
        municipio: 'Aguascalientes',
        estado: 'Aguascalientes',
        telefono: '449-234-5678',
        horario: '7:00 AM - 11:00 PM',
        latitud: 21.8950,
        longitud: -102.2850,
        calificacion: 4.5,
        totalReviews: 532,
        aceptaPuntos: true,
        descuentoMax: 15,
      ),
      Comercio(
        id: '3',
        nombre: 'Tienda Orgánica',
        descripcion: 'Productos orgánicos y naturales',
        categoria: 'Salud',
        direccion: 'Calle Madero 789',
        municipio: 'Aguascalientes',
        estado: 'Aguascalientes',
        telefono: '449-345-6789',
        horario: '9:00 AM - 8:00 PM',
        latitud: 21.8750,
        longitud: -102.2950,
        calificacion: 4.7,
        totalReviews: 128,
        aceptaPuntos: true,
        descuentoMax: 30,
      ),
      Comercio(
        id: '4',
        nombre: 'Farmacia Natural',
        descripcion: 'Medicamentos naturales y productos ecológicos',
        categoria: 'Salud',
        direccion: 'Av. Universidad 321',
        municipio: 'Aguascalientes',
        estado: 'Aguascalientes',
        telefono: '449-456-7890',
        horario: '8:00 AM - 10:00 PM',
        latitud: 21.9050,
        longitud: -102.3150,
        calificacion: 4.6,
        totalReviews: 89,
        aceptaPuntos: true,
        descuentoMax: 20,
      ),
      Comercio(
        id: '5',
        nombre: 'BikeShop Eco',
        descripcion: 'Bicicletas y transporte sustentable',
        categoria: 'Transporte',
        direccion: 'Calle Allende 555',
        municipio: 'Aguascalientes',
        estado: 'Aguascalientes',
        telefono: '449-567-8901',
        horario: '10:00 AM - 7:00 PM',
        latitud: 21.8650,
        longitud: -102.2750,
        calificacion: 4.9,
        totalReviews: 156,
        aceptaPuntos: true,
        descuentoMax: 15,
      ),
      Comercio(
        id: '6',
        nombre: 'Restaurante Verde',
        descripcion: 'Comida vegetariana y vegana',
        categoria: 'Alimentos',
        direccion: 'Plaza Principal 100',
        municipio: 'Aguascalientes',
        estado: 'Aguascalientes',
        telefono: '449-678-9012',
        horario: '12:00 PM - 10:00 PM',
        latitud: 21.8820,
        longitud: -102.2960,
        calificacion: 4.7,
        totalReviews: 312,
        aceptaPuntos: true,
        descuentoMax: 20,
      ),
    ];
  }
}