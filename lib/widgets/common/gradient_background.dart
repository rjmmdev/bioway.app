import 'package:flutter/material.dart';
import '../../utils/colors.dart';

class GradientBackground extends StatelessWidget {
  final Widget child;
  final bool showPattern;
  
  const GradientBackground({
    super.key,
    required this.child,
    this.showPattern = false,
  });
  
  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [
            BioWayColors.primaryGreen,
            BioWayColors.mediumGreen,
          ],
        ),
      ),
      child: Stack(
        children: [
          if (showPattern) _buildPattern(),
          child,
        ],
      ),
    );
  }
  
  Widget _buildPattern() {
    return Positioned.fill(
      child: CustomPaint(
        painter: PatternPainter(),
      ),
    );
  }
}

class PatternPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = Colors.white.withOpacity(0.05)
      ..style = PaintingStyle.fill;
    
    const double spacing = 40;
    const double radius = 3;
    
    for (double x = 0; x < size.width; x += spacing) {
      for (double y = 0; y < size.height; y += spacing) {
        canvas.drawCircle(Offset(x, y), radius, paint);
      }
    }
  }
  
  @override
  bool shouldRepaint(CustomPainter oldDelegate) => false;
}