class BioWayUser {
  final String uid;
  final String email;
  final String nombre;
  final String tipoUsuario;
  final bool isBrindador;
  final String? fotoPerfil;
  final Map<String, dynamic>? empresa;
  final int puntos;
  final String nivel;
  final double co2Evitado;
  final Map<String, int> materialesReciclados;
  final DateTime fechaRegistro;
  final bool activo;
  final int bioCoins;
  final String colonia;
  final String municipio;
  final double totalKgReciclados;
  final double totalCO2Evitado;
  final String direccion;

  BioWayUser({
    required this.uid,
    required this.email,
    required this.nombre,
    required this.tipoUsuario,
    this.isBrindador = true,
    this.fotoPerfil,
    this.empresa,
    this.puntos = 0,
    this.nivel = 'Semilla Verde',
    this.co2Evitado = 0,
    Map<String, int>? materialesReciclados,
    DateTime? fechaRegistro,
    this.activo = true,
    this.bioCoins = 0,
    this.colonia = '',
    this.municipio = '',
    this.totalKgReciclados = 0,
    this.totalCO2Evitado = 0,
    this.direccion = '',
  })  : materialesReciclados = materialesReciclados ?? {},
        fechaRegistro = fechaRegistro ?? DateTime.now();

  Map<String, dynamic> toMap() {
    return {
      'uid': uid,
      'email': email,
      'nombre': nombre,
      'tipoUsuario': tipoUsuario,
      'isBrindador': isBrindador,
      'fotoPerfil': fotoPerfil,
      'empresa': empresa,
      'puntos': puntos,
      'nivel': nivel,
      'co2Evitado': co2Evitado,
      'materialesReciclados': materialesReciclados,
      'fechaRegistro': fechaRegistro.toIso8601String(),
      'activo': activo,
      'bioCoins': bioCoins,
      'colonia': colonia,
      'municipio': municipio,
      'totalKgReciclados': totalKgReciclados,
      'totalCO2Evitado': totalCO2Evitado,
      'direccion': direccion,
    };
  }

  factory BioWayUser.fromMap(Map<String, dynamic> map) {
    return BioWayUser(
      uid: map['uid'] ?? '',
      email: map['email'] ?? '',
      nombre: map['nombre'] ?? '',
      tipoUsuario: map['tipoUsuario'] ?? 'brindador',
      isBrindador: map['isBrindador'] ?? true,
      fotoPerfil: map['fotoPerfil'],
      empresa: map['empresa'],
      puntos: map['puntos'] ?? 0,
      nivel: map['nivel'] ?? 'Semilla Verde',
      co2Evitado: (map['co2Evitado'] ?? 0).toDouble(),
      materialesReciclados: Map<String, int>.from(map['materialesReciclados'] ?? {}),
      fechaRegistro: map['fechaRegistro'] != null
          ? DateTime.parse(map['fechaRegistro'])
          : DateTime.now(),
      activo: map['activo'] ?? true,
      bioCoins: map['bioCoins'] ?? 0,
      colonia: map['colonia'] ?? '',
      municipio: map['municipio'] ?? '',
      totalKgReciclados: (map['totalKgReciclados'] ?? 0).toDouble(),
      totalCO2Evitado: (map['totalCO2Evitado'] ?? 0).toDouble(),
      direccion: map['direccion'] ?? '',
    );
  }
}