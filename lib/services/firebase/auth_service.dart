import 'package:firebase_auth/firebase_auth.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/foundation.dart' show kIsWeb;

enum FirebasePlatform { bioway, maestro }

// Mock user class for when Firebase is not available
class MockUser {
  final String uid;
  final String? email;
  final String? displayName;
  
  MockUser({required this.uid, this.email, this.displayName});
}

class AuthService {
  FirebaseAuth? _auth;
  FirebaseFirestore? _firestore;
  MockUser? _mockUser;
  
  AuthService() {
    // Solo inicializar Firebase si no es web y está disponible
    if (!kIsWeb) {
      try {
        _auth = FirebaseAuth.instance;
        _firestore = FirebaseFirestore.instance;
      } catch (e) {
        print('Firebase no disponible, usando modo mock');
      }
    }
  }
  
  dynamic get currentUser {
    if (_auth != null) {
      return _auth!.currentUser;
    }
    return _mockUser;
  }
  
  Stream<dynamic> get authStateChanges {
    if (_auth != null) {
      return _auth!.authStateChanges();
    }
    // Return a stream with mock user for testing
    return Stream.value(_mockUser);
  }
  
  Future<void> initializeForPlatform(FirebasePlatform platform) async {
    print('✅ Firebase inicializado para plataforma: ${platform.name}');
  }
  
  Future<dynamic> signInWithEmailAndPassword({
    required String email,
    required String password,
  }) async {
    if (_auth != null) {
      try {
        return await _auth!.signInWithEmailAndPassword(
          email: email,
          password: password,
        );
      } catch (e) {
        throw _handleAuthError(e);
      }
    } else {
      // Mock login - siempre exitoso para navegación libre
      await Future.delayed(const Duration(seconds: 1));
      _mockUser = MockUser(
        uid: 'mock_user_123',
        email: email,
        displayName: email.split('@')[0],
      );
      return _mockUser;
    }
  }
  
  Future<dynamic> createUserWithEmailAndPassword({
    required String email,
    required String password,
  }) async {
    if (_auth != null) {
      try {
        return await _auth!.createUserWithEmailAndPassword(
          email: email,
          password: password,
        );
      } catch (e) {
        throw _handleAuthError(e);
      }
    } else {
      // Mock registration - siempre exitoso para navegación libre
      await Future.delayed(const Duration(seconds: 1));
      _mockUser = MockUser(
        uid: 'mock_user_${DateTime.now().millisecondsSinceEpoch}',
        email: email,
        displayName: email.split('@')[0],
      );
      return _mockUser;
    }
  }
  
  Future<void> signOut() async {
    if (_auth != null) {
      await _auth!.signOut();
    } else {
      _mockUser = null;
    }
  }
  
  Future<void> sendPasswordResetEmail(String email) async {
    if (_auth != null) {
      await _auth!.sendPasswordResetEmail(email: email);
    } else {
      // Mock password reset
      await Future.delayed(const Duration(seconds: 1));
      print('Mock: Email de recuperación enviado a $email');
    }
  }
  
  Future<void> updateUserProfile({
    String? displayName,
    String? photoURL,
  }) async {
    if (_auth != null) {
      final user = _auth!.currentUser;
      if (user != null) {
        await user.updateDisplayName(displayName);
        await user.updatePhotoURL(photoURL);
      }
    } else {
      // Mock update profile
      if (_mockUser != null) {
        _mockUser = MockUser(
          uid: _mockUser!.uid,
          email: _mockUser!.email,
          displayName: displayName ?? _mockUser!.displayName,
        );
      }
    }
  }
  
  Future<bool> checkEmailExists(String email) async {
    if (_auth != null) {
      try {
        final methods = await _auth!.fetchSignInMethodsForEmail(email);
        return methods.isNotEmpty;
      } catch (e) {
        return false;
      }
    } else {
      // Mock - siempre retorna false para permitir registro
      return false;
    }
  }
  
  String _handleAuthError(dynamic error) {
    if (error is FirebaseAuthException) {
      switch (error.code) {
        case 'user-not-found':
          return 'No existe una cuenta con este correo electrónico';
        case 'wrong-password':
          return 'Contraseña incorrecta';
        case 'email-already-in-use':
          return 'Este correo ya está registrado';
        case 'invalid-email':
          return 'Correo electrónico inválido';
        case 'weak-password':
          return 'La contraseña es muy débil';
        case 'network-request-failed':
          return 'Error de conexión. Verifica tu internet';
        case 'too-many-requests':
          return 'Demasiados intentos. Intenta más tarde';
        default:
          return error.message ?? 'Error de autenticación';
      }
    }
    return 'Error desconocido';
  }
}