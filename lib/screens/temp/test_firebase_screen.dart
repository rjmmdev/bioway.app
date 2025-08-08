import 'package:flutter/material.dart';
import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:firebase_core/firebase_core.dart';
import '../../utils/colors.dart';
import '../../services/firebase/user_management_service.dart';

class TestFirebaseScreen extends StatefulWidget {
  const TestFirebaseScreen({super.key});

  @override
  State<TestFirebaseScreen> createState() => _TestFirebaseScreenState();
}

class _TestFirebaseScreenState extends State<TestFirebaseScreen> {
  final FirebaseFirestore _firestore = FirebaseFirestore.instance;
  final FirebaseAuth _auth = FirebaseAuth.instance;
  final UserManagementService _userService = UserManagementService();
  
  String _testResult = 'Presiona el botón para probar Firebase';
  bool _isLoading = false;
  List<String> _logs = [];
  
  void _addLog(String message) {
    setState(() {
      _logs.add('[${DateTime.now().toLocal().toString().split('.')[0]}] $message');
      print(message);
    });
  }

  Future<void> _testFirebaseConnection() async {
    setState(() {
      _isLoading = true;
      _testResult = 'Probando conexión con Firebase...';
      _logs.clear();
    });

    try {
      // Test 1: Verificar inicialización de Firebase
      _addLog('🔵 Test 1: Verificando inicialización de Firebase...');
      setState(() => _testResult = 'Test 1: Verificando inicialización...');
      
      try {
        final app = Firebase.apps.first;
        _addLog('✅ Firebase inicializado: ${app.name}');
        _addLog('   - Proyecto: ${app.options.projectId}');
      } catch (e) {
        _addLog('❌ Firebase NO está inicializado: $e');
        return;
      }
      
      // Test 2: Verificar Auth
      _addLog('\n🔵 Test 2: Verificando Firebase Auth...');
      setState(() => _testResult = 'Test 2: Verificando Auth...');
      
      try {
        final currentUser = _auth.currentUser;
        if (currentUser != null) {
          _addLog('✅ Usuario actual: ${currentUser.email}');
        } else {
          _addLog('✅ Firebase Auth conectado (sin usuario actual)');
        }
      } catch (e) {
        _addLog('❌ Error con Firebase Auth: $e');
      }
      
      // Test 3: Escribir datos de prueba
      _addLog('\n🔵 Test 3: Escribiendo datos de prueba...');
      setState(() => _testResult = 'Test 3: Escribiendo datos de prueba...');
      
      final testData = {
        'test': true,
        'timestamp': FieldValue.serverTimestamp(),
        'message': 'Prueba desde BioWay App',
        'fecha': DateTime.now().toIso8601String(),
      };
      
      try {
        await _firestore.collection('test').doc('test_doc').set(testData);
        _addLog('✅ Datos escritos en: test/test_doc');
      } catch (e) {
        _addLog('❌ Error escribiendo: $e');
        if (e.toString().contains('permission')) {
          _addLog('⚠️ Problema de permisos - Revisar reglas de Firestore');
        }
      }
      
      // Test 4: Leer los datos
      _addLog('\n🔵 Test 4: Leyendo datos de prueba...');
      setState(() => _testResult = 'Test 4: Leyendo datos...');
      
      try {
        final doc = await _firestore.collection('test').doc('test_doc').get();
        if (doc.exists) {
          _addLog('✅ Datos leídos correctamente');
        } else {
          _addLog('⚠️ Documento no existe');
        }
      } catch (e) {
        _addLog('❌ Error leyendo: $e');
      }
      
      // Test 5: Crear usuario de prueba con el servicio
      _addLog('\n🔵 Test 5: Creando usuario de prueba con UserManagementService...');
      setState(() => _testResult = 'Test 5: Creando usuario de prueba...');
      
      try {
        final testEmail = 'test_${DateTime.now().millisecondsSinceEpoch}@bioway.com';
        _addLog('📧 Email de prueba: $testEmail');
        
        final userData = await _userService.createAdminUser(
          email: testEmail,
          password: 'Test123!',
          nombre: 'Usuario de Prueba',
          telefono: '555-0000',
          empresa: 'Test Company',
        );
        
        if (userData != null) {
          _addLog('✅ Usuario creado exitosamente');
          _addLog('   - UID: ${userData['uid']}');
          
          // Verificar en Firestore
          final verifyDoc = await _firestore
              .collection('usuarios/administradores')
              .doc(userData['uid'])
              .get();
          
          if (verifyDoc.exists) {
            _addLog('✅ VERIFICADO: Usuario guardado en Firestore');
            _addLog('   - Path: usuarios/administradores/${userData['uid']}');
            
            // Limpiar: eliminar usuario de prueba
            final user = _auth.currentUser;
            if (user != null) {
              await user.delete();
              _addLog('🧹 Usuario de prueba eliminado de Auth');
            }
          } else {
            _addLog('❌ Usuario NO encontrado en Firestore');
          }
        } else {
          _addLog('❌ No se pudo crear el usuario');
        }
      } catch (e) {
        _addLog('❌ Error creando usuario: $e');
        if (e.toString().contains('permission')) {
          _addLog('⚠️ Problema de permisos en Firestore');
        }
        if (e.toString().contains('email-already-in-use')) {
          _addLog('⚠️ El email ya está en uso');
        }
      }
      
      // Test 6: Verificar estructura de colecciones
      _addLog('\n🔵 Test 6: Verificando estructura de colecciones...');
      setState(() => _testResult = 'Test 6: Verificando estructura...');
      
      try {
        // Con la nueva estructura simplificada, no necesitamos documentos padre
        // Solo verificamos que podemos acceder a las colecciones
        final collections = ['administradores', 'centros_acopio', 'brindadores', 'recolectores'];
        
        for (String collection in collections) {
          final snapshot = await _firestore
              .collection('usuarios/$collection')
              .limit(1)
              .get();
          _addLog('✅ Colección usuarios/$collection accesible');
        }
        
        _addLog('✅ Estructura de colecciones verificada');
      } catch (e) {
        _addLog('❌ Error verificando estructura: $e');
      }
      
      // Resumen final
      _addLog('\n' + '='*50);
      _addLog('📊 RESUMEN DE PRUEBAS');
      _addLog('='*50);
      
      final summary = _logs.where((log) => log.contains('✅') || log.contains('❌')).toList();
      
      setState(() {
        _testResult = '''
🔍 DIAGNÓSTICO COMPLETADO

Revisa los logs detallados abajo para ver el resultado de cada prueba.

Si hay errores, verifica:
1. Las reglas de Firestore en Firebase Console
2. La configuración del proyecto
3. Tu conexión a internet
          ''';
      });
      
    } catch (e) {
      _addLog('\n❌ ERROR GENERAL: $e');
      setState(() {
        _testResult = '''
❌ ERROR EN LA PRUEBA:

${e.toString()}

Revisa los logs para más detalles.
        ''';
      });
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _showFirestoreRules() async {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Reglas de Firestore Recomendadas'),
        content: SingleChildScrollView(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              const Text(
                'Para desarrollo (INSEGURO):',
                style: TextStyle(fontWeight: FontWeight.bold, color: Colors.red),
              ),
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.grey[100],
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Text(
                  '''rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if true;
    }
  }
}''',
                  style: TextStyle(fontFamily: 'monospace', fontSize: 12),
                ),
              ),
              const SizedBox(height: 16),
              const Text(
                'Para producción (SEGURO):',
                style: TextStyle(fontWeight: FontWeight.bold, color: Colors.green),
              ),
              const SizedBox(height: 8),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.grey[100],
                  borderRadius: BorderRadius.circular(8),
                ),
                child: const Text(
                  '''rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Usuarios autenticados pueden leer y escribir sus propios datos
    match /usuarios/{userType}/lista/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Contadores pueden ser actualizados por usuarios autenticados
    match /usuarios/{userType} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
  }
}''',
                  style: TextStyle(fontFamily: 'monospace', fontSize: 12),
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Cerrar'),
          ),
        ],
      ),
    );
  }
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: BioWayColors.backgroundGrey,
      appBar: AppBar(
        title: const Text('Test Firebase Connection'),
        backgroundColor: BioWayColors.primaryGreen,
        foregroundColor: Colors.white,
      ),
      body: Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          children: [
            // Información del proyecto
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: BioWayColors.primaryGreen.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                  color: BioWayColors.primaryGreen,
                  width: 1,
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Proyecto Firebase:',
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      color: BioWayColors.darkGreen,
                    ),
                  ),
                  const SizedBox(height: 4),
                  const Text('bioway-mexico'),
                  const SizedBox(height: 12),
                  Text(
                    'Usuario actual:',
                    style: TextStyle(
                      fontWeight: FontWeight.bold,
                      color: BioWayColors.darkGreen,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(_auth.currentUser?.email ?? 'No autenticado'),
                ],
              ),
            ),
            
            const SizedBox(height: 20),
            
            // Botones de acción
            Row(
              children: [
                Expanded(
                  child: SizedBox(
                    height: 56,
                    child: ElevatedButton.icon(
                      onPressed: _isLoading ? null : _testFirebaseConnection,
                      icon: _isLoading 
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(
                                color: Colors.white,
                                strokeWidth: 2,
                              ),
                            )
                          : const Icon(Icons.science),
                      label: Text(
                        _isLoading ? 'Probando...' : 'Ejecutar Prueba',
                        style: const TextStyle(fontSize: 14),
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: BioWayColors.primaryGreen,
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: SizedBox(
                    height: 56,
                    child: ElevatedButton.icon(
                      onPressed: _showFirestoreRules,
                      icon: const Icon(Icons.security),
                      label: const Text(
                        'Ver Reglas',
                        style: TextStyle(fontSize: 14),
                      ),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.orange,
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
            
            const SizedBox(height: 20),
            
            // Resultado de la prueba
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: _testResult.contains('ERROR') 
                    ? Colors.red.withValues(alpha: 0.1)
                    : _testResult.contains('COMPLETADO')
                        ? Colors.green.withValues(alpha: 0.1)
                        : Colors.blue.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                  color: _testResult.contains('ERROR')
                      ? Colors.red
                      : _testResult.contains('COMPLETADO')
                          ? Colors.green
                          : Colors.blue,
                ),
              ),
              child: Text(
                _testResult,
                style: TextStyle(
                  color: _testResult.contains('ERROR')
                      ? Colors.red
                      : _testResult.contains('COMPLETADO')
                          ? Colors.green
                          : Colors.blue,
                  fontSize: 14,
                ),
              ),
            ),
            
            const SizedBox(height: 20),
            
            // Logs detallados
            Expanded(
              child: Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.black87,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.grey),
                ),
                child: _logs.isEmpty
                    ? const Center(
                        child: Text(
                          'Los logs aparecerán aquí...',
                          style: TextStyle(color: Colors.grey),
                        ),
                      )
                    : ListView.builder(
                        itemCount: _logs.length,
                        itemBuilder: (context, index) {
                          final log = _logs[index];
                          Color textColor = Colors.white;
                          
                          if (log.contains('✅')) {
                            textColor = Colors.green;
                          } else if (log.contains('❌')) {
                            textColor = Colors.red;
                          } else if (log.contains('⚠️')) {
                            textColor = Colors.orange;
                          } else if (log.contains('🔵') || log.contains('📌')) {
                            textColor = Colors.blue;
                          } else if (log.contains('📧') || log.contains('📊')) {
                            textColor = Colors.cyan;
                          }
                          
                          return Padding(
                            padding: const EdgeInsets.symmetric(vertical: 2),
                            child: Text(
                              log,
                              style: TextStyle(
                                color: textColor,
                                fontFamily: 'monospace',
                                fontSize: 12,
                              ),
                            ),
                          );
                        },
                      ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}