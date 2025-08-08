class ProductoDescuento {
  final String id;
  final String comercioId;
  final String nombre;
  final String descripcion;
  final double precioOriginal;
  final double precioDescuento;
  final int puntosRequeridos;
  final int bioCoinsCosto;
  final String? imagen;
  final bool destacado;
  final bool disponible;
  final DateTime? validoHasta;
  final String icono;
  final double descuentoPorcentaje;

  ProductoDescuento({
    required this.id,
    required this.comercioId,
    required this.nombre,
    required this.descripcion,
    required this.precioOriginal,
    required this.precioDescuento,
    required this.puntosRequeridos,
    int? bioCoinsCosto,
    this.imagen,
    this.destacado = false,
    this.disponible = true,
    this.validoHasta,
    String? icono,
    double? descuentoPorcentaje,
  }) : bioCoinsCosto = bioCoinsCosto ?? puntosRequeridos,
       icono = icono ?? '🎁',
       descuentoPorcentaje = descuentoPorcentaje ?? ((precioOriginal - precioDescuento) / precioOriginal * 100);

  double get porcentajeDescuento {
    return ((precioOriginal - precioDescuento) / precioOriginal * 100);
  }

  static List<ProductoDescuento> getMockProductos() {
    return [
      ProductoDescuento(
        id: '1',
        comercioId: '1',
        nombre: 'Café Americano',
        descripcion: 'Café orgánico de comercio justo',
        precioOriginal: 45.0,
        precioDescuento: 33.75,
        puntosRequeridos: 50,
        destacado: true,
        disponible: true,
      ),
      ProductoDescuento(
        id: '2',
        comercioId: '1',
        nombre: 'Sandwich Vegano',
        descripcion: 'Pan integral con vegetales frescos',
        precioOriginal: 85.0,
        precioDescuento: 68.0,
        puntosRequeridos: 80,
        destacado: false,
        disponible: true,
      ),
      ProductoDescuento(
        id: '3',
        comercioId: '2',
        nombre: 'Bolsa Reutilizable',
        descripcion: 'Bolsa de tela ecológica',
        precioOriginal: 120.0,
        precioDescuento: 84.0,
        puntosRequeridos: 100,
        destacado: true,
        disponible: true,
      ),
      ProductoDescuento(
        id: '4',
        comercioId: '2',
        nombre: 'Pack Productos de Limpieza',
        descripcion: 'Productos biodegradables',
        precioOriginal: 250.0,
        precioDescuento: 200.0,
        puntosRequeridos: 150,
        destacado: false,
        disponible: true,
      ),
      ProductoDescuento(
        id: '5',
        comercioId: '3',
        nombre: 'Suplemento Natural',
        descripcion: 'Vitaminas orgánicas',
        precioOriginal: 450.0,
        precioDescuento: 315.0,
        puntosRequeridos: 200,
        destacado: true,
        disponible: true,
      ),
    ];
  }
}