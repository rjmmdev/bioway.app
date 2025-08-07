import 'package:shared_preferences/shared_preferences.dart';

class UserSessionService {
  static UserSessionService? _instance;
  
  UserSessionService._();
  
  factory UserSessionService() {
    _instance ??= UserSessionService._();
    return _instance!;
  }
  
  Future<void> saveSession(String userId, String userRole) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('userId', userId);
    await prefs.setString('userRole', userRole);
  }
  
  Future<Map<String, String?>> getSession() async {
    final prefs = await SharedPreferences.getInstance();
    return {
      'userId': prefs.getString('userId'),
      'userRole': prefs.getString('userRole'),
    };
  }
  
  Future<void> clearSession() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('userId');
    await prefs.remove('userRole');
  }
  
  Future<bool> hasSession() async {
    final session = await getSession();
    return session['userId'] != null && session['userRole'] != null;
  }
}