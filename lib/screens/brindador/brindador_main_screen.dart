import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../../utils/colors.dart';
import 'widgets/brindador_bottom_navigation_bar.dart';
import 'brindador_dashboard_screen.dart';
import 'brindador_comercio_local_screen.dart';
import 'brindador_perfil_competencias_screen.dart';

class BrindadorMainScreen extends StatefulWidget {
  const BrindadorMainScreen({super.key});

  @override
  State<BrindadorMainScreen> createState() => _BrindadorMainScreenState();
}

class _BrindadorMainScreenState extends State<BrindadorMainScreen> {
  final PageController _pageController = PageController();
  int _currentIndex = 0;
  bool _isNavigating = false;

  @override
  void dispose() {
    _pageController.dispose();
    super.dispose();
  }

  void _onNavItemTapped(int index) {
    if (index == _currentIndex || _isNavigating) return;
    
    HapticFeedback.lightImpact();
    
    _isNavigating = true;
    
    // Actualizar el índice inmediatamente para respuesta visual rápida
    setState(() {
      _currentIndex = index;
    });
    
    // Navegar a la página con animación
    _pageController.animateToPage(
      index,
      duration: const Duration(milliseconds: 250),
      curve: Curves.easeInOutCubic,
    ).then((_) {
      _isNavigating = false;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: PageView(
        controller: _pageController,
        physics: const NeverScrollableScrollPhysics(), // Deshabilitamos el swipe manual
        children: const [
          BrindadorDashboardScreen(),
          BrindadorComercioLocalScreen(),
          BrindadorPerfilCompetenciasScreen(),
        ],
      ),
      bottomNavigationBar: BrindadorBottomNavigationBar(
        currentIndex: _currentIndex,
        onTap: _onNavItemTapped,
      ),
    );
  }
}