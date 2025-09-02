import 'package:firebase_auth/firebase_auth.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter/foundation.dart' show kIsWeb;
import '../firebase/auth_service.dart';

class BioWayAuthService {
  final AuthService _authService = AuthService();
  FirebaseFirestore? _firestore;
  
  BioWayAuthService() {
    // Solo inicializar Firestore si no es web
    if (!kIsWeb) {
      try {
        _firestore = FirebaseFirestore.instance;
      } catch (e) {
        print('Firestore no disponible');
      }
    }
  }
  
  Future<dynamic> registrarBrindador({
    required String email,
    required String password,
    required String nombre,
    required String direccion,
    required String numeroExterior,
    required String codigoPostal,
    required String estado,
    required String municipio,
    required String colonia,
  }) async {
    try {
      final userCredential = await _authService.createUserWithEmailAndPassword(
        email: email,
        password: password,
      );
      
      // Si es un MockUser (no hay Firebase), solo retornar el usuario
      if (userCredential is MockUser) {
        return userCredential;
      }
      
      // Si hay Firebase y Firestore disponible
      if (userCredential is UserCredential && userCredential.user != null && _firestore != null) {
        await _firestore!.collection('usuarios').doc(userCredential.user!.uid).set({
          'uid': userCredential.user!.uid,
          'email': email,
          'nombre': nombre,
          'tipoUsuario': 'brindador',
          'direccion': {
            'calle': direccion,
            'numeroExterior': numeroExterior,
            'codigoPostal': codigoPostal,
            'estado': estado,
            'municipio': municipio,
            'colonia': colonia,
          },
          'puntos': 0,
          'nivel': 1,
          'activo': true,
          'fechaRegistro': FieldValue.serverTimestamp(),
          'ultimaActualizacion': FieldValue.serverTimestamp(),
        });
        
        await userCredential.user!.updateDisplayName(nombre);
        return userCredential.user;
      }
      
      return userCredential;
    } catch (e) {
      throw e;
    }
  }
  
  Future<dynamic> registrarRecolector({
    required String email,
    required String password,
    required String nombre,
    String? empresa,
  }) async {
    try {
      final userCredential = await _authService.createUserWithEmailAndPassword(
        email: email,
        password: password,
      );
      
      // Si es un MockUser (no hay Firebase), solo retornar el usuario
      if (userCredential is MockUser) {
        return userCredential;
      }
      
      // Si hay Firebase y Firestore disponible
      if (userCredential is UserCredential && userCredential.user != null && _firestore != null) {
        await _firestore!.collection('usuarios').doc(userCredential.user!.uid).set({
          'uid': userCredential.user!.uid,
          'email': email,
          'nombre': nombre,
          'tipoUsuario': 'recolector',
          'empresa': empresa ?? 'Ninguna',
          'materialesRecolectados': 0,
          'calificacion': 5.0,
          'activo': true,
          'verificado': false,
          'fechaRegistro': FieldValue.serverTimestamp(),
          'ultimaActualizacion': FieldValue.serverTimestamp(),
        });
        
        await userCredential.user!.updateDisplayName(nombre);
        return userCredential.user;
      }
      
      return userCredential;
    } catch (e) {
      throw e;
    }
  }
  
  Future<Map<String, dynamic>?> iniciarSesion({
    required String email,
    required String password,
  }) async {
    try {
      final userCredential = await _authService.signInWithEmailAndPassword(
        email: email,
        password: password,
      );
      
      // Si es un MockUser (no hay Firebase), retornar datos simulados
      if (userCredential is MockUser) {
        return {
          'uid': userCredential.uid,
          'email': userCredential.email,
          'nombre': userCredential.displayName ?? 'Usuario',
          'tipoUsuario': 'brindador',
          'puntos': 1250,
          'nivel': 3,
          'activo': true,
        };
      }
      
      // Si hay Firebase y Firestore disponible
      if (userCredential is UserCredential && userCredential.user != null && _firestore != null) {
        final userDoc = await _firestore!
            .collection('usuarios')
            .doc(userCredential.user!.uid)
            .get();
        
        if (userDoc.exists) {
          await _firestore!
              .collection('usuarios')
              .doc(userCredential.user!.uid)
              .update({
            'ultimoAcceso': FieldValue.serverTimestamp(),
          });
          
          return userDoc.data();
        }
      }
      
      return null;
    } catch (e) {
      throw e;
    }
  }
  
  Future<void> cerrarSesion() async {
    await _authService.signOut();
  }
  
  Future<Map<String, dynamic>?> obtenerUsuarioActual() async {
    final user = _authService.currentUser;
    
    // Si es un MockUser, retornar datos simulados
    if (user is MockUser) {
      return {
        'uid': user.uid,
        'email': user.email,
        'nombre': user.displayName ?? 'Usuario',
        'tipoUsuario': 'brindador',
        'puntos': 1250,
        'nivel': 3,
        'activo': true,
      };
    }
    
    // Si es un User de Firebase
    if (user is User && _firestore != null) {
      final userDoc = await _firestore!
          .collection('usuarios')
          .doc(user.uid)
          .get();
      
      if (userDoc.exists) {
        return userDoc.data();
      }
    }
    
    return null;
  }
  
  Stream<User?> get authStateChanges {
    // Convertir el Stream<dynamic> a Stream<User?>
    return _authService.authStateChanges.map((user) {
      if (user is User) {
        return user;
      }
      // Si es MockUser o null, retornar null
      return null;
    });
  }
  
  Future<void> actualizarPerfil({
    required String uid,
    required Map<String, dynamic> datos,
  }) async {
    if (_firestore != null) {
      datos['ultimaActualizacion'] = FieldValue.serverTimestamp();
      await _firestore!.collection('usuarios').doc(uid).update(datos);
    } else {
      // En modo mock, solo imprimir un mensaje
      print('Mock: Perfil actualizado para usuario $uid');
    }
  }
  
  Future<void> enviarCorreoRecuperacion(String email) async {
    await _authService.sendPasswordResetEmail(email);
  }
}