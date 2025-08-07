import 'package:flutter/material.dart';

class BioWayColors {
  // Colores principales
  static Color primaryGreen = const Color(0xFF4CAF50);
  static Color darkGreen = const Color(0xFF2E7D32);
  static Color lightGreen = const Color(0xFF81C784);
  static Color accentGreen = const Color(0xFF66BB6A);
  
  // Colores secundarios
  static Color orange = const Color(0xFFFF9800);
  static Color blue = const Color(0xFF2196F3);
  static Color red = const Color(0xFFF44336);
  static Color yellow = const Color(0xFFFFEB3B);
  
  // Colores neutros
  static Color white = const Color(0xFFFFFFFF);
  static Color black = const Color(0xFF000000);
  static Color grey = const Color(0xFF9E9E9E);
  static Color lightGrey = const Color(0xFFE0E0E0);
  static Color darkGrey = const Color(0xFF616161);
  
  // Gradientes
  static List<Color> backgroundGradient = [
    const Color(0xFFF5F5F5),
    const Color(0xFFE8F5E9),
  ];
  
  static List<Color> primaryGradient = [
    primaryGreen,
    darkGreen,
  ];
  
  // Colores para categorías específicas
  static Color plastico = const Color(0xFF1976D2);
  static Color papel = const Color(0xFF388E3C);
  static Color vidrio = const Color(0xFF7B1FA2);
  static Color metal = const Color(0xFF455A64);
  static Color organico = const Color(0xFF6D4C41);
  static Color peligroso = const Color(0xFFD32F2F);
  static Color electronicos = const Color(0xFF424242);
}