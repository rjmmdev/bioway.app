class UserState {
  final String userId;
  final String nombre;
  final int puntosAcumulados;
  final int nivel;
  final int reciclajesTotales;
  final double kgReciclados;
  final Map<String, dynamic> estadisticas;
  final bool puedeBrindar;
  
  UserState({
    required this.userId,
    required this.nombre,
    required this.puntosAcumulados,
    required this.nivel,
    required this.reciclajesTotales,
    required this.kgReciclados,
    required this.estadisticas,
    this.puedeBrindar = true,
  });
  
  factory UserState.fromMap(Map<String, dynamic> map) {
    return UserState(
      userId: map['userId'] ?? '',
      nombre: map['nombre'] ?? '',
      puntosAcumulados: map['puntosAcumulados'] ?? 0,
      nivel: map['nivel'] ?? 1,
      reciclajesTotales: map['reciclajesTotales'] ?? 0,
      kgReciclados: map['kgReciclados']?.toDouble() ?? 0.0,
      estadisticas: map['estadisticas'] ?? {},
      puedeBrindar: map['puedeBrindar'] ?? true,
    );
  }
  
  Map<String, dynamic> toMap() {
    return {
      'userId': userId,
      'nombre': nombre,
      'puntosAcumulados': puntosAcumulados,
      'nivel': nivel,
      'reciclajesTotales': reciclajesTotales,
      'kgReciclados': kgReciclados,
      'estadisticas': estadisticas,
      'puedeBrindar': puedeBrindar,
    };
  }
  
  static UserState getMockUserState() {
    return UserState(
      userId: '123',
      nombre: 'Usuario Demo',
      puntosAcumulados: 1250,
      nivel: 3,
      reciclajesTotales: 45,
      kgReciclados: 125.5,
      estadisticas: {
        'plastico': 45.5,
        'papel': 30.2,
        'vidrio': 25.8,
        'metal': 24.0,
      },
      puedeBrindar: true,
    );
  }
}