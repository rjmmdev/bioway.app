import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:easy_localization/easy_localization.dart';
import '../../utils/colors.dart';
import '../auth/bioway_login_screen.dart';

class LanguageSelectionScreen extends StatefulWidget {
  const LanguageSelectionScreen({Key? key}) : super(key: key);

  @override
  State<LanguageSelectionScreen> createState() => _LanguageSelectionScreenState();
}

class _LanguageSelectionScreenState extends State<LanguageSelectionScreen>
    with SingleTickerProviderStateMixin {
  late AnimationController _animationController;
  late Animation<double> _fadeAnimation;
  late Animation<double> _scaleAnimation;
  
  String _selectedLanguage = 'es'; // Default to Spanish
  
  @override
  void initState() {
    super.initState();
    _setupAnimations();
    _loadSavedLanguage();
  }
  
  void _setupAnimations() {
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 1500),
      vsync: this,
    );
    
    _fadeAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _animationController,
      curve: const Interval(0.0, 0.5, curve: Curves.easeIn),
    ));
    
    _scaleAnimation = Tween<double>(
      begin: 0.8,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _animationController,
      curve: const Interval(0.3, 1.0, curve: Curves.elasticOut),
    ));
    
    _animationController.forward();
  }
  
  Future<void> _loadSavedLanguage() async {
    setState(() {
      _selectedLanguage = context.locale.languageCode;
    });
  }
  
  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }
  
  void _selectLanguage(String languageCode) {
    setState(() {
      _selectedLanguage = languageCode;
    });
    
    // Vibración táctil
    HapticFeedback.lightImpact();
  }
  
  void _continueToLogin() async {
    // Cambiar el idioma usando easy_localization
    if (_selectedLanguage == 'en') {
      await context.setLocale(const Locale('en', 'US'));
    } else {
      await context.setLocale(const Locale('es', 'MX'));
    }
    
    // Navegar al login
    if (mounted) {
      Navigator.pushReplacement(
        context,
        PageRouteBuilder(
          pageBuilder: (context, animation, secondaryAnimation) => 
              const BioWayLoginScreen(),
          transitionsBuilder: (context, animation, secondaryAnimation, child) {
            return FadeTransition(
              opacity: animation,
              child: child,
            );
          },
          transitionDuration: const Duration(milliseconds: 500),
        ),
      );
    }
  }
  
  @override
  Widget build(BuildContext context) {
    SystemChrome.setSystemUIOverlayStyle(
      const SystemUiOverlayStyle(
        statusBarColor: Colors.transparent,
        statusBarIconBrightness: Brightness.light,
      ),
    );
    
    return Scaffold(
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: BioWayColors.backgroundGradient,
          ),
        ),
        child: SafeArea(
          child: AnimatedBuilder(
            animation: _animationController,
            builder: (context, child) {
              return FadeTransition(
                opacity: _fadeAnimation,
                child: Transform.scale(
                  scale: _scaleAnimation.value,
                  child: Padding(
                    padding: const EdgeInsets.all(24.0),
                    child: Column(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        // Logo
                        SvgPicture.asset(
                          'assets/logos/bioway_logo.svg',
                          width: 120,
                          height: 120,
                        ),
                        
                        const SizedBox(height: 48),
                        
                        // Título
                        Text(
                          'select_language'.tr(),
                          style: const TextStyle(
                            fontSize: 28,
                            fontWeight: FontWeight.bold,
                            color: Colors.white,
                          ),
                        ),
                        
                        const SizedBox(height: 12),
                        
                        // Subtítulo
                        Text(
                          'can_change_later'.tr(),
                          style: TextStyle(
                            fontSize: 16,
                            color: Colors.white.withOpacity(0.8),
                          ),
                          textAlign: TextAlign.center,
                        ),
                        
                        const SizedBox(height: 48),
                        
                        // Opciones de idioma
                        Column(
                          children: [
                            // Español
                            _buildLanguageOption(
                              flag: '🇲🇽',
                              language: 'Español',
                              subtitle: 'México',
                              code: 'es',
                            ),
                            
                            const SizedBox(height: 16),
                            
                            // English
                            _buildLanguageOption(
                              flag: '🇺🇸',
                              language: 'English',
                              subtitle: 'United States',
                              code: 'en',
                            ),
                          ],
                        ),
                        
                        const SizedBox(height: 48),
                        
                        // Botón de continuar
                        Container(
                          width: double.infinity,
                          height: 56,
                          decoration: BoxDecoration(
                            gradient: LinearGradient(
                              colors: [
                                BioWayColors.primaryGreen,
                                BioWayColors.primaryGreen.withOpacity(0.8),
                              ],
                            ),
                            borderRadius: BorderRadius.circular(16),
                            boxShadow: [
                              BoxShadow(
                                color: BioWayColors.primaryGreen.withOpacity(0.3),
                                blurRadius: 12,
                                offset: const Offset(0, 4),
                              ),
                            ],
                          ),
                          child: Material(
                            color: Colors.transparent,
                            child: InkWell(
                              onTap: _continueToLogin,
                              borderRadius: BorderRadius.circular(16),
                              child: Center(
                                child: Text(
                                  'continue'.tr(),
                                  style: const TextStyle(
                                    color: Colors.white,
                                    fontSize: 18,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }
  
  Widget _buildLanguageOption({
    required String flag,
    required String language,
    required String subtitle,
    required String code,
  }) {
    final isSelected = _selectedLanguage == code;
    
    return GestureDetector(
      onTap: () => _selectLanguage(code),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: isSelected 
              ? BioWayColors.primaryGreen.withOpacity(0.2)
              : Colors.white.withOpacity(0.1),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(
            color: isSelected 
                ? BioWayColors.primaryGreen
                : Colors.white.withOpacity(0.3),
            width: isSelected ? 2 : 1,
          ),
        ),
        child: Row(
          children: [
            // Bandera
            Text(
              flag,
              style: const TextStyle(fontSize: 32),
            ),
            
            const SizedBox(width: 16),
            
            // Textos
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    language,
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                      color: isSelected ? Colors.white : Colors.white.withOpacity(0.9),
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    subtitle,
                    style: TextStyle(
                      fontSize: 14,
                      color: isSelected 
                          ? Colors.white.withOpacity(0.8)
                          : Colors.white.withOpacity(0.6),
                    ),
                  ),
                ],
              ),
            ),
            
            // Check icon
            if (isSelected)
              Icon(
                Icons.check_circle,
                color: BioWayColors.primaryGreen,
                size: 28,
              ),
          ],
        ),
      ),
    );
  }
}