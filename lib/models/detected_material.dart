class DetectedMaterial {
  final String name;
  final String category;
  final double confidence;
  final String recyclingCode;
  final String instructions;
  final int pointsPerKg;
  final String imagePath;
  final DateTime detectedAt;

  DetectedMaterial({
    required this.name,
    required this.category,
    required this.confidence,
    required this.recyclingCode,
    required this.instructions,
    required this.pointsPerKg,
    required this.imagePath,
    required this.detectedAt,
  });

  Map<String, dynamic> toJson() {
    return {
      'name': name,
      'category': category,
      'confidence': confidence,
      'recyclingCode': recyclingCode,
      'instructions': instructions,
      'pointsPerKg': pointsPerKg,
      'imagePath': imagePath,
      'detectedAt': detectedAt.toIso8601String(),
    };
  }

  factory DetectedMaterial.fromJson(Map<String, dynamic> json) {
    return DetectedMaterial(
      name: json['name'],
      category: json['category'],
      confidence: json['confidence'],
      recyclingCode: json['recyclingCode'],
      instructions: json['instructions'],
      pointsPerKg: json['pointsPerKg'],
      imagePath: json['imagePath'],
      detectedAt: DateTime.parse(json['detectedAt']),
    );
  }
}

class MaterialCategory {
  final String name;
  final String recyclingCode;
  final String instructions;
  final int pointsPerKg;
  final double co2SavedPerKg; // kg de CO2 ahorrados por kg reciclado
  final List<String> keywords;

  const MaterialCategory({
    required this.name,
    required this.recyclingCode,
    required this.instructions,
    required this.pointsPerKg,
    required this.co2SavedPerKg,
    required this.keywords,
  });
}

