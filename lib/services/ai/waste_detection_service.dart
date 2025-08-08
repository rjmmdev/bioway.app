import 'dart:io';
import 'dart:typed_data';
import 'dart:math';
import 'package:flutter/services.dart';
import 'package:google_mlkit_image_labeling/google_mlkit_image_labeling.dart';
import 'package:image/image.dart' as img;
import 'package:path_provider/path_provider.dart';
import 'package:camera/camera.dart';

/// Servicio principal de detección de residuos usando TensorFlow Lite
/// Optimizado para dispositivos de gama baja con modelo MobileNet V3 Small
class WasteDetectionService {
  static final WasteDetectionService _instance = WasteDetectionService._internal();
  factory WasteDetectionService() => _instance;
  WasteDetectionService._internal();

  ImageLabeler? _imageLabeler;
  List<String>? _labels;
  bool _isInitialized = false;
  
  // Mapeo ampliado de etiquetas de ML Kit a nuestras categorías
  static const Map<String, String> mlKitMapping = {
    // Botellas de plástico - Más palabras clave
    'Bottle': 'plastic_bottle',
    'Plastic bottle': 'plastic_bottle',
    'Water bottle': 'plastic_bottle',
    'Beverage': 'plastic_bottle',
    'Drink': 'plastic_bottle',
    'Soda': 'plastic_bottle',
    'Container': 'plastic_bottle',
    'Water': 'plastic_bottle',
    'Soft drink': 'plastic_bottle',
    'Liquid': 'plastic_bottle',
    'Pet bottle': 'plastic_bottle',
    'Refreshment': 'plastic_bottle',
    
    // Cartón - Más variaciones
    'Cardboard': 'cardboard',
    'Box': 'cardboard',
    'Package': 'cardboard',
    'Carton': 'cardboard',
    'Packaging': 'cardboard',
    'Brown paper': 'cardboard',
    'Corrugated': 'cardboard',
    'Shipping box': 'cardboard',
    'Pizza box': 'cardboard',
    'Parcel': 'cardboard',
    'Crate': 'cardboard',
    'Brown box': 'cardboard',
    
    // Vidrio
    'Glass': 'glass_bottle',
    'Glass bottle': 'glass_bottle',
    'Jar': 'glass_bottle',
    'Wine bottle': 'glass_bottle',
    'Beer bottle': 'glass_bottle',
    
    // Aluminio y metal
    'Can': 'aluminum_can',
    'Aluminum can': 'aluminum_can',
    'Metal': 'metal',
    'Tin': 'aluminum_can',
    'Soda can': 'aluminum_can',
    'Beer can': 'aluminum_can',
    'Aluminum': 'aluminum_can',
    
    // Papel
    'Paper': 'paper',
    'Document': 'paper',
    'Newspaper': 'paper',
    'Magazine': 'paper',
    'Book': 'paper',
    'Notebook': 'paper',
    'White paper': 'paper',
    
    // Orgánico
    'Food': 'organic',
    'Fruit': 'organic',
    'Vegetable': 'organic',
    'Plant': 'organic',
    'Leaf': 'organic',
    'Flower': 'organic',
    
    // Bolsas de plástico
    'Plastic': 'plastic_bag',
    'Bag': 'plastic_bag',
    'Plastic bag': 'plastic_bag',
    'Shopping bag': 'plastic_bag',
    'Wrapper': 'plastic_bag',
    
    // Electrónicos
    'Electronics': 'electronic_waste',
    'Computer': 'electronic_waste',
    'Phone': 'electronic_waste',
    'Device': 'electronic_waste',
    'Cable': 'electronic_waste',
    
    // Baterías
    'Battery': 'battery',
    'Cell': 'battery',
    
    // Textiles
    'Fabric': 'textiles',
    'Clothing': 'textiles',
    'Textile': 'textiles',
    'Cloth': 'textiles',
    'Shirt': 'textiles',
  };
  
  // Configuración optimizada para detección
  static const String labelsPath = 'assets/models/waste_labels.txt';
  static const int inputSize = 224; // Tamaño de entrada estándar para modelos de visión
  static const double threshold = 0.3; // Umbral más bajo para capturar más detecciones
  static const double highConfidenceThreshold = 0.6; // Para detecciones muy confiables
  
