class BioWayUser {
  final String id;
  final String uid;
  final String email;
  final String nombre;
  final String telefono;
  final String rol;
  final String tipoUsuario;
  final String? empresa;
  final String? fotoPerfil;
  final int puntosAcumulados;
  final int bioCoins;
  final int nivel;
  final String nivelNombre;
  final Map<String, dynamic> estadisticas;
  final DateTime fechaRegistro;
  final bool activo;
  final String direccion;
  final String numeroExterior;
  final String codigoPostal;
  final String estado;
  final String municipio;
  final String colonia;
  final int totalResiduosBrindados;
  final double totalKgReciclados;
  final double totalCO2Evitado;

  BioWayUser({
    required this.id,
    String? uid,
    required this.email,
    required this.nombre,
    String? telefono,
    String? rol,
    String? tipoUsuario,
    this.empresa,
    this.fotoPerfil,
    int? puntosAcumulados,
    int? bioCoins,
    int? nivel,
    String? nivelNombre,
    Map<String, dynamic>? estadisticas,
    required this.fechaRegistro,
    bool? activo,
    String? direccion,
    String? numeroExterior,
    String? codigoPostal,
    String? estado,
    String? municipio,
    String? colonia,
    int? totalResiduosBrindados,
    double? totalKgReciclados,
    double? totalCO2Evitado,
  }) : uid = uid ?? id,
       telefono = telefono ?? '',
       rol = rol ?? 'brindador',
       tipoUsuario = tipoUsuario ?? rol ?? 'brindador',
       puntosAcumulados = puntosAcumulados ?? 0,
       bioCoins = bioCoins ?? puntosAcumulados ?? 0,
       nivel = nivel ?? 1,
       nivelNombre = nivelNombre ?? 'Principiante',
       estadisticas = estadisticas ?? {},
       activo = activo ?? true,
       direccion = direccion ?? '',
       numeroExterior = numeroExterior ?? '',
       codigoPostal = codigoPostal ?? '',
       estado = estado ?? '',
       municipio = municipio ?? '',
       colonia = colonia ?? '',
       totalResiduosBrindados = totalResiduosBrindados ?? 0,
       totalKgReciclados = totalKgReciclados ?? 0.0,
       totalCO2Evitado = totalCO2Evitado ?? 0.0;

  factory BioWayUser.fromMap(Map<String, dynamic> map) {
    return BioWayUser(
      id: map['id'] ?? '',
      uid: map['uid'],
      email: map['email'] ?? '',
      nombre: map['nombre'] ?? '',
      telefono: map['telefono'],
      rol: map['rol'],
      tipoUsuario: map['tipoUsuario'],
      empresa: map['empresa'],
      fotoPerfil: map['fotoPerfil'],
      puntosAcumulados: map['puntosAcumulados'],
      bioCoins: map['bioCoins'],
      nivel: map['nivel'],
      nivelNombre: map['nivelNombre'],
      estadisticas: map['estadisticas'],
      fechaRegistro: map['fechaRegistro'] != null
          ? DateTime.parse(map['fechaRegistro'])
          : DateTime.now(),
      activo: map['activo'],
      direccion: map['direccion'],
      numeroExterior: map['numeroExterior'],
      codigoPostal: map['codigoPostal'],
      estado: map['estado'],
      municipio: map['municipio'],
      colonia: map['colonia'],
      totalResiduosBrindados: map['totalResiduosBrindados'],
      totalKgReciclados: map['totalKgReciclados']?.toDouble(),
      totalCO2Evitado: map['totalCO2Evitado']?.toDouble(),
    );
  }

  Map<String, dynamic> toMap() {
    return {
      'id': id,
      'email': email,
      'nombre': nombre,
      'telefono': telefono,
      'rol': rol,
      'empresa': empresa,
      'fotoPerfil': fotoPerfil,
      'puntosAcumulados': puntosAcumulados,
      'nivel': nivel,
      'estadisticas': estadisticas,
      'fechaRegistro': fechaRegistro.toIso8601String(),
      'activo': activo,
    };
  }
}