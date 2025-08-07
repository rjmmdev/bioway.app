class Horario {
  final int day;
  final String dayName;
  final String startTime;
  final String endTime;
  final bool isActive;
  final String tipo;
  final int puntosRecompensa;
  final String dia;
  final String? matinfo;
  final String horario;
  final String cantidadMinima;
  final String? qnr;
  final int numDia;
  
  Horario({
    required this.day,
    required this.dayName,
    required this.startTime,
    required this.endTime,
    required this.isActive,
    required this.tipo,
    required this.puntosRecompensa,
    String? dia,
    this.matinfo,
    String? horario,
    String? cantidadMinima,
    this.qnr,
    int? numDia,
  }) : dia = dia ?? dayName,
       horario = horario ?? '$startTime - $endTime',
       cantidadMinima = cantidadMinima ?? '5 kg',
       numDia = numDia ?? day;
  
  factory Horario.fromMap(Map<String, dynamic> map) {
    return Horario(
      day: map['day'] ?? 0,
      dayName: map['dayName'] ?? '',
      startTime: map['startTime'] ?? '',
      endTime: map['endTime'] ?? '',
      isActive: map['isActive'] ?? false,
      tipo: map['tipo'] ?? 'regular',
      puntosRecompensa: map['puntosRecompensa'] ?? 10,
      dia: map['dia'],
      matinfo: map['matinfo'],
      horario: map['horario'],
      cantidadMinima: map['cantidadMinima'],
      qnr: map['qnr'],
      numDia: map['numDia'],
    );
  }
  
  Map<String, dynamic> toMap() {
    return {
      'day': day,
      'dayName': dayName,
      'startTime': startTime,
      'endTime': endTime,
      'isActive': isActive,
      'tipo': tipo,
      'puntosRecompensa': puntosRecompensa,
      'dia': dia,
      'matinfo': matinfo,
      'horario': horario,
      'cantidadMinima': cantidadMinima,
      'qnr': qnr,
      'numDia': numDia,
    };
  }
  
  static List<Horario> getMockHorarios() {
    return [
      Horario(
        day: 1,
        dayName: 'Lunes',
        startTime: '08:00',
        endTime: '14:00',
        isActive: true,
        tipo: 'regular',
        puntosRecompensa: 10,
      ),
      Horario(
        day: 2,
        dayName: 'Martes',
        startTime: '08:00',
        endTime: '14:00',
        isActive: true,
        tipo: 'regular',
        puntosRecompensa: 10,
      ),
      Horario(
        day: 3,
        dayName: 'Miércoles',
        startTime: '08:00',
        endTime: '14:00',
        isActive: true,
        tipo: 'regular',
        puntosRecompensa: 10,
      ),
      Horario(
        day: 4,
        dayName: 'Jueves',
        startTime: '08:00',
        endTime: '14:00',
        isActive: true,
        tipo: 'regular',
        puntosRecompensa: 10,
      ),
      Horario(
        day: 5,
        dayName: 'Viernes',
        startTime: '08:00',
        endTime: '14:00',
        isActive: true,
        tipo: 'especial',
        puntosRecompensa: 20,
      ),
    ];
  }
}