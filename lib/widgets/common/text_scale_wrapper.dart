import 'package:flutter/material.dart';

/// Widget wrapper que ignora las configuraciones de escala de texto del sistema
/// Útil para prevenir problemas de overflow en diseños sensibles
class TextScaleWrapper extends StatelessWidget {
  final Widget child;
  final double? textScaleFactor;
  final bool ignoreBoldText;

  const TextScaleWrapper({
    Key? key,
    required this.child,
    this.textScaleFactor = 1.0,
    this.ignoreBoldText = true,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MediaQuery(
      data: MediaQuery.of(context).copyWith(
        textScaler: TextScaler.linear(textScaleFactor ?? 1.0),
        boldText: ignoreBoldText ? false : MediaQuery.of(context).boldText,
      ),
      child: child,
    );
  }
}

/// Widget que envuelve solo un Text específico para ignorar la escala
class FixedScaleText extends StatelessWidget {
  final String text;
  final TextStyle? style;
  final TextAlign? textAlign;
  final int? maxLines;
  final TextOverflow? overflow;
  final double scaleFactor;

  const FixedScaleText(
    this.text, {
    Key? key,
    this.style,
    this.textAlign,
    this.maxLines,
    this.overflow,
    this.scaleFactor = 1.0,
  }) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MediaQuery(
      data: MediaQuery.of(context).copyWith(
        textScaler: TextScaler.linear(scaleFactor),
      ),
      child: Text(
        text,
        style: style,
        textAlign: textAlign,
        maxLines: maxLines,
        overflow: overflow,
      ),
    );
  }
}

/// Extensión para aplicar fácilmente escala fija a cualquier widget
extension TextScaleExtension on Widget {
  Widget withFixedTextScale([double scale = 1.0]) {
    return Builder(
      builder: (context) => MediaQuery(
        data: MediaQuery.of(context).copyWith(
          textScaler: TextScaler.linear(scale),
        ),
        child: this,
      ),
    );
  }
  
  Widget ignoringTextScaling() {
    return withFixedTextScale(1.0);
  }
}