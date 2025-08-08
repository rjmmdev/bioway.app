import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class UserSessionService {
  static final UserSessionService _instance = UserSessionService._internal();
  
  factory UserSessionService() {
    return _instance;
  }
  
  UserSessionService._internal();

  Future<void> saveSession(String userId, String userType) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('userId', userId);
    await prefs.setString('userType', userType);
    await prefs.setString('lastLogin', DateTime.now().toIso8601String());
  }

  Future<Map<String, String?>> getSession() async {
    final prefs = await SharedPreferences.getInstance();
    return {
      'userId': prefs.getString('userId'),
      'userType': prefs.getString('userType'),
      'lastLogin': prefs.getString('lastLogin'),
    };
  }

  Future<void> clearSession() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('userId');
    await prefs.remove('userType');
    await prefs.remove('lastLogin');
    debugPrint('Sesión limpiada correctamente');
  }

  Future<bool> hasSession() async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('userId') != null;
  }
}