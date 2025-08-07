import 'package:flutter/material.dart';

class BioWayLevels {
  static const Map<int, LevelInfo> levels = {
    1: LevelInfo(
      nombre: 'Principiante',
      puntosMinimos: 0,
      puntosMaximos: 100,
      color: Colors.green,
      icono: Icons.eco,
    ),
    2: LevelInfo(
      nombre: 'Aprendiz',
      puntosMinimos: 101,
      puntosMaximos: 500,
      color: Colors.lightGreen,
      icono: Icons.nature,
    ),
    3: LevelInfo(
      nombre: 'Colaborador',
      puntosMinimos: 501,
      puntosMaximos: 1500,
      color: Colors.blue,
      icono: Icons.recycling,
    ),
    4: LevelInfo(
      nombre: 'Experto',
      puntosMinimos: 1501,
      puntosMaximos: 3000,
      color: Colors.purple,
      icono: Icons.star,
    ),
    5: LevelInfo(
      nombre: 'Maestro',
      puntosMinimos: 3001,
      puntosMaximos: 999999,
      color: Colors.orange,
      icono: Icons.workspace_premium,
    ),
  };

  static LevelInfo getLevelInfo(int nivel) {
    return levels[nivel] ?? levels[1]!;
  }

  static int calculateLevel(int puntos) {
    for (var entry in levels.entries) {
      if (puntos >= entry.value.puntosMinimos &&
          puntos <= entry.value.puntosMaximos) {
        return entry.key;
      }
    }
    return 1;
  }

  static double calculateProgress(int puntos, int nivel) {
    final levelInfo = getLevelInfo(nivel);
    final rangoTotal =
        levelInfo.puntosMaximos - levelInfo.puntosMinimos;
    final puntosEnNivel = puntos - levelInfo.puntosMinimos;
    return (puntosEnNivel / rangoTotal).clamp(0.0, 1.0);
  }
}

class LevelInfo {
  final String nombre;
  final int puntosMinimos;
  final int puntosMaximos;
  final Color color;
  final IconData icono;

  const LevelInfo({
    required this.nombre,
    required this.puntosMinimos,
    required this.puntosMaximos,
    required this.color,
    required this.icono,
  });
}