import 'dart:io';
import 'dart:math' as math;
import 'package:google_mlkit_image_labeling/google_mlkit_image_labeling.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'package:easy_localization/easy_localization.dart';
import '../models/detected_material.dart';

/// Servicio mejorado de detección de residuos con algoritmo optimizado para PET
/// Versión 2.0 - Mejorado para detectar botellas PET transparentes sin etiqueta
class WasteDetectionServiceV2 {
  static final WasteDetectionServiceV2 _instance = WasteDetectionServiceV2._internal();
  factory WasteDetectionServiceV2() => _instance;
  WasteDetectionServiceV2._internal();

  ImageLabeler? _imageLabeler;
  TextRecognizer? _textRecognizer;
  bool _isInitialized = false;

  // Mapeo de categorías con índices actualizados
  static const Map<String, int> categoryIndices = {
    'PET': 0,
    'Cartón': 1,
    'TetraPak': 2,
    'Vidrio': 3,
    'Aluminio': 4,
    'Papel': 5,
    'Orgánico': 6,
    'Metal': 7,
    'Bolsas': 8,
    'Poliestireno': 9,
    'Electrónicos': 10,
    'Baterías': 11,
    'Textiles': 12,
  };

  Future<void> initialize() async {
    if (_isInitialized && _imageLabeler != null && _textRecognizer != null) {
      return;
    }
    
    try {
      await dispose();
      await Future.delayed(const Duration(milliseconds: 100));
      
      _imageLabeler = ImageLabeler(
        options: ImageLabelerOptions(
          confidenceThreshold: 0.05, // Threshold más bajo para capturar más etiquetas
        ),
      );
      
      _textRecognizer = TextRecognizer();
      
      _isInitialized = true;
      print('✅ Servicios de detección V2 inicializados correctamente');
    } catch (e) {
      print('❌ Error al inicializar detectores: $e');
      _isInitialized = false;
      await dispose();
    }
  }

  Future<DetectedMaterial?> detectMaterial(File imageFile) async {
    try {
      if (!await imageFile.exists()) {
        throw Exception('El archivo de imagen no existe');
      }
      
      if (!_isInitialized || _imageLabeler == null || _textRecognizer == null) {
        await initialize();
      }
      
      if (_imageLabeler == null || _textRecognizer == null) {
        throw Exception('No se pudo inicializar los detectores');
      }

      print('\n🔍 Procesando imagen: ${imageFile.path}');
      
      InputImage? inputImage;
      try {
        inputImage = InputImage.fromFile(imageFile);
      } catch (e) {
        print('❌ Error al crear InputImage: $e');
        throw Exception('No se pudo procesar la imagen');
      }
      
      // Ejecutar análisis en paralelo
      final results = await Future.wait([
        _imageLabeler!.processImage(inputImage).timeout(
          const Duration(seconds: 10),
          onTimeout: () => <ImageLabel>[],
        ),
        _textRecognizer!.processImage(inputImage).timeout(
          const Duration(seconds: 10),
          onTimeout: () => RecognizedText(text: '', blocks: []),
        ),
      ]);
      
      final labels = results[0] as List<ImageLabel>;
      final recognizedText = results[1] as RecognizedText;
      
      // Logging detallado
      _logDetectionResults(labels, recognizedText);
      
      if (labels.isEmpty && recognizedText.text.isEmpty) {
        return _createUnidentifiedMaterial(imageFile.path);
      }

      // Usar el nuevo algoritmo mejorado
      return _analyzeWithEnhancedAlgorithm(labels, recognizedText, imageFile.path);
    } catch (e) {
      print('❌ Error al detectar material: $e');
      return _createErrorMaterial(imageFile.path);
    }
  }

