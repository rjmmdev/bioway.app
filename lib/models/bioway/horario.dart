import 'package:flutter/material.dart';
import '../../l10n/app_localizations.dart';

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
  
  static String _translateMaterial(String material, AppLocalizations? loc) {
    if (loc == null) return material;
    
    switch (material) {
      case 'PET':
        return loc.petBottles.replaceAll('Botellas ', '');
      case 'Cartón':
        return loc.cardboard;
      case 'Papel':
        return loc.paper;
      case 'Aluminio':
        return loc.aluminum;
      case 'Latas':
        return loc.cans;
      case 'Tetra Pak':
        return loc.tetraPak;
      case 'Vidrio':
        return loc.glass;
      case 'Metal':
        return loc.metal;
      case 'Acero':
        return loc.steel;
      case 'Periódico':
        return loc.newspaper;
      case 'Revistas':
        return loc.magazines;
      case 'HDPE':
        return loc.hdpe;
      case 'Todos los materiales':
        return loc.allMaterials;
      case 'Plásticos mixtos':
        return loc.mixedPlastics;
      case 'Bolsas':
        return loc.bags;
      case 'Envolturas':
        return loc.wrappers;
      default:
        return material;
    }
  }
  
  static List<String> _translateMaterials(List<String> materials, AppLocalizations? loc) {
    if (loc == null) return materials;
    return materials.map((m) => _translateMaterial(m, loc)).toList();
  }

  static List<Horario> getMockHorarios([BuildContext? context]) {
    final loc = context != null ? AppLocalizations.of(context) : null;
    return [
      Horario(
        diaSemana: 1,
        nombreDia: loc?.monday ?? 'Lunes',
        activo: true,
        materiales: _translateMaterials(['PET', 'Cartón', 'Papel'], loc),
        horaInicio: const TimeOfDay(hour: 8, minute: 0),
        horaFin: const TimeOfDay(hour: 12, minute: 0),
        cantidadMinima: '2 kg',
        qnr: 'Orgánicos, pilas, electrónicos',
      ),
      Horario(
        diaSemana: 2,
        nombreDia: loc?.tuesday ?? 'Martes',
        activo: true,
        materiales: _translateMaterials(['Aluminio', 'Latas', 'Tetra Pak'], loc),
        horaInicio: const TimeOfDay(hour: 9, minute: 0),
        horaFin: const TimeOfDay(hour: 13, minute: 0),
        cantidadMinima: '1.5 kg',
        qnr: 'Vidrio, residuos peligrosos',
      ),
      Horario(
        diaSemana: 3,
        nombreDia: loc?.wednesday ?? 'Miércoles',
        activo: true,
        materiales: _translateMaterials(['Vidrio', 'Metal', 'Acero'], loc),
        horaInicio: const TimeOfDay(hour: 8, minute: 0),
        horaFin: const TimeOfDay(hour: 12, minute: 0),
        cantidadMinima: '3 kg',
        qnr: 'Plásticos, papel, orgánicos',
      ),
      Horario(
        diaSemana: 4,
        nombreDia: loc?.thursday ?? 'Jueves',
        activo: true,
        materiales: _translateMaterials(['Papel', 'Periódico', 'Revistas'], loc),
        horaInicio: const TimeOfDay(hour: 10, minute: 0),
        horaFin: const TimeOfDay(hour: 14, minute: 0),
        cantidadMinima: '2 kg',
        qnr: 'Plásticos, metal, orgánicos',
      ),
      Horario(
        diaSemana: 5,
        nombreDia: loc?.friday ?? 'Viernes',
        activo: true,
        materiales: _translateMaterials(['PET', 'HDPE', 'Cartón'], loc),
        horaInicio: const TimeOfDay(hour: 8, minute: 0),
        horaFin: const TimeOfDay(hour: 14, minute: 0),
        cantidadMinima: '2.5 kg',
        qnr: 'Vidrio, metal, orgánicos',
      ),
      Horario(
        diaSemana: 6,
        nombreDia: loc?.saturday ?? 'Sábado',
        activo: true,
        materiales: _translateMaterials(['Todos los materiales'], loc),
        horaInicio: const TimeOfDay(hour: 9, minute: 0),
        horaFin: const TimeOfDay(hour: 13, minute: 0),
        cantidadMinima: '1 kg',
        qnr: 'Residuos peligrosos, pilas',
      ),
      Horario(
        diaSemana: 7,
        nombreDia: loc?.sunday ?? 'Domingo',
        activo: true,
        materiales: _translateMaterials(['Plásticos mixtos', 'Bolsas', 'Envolturas'], loc),
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