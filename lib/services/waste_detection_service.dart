import 'dart:io';
import 'package:google_mlkit_image_labeling/google_mlkit_image_labeling.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import '../models/detected_material.dart';

class WasteDetectionService {
  static final WasteDetectionService _instance = WasteDetectionService._internal();
  factory WasteDetectionService() => _instance;
  WasteDetectionService._internal();

  ImageLabeler? _imageLabeler;
  TextRecognizer? _textRecognizer;
  bool _isInitialized = false;

  Future<void> initialize() async {
    // Solo inicializar si no está ya inicializado
    if (_isInitialized && _imageLabeler != null && _textRecognizer != null) {
      return;
    }
    
    try {
      // Cerrar y limpiar recursos anteriores si existen
      await dispose();
      
      // Pequeña pausa para asegurar liberación de recursos
      await Future.delayed(const Duration(milliseconds: 100));
      
      _imageLabeler = ImageLabeler(
        options: ImageLabelerOptions(
          confidenceThreshold: 0.1, // Bajamos para ver más etiquetas
        ),
      );
      
      _textRecognizer = TextRecognizer();
      
      _isInitialized = true;
      print('Servicios de detección inicializados correctamente');
    } catch (e) {
      print('Error al inicializar detectores: $e');
      _isInitialized = false;
      // Limpiar en caso de error
      await dispose();
    }
  }

  Future<DetectedMaterial?> detectMaterial(File imageFile) async {
    try {
      // Verificar que el archivo existe
      if (!await imageFile.exists()) {
        throw Exception('El archivo de imagen no existe');
      }
      
      // Asegurar inicialización
      if (!_isInitialized || _imageLabeler == null || _textRecognizer == null) {
        await initialize();
      }
      
      if (_imageLabeler == null || _textRecognizer == null) {
        throw Exception('No se pudo inicializar los detectores');
      }

      print('Procesando imagen: ${imageFile.path}');
      
      // Crear InputImage con manejo de errores
      InputImage? inputImage;
      try {
        inputImage = InputImage.fromFile(imageFile);
      } catch (e) {
        print('Error al crear InputImage: $e');
        throw Exception('No se pudo procesar la imagen');
      }
      
      // Ejecutar análisis con timeout para evitar bloqueos
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
      
      // LOG DETALLADO PARA DEBUGGING
      print('====== DETECCIÓN DE MATERIAL ======');
      print('Total etiquetas detectadas: ${labels.length}');
      for (var i = 0; i < labels.length && i < 10; i++) {
        print('  ${i+1}. ${labels[i].label} - Confianza: ${(labels[i].confidence * 100).toStringAsFixed(1)}%');
      }
      
      // Mostrar texto detectado
      if (recognizedText.text.isNotEmpty) {
        print('\n📝 TEXTO DETECTADO:');
        final textPreview = recognizedText.text.length > 200 
            ? '${recognizedText.text.substring(0, 200)}...'
            : recognizedText.text;
        print(textPreview);
      }
      print('==================================');
      
      if (labels.isEmpty && recognizedText.text.isEmpty) {
        return DetectedMaterial(
          name: 'Material no identificado',
          category: 'No Identificado',
          confidence: 0.0,
          recyclingCode: 'N/A',
          instructions: 'No se pudo identificar el material. Intenta con otra imagen.',
          pointsPerKg: 0,
          imagePath: imageFile.path,
          detectedAt: DateTime.now(),
        );
      }

      // Analizar con información combinada
      return _analyzeWithContext(labels, recognizedText, imageFile.path);
    } catch (e) {
      print('Error al detectar material: $e');
      return DetectedMaterial(
        name: 'Error en detección',
        category: 'Error',
        confidence: 0.0,
        recyclingCode: 'N/A',
        instructions: 'Ocurrió un error al procesar la imagen. Por favor intenta de nuevo.',
        pointsPerKg: 0,
        imagePath: imageFile.path,
        detectedAt: DateTime.now(),
      );
    }
  }

