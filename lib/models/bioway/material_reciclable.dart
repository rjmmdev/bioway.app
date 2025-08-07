import 'package:flutter/material.dart';

class MaterialReciclable {
  final String id;
  final String nombre;
  final Color color;
  final IconData icono;
  final String categoria;
  
  const MaterialReciclable({
    required this.id,
    required this.nombre,
    required this.color,
    required this.icono,
    required this.categoria,
  });
  
  static const List<MaterialReciclable> materiales = [
    MaterialReciclable(
      id: 'pet_tipo1',
      nombre: 'PET',
      color: Color(0xFF4CAF50),
      icono: Icons.local_drink,
      categoria: 'Plástico',
    ),
    MaterialReciclable(
      id: 'hdpe',
      nombre: 'HDPE',
      color: Color(0xFF2196F3),
      icono: Icons.water_drop,
      categoria: 'Plástico',
    ),
    MaterialReciclable(
      id: 'papel',
      nombre: 'Papel',
      color: Color(0xFFFF9800),
      icono: Icons.description,
      categoria: 'Papel',
    ),
    MaterialReciclable(
      id: 'carton',
      nombre: 'Cartón',
      color: Color(0xFF795548),
      icono: Icons.inventory_2,
      categoria: 'Papel',
    ),
    MaterialReciclable(
      id: 'vidrio',
      nombre: 'Vidrio',
      color: Color(0xFF00BCD4),
      icono: Icons.wine_bar,
      categoria: 'Vidrio',
    ),
    MaterialReciclable(
      id: 'metal',
      nombre: 'Metal',
      color: Color(0xFF9E9E9E),
      icono: Icons.build,
      categoria: 'Metal',
    ),
  ];
  
  static MaterialReciclable? getById(String id) {
    try {
      return materiales.firstWhere((m) => m.id == id);
    } catch (e) {
      return null;
    }
  }
}