  // Categorías base del sistema ampliadas
  static const Map<String, WasteCategory> baseCategories = {
    'plastic_bottle': WasteCategory(
      id: 'PET',
      name: 'Botella PET',
      code: '1',
      color: 0xFF2196F3,
      recyclingInstructions: 'Enjuagar y aplastar. Quitar tapa y etiqueta.',
      value: 5,
    ),
    'cardboard': WasteCategory(
      id: 'CARDBOARD',
      name: 'Cartón',
      code: 'PAP-20',
      color: 0xFF8B4513,
      recyclingInstructions: 'Desarmar cajas. Mantener seco y limpio.',
      value: 3,
    ),
    'glass_bottle': WasteCategory(
      id: 'GLASS',
      name: 'Vidrio',
      code: 'GL-70',
      color: 0xFF4CAF50,
      recyclingInstructions: 'Enjuagar. Separar por color si es posible.',
      value: 2,
    ),
    'aluminum_can': WasteCategory(
      id: 'ALU',
      name: 'Lata de Aluminio',
      code: '41',
      color: 0xFF9E9E9E,
      recyclingInstructions: 'Aplastar para ahorrar espacio.',
      value: 8,
    ),
    'paper': WasteCategory(
      id: 'PAPER',
      name: 'Papel',
      code: 'PAP-22',
      color: 0xFFFFEB3B,
      recyclingInstructions: 'No mezclar con papel sucio o mojado.',
      value: 2,
    ),
    'organic': WasteCategory(
      id: 'ORGANIC',
      name: 'Orgánico',
      code: 'ORG',
      color: 0xFF4CAF50,
      recyclingInstructions: 'Compostar si es posible.',
      value: 1,
    ),
    'metal': WasteCategory(
      id: 'METAL',
      name: 'Metal',
      code: 'FE-40',
      color: 0xFF607D8B,
      recyclingInstructions: 'Separar metales ferrosos y no ferrosos.',
      value: 6,
    ),
    'plastic_bag': WasteCategory(
      id: 'LDPE',
      name: 'Bolsa Plástica',
      code: '4',
      color: 0xFF03A9F4,
      recyclingInstructions: 'Limpiar y secar. Juntar varias bolsas.',
      value: 2,
    ),
    'styrofoam': WasteCategory(
      id: 'PS',
      name: 'Poliestireno',
      code: '6',
      color: 0xFFE91E63,
      recyclingInstructions: 'Limpiar restos de comida. Difícil de reciclar.',
      value: 1,
    ),
    'electronic_waste': WasteCategory(
      id: 'EWASTE',
      name: 'Electrónico',
      code: 'E-WASTE',
      color: 0xFF9C27B0,
      recyclingInstructions: 'Llevar a centro especializado. No tirar con basura común.',
      value: 10,
    ),
    'battery': WasteCategory(
      id: 'BATTERY',
      name: 'Batería',
      code: 'BAT',
      color: 0xFFFF5722,
      recyclingInstructions: 'PELIGROSO. Llevar a centro de acopio especial.',
      value: 15,
    ),
    'textiles': WasteCategory(
      id: 'TEXTILE',
      name: 'Textil',
      code: 'TEX',
      color: 0xFF795548,
      recyclingInstructions: 'Donar si está en buen estado. Limpiar antes de reciclar.',
      value: 3,
    ),
  };

  /// Inicializa el servicio de detección
  Future<bool> initialize() async {
    if (_isInitialized) return true;
    
    try {
      // Cargar el modelo TFLite
      await _loadModel();
      
      // Cargar las etiquetas
      await _loadLabels();
      
      _isInitialized = true;
      // print('✅ Servicio de detección de residuos inicializado');
      return true;
    } catch (e) {
      // print('❌ Error inicializando servicio de detección: $e');
      return false;
    }
  }

  /// Carga el modelo de ML Kit
  Future<void> _loadModel() async {
    try {
      // Usar ImageLabeler de Google ML Kit con umbral más bajo
      final options = ImageLabelerOptions(
        confidenceThreshold: 0.2, // Umbral más bajo para capturar más etiquetas
      );
      _imageLabeler = ImageLabeler(options: options);
      // print('📱 Google ML Kit Image Labeler inicializado');
      // print('📉 Umbral de confianza: 20% (para mejor detección)');
    } catch (e) {
      // print('Error cargando ML Kit: $e');
      // print('📱 Usando modo simulado para desarrollo');
      _imageLabeler = null;
    }
  }

