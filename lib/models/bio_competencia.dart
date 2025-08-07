class BioCompetencia {
  final String id;
  final String userId;
  final String nombre;
  final String userName;
  final String avatar;
  final String userAvatar;
  final int puntos;
  final int puntosSemanales;
  final int puntosTotales;
  final int nivel;
  final int posicion;
  final int posicionRanking;
  final double kgReciclados;
  final double co2Evitado;
  final int reciclajesTotales;
  final int reciclajesEstaSemana;
  final List<String> logros;
  final bool esMiPerfil;
  final int bioImpulso;
  final int bioImpulsoMaximo;
  final bool bioImpulsoActivo;
  final DateTime ultimaActividad;
  final DateTime inicioSemanaActual;
  final String insigniaActual;
  
  BioCompetencia({
    String? id,
    required this.userId,
    String? nombre,
    String? userName,
    String? avatar,
    String? userAvatar,
    int? puntos,
    int? puntosSemanales,
    int? puntosTotales,
    required this.nivel,
    int? posicion,
    int? posicionRanking,
    required this.kgReciclados,
    double? co2Evitado,
    int? reciclajesTotales,
    int? reciclajesEstaSemana,
    List<String>? logros,
    this.esMiPerfil = false,
    int? bioImpulso,
    int? bioImpulsoMaximo,
    bool? bioImpulsoActivo,
    DateTime? ultimaActividad,
    DateTime? inicioSemanaActual,
    String? insigniaActual,
  }) : id = id ?? userId,
       nombre = nombre ?? userName ?? 'Usuario',
       userName = userName ?? nombre ?? 'Usuario',
       avatar = avatar ?? userAvatar ?? '👤',
       userAvatar = userAvatar ?? avatar ?? '👤',
       puntos = puntos ?? puntosSemanales ?? puntosTotales ?? 0,
       puntosSemanales = puntosSemanales ?? puntos ?? 0,
       puntosTotales = puntosTotales ?? puntos ?? 0,
       posicion = posicion ?? posicionRanking ?? 0,
       posicionRanking = posicionRanking ?? posicion ?? 0,
       co2Evitado = co2Evitado ?? 0.0,
       reciclajesTotales = reciclajesTotales ?? 0,
       reciclajesEstaSemana = reciclajesEstaSemana ?? 0,
       logros = logros ?? [],
       bioImpulso = bioImpulso ?? 0,
       bioImpulsoMaximo = bioImpulsoMaximo ?? 10,
       bioImpulsoActivo = bioImpulsoActivo ?? false,
       ultimaActividad = ultimaActividad ?? DateTime.now(),
       inicioSemanaActual = inicioSemanaActual ?? DateTime.now(),
       insigniaActual = insigniaActual ?? '🌟';
  
  factory BioCompetencia.fromMap(Map<String, dynamic> map) {
    return BioCompetencia(
      id: map['id'] ?? '',
      userId: map['userId'] ?? '',
      nombre: map['nombre'] ?? '',
      avatar: map['avatar'] ?? '👤',
      puntos: map['puntos'] ?? 0,
      nivel: map['nivel'] ?? 1,
      posicion: map['posicion'] ?? 0,
      kgReciclados: map['kgReciclados']?.toDouble() ?? 0.0,
      reciclajesTotales: map['reciclajesTotales'] ?? 0,
      logros: List<String>.from(map['logros'] ?? []),
      esMiPerfil: map['esMiPerfil'] ?? false,
    );
  }
  
  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'userId': userId,
      'nombre': nombre,
      'avatar': avatar,
      'puntos': puntos,
      'nivel': nivel,
      'posicion': posicion,
      'kgReciclados': kgReciclados,
      'reciclajesTotales': reciclajesTotales,
      'logros': logros,
      'esMiPerfil': esMiPerfil,
    };
  }
  
  static List<BioCompetencia> getMockRanking() {
    return [
      BioCompetencia(
        id: '1',
        userId: '123',
        nombre: 'María García',
        avatar: '👩',
        puntos: 2500,
        nivel: 5,
        posicion: 1,
        kgReciclados: 250.5,
        reciclajesTotales: 120,
        logros: ['🏆', '⭐', '🌟'],
        esMiPerfil: false,
      ),
      BioCompetencia(
        id: '2',
        userId: '124',
        nombre: 'Juan Pérez',
        avatar: '👨',
        puntos: 2350,
        nivel: 4,
        posicion: 2,
        kgReciclados: 220.3,
        reciclajesTotales: 110,
        logros: ['⭐', '🌟'],
        esMiPerfil: false,
      ),
      BioCompetencia(
        id: '3',
        userId: '125',
        nombre: 'Ana López',
        avatar: '👩',
        puntos: 2100,
        nivel: 4,
        posicion: 3,
        kgReciclados: 195.8,
        reciclajesTotales: 95,
        logros: ['⭐'],
        esMiPerfil: false,
      ),
    ];
  }
}