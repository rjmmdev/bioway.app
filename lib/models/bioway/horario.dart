import 'package:flutter/material.dart';

class Horario {
  final int diaSemana;
  final String nombreDia;
  final bool activo;
  final List<String> materiales;
  final TimeOfDay? horaInicio;
  final TimeOfDay? horaFin;
  final String? descripcion;
  final String? dia;
  final String? matinfo;
  final String? horario;
  final String? cantidadMinima;
  final String? qnr;
  final int? numDia;

  Horario({
    required this.diaSemana,
    required this.nombreDia,
    required this.activo,
    required this.materiales,
    this.horaInicio,
    this.horaFin,
    this.descripcion,
    String? dia,
    String? matinfo,
    String? horario,
    this.cantidadMinima,
    this.qnr,
    int? numDia,
  }) : dia = dia ?? nombreDia,
       matinfo = matinfo ?? materiales.join(', '),
       horario = horario ?? (horaInicio != null && horaFin != null 
         ? '${_formatTime(horaInicio)} - ${_formatTime(horaFin)}' 
         : 'Sin horario'),
       numDia = numDia ?? diaSemana;

  static String _formatTime(TimeOfDay time) {
    final hour = time.hour.toString().padLeft(2, '0');
    final minute = time.minute.toString().padLeft(2, '0');
    final period = time.hour >= 12 ? 'PM' : 'AM';
    final displayHour = time.hour > 12 ? time.hour - 12 : (time.hour == 0 ? 12 : time.hour);
    return '$displayHour:$minute $period';
  }

  static List<Horario> getMockHorarios() {
    return [
      Horario(
        diaSemana: 1,
        nombreDia: 'Lunes',
        activo: true,
        materiales: ['PET', 'Cartón', 'Papel'],
        horaInicio: const TimeOfDay(hour: 8, minute: 0),
        horaFin: const TimeOfDay(hour: 12, minute: 0),
        cantidadMinima: '2 kg',
        qnr: 'Orgánicos, pilas, electrónicos',
      ),
      Horario(
        diaSemana: 2,
        nombreDia: 'Martes',
        activo: true,
        materiales: ['Aluminio', 'Latas', 'Tetra Pak'],
        horaInicio: const TimeOfDay(hour: 9, minute: 0),
        horaFin: const TimeOfDay(hour: 13, minute: 0),
        cantidadMinima: '1.5 kg',
        qnr: 'Vidrio, residuos peligrosos',
      ),
      Horario(
        diaSemana: 3,
        nombreDia: 'Miércoles',
        activo: true,
        materiales: ['Vidrio', 'Metal', 'Acero'],
        horaInicio: const TimeOfDay(hour: 8, minute: 0),
        horaFin: const TimeOfDay(hour: 12, minute: 0),
        cantidadMinima: '3 kg',
        qnr: 'Plásticos, papel, orgánicos',
      ),
      Horario(
        diaSemana: 4,
        nombreDia: 'Jueves',
        activo: true,
        materiales: ['Papel', 'Periódico', 'Revistas'],
        horaInicio: const TimeOfDay(hour: 10, minute: 0),
        horaFin: const TimeOfDay(hour: 14, minute: 0),
        cantidadMinima: '2 kg',
        qnr: 'Plásticos, metal, orgánicos',
      ),
      Horario(
        diaSemana: 5,
        nombreDia: 'Viernes',
        activo: true,
        materiales: ['PET', 'HDPE', 'Cartón'],
        horaInicio: const TimeOfDay(hour: 8, minute: 0),
        horaFin: const TimeOfDay(hour: 14, minute: 0),
        cantidadMinima: '2.5 kg',
        qnr: 'Vidrio, metal, orgánicos',
      ),
      Horario(
        diaSemana: 6,
        nombreDia: 'Sábado',
        activo: true,
        materiales: ['Todos los materiales'],
        horaInicio: const TimeOfDay(hour: 9, minute: 0),
        horaFin: const TimeOfDay(hour: 13, minute: 0),
        cantidadMinima: '1 kg',
        qnr: 'Residuos peligrosos, pilas',
      ),
      Horario(
        diaSemana: 7,
        nombreDia: 'Domingo',
        activo: true,
        materiales: ['Plásticos mixtos', 'Bolsas', 'Envolturas'],
        horaInicio: const TimeOfDay(hour: 10, minute: 0),
        horaFin: const TimeOfDay(hour: 12, minute: 0),
        cantidadMinima: '1.5 kg',
        qnr: 'Vidrio, metal, orgánicos',
      ),
    ];
  }

  String get horaFormatted {
    if (horaInicio == null || horaFin == null) return 'Sin horario';
    return '${_formatTime(horaInicio!)} - ${_formatTime(horaFin!)}';
  }
}