  void _logDetectionResults(List<ImageLabel> labels, RecognizedText recognizedText) {
    print('\n📊 ====== ANÁLISIS DE IMAGEN ======');
    print('📷 Total etiquetas detectadas: ${labels.length}');
    
    // Mostrar top 15 etiquetas
    for (var i = 0; i < labels.length && i < 15; i++) {
      final label = labels[i];
      final confidence = (label.confidence * 100).toStringAsFixed(1);
      print('   ${(i+1).toString().padLeft(2)}. ${label.label.padRight(20)} - Confianza: $confidence%');
    }
    
    if (recognizedText.text.isNotEmpty) {
      print('\n📝 TEXTO DETECTADO:');
      final textPreview = recognizedText.text.length > 200 
          ? '${recognizedText.text.substring(0, 200)}...'
          : recognizedText.text;
      print(textPreview);
    }
    print('=================================\n');
  }

  DetectedMaterial _analyzeWithEnhancedAlgorithm(
    List<ImageLabel> labels, 
    RecognizedText recognizedText, 
    String imagePath
  ) {
    // Sistema de puntuación multicapa con análisis contextual mejorado
    final scores = MaterialScores();
    final context = _analyzeContext(labels, recognizedText);
    
    print('🧠 ANÁLISIS CONTEXTUAL MEJORADO:');
    print('  • Forma detectada: ${context.shapeType}');
    print('  • Transparencia estimada: ${context.isTransparent ? "Sí" : "No"}');
    print('  • Indicadores de bebida: ${context.beverageIndicators}');
    print('  • Indicadores de plástico: ${context.plasticIndicators}');
    
    // CAPA 1: Análisis de texto OCR (máxima prioridad)
    if (recognizedText.text.isNotEmpty) {
      _analyzeTextLayer(recognizedText.text.toLowerCase(), scores, context);
    }
    
    // CAPA 2: Análisis de forma y características visuales
    _analyzeShapeLayer(labels, scores, context);
    
    // CAPA 3: Análisis de etiquetas ML Kit
    _analyzeLabelLayer(labels, scores, context);
    
    // CAPA 4: Validación cruzada y ajustes contextuales
    _applyCrossValidation(scores, context);
    
    // Determinar resultado final
    return _determineFinalResult(scores, labels, imagePath);
  }

  AnalysisContext _analyzeContext(List<ImageLabel> labels, RecognizedText text) {
    final context = AnalysisContext();
    
    for (final label in labels) {
      final lower = label.label.toLowerCase();
      final confidence = label.confidence;
      
      // Detectar forma de botella
      if (_isBottleShape(lower)) {
        context.shapeType = 'bottle';
        context.shapeConfidence = math.max(context.shapeConfidence, confidence);
      }
      
      // Detectar transparencia
      if (_indicatesTransparency(lower)) {
        context.isTransparent = true;
        context.transparencyConfidence = math.max(context.transparencyConfidence, confidence);
      }
      
      // Indicadores de bebida
      if (_isBeverageIndicator(lower)) {
        context.beverageIndicators++;
        context.beverageConfidence = math.max(context.beverageConfidence, confidence);
      }
      
      // Indicadores de plástico
      if (_isPlasticIndicator(lower)) {
        context.plasticIndicators++;
        context.plasticConfidence = math.max(context.plasticConfidence, confidence);
      }
      
      // Indicadores de metal (para descartar aluminio)
      if (_isMetalIndicator(lower)) {
        context.metalIndicators++;
      }
      
      // Indicadores de vidrio
      if (_isGlassIndicator(lower)) {
        context.glassIndicators++;
      }
    }
    
    // Analizar texto para contexto adicional
    final textLower = text.text.toLowerCase();
    if (textLower.contains('pet') || textLower.contains('plastic')) {
      context.plasticIndicators += 2;
    }
    if (textLower.contains('recycle') || textLower.contains('♻')) {
      context.hasRecyclingSymbol = true;
    }
    
    return context;
  }

  bool _isBottleShape(String label) {
    return label.contains('bottle') ||
           label.contains('botella') ||
           label.contains('cylinder') ||
           label.contains('container') ||
           label.contains('vase') ||
           label.contains('drinkware') ||
           label.contains('tableware');
  }

  bool _indicatesTransparency(String label) {
    return label.contains('glass') ||
           label.contains('crystal') ||
           label.contains('transparent') ||
           label.contains('clear') ||
           label.contains('water') ||
           label.contains('monochrome') ||
           label.contains('ice');
  }

