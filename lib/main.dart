import 'package:flutter/material.dart';
import 'screens/splash/splash_screen.dart';

void main() {
  runApp(const BioWayApp());
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
      home: const SplashScreen(),
    );
  }
}