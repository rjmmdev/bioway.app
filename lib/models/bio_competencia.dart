class BioCompetencia {
  final String userId;
  final String nombre;
  final String fotoPerfil;
  final int puntos;
  final String nivel;
  final double co2Evitado;
  final int posicion;
  final Map<String, int> materialesReciclados;
  final int diasConsecutivos;
  final String userName;
  final int posicionRanking;
  final double kgReciclados;
  final int puntosTotales;
  final int bioImpulso;
  final bool bioImpulsoActivo;
  final int bioImpulsoMaximo;
  final double totalKgReciclados;
  final String userAvatar;

  BioCompetencia({
    required this.userId,
    required this.nombre,
    required this.fotoPerfil,
    required this.puntos,
    required this.nivel,
    required this.co2Evitado,
    required this.posicion,
    required this.materialesReciclados,
    required this.diasConsecutivos,
    String? userName,
    int? posicionRanking,
    double? kgReciclados,
    int? puntosTotales,
    int? bioImpulso,
    bool? bioImpulsoActivo,
    int? bioImpulsoMaximo,
    double? totalKgReciclados,
    String? userAvatar,
  }) : userName = userName ?? nombre,
       posicionRanking = posicionRanking ?? posicion,
       kgReciclados = kgReciclados ?? co2Evitado,
       puntosTotales = puntosTotales ?? puntos,
       bioImpulso = bioImpulso ?? diasConsecutivos,
       bioImpulsoActivo = bioImpulsoActivo ?? (diasConsecutivos > 0),
       bioImpulsoMaximo = bioImpulsoMaximo ?? diasConsecutivos,
       totalKgReciclados = totalKgReciclados ?? co2Evitado,
       userAvatar = userAvatar ?? fotoPerfil;
}