  bool _isBeverageIndicator(String label) {
    return label.contains('drink') ||
           label.contains('beverage') ||
           label.contains('cola') ||
           label.contains('soda') ||
           label.contains('juice') ||
           label.contains('water') ||
           label.contains('liquid') ||
           label.contains('wine');
  }

  bool _isPlasticIndicator(String label) {
    return label.contains('plastic') ||
           label.contains('pet') ||
           label.contains('bottle') ||
           label.contains('container') ||
           label.contains('package');
  }

  bool _isMetalIndicator(String label) {
    return label.contains('metal') ||
           label.contains('aluminum') ||
           label.contains('tin') ||
           label.contains('can') ||
           label.contains('steel') ||
           label.contains('iron');
  }

  bool _isGlassIndicator(String label) {
    return label.contains('glass') && !label.contains('eyeglass') && !label.contains('sunglasses') ||
           label.contains('jar') ||
           label.contains('wine') && label.contains('bottle');
  }

  void _analyzeTextLayer(String text, MaterialScores scores, AnalysisContext context) {
    print('\n📝 CAPA 1: Análisis de texto OCR');
    
    // Palabras clave específicas para PET/Plástico
    final petKeywords = [
      'pet', 'pete', 'plastic', 'plástico',
      'coca', 'cola', 'pepsi', 'sprite', 'fanta',
      'agua', 'water', 'refresco', 'soda',
      'botella', 'bottle', 'envase'
    ];
    
    for (final keyword in petKeywords) {
      if (text.contains(keyword)) {
        scores.addScore('PET', 3.0);
        print('  ✓ Palabra clave PET encontrada: "$keyword"');
      }
    }
    
    // Códigos de reciclaje
    final recyclePattern = RegExp(r'[♻️🔄]?\s*([1-7])');
    final matches = recyclePattern.allMatches(text);
    for (final match in matches) {
      final code = match.group(1);
      if (code == '1') {
        scores.addScore('PET', 5.0); // Código 1 es definitivamente PET
        print('  ✓ Código de reciclaje #1 (PET) detectado');
      }
    }
  }

  void _analyzeShapeLayer(List<ImageLabel> labels, MaterialScores scores, AnalysisContext context) {
    print('\n🔍 CAPA 2: Análisis de forma y características');
    
    // REGLA CLAVE: Botella + Transparente + No metal = Muy probablemente PET
    if (context.shapeType == 'bottle') {
      print('  ✓ Forma de botella detectada (confianza: ${(context.shapeConfidence * 100).toStringAsFixed(1)}%)');
      
      if (context.isTransparent && context.metalIndicators == 0) {
        // Botella transparente sin indicadores de metal = PET con alta probabilidad
        scores.addScore('PET', context.shapeConfidence * 8.0);
        print('  ✓ Botella transparente sin metal → Alta probabilidad de PET');
      } else if (context.plasticIndicators > 0) {
        scores.addScore('PET', context.shapeConfidence * 6.0);
        print('  ✓ Botella con indicadores de plástico');
      } else if (context.glassIndicators > 2) {
        scores.addScore('Vidrio', context.shapeConfidence * 4.0);
        print('  ✓ Botella con múltiples indicadores de vidrio');
      } else {
        // Por defecto, las botellas tienden a ser PET
        scores.addScore('PET', context.shapeConfidence * 4.0);
        print('  ✓ Botella (asumiendo PET por defecto)');
      }
    }
    
    // Si hay indicadores de bebida, reforzar PET
    if (context.beverageIndicators > 0) {
      scores.addScore('PET', context.beverageConfidence * 3.0);
      print('  ✓ Indicadores de bebida detectados');
    }
  }

