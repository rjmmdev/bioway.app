class UserState {
  final int puntos;
  final String nivel;
  final double co2Evitado;
  final int diasConsecutivos;
  final Map<String, int> materialesReciclados;
  final int totalRecolecciones;
  final String nombre;
  final bool puedeBrindar;

  UserState({
    required this.puntos,
    required this.nivel,
    required this.co2Evitado,
    required this.diasConsecutivos,
    required this.materialesReciclados,
    required this.totalRecolecciones,
    this.nombre = 'Usuario',
    this.puedeBrindar = true,
  });

  static UserState getMockUserState() {
    return UserState(
      puntos: 1250,
      nivel: 'Guardián Verde',
      co2Evitado: 250.8,
      diasConsecutivos: 15,
      materialesReciclados: {
        'pet_tipo1': 45,
        'carton': 32,
        'papel': 28,
        'vidrio': 15,
        'metal': 8,
      },
      totalRecolecciones: 128,
      nombre: 'Juan Pérez',
      puedeBrindar: true,
    );
  }
}