class RecyclableMaterials {
  static const List<MaterialCategory> categories = [
    MaterialCategory(
      name: 'Botellas PET',
      recyclingCode: 'PET 1',
      instructions: 'Lavar y quitar etiquetas. Aplastar para ahorrar espacio.',
      pointsPerKg: 50,
      co2SavedPerKg: 2.5, // Reciclar 1 kg de PET ahorra ~2.5 kg de CO2
      keywords: ['bottle', 'plastic bottle', 'pet', 'water bottle', 'soda bottle', 'beverage'],
    ),
    MaterialCategory(
      name: 'Cartón',
      recyclingCode: 'PAP 20-22',
      instructions: 'Aplanar cajas y quitar cintas adhesivas.',
      pointsPerKg: 20,
      co2SavedPerKg: 0.9, // Reciclar 1 kg de cartón ahorra ~0.9 kg de CO2
      keywords: ['cardboard', 'box', 'carton', 'package', 'shipping box'],
    ),
    MaterialCategory(
      name: 'Tetra Pak',
      recyclingCode: 'C/PAP 84',
      instructions: 'Enjuagar y aplastar. Separar del cartón regular.',
      pointsPerKg: 35,
      co2SavedPerKg: 1.2, // Reciclar 1 kg de Tetra Pak ahorra ~1.2 kg de CO2
      keywords: ['tetra pak', 'tetrapak', 'milk carton', 'juice box', 'beverage carton', 'leche', 'jugo'],
    ),
    MaterialCategory(
      name: 'Vidrio',
      recyclingCode: 'GL 70-74',
      instructions: 'Limpiar y separar por colores si es posible.',
      pointsPerKg: 15,
      co2SavedPerKg: 0.6, // Reciclar 1 kg de vidrio ahorra ~0.6 kg de CO2
      keywords: ['glass', 'bottle', 'jar', 'glass bottle', 'glass container'],
    ),
    MaterialCategory(
      name: 'Aluminio',
      recyclingCode: 'ALU 41',
      instructions: 'Lavar y aplastar latas para ahorrar espacio.',
      pointsPerKg: 80,
      co2SavedPerKg: 9.0, // Reciclar 1 kg de aluminio ahorra ~9 kg de CO2
      keywords: ['aluminum', 'can', 'soda can', 'beer can', 'aluminum can', 'tin'],
    ),
    MaterialCategory(
      name: 'Papel',
      recyclingCode: 'PAP 20',
      instructions: 'Mantener seco y sin grasa. Separar papel blanco del color.',
      pointsPerKg: 10,
      co2SavedPerKg: 0.7, // Reciclar 1 kg de papel ahorra ~0.7 kg de CO2
      keywords: ['paper', 'document', 'newspaper', 'magazine', 'book', 'notebook'],
    ),
    MaterialCategory(
      name: 'Orgánico',
      recyclingCode: 'ORG',
      instructions: 'Restos de comida para composta. Mantener separado de otros residuos.',
      pointsPerKg: 5,
      co2SavedPerKg: 0.3, // Compostar 1 kg de orgánico ahorra ~0.3 kg de CO2
      keywords: ['food', 'organic', 'compost', 'vegetable', 'fruit', 'waste'],
    ),
    MaterialCategory(
      name: 'Metal',
      recyclingCode: 'FE 40',
      instructions: 'Limpiar y separar por tipo de metal.',
      pointsPerKg: 60,
      co2SavedPerKg: 1.8, // Reciclar 1 kg de metal ahorra ~1.8 kg de CO2
      keywords: ['metal', 'iron', 'steel', 'copper', 'scrap metal'],
    ),
    MaterialCategory(
      name: 'Bolsas Plásticas',
      recyclingCode: 'LDPE 4',
      instructions: 'Limpiar y secar. Juntar varias en una sola bolsa.',
      pointsPerKg: 25,
      co2SavedPerKg: 2.0, // Reciclar 1 kg de LDPE ahorra ~2 kg de CO2
      keywords: ['plastic bag', 'bag', 'shopping bag', 'plastic wrap'],
    ),
    MaterialCategory(
      name: 'Poliestireno',
      recyclingCode: 'PS 6',
      instructions: 'Limpiar y separar del resto de plásticos.',
      pointsPerKg: 30,
      co2SavedPerKg: 1.5, // Reciclar 1 kg de poliestireno ahorra ~1.5 kg de CO2
      keywords: ['styrofoam', 'polystyrene', 'foam', 'packaging foam'],
    ),
    MaterialCategory(
      name: 'Electrónicos',
      recyclingCode: 'WEEE',
      instructions: 'No mezclar con otros residuos. Contiene materiales valiosos.',
      pointsPerKg: 100,
      co2SavedPerKg: 3.5, // Reciclar 1 kg de electrónicos ahorra ~3.5 kg de CO2
      keywords: ['electronic', 'computer', 'phone', 'circuit', 'device', 'gadget'],
    ),
    MaterialCategory(
      name: 'Baterías',
      recyclingCode: 'BAT',
      instructions: 'Manejo especial. No tirar a la basura común.',
      pointsPerKg: 120,
      co2SavedPerKg: 4.0, // Reciclar 1 kg de baterías ahorra ~4 kg de CO2
      keywords: ['battery', 'batteries', 'cell', 'power cell'],
    ),
    MaterialCategory(
      name: 'Textiles',
      recyclingCode: 'TEX 60-63',
      instructions: 'Limpiar y secar. Separar por tipo de tela.',
      pointsPerKg: 35,
      co2SavedPerKg: 3.0, // Reciclar 1 kg de textiles ahorra ~3 kg de CO2
      keywords: ['textile', 'clothing', 'fabric', 'clothes', 'shirt', 'pants'],
    ),
  ];

  static MaterialCategory? findCategory(List<String> labels) {
    for (final label in labels) {
      final lowerLabel = label.toLowerCase();
      for (final category in categories) {
        for (final keyword in category.keywords) {
          if (lowerLabel.contains(keyword) || keyword.contains(lowerLabel)) {
            return category;
          }
        }
      }
    }
    return null;
  }
}