  DetectedMaterial _analyzeWithContext(List<ImageLabel> labels, RecognizedText recognizedText, String imagePath) {
    // Análisis de texto OCR para palabras clave
    final textLower = recognizedText.text.toLowerCase();
    final textIndicators = _analyzeTextForMaterialIndicators(textLower);
    
    print('\n📊 ANÁLISIS CONTEXTUAL:');
    if (textIndicators.isNotEmpty) {
      print('Indicadores de texto encontrados:');
      textIndicators.forEach((material, keywords) {
        print('  • $material: ${keywords.join(', ')}');
      });
    }
    
    // Usar el análisis mejorado con información de texto
    return _analyzeAndCategorize(labels, imagePath, textIndicators);
  }
  
  Map<String, List<String>> _analyzeTextForMaterialIndicators(String text) {
    final indicators = <String, List<String>>{};
    
    // Palabras clave para TETRA PAK
    final tetraPakKeywords = [
      'tetra', 'tetrapak', 'tetra pak',
      'leche', 'milk', 'lácteo', 'dairy',
      'jugo', 'juice', 'zumo',
      'alpura', 'lala', 'santa clara', 'sello rojo',
      'jumex', 'del valle', 'boing',
      'ultra pasteurizada', 'uht', 'larga vida'
    ];
    
    // Palabras clave para CARTÓN/PAPEL
    final cardboardKeywords = [
      'cereal', 'cereales', 'kellogg', 'nestle', 'quaker',
      'caja', 'cartón', 'cardboard', 'box',
      'fibra', 'fiber', 'granos', 'grain',
      'recicla', 'recycle', 'papel', 'paper',
      'cartulina', 'carton'
    ];
    
    // Palabras clave para PLÁSTICO
    final plasticKeywords = [
      'pet', 'pete', 'hdpe', 'ldpe', 'pp', 'ps',
      'plástico', 'plastic', 'polietileno', 'polipropileno',
      'botella', 'bottle', 'envase', 'container',
      'coca', 'cola', 'pepsi', 'sprite', 'fanta',
      'agua', 'water', 'refresco', 'soda'
    ];
    
    // Palabras clave para VIDRIO
    final glassKeywords = [
      'vidrio', 'glass', 'cristal',
      'frasco', 'jar', 'conserva',
      'vino', 'wine', 'cerveza', 'beer',
      'mermelada', 'jam'
    ];
    
    // Palabras clave para METAL/ALUMINIO
    final metalKeywords = [
      'aluminio', 'aluminum', 'alu',
      'lata', 'can', 'tin',
      'metal', 'acero', 'steel',
      'conserva', 'atún', 'tuna'
    ];
    
    // Palabras clave para ORGÁNICO
    final organicKeywords = [
      'orgánico', 'organic', 'compost',
      'biodegradable', 'natural',
      'fruta', 'fruit', 'verdura', 'vegetable',
      'cáscara', 'peel', 'restos', 'waste'
    ];
    
    // Buscar indicadores en el texto
    // Tetra Pak primero (mayor prioridad)
    for (final keyword in tetraPakKeywords) {
      if (text.contains(keyword)) {
        indicators.putIfAbsent('Tetra Pak', () => []).add(keyword);
      }
    }
    
    for (final keyword in cardboardKeywords) {
      if (text.contains(keyword)) {
        indicators.putIfAbsent('Cartón', () => []).add(keyword);
      }
    }
    
    for (final keyword in plasticKeywords) {
      if (text.contains(keyword)) {
        indicators.putIfAbsent('Plástico', () => []).add(keyword);
      }
    }
    
    for (final keyword in glassKeywords) {
      if (text.contains(keyword)) {
        indicators.putIfAbsent('Vidrio', () => []).add(keyword);
      }
    }
    
    for (final keyword in metalKeywords) {
      if (text.contains(keyword)) {
        indicators.putIfAbsent('Metal', () => []).add(keyword);
      }
    }
    
    for (final keyword in organicKeywords) {
      if (text.contains(keyword)) {
        indicators.putIfAbsent('Orgánico', () => []).add(keyword);
      }
    }
    
    // Buscar códigos de reciclaje específicos
    final recycleCodePattern = RegExp(r'\b(pet|pete|hdpe|pvc|ldpe|pp|ps|other)\b', caseSensitive: false);
    final matches = recycleCodePattern.allMatches(text);
    for (final match in matches) {
      indicators.putIfAbsent('Plástico', () => []).add('Código: ${match.group(0)}');
    }
    
    // Buscar números de reciclaje (1-7)
    final recycleNumberPattern = RegExp(r'[♻️🔄]?\s*([1-7])\s*(?:pet|hdpe|pvc|ldpe|pp|ps)?', caseSensitive: false);
    final numberMatches = recycleNumberPattern.allMatches(text);
    for (final match in numberMatches) {
      final number = match.group(1);
      if (number == '1' || number == '2' || number == '4' || number == '5') {
        indicators.putIfAbsent('Plástico', () => []).add('Reciclaje #$number');
      }
    }
    
    return indicators;
  }
  