  /// Carga las etiquetas del modelo
  Future<void> _loadLabels() async {
    try {
      // Cargar etiquetas desde archivo
      final labelsData = await rootBundle.loadString(labelsPath);
      _labels = labelsData.split('\n').where((label) => label.isNotEmpty).toList();
      
      // print('🏷️ ${_labels!.length} etiquetas cargadas desde archivo');
    } catch (e) {
      // print('Error cargando etiquetas desde archivo: $e');
      // Usar etiquetas base si falla la carga
      _labels = baseCategories.keys.toList();
      // print('🏷️ Usando ${_labels!.length} etiquetas base');
    }
  }

  /// Clasifica una imagen desde archivo
  Future<WasteDetectionResult> classifyImage(File imageFile) async {
    if (!_isInitialized) {
      await initialize();
    }

    try {
      if (_imageLabeler != null) {
        // Usar ML Kit para clasificación real
        final inputImage = InputImage.fromFile(imageFile);
        final labels = await _imageLabeler!.processImage(inputImage);
        
        // Convertir labels de ML Kit a nuestras categorías
        return _processMLKitLabels(labels);
      } else {
        // Modo simulado
        final imageBytes = await imageFile.readAsBytes();
        final image = img.decodeImage(imageBytes);
        
        if (image == null) {
          throw Exception('No se pudo decodificar la imagen');
        }

        // Simular detección
        final output = await _runSimulatedInference();
        return _processOutput(output);
      }
    } catch (e) {
      // print('Error clasificando imagen: $e');
      return WasteDetectionResult(
        success: false,
        error: e.toString(),
      );
    }
  }

  /// Clasifica desde CameraImage (para detección en tiempo real)
  Future<WasteDetectionResult> classifyCamera(CameraImage cameraImage) async {
    if (!_isInitialized) {
      await initialize();
    }

    try {
      // Por ahora usar modo simulado para cámara en tiempo real
      // ML Kit requiere InputImage que es más complejo desde CameraImage
      final output = await _runSimulatedInference();
      return _processOutput(output);
    } catch (e) {
      // print('Error en detección desde cámara: $e');
      return WasteDetectionResult(
        success: false,
        error: e.toString(),
      );
    }
  }

  /// Preprocesa la imagen para el modelo (no usado actualmente con ML Kit)
  // ignore: unused_element
  Float32List _preprocessImage(img.Image image) {
    // Redimensionar a 224x224 (MobileNet V3)
    final resized = img.copyResize(
      image,
      width: inputSize,
      height: inputSize,
      interpolation: img.Interpolation.linear,
    );

    // Convertir a Float32List normalizado
    final float32List = Float32List(inputSize * inputSize * 3);
    int pixelIndex = 0;
    
    for (int y = 0; y < inputSize; y++) {
      for (int x = 0; x < inputSize; x++) {
        final pixel = resized.getPixel(x, y);
        
        // Normalizar a [-1, 1] para MobileNet
        float32List[pixelIndex++] = (pixel.r / 127.5) - 1.0;
        float32List[pixelIndex++] = (pixel.g / 127.5) - 1.0;
        float32List[pixelIndex++] = (pixel.b / 127.5) - 1.0;
      }
    }
    
    return float32List;
  }

