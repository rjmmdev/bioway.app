import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';

class UserManagementService {
  final FirebaseAuth _auth = FirebaseAuth.instance;
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;

  // Crear usuario administrador
  Future<Map<String, dynamic>?> createAdminUser({
    required String email,
    required String password,
    required String nombre,
    required String telefono,
    String? empresa,
  }) async {
    try {
      // Crear usuario en Firebase Auth
      final UserCredential userCredential = await _auth.createUserWithEmailAndPassword(
        email: email,
        password: password,
      );

      final User? user = userCredential.user;
      if (user != null) {
        // Crear documento en Firestore
        final userData = {
          'uid': user.uid,
          'email': email,
          'nombre': nombre,
          'telefono': telefono,
          'empresa': empresa ?? 'BioWay Admin',
          'userType': 'admin',
          'role': 'administrador',
          'permissions': {
            'canManageUsers': true,
            'canManageCentros': true,
            'canManageEmpresas': true,
            'canViewReports': true,
            'canManageSystem': true,
          },
          'isActive': true,
          'createdAt': FieldValue.serverTimestamp(),
          'updatedAt': FieldValue.serverTimestamp(),
        };

        await _firestore.collection('users').doc(user.uid).set(userData);
        
        // Actualizar displayName del usuario
        await user.updateDisplayName(nombre);
        
        return userData;
      }
    } catch (e) {
      print('Error creando usuario administrador: $e');
      throw e;
    }
    return null;
  }

  // Crear usuario centro de acopio
  Future<Map<String, dynamic>?> createCentroAcopioUser({
    required String email,
    required String password,
    required String nombreCentro,
    required String direccion,
    required String telefono,
    required String responsable,
    required double latitud,
    required double longitud,
    List<String>? materialesAceptados,
    Map<String, String>? horarios,
  }) async {
    try {
      // Crear usuario en Firebase Auth
      final UserCredential userCredential = await _auth.createUserWithEmailAndPassword(
        email: email,
        password: password,
      );

      final User? user = userCredential.user;
      if (user != null) {
        // Crear documento del centro en Firestore
        final centroData = {
          'uid': user.uid,
          'email': email,
          'nombreCentro': nombreCentro,
          'responsable': responsable,
          'telefono': telefono,
          'direccion': direccion,
          'ubicacion': {
            'latitud': latitud,
            'longitud': longitud,
          },
          'userType': 'centro_acopio',
          'role': 'centro',
          'materialesAceptados': materialesAceptados ?? [
            'Plástico PET',
            'Cartón',
            'Papel',
            'Vidrio',
            'Aluminio',
          ],
          'horarios': horarios ?? {
            'lunes': '8:00 AM - 5:00 PM',
            'martes': '8:00 AM - 5:00 PM',
            'miércoles': '8:00 AM - 5:00 PM',
            'jueves': '8:00 AM - 5:00 PM',
            'viernes': '8:00 AM - 5:00 PM',
            'sábado': '9:00 AM - 2:00 PM',
            'domingo': 'Cerrado',
          },
          'estadisticas': {
            'totalRecibido': 0,
            'totalPagado': 0,
            'recolectoresAtendidos': 0,
          },
          'isActive': true,
          'verificado': true,
          'createdAt': FieldValue.serverTimestamp(),
          'updatedAt': FieldValue.serverTimestamp(),
        };

        // Guardar en colección users
        await _firestore.collection('users').doc(user.uid).set(centroData);
        
        // También guardar en colección centros_acopio para búsquedas
        await _firestore.collection('centros_acopio').doc(user.uid).set(centroData);
        
        // Actualizar displayName del usuario
        await user.updateDisplayName(nombreCentro);
        
        return centroData;
      }
    } catch (e) {
      print('Error creando centro de acopio: $e');
      throw e;
    }
    return null;
  }

  // Obtener usuario actual
  Future<Map<String, dynamic>?> getCurrentUser() async {
    try {
      final User? user = _auth.currentUser;
      if (user != null) {
        final doc = await _firestore.collection('users').doc(user.uid).get();
        if (doc.exists) {
          return doc.data();
        }
      }
    } catch (e) {
      print('Error obteniendo usuario actual: $e');
    }
    return null;
  }

  // Verificar si un email ya está registrado
  Future<bool> isEmailRegistered(String email) async {
    try {
      final methods = await _auth.fetchSignInMethodsForEmail(email);
      return methods.isNotEmpty;
    } catch (e) {
      print('Error verificando email: $e');
      return false;
    }
  }

  // Actualizar información del usuario
  Future<void> updateUserData(String uid, Map<String, dynamic> data) async {
    try {
      data['updatedAt'] = FieldValue.serverTimestamp();
      await _firestore.collection('users').doc(uid).update(data);
    } catch (e) {
      print('Error actualizando usuario: $e');
      throw e;
    }
  }

  // Obtener todos los centros de acopio
  Future<List<Map<String, dynamic>>> getAllCentrosAcopio() async {
    try {
      final QuerySnapshot snapshot = await _firestore
          .collection('centros_acopio')
          .where('isActive', isEqualTo: true)
          .get();
      
      return snapshot.docs.map((doc) {
        final data = doc.data() as Map<String, dynamic>;
        data['id'] = doc.id;
        return data;
      }).toList();
    } catch (e) {
      print('Error obteniendo centros de acopio: $e');
      return [];
    }
  }

  // Crear usuarios de prueba
  Future<void> createTestUsers() async {
    try {
      // Usuario administrador de prueba
      await createAdminUser(
        email: 'admin@bioway.com',
        password: 'Admin123!',
        nombre: 'Administrador BioWay',
        telefono: '555-0001',
        empresa: 'BioWay Sistema',
      );

      // Centro de acopio de prueba
      await createCentroAcopioUser(
        email: 'centro1@bioway.com',
        password: 'Centro123!',
        nombreCentro: 'Centro de Acopio Norte',
        direccion: 'Av. Principal #123, Zona Norte',
        telefono: '555-1001',
        responsable: 'Juan Pérez',
        latitud: 19.4326,
        longitud: -99.1332,
      );

      print('Usuarios de prueba creados exitosamente');
    } catch (e) {
      print('Error creando usuarios de prueba: $e');
    }
  }
}