  DetectedMaterial _analyzeAndCategorize(List<ImageLabel> labels, String imagePath, [Map<String, List<String>>? textIndicators]) {
    // Sistema de puntuación mejorado con prioridades
    final categoryScores = <MaterialCategory, double>{};
    final detectedKeywords = <String, MaterialCategory>{};
    
    // PRIORIDAD MÁXIMA: Si el OCR detectó palabras clave claras, darles máxima prioridad
    if (textIndicators != null && textIndicators.isNotEmpty) {
      print('\n🎯 APLICANDO INDICADORES DE TEXTO OCR:');
      
      // Tetra Pak detectado por OCR tiene máxima prioridad
      if (textIndicators.containsKey('Tetra Pak')) {
        final keywords = textIndicators['Tetra Pak']!;
        _addScore(categoryScores, RecyclableMaterials.categories[2], 8.0); // Tetra Pak (índice 2)
        print('  ✅ TETRA PAK detectado por OCR (${keywords.join(', ')}) - Boost: 8.0');
      }
      
      // Cartón/Cereal detectado por OCR tiene segunda prioridad
      else if (textIndicators.containsKey('Cartón')) {
        final keywords = textIndicators['Cartón']!;
        final isCereal = keywords.any((k) => ['cereal', 'cereales', 'kellogg', 'nestle', 'quaker'].contains(k));
        final boost = isCereal ? 8.0 : 5.0; // Mayor boost si es específicamente cereal
        _addScore(categoryScores, RecyclableMaterials.categories[1], boost); // Cartón
        print('  ✅ CARTÓN detectado por OCR (${keywords.join(', ')}) - Boost: $boost');
      }
      
      // Plástico detectado por OCR
      if (textIndicators.containsKey('Plástico')) {
        final keywords = textIndicators['Plástico']!;
        _addScore(categoryScores, RecyclableMaterials.categories[0], 6.0); // PET
        print('  ✅ PLÁSTICO detectado por OCR (${keywords.join(', ')})');
      }
      
      // Metal detectado por OCR
      if (textIndicators.containsKey('Metal')) {
        final keywords = textIndicators['Metal']!;
        final isAluminum = keywords.any((k) => k.contains('aluminio') || k.contains('aluminum') || k.contains('lata'));
        if (isAluminum) {
          _addScore(categoryScores, RecyclableMaterials.categories[4], 6.0); // Aluminio
          print('  ✅ ALUMINIO detectado por OCR (${keywords.join(', ')})');
        } else {
          _addScore(categoryScores, RecyclableMaterials.categories[7], 5.0); // Metal genérico
          print('  ✅ METAL detectado por OCR (${keywords.join(', ')})');
        }
      }
      
      // Vidrio detectado por OCR
      if (textIndicators.containsKey('Vidrio')) {
        final keywords = textIndicators['Vidrio']!;
        _addScore(categoryScores, RecyclableMaterials.categories[3], 5.0); // Vidrio
        print('  ✅ VIDRIO detectado por OCR (${keywords.join(', ')})');
      }
    }
    
    // PRIORIDAD SECUNDARIA: Detección por visión
    bool isBeverage = false;
    bool hasPlasticIndicator = false;
    bool hasGlassIndicator = false;
    double maxBeverageConfidence = 0.0;
    
    // Primera pasada: identificar indicadores clave
    for (final label in labels) {
      final lowerLabel = label.label.toLowerCase();
      final confidence = label.confidence;
      
      // Detectar bebidas específicas (ALTA PRIORIDAD)
      if (lowerLabel.contains('cola') || 
          lowerLabel.contains('soda') ||
          lowerLabel.contains('soft drink') ||
          lowerLabel.contains('coca') ||
          lowerLabel.contains('pepsi') ||
          lowerLabel.contains('sprite') ||
          lowerLabel.contains('fanta') ||
          lowerLabel.contains('juice') ||
          lowerLabel.contains('beverage') ||
          lowerLabel.contains('drink')) {
        isBeverage = true;
        maxBeverageConfidence = confidence > maxBeverageConfidence ? confidence : maxBeverageConfidence;
        print('🥤 BEBIDA DETECTADA: "$lowerLabel" con ${(confidence * 100).toStringAsFixed(1)}% confianza');
      }
      
      // Detectar indicadores de plástico
      if (lowerLabel.contains('plastic') || 
          lowerLabel.contains('pet') ||
          lowerLabel.contains('bottle') ||
          lowerLabel.contains('container')) {
        hasPlasticIndicator = true;
      }
      
      // Detectar indicadores de vidrio
      if (lowerLabel.contains('glass') || 
          lowerLabel.contains('wine') ||
          lowerLabel.contains('beer') ||
          lowerLabel.contains('jar')) {
        hasGlassIndicator = true;
      }
    }
    
    // Si detectamos una bebida con alta confianza, es muy probable que sea PET
    if (isBeverage && maxBeverageConfidence > 0.3) {
      // Las bebidas como Cola son casi siempre en botellas PET
      if (!hasGlassIndicator || hasPlasticIndicator) {
        _addScore(categoryScores, RecyclableMaterials.categories[0], maxBeverageConfidence * 5.0); // PET - peso máximo
        print('  ✅ CONFIRMADO: BOTELLA PET DE BEBIDA (alta prioridad)');
      }
    }
    
    // Analizar cada etiqueta con el contexto establecido
    for (final label in labels) {
      final lowerLabel = label.label.toLowerCase();
      final confidence = label.confidence;
      
      print('Analizando etiqueta: "$lowerLabel" con confianza: ${(confidence * 100).toStringAsFixed(1)}%');
      
      // PRIORIDAD 1: Cola/Soda = Botella PET
      if (lowerLabel == 'cola' || 
          lowerLabel == 'soda' ||
          lowerLabel == 'coca cola' ||
          lowerLabel == 'soft drink') {
        _addScore(categoryScores, RecyclableMaterials.categories[0], confidence * 4.0); // PET
        print('  -> Detectado como BOTELLA PET (bebida gaseosa)');
        continue; // Saltar otros análisis para esta etiqueta
      }
      
      // PRIORIDAD 2: Botellas en general
      if (lowerLabel.contains('bottle') || 
          lowerLabel.contains('botella')) {
        
        // Si ya sabemos que es una bebida, es PET
        if (isBeverage) {
          _addScore(categoryScores, RecyclableMaterials.categories[0], confidence * 3.5); // PET
          print('  -> Detectado como BOTELLA PET');
        } else if (!hasGlassIndicator) {
          // Por defecto, las botellas son PET a menos que haya evidencia de vidrio
          _addScore(categoryScores, RecyclableMaterials.categories[0], confidence * 3.0); // PET
          print('  -> Detectado como posible BOTELLA PET');
        } else {
          _addScore(categoryScores, RecyclableMaterials.categories[3], confidence * 2.0); // Vidrio
          print('  -> Detectado como posible BOTELLA DE VIDRIO');
        }
      }
      
      // Bebidas y jugos
      else if (lowerLabel.contains('juice') || 
               lowerLabel.contains('beverage') ||
               lowerLabel.contains('drink') ||
               lowerLabel.contains('water')) {
        _addScore(categoryScores, RecyclableMaterials.categories[0], confidence * 2.5); // PET
        print('  -> Detectado como ENVASE DE BEBIDA (PET)');
      }
      
      // Detección directa de plástico
      else if (lowerLabel.contains('plastic') || 
               lowerLabel.contains('plástico') ||
               lowerLabel.contains('pet')) {
        _addScore(categoryScores, RecyclableMaterials.categories[0], confidence * 2.5); // PET
        print('  -> Detectado como PLÁSTICO');
      }
      
      // PRIORIDAD 2: Latas de metal/aluminio
      else if (lowerLabel.contains('can') || 
               lowerLabel.contains('lata') ||
               lowerLabel.contains('aluminum') ||
               lowerLabel.contains('aluminio') ||
               lowerLabel.contains('tin')) {
        _addScore(categoryScores, RecyclableMaterials.categories[4], confidence * 2.5); // Aluminio
        print('  -> Detectado como LATA DE ALUMINIO');
      }
      
      // Metal genérico (menor prioridad que latas)
      else if (lowerLabel.contains('metal') || 
               lowerLabel.contains('iron') ||
               lowerLabel.contains('steel')) {
        _addScore(categoryScores, RecyclableMaterials.categories[7], confidence * 1.5); // Metal
        print('  -> Detectado como METAL');
      }
      
      // Tetra Pak (detectar antes que cartón regular)
      else if (lowerLabel.contains('tetra') || 
               lowerLabel.contains('milk') ||
               lowerLabel.contains('leche') ||
               (lowerLabel.contains('juice') && lowerLabel.contains('box')) ||
               (lowerLabel.contains('jugo') && lowerLabel.contains('caja'))) {
        _addScore(categoryScores, RecyclableMaterials.categories[2], confidence * 2.2); // Tetra Pak (índice 2 después de añadirlo)
        print('  -> Detectado como TETRA PAK');
      }
      
      // Cartón
      else if (lowerLabel.contains('cardboard') || 
               lowerLabel.contains('cartón') ||
               lowerLabel.contains('box') ||
               lowerLabel.contains('caja') ||
               lowerLabel.contains('package')) {
        _addScore(categoryScores, RecyclableMaterials.categories[1], confidence * 2.0); // Cartón
        print('  -> Detectado como CARTÓN');
      }
      
      // Vidrio (solo si es específico)
      else if (lowerLabel.contains('glass') || 
               lowerLabel.contains('vidrio') ||
               lowerLabel.contains('jar') ||
               lowerLabel.contains('frasco')) {
        _addScore(categoryScores, RecyclableMaterials.categories[3], confidence * 2.0); // Vidrio
        print('  -> Detectado como VIDRIO');
      }
      
      // Papel
      else if (lowerLabel.contains('paper') || 
               lowerLabel.contains('papel') ||
               lowerLabel.contains('document') ||
               lowerLabel.contains('newspaper')) {
        _addScore(categoryScores, RecyclableMaterials.categories[5], confidence * 1.8); // Papel
        print('  -> Detectado como PAPEL');
      }
      
      // Bolsas plásticas
      else if (lowerLabel.contains('bag') || 
               lowerLabel.contains('bolsa')) {
        _addScore(categoryScores, RecyclableMaterials.categories[8], confidence * 1.5); // Bolsas
        print('  -> Detectado como BOLSA PLÁSTICA');
      }
      
      // Orgánico (solo si no es una bebida embotellada)
      else if (!isBeverage && (
               lowerLabel.contains('food') || 
               lowerLabel.contains('comida') ||
               lowerLabel.contains('organic') ||
               lowerLabel.contains('fruit') ||
               lowerLabel.contains('vegetable'))) {
        _addScore(categoryScores, RecyclableMaterials.categories[6], confidence * 0.8); // Orgánico - peso reducido
        print('  -> Detectado como ORGÁNICO');
      }
      
      // Poliestireno
      else if (lowerLabel.contains('foam') || 
               lowerLabel.contains('styrofoam') ||
               lowerLabel.contains('unicel')) {
        _addScore(categoryScores, RecyclableMaterials.categories[9], confidence * 1.8); // Poliestireno
        print('  -> Detectado como POLIESTIRENO');
      }
      
      // Electrónicos
      else if (lowerLabel.contains('electronic') || 
               lowerLabel.contains('computer') ||
               lowerLabel.contains('phone') ||
               lowerLabel.contains('device')) {
        _addScore(categoryScores, RecyclableMaterials.categories[10], confidence * 2.0); // Electrónicos
        print('  -> Detectado como ELECTRÓNICO');
      }
      
      // Baterías
      else if (lowerLabel.contains('battery') || 
               lowerLabel.contains('batería') ||
               lowerLabel.contains('pila')) {
        _addScore(categoryScores, RecyclableMaterials.categories[11], confidence * 2.0); // Baterías
        print('  -> Detectado como BATERÍA');
      }
      
      // Textiles
      else if (lowerLabel.contains('clothing') || 
               lowerLabel.contains('textile') ||
               lowerLabel.contains('fabric') ||
               lowerLabel.contains('ropa')) {
        _addScore(categoryScores, RecyclableMaterials.categories[12], confidence * 1.5); // Textiles
        print('  -> Detectado como TEXTIL');
      }
      
      // Palabras que refuerzan categorías existentes
      if (lowerLabel.contains('container') || lowerLabel.contains('package')) {
        // Podría ser plástico o cartón, dar un pequeño boost
        if (categoryScores.containsKey(RecyclableMaterials.categories[0])) {
          _addScore(categoryScores, RecyclableMaterials.categories[0], confidence * 0.3);
        }
        if (categoryScores.containsKey(RecyclableMaterials.categories[1])) {
          _addScore(categoryScores, RecyclableMaterials.categories[1], confidence * 0.3);
        }
      }
    }
    
    // Imprimir puntuaciones finales
    print('\n====== PUNTUACIONES FINALES ======');
    categoryScores.forEach((category, score) {
      print('${category.name}: ${score.toStringAsFixed(2)}');
    });
    print('==================================\n');
    
    // Encontrar la categoría con mayor puntuación
    if (categoryScores.isEmpty) {
      // Si no se encontró ninguna categoría, intentar con análisis general
      return _generalAnalysis(labels, imagePath);
    }
    
    final bestCategory = categoryScores.entries
        .reduce((a, b) => a.value > b.value ? a : b)
        .key;
    
    // Calcular confianza basada en la puntuación y el contexto
    final maxScore = categoryScores[bestCategory]!;
    double normalizedConfidence;
    
    // Si es una bebida claramente identificada, alta confianza
    if (isBeverage && bestCategory == RecyclableMaterials.categories[0]) {
      normalizedConfidence = (maxBeverageConfidence * 1.2).clamp(0.7, 0.95);
    } else {
      // Normalización mejorada considerando el peso máximo posible
      normalizedConfidence = (maxScore / 5.0).clamp(0.3, 0.95);
    }
    
    // Determinar el nombre descriptivo basado en las etiquetas principales
    String detectedName = labels.first.label;
    if (isBeverage && bestCategory == RecyclableMaterials.categories[0]) {
      // Buscar la etiqueta más específica de bebida
      for (final label in labels) {
        final lower = label.label.toLowerCase();
        if (lower.contains('cola') || lower.contains('soda') || 
            lower.contains('juice') || lower.contains('beverage')) {
          detectedName = label.label;
          break;
        }
      }
    }
    
    print('RESULTADO FINAL: ${bestCategory.name} con confianza ${(normalizedConfidence * 100).toStringAsFixed(1)}%\n');
    
    return DetectedMaterial(
      name: detectedName,
      category: bestCategory.name,
      confidence: normalizedConfidence,
      recyclingCode: bestCategory.recyclingCode,
      instructions: bestCategory.instructions,
      pointsPerKg: bestCategory.pointsPerKg,
      imagePath: imagePath,
      detectedAt: DateTime.now(),
    );
  }
  