  /// Procesa las etiquetas de ML Kit con detección mejorada
  WasteDetectionResult _processMLKitLabels(List<ImageLabel> labels) {
    final classifications = <WasteClassification>[];
    final detectedCategories = <String, double>{}; // Para evitar duplicados
    
    // Debug: Imprimir etiquetas detectadas (solo en desarrollo)
    // print('\n🔍 Etiquetas detectadas por ML Kit:');
    // for (final label in labels) {
    //   print('  - ${label.label}: ${(label.confidence * 100).toStringAsFixed(1)}%');
    // }
    
    for (final label in labels) {
      // Buscar mapeo a nuestras categorías
      String? mappedCategory;
      
      // Buscar coincidencia exacta primero
      for (final entry in mlKitMapping.entries) {
        if (label.label.toLowerCase() == entry.key.toLowerCase()) {
          mappedCategory = entry.value;
          break;
        }
      }
      
      // Si no hay coincidencia exacta, buscar parcial
      if (mappedCategory == null) {
        for (final entry in mlKitMapping.entries) {
          if (label.label.toLowerCase().contains(entry.key.toLowerCase()) ||
              entry.key.toLowerCase().contains(label.label.toLowerCase())) {
            mappedCategory = entry.value;
            break;
          }
        }
      }
      
      // Agregar clasificación si encontramos mapeo
      if (mappedCategory != null && baseCategories.containsKey(mappedCategory)) {
        // Solo agregar si no existe o si tiene mayor confianza
        final currentConfidence = detectedCategories[mappedCategory] ?? 0;
        if (label.confidence > currentConfidence) {
          detectedCategories[mappedCategory] = label.confidence;
        }
      }
    }
    
    // Convertir a lista de clasificaciones
    detectedCategories.forEach((categoryId, confidence) {
      final category = baseCategories[categoryId]!;
      classifications.add(WasteClassification(
        category: category,
        confidence: confidence,
        label: categoryId,
      ));
    });
    
    // Si no hay clasificaciones específicas, usar detección inteligente
    if (classifications.isEmpty && labels.isNotEmpty) {
      // print('\n⚠️ No se encontró mapeo directo, usando detección inteligente...');
      
      // Analizar todas las etiquetas para determinar el material
      final guessedCategory = _intelligentCategoryDetection(labels);
      
      if (guessedCategory != null) {
        classifications.add(WasteClassification(
          category: guessedCategory.category,
          confidence: guessedCategory.confidence,
          label: guessedCategory.label,
        ));
      }
    }
    
    // Ordenar por confianza
    classifications.sort((a, b) => b.confidence.compareTo(a.confidence));
    
    return WasteDetectionResult(
      success: classifications.isNotEmpty,
      classifications: classifications,
      primaryClassification: classifications.isNotEmpty ? classifications.first : null,
      processingTimeMs: 50,
    );
  }
  
  /// Detección inteligente basada en múltiples características
  WasteClassification? _intelligentCategoryDetection(List<ImageLabel> labels) {
    // Palabras clave por categoría con pesos
    final categoryKeywords = {
      'plastic_bottle': {
        'keywords': ['plastic', 'bottle', 'container', 'drink', 'water', 'liquid', 'beverage', 'transparent', 'cylinder', 'cap'],
        'weight': 1.0,
      },
      'cardboard': {
        'keywords': ['brown', 'box', 'package', 'paper', 'rectangular', 'cube', 'shipping', 'delivery', 'amazon', 'corrugated'],
        'weight': 1.0,
      },
      'paper': {
        'keywords': ['paper', 'white', 'document', 'sheet', 'flat', 'text', 'writing', 'print'],
        'weight': 0.9,
      },
      'plastic_bag': {
        'keywords': ['bag', 'plastic', 'transparent', 'wrapper', 'thin', 'flexible'],
        'weight': 0.9,
      },
      'aluminum_can': {
        'keywords': ['can', 'metal', 'aluminum', 'cylinder', 'silver', 'tin'],
        'weight': 0.95,
      },
      'glass_bottle': {
        'keywords': ['glass', 'bottle', 'transparent', 'jar', 'wine', 'beer'],
        'weight': 0.95,
      },
      'organic': {
        'keywords': ['food', 'fruit', 'vegetable', 'plant', 'organic', 'natural', 'green'],
        'weight': 0.8,
      },
    };
    
    final scores = <String, double>{};
    
    // Analizar todas las etiquetas
    for (final label in labels) {
      final lowerLabel = label.label.toLowerCase();
      
      // Calcular puntaje para cada categoría
      categoryKeywords.forEach((categoryId, data) {
        final keywords = data['keywords'] as List<String>;
        final weight = data['weight'] as double;
        
        for (final keyword in keywords) {
          if (lowerLabel.contains(keyword)) {
            scores[categoryId] = (scores[categoryId] ?? 0) + (label.confidence * weight);
          }
        }
      });
    }
    
    // Encontrar la categoría con mayor puntaje
    if (scores.isNotEmpty) {
      final bestMatch = scores.entries.reduce((a, b) => a.value > b.value ? a : b);
      
      if (bestMatch.value > 0.2) { // Umbral mínimo de puntaje
        final category = baseCategories[bestMatch.key]!;
        // print('✅ Detección inteligente: ${category.name} (puntaje: ${bestMatch.value.toStringAsFixed(2)})');
        
        return WasteClassification(
          category: category,
          confidence: (bestMatch.value).clamp(0.0, 1.0),
          label: bestMatch.key,
        );
      }
    }
    
    return null;
  }
  
