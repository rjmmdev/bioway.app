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
      print('🔵 Iniciando creación de administrador...');
      print('📧 Email: $email');
      
      // Crear usuario en Firebase Auth
      final UserCredential userCredential = await _auth.createUserWithEmailAndPassword(
        email: email,
        password: password,
      );
      print('✅ Usuario creado en Firebase Auth: ${userCredential.user?.uid}');

      final User? user = userCredential.user;
      if (user != null) {
        // Crear documento en Firestore con la estructura correcta
        final userData = {
          'uid': user.uid,
          'email': email,
          'nombre': nombre,
          'telefono': telefono,
          'empresa': empresa ?? 'BioWay Admin',
          'userType': 'administrador',
          'role': 'administrador',
          'permissions': {
            'canManageUsers': true,
            'canManageCentros': true,
            'canManageEmpresas': true,
            'canViewReports': true,
            'canManageSystem': true,
          },
          'isActive': true,
        };

        print('📝 Guardando en Firestore: usuarios/administradores/lista/${user.uid}');
        
        // Guardar en la estructura: usuarios -> administradores -> {uid}
        await _firestore
            .collection('usuarios')
            .doc('administradores')
            .collection('lista')
            .doc(user.uid)
            .set(userData);
        
        print('✅ Datos guardados en Firestore');
        
        
        // Actualizar displayName del usuario
        await user.updateDisplayName(nombre);
        
        // Verificar que se guardó correctamente
        final verifyDoc = await _firestore
            .collection('usuarios')
            .doc('administradores')
            .collection('lista')
            .doc(user.uid)
            .get();
        
        if (verifyDoc.exists) {
          print('✅ VERIFICACIÓN: Usuario administrador guardado correctamente en Firestore');
          return userData;
        } else {
          print('❌ ERROR: No se pudo verificar el guardado en Firestore');
          throw Exception('No se pudo verificar el guardado en Firestore');
        }
      }
    } catch (e) {
      print('❌ Error creando usuario administrador: $e');
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
      print('🔵 Iniciando creación de centro de acopio...');
      print('📧 Email: $email');
      print('🏪 Centro: $nombreCentro');
      
      // Crear usuario en Firebase Auth
      final UserCredential userCredential = await _auth.createUserWithEmailAndPassword(
        email: email,
        password: password,
      );
      print('✅ Usuario creado en Firebase Auth: ${userCredential.user?.uid}');

      final User? user = userCredential.user;
      if (user != null) {
        // Crear documento del centro en Firestore con la estructura correcta
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
          'userType': 'centros_acopio',
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
        };

        print('📝 Guardando en Firestore: usuarios/centros_acopio/lista/${user.uid}');
        
        // Guardar en la estructura: usuarios -> centros_acopio -> lista -> {uid}
        await _firestore
            .collection('usuarios')
            .doc('centros_acopio')
            .collection('lista')
            .doc(user.uid)
            .set(centroData);
        
        print('✅ Datos guardados en Firestore');
        
        // También guardar una referencia en la colección principal para búsquedas rápidas
        await _firestore.collection('usuarios').doc('centros_acopio').set({
          'totalCentros': FieldValue.increment(1),
          'ultimaActualizacion': FieldValue.serverTimestamp(),
        }, SetOptions(merge: true));
        
        print('✅ Contador actualizado');
        
        // Actualizar displayName del usuario
        await user.updateDisplayName(nombreCentro);
        
        // Verificar que se guardó correctamente
        final verifyDoc = await _firestore
            .collection('usuarios')
            .doc('centros_acopio')
            .collection('lista')
            .doc(user.uid)
            .get();
        
        if (verifyDoc.exists) {
          print('✅ VERIFICACIÓN: Centro de acopio guardado correctamente en Firestore');
          return centroData;
        } else {
          print('❌ ERROR: No se pudo verificar el guardado en Firestore');
          throw Exception('No se pudo verificar el guardado en Firestore');
        }
      }
    } catch (e) {
      print('❌ Error creando centro de acopio: $e');
      throw e;
    }
    return null;
  }

  // Obtener usuario actual
  Future<Map<String, dynamic>?> getCurrentUser() async {
    try {
      final User? user = _auth.currentUser;
      if (user != null) {
        // Buscar en diferentes colecciones según el tipo de usuario
        // Primero intentar en administradores
        var doc = await _firestore
            .collection('usuarios')
            .doc('administradores')
            .collection('lista')
            .doc(user.uid)
            .get();
        
        if (doc.exists) {
          return doc.data();
        }
        
        // Luego en centros de acopio
        doc = await _firestore
            .collection('usuarios')
            .doc('centros_acopio')
            .collection('lista')
            .doc(user.uid)
            .get();
        
        if (doc.exists) {
          return doc.data();
        }
        
        // Luego en brindadores
        doc = await _firestore
            .collection('usuarios')
            .doc('brindadores')
            .collection('lista')
            .doc(user.uid)
            .get();
        
        if (doc.exists) {
          return doc.data();
        }
        
        // Finalmente en recolectores
        doc = await _firestore
            .collection('usuarios')
            .doc('recolectores')
            .collection('lista')
            .doc(user.uid)
            .get();
        
        if (doc.exists) {
          return doc.data();
        }
      }
    } catch (e) {
      print('Error obteniendo usuario actual: $e');
    }
    return null;
  }
  
  // Crear usuario brindador (desde el registro normal)
  Future<Map<String, dynamic>?> createBrindadorUser({
    required String email,
    required String password,
    required String nombre,
    required String telefono,
    String? direccion,
  }) async {
    try {
      // Crear usuario en Firebase Auth
      final UserCredential userCredential = await _auth.createUserWithEmailAndPassword(
        email: email,
        password: password,
      );

      final User? user = userCredential.user;
      if (user != null) {
        // Crear documento en Firestore con la estructura correcta
        final userData = {
          'uid': user.uid,
          'email': email,
          'nombre': nombre,
          'telefono': telefono,
          'direccion': direccion ?? '',
          'userType': 'brindador',
          'role': 'brindador',
          'puntos': 0,
          'nivel': 'Principiante',
          'totalReciclado': 0,
          'isActive': true,
        };

        // Guardar en la estructura: usuarios -> brindadores -> lista -> {uid}
        await _firestore
            .collection('usuarios')
            .doc('brindadores')
            .collection('lista')
            .doc(user.uid)
            .set(userData);
        
        // También guardar una referencia en la colección principal
        await _firestore.collection('usuarios').doc('brindadores').set({
          'totalUsuarios': FieldValue.increment(1),
          'ultimaActualizacion': FieldValue.serverTimestamp(),
        }, SetOptions(merge: true));
        
        // Actualizar displayName del usuario
        await user.updateDisplayName(nombre);
        
        return userData;
      }
    } catch (e) {
      print('Error creando usuario brindador: $e');
      throw e;
    }
    return null;
  }
  
  // Crear usuario recolector
  Future<Map<String, dynamic>?> createRecolectorUser({
    required String email,
    required String password,
    required String nombre,
    required String telefono,
    String? vehiculo,
    String? zona,
  }) async {
    try {
      // Crear usuario en Firebase Auth
      final UserCredential userCredential = await _auth.createUserWithEmailAndPassword(
        email: email,
        password: password,
      );

      final User? user = userCredential.user;
      if (user != null) {
        // Crear documento en Firestore con la estructura correcta
        final userData = {
          'uid': user.uid,
          'email': email,
          'nombre': nombre,
          'telefono': telefono,
          'vehiculo': vehiculo ?? 'Bicicleta',
          'zona': zona ?? 'Sin asignar',
          'userType': 'recolector',
          'role': 'recolector',
          'totalRecolectado': 0,
          'viajesCompletados': 0,
          'calificacion': 5.0,
          'isActive': true,
          'disponible': true,
        };

        // Guardar en la estructura: usuarios -> recolectores -> lista -> {uid}
        await _firestore
            .collection('usuarios')
            .doc('recolectores')
            .collection('lista')
            .doc(user.uid)
            .set(userData);
        
        // También guardar una referencia en la colección principal
        await _firestore.collection('usuarios').doc('recolectores').set({
          'totalUsuarios': FieldValue.increment(1),
          'ultimaActualizacion': FieldValue.serverTimestamp(),
        }, SetOptions(merge: true));
        
        // Actualizar displayName del usuario
        await user.updateDisplayName(nombre);
        
        return userData;
      }
    } catch (e) {
      print('Error creando usuario recolector: $e');
      throw e;
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
  Future<void> updateUserData(String uid, String userType, Map<String, dynamic> data) async {
    try {
      await _firestore.collection('usuarios').doc(userType).collection('lista').doc(uid).update(data);
    } catch (e) {
      print('Error actualizando usuario: $e');
      throw e;
    }
  }

  // Obtener todos los centros de acopio
  Future<List<Map<String, dynamic>>> getAllCentrosAcopio() async {
    try {
      final QuerySnapshot snapshot = await _firestore
          .collection('usuarios')
          .doc('centros_acopio')
          .collection('lista')
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