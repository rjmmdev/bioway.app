import 'package:firebase_auth/firebase_auth.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import '../firebase/auth_service.dart';

class BioWayAuthService {
  final AuthService _authService = AuthService();
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;
  
  Future<User?> registrarBrindador({
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
      
      if (userCredential.user != null) {
        await _firestore.collection('usuarios').doc(userCredential.user!.uid).set({
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
      }
      
      return userCredential.user;
    } catch (e) {
      throw e;
    }
  }
  
  Future<User?> registrarRecolector({
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
      
      if (userCredential.user != null) {
        await _firestore.collection('usuarios').doc(userCredential.user!.uid).set({
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
      }
      
      return userCredential.user;
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
      
      if (userCredential.user != null) {
        final userDoc = await _firestore
            .collection('usuarios')
            .doc(userCredential.user!.uid)
            .get();
        
        if (userDoc.exists) {
          await _firestore
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
    if (user != null) {
      final userDoc = await _firestore
          .collection('usuarios')
          .doc(user.uid)
          .get();
      
      if (userDoc.exists) {
        return userDoc.data();
      }
    }
    return null;
  }
  
  Stream<User?> get authStateChanges => _authService.authStateChanges;
  
  Future<void> actualizarPerfil({
    required String uid,
    required Map<String, dynamic> datos,
  }) async {
    datos['ultimaActualizacion'] = FieldValue.serverTimestamp();
    await _firestore.collection('usuarios').doc(uid).update(datos);
  }
  
  Future<void> enviarCorreoRecuperacion(String email) async {
    await _authService.sendPasswordResetEmail(email);
  }
}