  void _analyzeLabelLayer(List<ImageLabel> labels, MaterialScores scores, AnalysisContext context) {
    print('\n🏷️ CAPA 3: Análisis de etiquetas ML');
    
    for (final label in labels) {
      final lower = label.label.toLowerCase();
      final confidence = label.confidence;
      
      // Análisis específico para combatir falsos positivos de "metal"
      if (lower == 'tableware' || lower == 'drinkware') {
        // Estos términos a menudo aparecen con botellas PET transparentes
        if (context.isTransparent || context.shapeType == 'bottle') {
          scores.addScore('PET', confidence * 2.5);
          print('  ✓ Tableware/Drinkware + transparente → PET');
        }
        continue;
      }
      
      // Detección directa de materiales específicos
      if (lower.contains('cola') || lower.contains('soda') || lower == 'soft drink') {
        scores.addScore('PET', confidence * 6.0);
        print('  ✓ Bebida gaseosa detectada → PET');
        continue;
      }
      
      if (lower.contains('water') || lower == 'mineral water' || lower == 'drinking water') {
        scores.addScore('PET', confidence * 5.0);
        print('  ✓ Agua embotellada detectada → PET');
        continue;
      }
      
      // Reducir peso de etiquetas ambiguas cuando hay contexto de botella
      if (lower.contains('metal') && context.shapeType == 'bottle' && context.isTransparent) {
        // Si es una botella transparente, el "metal" es probablemente un error
        scores.addScore('Metal', confidence * 0.3); // Peso muy reducido
        print('  ⚠ Metal detectado pero es botella transparente (peso reducido)');
        continue;
      }
      
      // Manejo especial para "aluminum" cuando hay forma de botella
      if ((lower.contains('aluminum') || lower.contains('tin')) && context.shapeType != 'can') {
        scores.addScore('Aluminio', confidence * 0.5); // Peso reducido si no es lata
        continue;
      }
      
      // Análisis estándar para otros materiales
      _analyzeStandardLabel(lower, confidence, scores);
    }
  }

  void _analyzeStandardLabel(String label, double confidence, MaterialScores scores) {
    // PET/Plástico
    if (label.contains('plastic') || label.contains('pet') || 
        label.contains('bottle') && !label.contains('glass')) {
      scores.addScore('PET', confidence * 3.0);
    }
    
    // Aluminio (solo si es claramente una lata)
    else if (label.contains('can') || (label.contains('aluminum') && label.contains('can'))) {
      scores.addScore('Aluminio', confidence * 3.0);
    }
    
    // Vidrio
    else if (label.contains('glass') && !label.contains('eyeglass')) {
      scores.addScore('Vidrio', confidence * 2.5);
    }
    
    // Cartón
    else if (label.contains('cardboard') || label.contains('box')) {
      scores.addScore('Cartón', confidence * 2.0);
    }
    
    // Papel
    else if (label.contains('paper')) {
      scores.addScore('Papel', confidence * 2.0);
    }
  }

  void _applyCrossValidation(MaterialScores scores, AnalysisContext context) {
    print('\n✅ CAPA 4: Validación cruzada');
    
    // REGLA 1: Si tenemos forma de botella + transparencia + no hay fuerte evidencia de vidrio
    if (context.shapeType == 'bottle' && context.isTransparent) {
      if (context.glassIndicators < 2 && context.plasticIndicators >= context.glassIndicators) {
        // Boost significativo para PET
        final currentPET = scores.getScore('PET');
        final currentGlass = scores.getScore('Vidrio');
        
        if (currentPET < currentGlass * 1.5) {
          scores.addScore('PET', 5.0);
          print('  ✓ Ajuste: Botella transparente → Boost PET');
        }
      }
    }
    
    // REGLA 2: Si hay muchos indicadores de bebida sin metal, es PET
    if (context.beverageIndicators > 1 && context.metalIndicators == 0) {
      scores.multiplyScore('PET', 1.5);
      print('  ✓ Ajuste: Múltiples indicadores de bebida sin metal → Boost PET');
    }
    
    // REGLA 3: Reducir aluminio si no hay forma de lata
    if (context.shapeType == 'bottle' || context.isTransparent) {
      scores.multiplyScore('Aluminio', 0.3);
      print('  ✓ Ajuste: No es forma de lata → Reducir aluminio');
    }
    
    // REGLA 4: Si hay símbolo de reciclaje, boost al material líder
    if (context.hasRecyclingSymbol) {
      final topMaterial = scores.getTopMaterial();
      if (topMaterial != null) {
        scores.multiplyScore(topMaterial, 1.2);
        print('  ✓ Ajuste: Símbolo de reciclaje → Boost material líder');
      }
    }
  }