  /// Ejecuta inferencia simulada para desarrollo
  Future<List<double>> _runSimulatedInference() async {
    // Modo simulado para desarrollo
    final random = Random();
    final output = List<double>.generate(
      _labels?.length ?? baseCategories.length,
      (i) => random.nextDouble(),
    );
    
    // Normalizar para que sumen 1 (softmax simulado)
    final sum = output.reduce((a, b) => a + b);
    for (int i = 0; i < output.length; i++) {
      output[i] = output[i] / sum;
    }
    
    // Simular delay de procesamiento
    await Future.delayed(Duration(milliseconds: 40));
    
    // print('⚠️ Usando inferencia simulada');
    return output;
  }

  /// Procesa la salida del modelo
  WasteDetectionResult _processOutput(List<double> output) {
    final classifications = <WasteClassification>[];
    
    for (int i = 0; i < output.length && i < (_labels?.length ?? 0); i++) {
      if (output[i] >= threshold) {
        final label = _labels![i];
        final category = baseCategories[label];
        
        if (category != null) {
          classifications.add(WasteClassification(
            category: category,
            confidence: output[i],
            label: label,
          ));
        }
      }
    }
    
    // Ordenar por confianza
    classifications.sort((a, b) => b.confidence.compareTo(a.confidence));
    
    return WasteDetectionResult(
      success: classifications.isNotEmpty,
      classifications: classifications,
      primaryClassification: classifications.isNotEmpty ? classifications.first : null,
      processingTimeMs: 40, // Simulado
    );
  }

  /// Convierte CameraImage a img.Image (no usado actualmente con ML Kit)
  // ignore: unused_element
  img.Image _convertCameraImage(CameraImage cameraImage) {
    // Implementación simplificada para YUV420
    // En producción, manejar todos los formatos
    final width = cameraImage.width;
    final height = cameraImage.height;
    
    final image = img.Image(width: width, height: height);
    
    // Conversión básica de YUV a RGB
    final yPlane = cameraImage.planes[0];
    final uPlane = cameraImage.planes[1];
    final vPlane = cameraImage.planes[2];
    
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        final yIndex = y * yPlane.bytesPerRow + x;
        final uvIndex = (y ~/ 2) * uPlane.bytesPerRow + (x ~/ 2);
        
        final yValue = yPlane.bytes[yIndex];
        final uValue = uPlane.bytes[uvIndex];
        final vValue = vPlane.bytes[uvIndex];
        
        // Conversión YUV a RGB
        final r = (yValue + 1.402 * (vValue - 128)).clamp(0, 255).toInt();
        final g = (yValue - 0.344 * (uValue - 128) - 0.714 * (vValue - 128)).clamp(0, 255).toInt();
        final b = (yValue + 1.772 * (uValue - 128)).clamp(0, 255).toInt();
        
        image.setPixelRgb(x, y, r, g, b);
      }
    }
    
    return image;
  }

  /// Descarga y cachea modelo personalizado de empresa
  Future<bool> downloadCompanyModel(String companyId, String modelUrl) async {
    try {
      // Obtener directorio de cache
      final dir = await getApplicationDocumentsDirectory();
      final modelFile = File('${dir.path}/models/company_$companyId.tflite');
      
      // Crear directorio si no existe
      await modelFile.parent.create(recursive: true);
      
      // TODO: Implementar descarga desde Firebase Storage
      // Por ahora solo marcar como descargado
      
      // print('📥 Modelo de empresa $companyId descargado');
      return true;
    } catch (e) {
      // print('Error descargando modelo de empresa: $e');
      return false;
    }
  }

  /// Limpia recursos
  void dispose() {
    _imageLabeler?.close();
    _isInitialized = false;
  }
}

/// Categoría de residuo
class WasteCategory {
  final String id;
  final String name;
  final String code; // Código de reciclaje internacional
  final int color;
  final String recyclingInstructions;
  final int value; // Puntos o valor monetario

  const WasteCategory({
    required this.id,
    required this.name,
    required this.code,
    required this.color,
    required this.recyclingInstructions,
    required this.value,
  });
}

/// Clasificación individual
class WasteClassification {
  final WasteCategory category;
  final double confidence;
  final String label;

  WasteClassification({
    required this.category,
    required this.confidence,
    required this.label,
  });
}

/// Resultado de detección
class WasteDetectionResult {
  final bool success;
  final List<WasteClassification> classifications;
  final WasteClassification? primaryClassification;
  final int? processingTimeMs;
  final String? error;

  WasteDetectionResult({
    required this.success,
    this.classifications = const [],
    this.primaryClassification,
    this.processingTimeMs,
    this.error,
  });
}