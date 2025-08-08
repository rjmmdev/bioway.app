import 'package:flutter/material.dart';
import '../../utils/colors.dart';

class MaterialReciclable {
  final String id;
  final String codigo;
  final String nombre;
  final String categoria;
  final Color color;
  final IconData icono;
  final String descripcion;
  final String instrucciones;
  final double puntosBase;
  final double valorKg;

  MaterialReciclable({
    required this.id,
    required this.codigo,
    required this.nombre,
    required this.categoria,
    required this.color,
    required this.icono,
    required this.descripcion,
    required this.instrucciones,
    required this.puntosBase,
    required this.valorKg,
  });

  double get puntosPerKg => puntosBase;

  static MaterialReciclable? findById(String id) {
    try {
      return materiales.firstWhere((m) => m.id == id);
    } catch (e) {
      return null;
    }
  }

  static List<MaterialReciclable> materiales = [
    MaterialReciclable(
      id: 'pet_tipo1',
      codigo: 'PET1',
      nombre: 'PET Tipo 1',
      categoria: 'plasticos',
      color: BioWayColors.petBlue,
      icono: Icons.local_drink,
      descripcion: 'Botellas de agua y refrescos',
      instrucciones: 'Lavar y quitar etiquetas',
      puntosBase: 10,
      valorKg: 5.50,
    ),
    MaterialReciclable(
      id: 'hdpe',
      codigo: 'HDPE',
      nombre: 'HDPE',
      categoria: 'plasticos',
      color: BioWayColors.hdpeGreen,
      icono: Icons.cleaning_services,
      descripcion: 'Envases de detergente y shampoo',
      instrucciones: 'Enjuagar bien',
      puntosBase: 8,
      valorKg: 4.50,
    ),
    MaterialReciclable(
      id: 'carton',
      codigo: 'CARTON',
      nombre: 'Cartón',
      categoria: 'papel',
      color: BioWayColors.multilaminadoBrown,
      icono: Icons.inventory_2,
      descripcion: 'Cajas y empaques',
      instrucciones: 'Aplanar y quitar cintas',
      puntosBase: 5,
      valorKg: 2.50,
    ),
    MaterialReciclable(
      id: 'papel',
      codigo: 'PAPEL',
      nombre: 'Papel',
      categoria: 'papel',
      color: Colors.blueGrey,
      icono: Icons.description,
      descripcion: 'Periódico, revistas, papel de oficina',
      instrucciones: 'Sin grapas ni clips',
      puntosBase: 4,
      valorKg: 2.00,
    ),
    MaterialReciclable(
      id: 'vidrio',
      codigo: 'VIDRIO',
      nombre: 'Vidrio',
      categoria: 'vidrio',
      color: BioWayColors.glassGreen,
      icono: Icons.wine_bar,
      descripcion: 'Botellas y frascos de vidrio',
      instrucciones: 'Limpiar y quitar tapas',
      puntosBase: 6,
      valorKg: 1.50,
    ),
    MaterialReciclable(
      id: 'metal',
      codigo: 'METAL',
      nombre: 'Metal',
      categoria: 'metal',
      color: BioWayColors.metalGrey,
      icono: Icons.recycling,
      descripcion: 'Latas de aluminio y acero',
      instrucciones: 'Aplastar para ahorrar espacio',
      puntosBase: 12,
      valorKg: 8.00,
    ),
    MaterialReciclable(
      id: 'raspa_cuero',
      codigo: 'RASPA',
      nombre: 'Raspa de Cuero',
      categoria: 'especiales',
      color: BioWayColors.multilaminadoBrown,
      icono: Icons.cut,
      descripcion: 'Residuos de cuero industrial',
      instrucciones: 'Solo para empresas autorizadas',
      puntosBase: 15,
      valorKg: 10.00,
    ),
  ];
}