  DetectedMaterial _determineFinalResult(MaterialScores scores, List<ImageLabel> labels, String imagePath) {
    print('\n📊 PUNTUACIONES FINALES:');
    scores.printScores();
    
    final result = scores.getBestMatch();
    
    if (result == null) {
      return _createUnidentifiedMaterial(imagePath);
    }
    
    // Mapear al índice correcto de RecyclableMaterials.categories
    final categoryIndex = categoryIndices[result.category] ?? 0;
    final category = RecyclableMaterials.categories[categoryIndex];
    
    // Determinar nombre descriptivo
    String detectedName = _getDescriptiveName(labels, result.category);
    
    // Ajustar confianza final
    double finalConfidence = result.confidence;
    if (result.category == 'PET' && result.score > 10) {
      finalConfidence = math.min(0.95, finalConfidence * 1.2);
    }
    
    print('\n✅ RESULTADO FINAL: ${category.name}');
    print('   Confianza: ${(finalConfidence * 100).toStringAsFixed(1)}%');
    print('   Puntuación: ${result.score.toStringAsFixed(2)}');
    
    return DetectedMaterial(
      name: detectedName,
      category: category.name,
      confidence: finalConfidence,
      recyclingCode: category.recyclingCode,
      instructions: category.instructions,
      pointsPerKg: category.pointsPerKg,
      imagePath: imagePath,
      detectedAt: DateTime.now(),
    );
  }

  String _getDescriptiveName(List<ImageLabel> labels, String category) {
    // Para PET, usar nombres genéricos descriptivos
    if (category == 'PET') {
      // Analizar las etiquetas para determinar el tipo de botella
      bool isWater = false;
      bool isSoda = false;
      bool isJuice = false;
      bool isBeverage = false;
      
      for (final label in labels) {
        final lower = label.label.toLowerCase();
        if (lower.contains('water') || lower.contains('agua')) {
          isWater = true;
        } else if (lower.contains('cola') || lower.contains('soda') || 
                   lower.contains('soft drink') || lower.contains('refresco')) {
          isSoda = true;
        } else if (lower.contains('juice') || lower.contains('jugo')) {
          isJuice = true;
        } else if (lower.contains('beverage') || lower.contains('drink')) {
          isBeverage = true;
        }
      }
      
      // Retornar descripción apropiada según el tipo detectado (con soporte i18n)
      if (isWater) {
        return 'bottle_water'.tr();
      } else if (isSoda) {
        return 'bottle_soda'.tr();
      } else if (isJuice) {
        return 'bottle_juice'.tr();
      } else if (isBeverage) {
        return 'bottle_beverage'.tr();
      } else {
        // Por defecto para cualquier botella PET
        return 'bottle_pet'.tr();
      }
    } 
    
    // Para Aluminio
    else if (category == 'Aluminio') {
      for (final label in labels) {
        final lower = label.label.toLowerCase();
        if (lower.contains('can')) {
          if (lower.contains('soda') || lower.contains('cola')) {
            return 'can_soda'.tr();
          } else if (lower.contains('beer') || lower.contains('cerveza')) {
            return 'can_beer'.tr();
          } else {
            return 'can_aluminum'.tr();
          }
        }
      }
      return 'can_aluminum'.tr();
    }
    
    // Para Vidrio
    else if (category == 'Vidrio') {
      for (final label in labels) {
        final lower = label.label.toLowerCase();
        if (lower.contains('bottle')) {
          if (lower.contains('wine')) {
            return 'bottle_wine'.tr();
          } else if (lower.contains('beer')) {
            return 'bottle_beer'.tr();
          } else {
            return 'bottle_glass'.tr();
          }
        } else if (lower.contains('jar')) {
          return 'jar_glass'.tr();
        }
      }
      return 'container_glass'.tr();
    }
    
    // Para Cartón
    else if (category == 'Cartón') {
      for (final label in labels) {
        final lower = label.label.toLowerCase();
        if (lower.contains('cereal')) {
          return 'box_cereal'.tr();
        } else if (lower.contains('milk') || lower.contains('leche')) {
          return 'box_milk'.tr();
        } else if (lower.contains('juice') || lower.contains('jugo')) {
          return 'box_juice'.tr();
        }
      }
      return 'box_cardboard'.tr();
    }
    
    // Para Papel
    else if (category == 'Papel') {
      for (final label in labels) {
        final lower = label.label.toLowerCase();
        if (lower.contains('newspaper')) {
          return 'newspaper'.tr();
        } else if (lower.contains('magazine')) {
          return 'magazine'.tr();
        } else if (lower.contains('document')) {
          return 'document'.tr();
        }
      }
      return 'paper'.tr();
    }
    
    // Para TetraPak
    else if (category == 'TetraPak') {
      return 'container_tetrapak'.tr();
    }
    
    // Para otros materiales, usar nombres genéricos
    return category;
  }

