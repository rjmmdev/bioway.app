import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class LanguageProvider extends ChangeNotifier {
  Locale _currentLocale = const Locale('es', 'MX');
  
  Locale get currentLocale => _currentLocale;
  
  LanguageProvider() {
    _loadSavedLanguage();
  }
  
  Future<void> _loadSavedLanguage() async {
    final prefs = await SharedPreferences.getInstance();
    final languageCode = prefs.getString('language_code') ?? 'es';
    final countryCode = prefs.getString('country_code') ?? 'MX';
    _currentLocale = Locale(languageCode, countryCode);
    notifyListeners();
  }
  
  Future<void> changeLanguage(Locale locale) async {
    print('🔄 Changing language to: ${locale.languageCode}_${locale.countryCode}');
    if (_currentLocale == locale) {
      print('⚠️ Same language, skipping');
      return;
    }
    
    _currentLocale = locale;
    
    // Guardar preferencia
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('language_code', locale.languageCode);
    await prefs.setString('country_code', locale.countryCode ?? '');
    
    print('✅ Language changed and saved');
    notifyListeners();
  }
  
  bool get isSpanish => _currentLocale.languageCode == 'es';
  
  void toggleLanguage() async {
    print('🌍 Toggle language called. Current: ${_currentLocale.languageCode}');
    if (isSpanish) {
      await changeLanguage(const Locale('en', 'US'));
    } else {
      await changeLanguage(const Locale('es', 'MX'));
    }
  }
}