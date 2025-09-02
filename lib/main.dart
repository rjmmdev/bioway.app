import 'package:flutter/material.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:easy_localization/easy_localization.dart';
import 'package:provider/provider.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import 'firebase_options.dart';
import 'screens/splash/splash_screen.dart';
import 'l10n/app_localizations.dart';
import 'providers/language_provider.dart';

void main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await EasyLocalization.ensureInitialized();
  
  // Disable Easy Localization debug logs
  EasyLocalization.logger.enableBuildModes = [];
  EasyLocalization.logger.enableLevels = [];
  
  // Solo inicializar Firebase si no es web
  if (!kIsWeb) {
    try {
      await Firebase.initializeApp(
        options: DefaultFirebaseOptions.currentPlatform,
      );
    } catch (e) {
      print('Error inicializando Firebase: $e');
    }
  } else {
    print('Ejecutando en web - Firebase no configurado para esta plataforma');
  }
  
  runApp(
    EasyLocalization(
      supportedLocales: const [
        Locale('es', 'MX'),
        Locale('en', 'US'),
      ],
      path: 'assets/translations',
      fallbackLocale: const Locale('es', 'MX'),
      useOnlyLangCode: false,
      useFallbackTranslations: true,
      saveLocale: true,
      child: ChangeNotifierProvider(
        create: (context) => LanguageProvider(),
        child: const BioWayApp(),
      ),
    ),
  );
}

class BioWayApp extends StatelessWidget {
  const BioWayApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'BioWay',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        primarySwatch: Colors.green,
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.green),
        useMaterial3: true,
      ),
      localizationsDelegates: [
        AppLocalizations.delegate,
        ...context.localizationDelegates,
      ],
      supportedLocales: context.supportedLocales,
      locale: context.locale,
      home: const SplashScreen(),
      builder: (context, child) {
        return MediaQuery(
          // Ignorar las configuraciones de accesibilidad del sistema
          data: MediaQuery.of(context).copyWith(
            textScaler: const TextScaler.linear(1.0), // Mantener el tamaño de texto fijo
            boldText: false, // Ignorar la configuración de texto en negrita
          ),
          child: child!,
        );
      },
    );
  }
}