  DetectedMaterial _createUnidentifiedMaterial(String imagePath) {
    return DetectedMaterial(
      name: 'Material no identificado',
      category: 'No Identificado',
      confidence: 0.0,
      recyclingCode: 'N/A',
      instructions: 'No se pudo identificar el material. Intenta con otra imagen con mejor iluminación.',
      pointsPerKg: 0,
      imagePath: imagePath,
      detectedAt: DateTime.now(),
    );
  }

  DetectedMaterial _createErrorMaterial(String imagePath) {
    return DetectedMaterial(
      name: 'Error en detección',
      category: 'Error',
      confidence: 0.0,
      recyclingCode: 'N/A',
      instructions: 'Ocurrió un error al procesar la imagen. Por favor intenta de nuevo.',
      pointsPerKg: 0,
      imagePath: imagePath,
      detectedAt: DateTime.now(),
    );
  }

  Future<void> dispose() async {
    if (_isInitialized) {
      if (_imageLabeler != null) {
        await _imageLabeler!.close();
        _imageLabeler = null;
      }
      if (_textRecognizer != null) {
        await _textRecognizer!.close();
        _textRecognizer = null;
      }
      _isInitialized = false;
    }
  }
}

/// Clase para manejar el contexto del análisis
class AnalysisContext {
  String shapeType = 'unknown';
  double shapeConfidence = 0.0;
  bool isTransparent = false;
  double transparencyConfidence = 0.0;
  int beverageIndicators = 0;
  double beverageConfidence = 0.0;
  int plasticIndicators = 0;
  double plasticConfidence = 0.0;
  int metalIndicators = 0;
  int glassIndicators = 0;
  bool hasRecyclingSymbol = false;
}

/// Clase para manejar las puntuaciones de materiales
class MaterialScores {
  final Map<String, double> _scores = {};
  
  void addScore(String material, double score) {
    _scores[material] = (_scores[material] ?? 0) + score;
  }
  
  void multiplyScore(String material, double factor) {
    if (_scores.containsKey(material)) {
      _scores[material] = _scores[material]! * factor;
    }
  }
  
  double getScore(String material) {
    return _scores[material] ?? 0;
  }
  
  String? getTopMaterial() {
    if (_scores.isEmpty) return null;
    return _scores.entries
        .reduce((a, b) => a.value > b.value ? a : b)
        .key;
  }
  
  MaterialResult? getBestMatch() {
    if (_scores.isEmpty) return null;
    
    final best = _scores.entries
        .reduce((a, b) => a.value > b.value ? a : b);
    
    // Calcular confianza normalizada
    final maxPossibleScore = 20.0; // Ajustar según el algoritmo
    final confidence = math.min(0.95, best.value / maxPossibleScore);
    
    return MaterialResult(
      category: best.key,
      score: best.value,
      confidence: confidence,
    );
  }
  
  void printScores() {
    final sorted = _scores.entries.toList()
      ..sort((a, b) => b.value.compareTo(a.value));
    
    for (final entry in sorted) {
      print('  ${entry.key}: ${entry.value.toStringAsFixed(2)}');
    }
  }
}

/// Resultado del análisis de material
class MaterialResult {
  final String category;
  final double score;
  final double confidence;
  
  MaterialResult({
    required this.category,
    required this.score,
    required this.confidence,
  });
}