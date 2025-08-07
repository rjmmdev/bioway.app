import 'package:flutter/material.dart';

class ProductoDescuento {
  final String id;
  final String nombre;
  final String descripcion;
  final double precioOriginal;
  final double precioDescuento;
  final int porcentajeDescuento;
  final String imagen;
  final String comercioId;
  final String comercioNombre;
  final DateTime fechaInicio;
  final DateTime fechaFin;
  final bool activo;
  final bool destacado;
  final IconData icono;
  final int bioCoinsCosto;
  final int descuentoPorcentaje;

  ProductoDescuento({
    required this.id,
    required this.nombre,
    required this.descripcion,
    required this.precioOriginal,
    required this.precioDescuento,
    required this.porcentajeDescuento,
    required this.imagen,
    required this.comercioId,
    required this.comercioNombre,
    required this.fechaInicio,
    required this.fechaFin,
    required this.activo,
    this.destacado = false,
    this.icono = Icons.card_giftcard,
    this.bioCoinsCosto = 100,
    this.descuentoPorcentaje = 10,
  });

  factory ProductoDescuento.fromMap(Map<String, dynamic> map) {
    return ProductoDescuento(
      id: map['id'] ?? '',
      nombre: map['nombre'] ?? '',
      descripcion: map['descripcion'] ?? '',
      precioOriginal: map['precioOriginal']?.toDouble() ?? 0.0,
      precioDescuento: map['precioDescuento']?.toDouble() ?? 0.0,
      porcentajeDescuento: map['porcentajeDescuento'] ?? 0,
      imagen: map['imagen'] ?? '',
      comercioId: map['comercioId'] ?? '',
      comercioNombre: map['comercioNombre'] ?? '',
      fechaInicio: map['fechaInicio'] != null
          ? DateTime.parse(map['fechaInicio'])
          : DateTime.now(),
      fechaFin: map['fechaFin'] != null
          ? DateTime.parse(map['fechaFin'])
          : DateTime.now().add(Duration(days: 30)),
      activo: map['activo'] ?? true,
      destacado: map['destacado'] ?? false,
      icono: _getIconFromString(map['icono'] ?? 'card_giftcard'),
      bioCoinsCosto: map['bioCoinsCosto'] ?? 100,
      descuentoPorcentaje: map['descuentoPorcentaje'] ?? 10,
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'nombre': nombre,
      'descripcion': descripcion,
      'precioOriginal': precioOriginal,
      'precioDescuento': precioDescuento,
      'porcentajeDescuento': porcentajeDescuento,
      'imagen': imagen,
      'comercioId': comercioId,
      'comercioNombre': comercioNombre,
      'fechaInicio': fechaInicio.toIso8601String(),
      'fechaFin': fechaFin.toIso8601String(),
      'activo': activo,
      'destacado': destacado,
      'icono': icono,
      'bioCoinsCosto': bioCoinsCosto,
      'descuentoPorcentaje': descuentoPorcentaje,
    };
  }

  bool get estaVigente {
    final now = DateTime.now();
    return now.isAfter(fechaInicio) && now.isBefore(fechaFin) && activo;
  }
  
  static List<ProductoDescuento> getMockProductos() {
    return [
      ProductoDescuento(
        id: '1',
        nombre: 'Café Orgánico',
        descripcion: 'Café orgánico de comercio justo',
        precioOriginal: 150.0,
        precioDescuento: 120.0,
        porcentajeDescuento: 20,
        imagen: 'https://via.placeholder.com/150',
        comercioId: '1',
        comercioNombre: 'Café Verde',
        fechaInicio: DateTime.now(),
        fechaFin: DateTime.now().add(Duration(days: 30)),
        activo: true,
        destacado: true,
        icono: Icons.local_cafe,
        bioCoinsCosto: 150,
        descuentoPorcentaje: 20,
      ),
    ];
  }
  
  static IconData _getIconFromString(String iconName) {
    switch (iconName) {
      case 'local_cafe':
        return Icons.local_cafe;
      case 'restaurant':
        return Icons.restaurant;
      case 'shopping_cart':
        return Icons.shopping_cart;
      default:
        return Icons.card_giftcard;
    }
  }
}