  void _addScore(Map<MaterialCategory, double> scores, MaterialCategory category, double score) {
    scores[category] = (scores[category] ?? 0) + score;
  }
  
  DetectedMaterial _generalAnalysis(List<ImageLabel> labels, String imagePath) {
    // Análisis general cuando no se puede categorizar específicamente
    final firstLabel = labels.first;
    final lowerLabel = firstLabel.label.toLowerCase();
    
    print('Análisis general para: $lowerLabel');
    
    // Buscar palabras clave genéricas
    if (lowerLabel.contains('plastic') || 
        lowerLabel.contains('bottle') ||
        lowerLabel.contains('container')) {
      return DetectedMaterial(
        name: firstLabel.label,
        category: 'Botellas PET',
        confidence: firstLabel.confidence * 0.7,
        recyclingCode: 'PET 1',
        instructions: 'Lavar y quitar etiquetas. Aplastar para ahorrar espacio.',
        pointsPerKg: 50,
        imagePath: imagePath,
        detectedAt: DateTime.now(),
      );
    }
    
    if (lowerLabel.contains('metal') || lowerLabel.contains('can')) {
      return DetectedMaterial(
        name: firstLabel.label,
        category: 'Aluminio',
        confidence: firstLabel.confidence * 0.7,
        recyclingCode: 'ALU 41',
        instructions: 'Lavar y aplastar latas para ahorrar espacio.',
        pointsPerKg: 80,
        imagePath: imagePath,
        detectedAt: DateTime.now(),
      );
    }
    
    return DetectedMaterial(
      name: firstLabel.label,
      category: 'No Clasificado',
      confidence: firstLabel.confidence,
      recyclingCode: 'N/A',
      instructions: 'Material no clasificado. Consulta con tu centro de reciclaje local.',
      pointsPerKg: 0,
      imagePath: imagePath,
      detectedAt: DateTime.now(),
    );
  }

  Future<List<DetectedMaterial>> detectMultipleMaterials(List<File> imageFiles) async {
    final materials = <DetectedMaterial>[];
    
    for (final file in imageFiles) {
      final material = await detectMaterial(file);
      if (material != null) {
        materials.add(material);
      }
    }
    
    return materials;
  }

  double calculateTotalPoints(List<DetectedMaterial> materials) {
    double totalPoints = 0;
    for (final material in materials) {
      // Asumiendo 0.5 kg por item detectado (puede ajustarse)
      totalPoints += material.pointsPerKg * 0.5;
    }
    return totalPoints;
  }

  Map<String, int> getMaterialsSummary(List<DetectedMaterial> materials) {
    final summary = <String, int>{};
    
    for (final material in materials) {
      summary[material.category] = (summary[material.category] ?? 0) + 1;
    }
    
    return summary;
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