import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import '../../services/firebase/user_management_service.dart';
import '../../utils/colors.dart';

class CentroAcopioRegisterScreen extends StatefulWidget {
  const CentroAcopioRegisterScreen({super.key});

  @override
  State<CentroAcopioRegisterScreen> createState() => _CentroAcopioRegisterScreenState();
}

class _CentroAcopioRegisterScreenState extends State<CentroAcopioRegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final UserManagementService _userService = UserManagementService();
  
  // Controladores
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();
  final _nombreCentroController = TextEditingController();
  final _responsableController = TextEditingController();
  final _telefonoController = TextEditingController();
  final _direccionController = TextEditingController();
  final _latitudController = TextEditingController();
  final _longitudController = TextEditingController();
  
  bool _isLoading = false;
  bool _obscurePassword = true;
  bool _obscureConfirmPassword = true;
  
  // Materiales aceptados
  final List<String> _materialesDisponibles = [
    'Plástico PET',
    'Cartón',
    'Papel',
    'Vidrio',
    'Aluminio',
    'Metal',
    'Electrónicos',
    'Baterías',
    'Aceite usado',
  ];
  
  List<String> _materialesSeleccionados = [
    'Plástico PET',
    'Cartón',
    'Papel',
    'Vidrio',
    'Aluminio',
  ];

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _confirmPasswordController.dispose();
    _nombreCentroController.dispose();
    _responsableController.dispose();
    _telefonoController.dispose();
    _direccionController.dispose();
    _latitudController.dispose();
    _longitudController.dispose();
    super.dispose();
  }

  Future<void> _handleRegister() async {
    if (_formKey.currentState!.validate()) {
      setState(() => _isLoading = true);
      
      try {
        print('🔵 Iniciando registro de centro de acopio...');
        
        // Verificar si el email ya está registrado
        final isRegistered = await _userService.isEmailRegistered(_emailController.text.trim());
        if (isRegistered) {
          _showMessage('Este email ya está registrado', isError: true);
          setState(() => _isLoading = false);
          return;
        }
        
        // Crear centro de acopio
        final centroData = await _userService.createCentroAcopioUser(
          email: _emailController.text.trim(),
          password: _passwordController.text,
          nombreCentro: _nombreCentroController.text.trim(),
          direccion: _direccionController.text.trim(),
          telefono: _telefonoController.text.trim(),
          responsable: _responsableController.text.trim(),
          latitud: double.tryParse(_latitudController.text) ?? 19.4326,
          longitud: double.tryParse(_longitudController.text) ?? -99.1332,
          materialesAceptados: _materialesSeleccionados,
        );
        
        if (centroData != null) {
          print('✅ Centro de acopio creado exitosamente');
          _showMessage(
            '✅ Centro de Acopio creado y guardado en Firebase\n' +
            'Centro: ${_nombreCentroController.text.trim()}\n' +
            'Email: ${_emailController.text.trim()}\n' +
            'UID: ${centroData['uid']}',
          );
          await Future.delayed(const Duration(seconds: 3));
          if (mounted) Navigator.pop(context);
        } else {
          throw Exception('No se pudo crear el centro de acopio');
        }
      } catch (e) {
        print('❌ Error en registro: $e');
        String errorMessage = 'Error: ';
        if (e.toString().contains('email-already-in-use')) {
          errorMessage = 'Este email ya está registrado';
        } else if (e.toString().contains('weak-password')) {
          errorMessage = 'La contraseña es muy débil';
        } else if (e.toString().contains('invalid-email')) {
          errorMessage = 'El email no es válido';
        } else if (e.toString().contains('network-request-failed')) {
          errorMessage = 'Error de conexión. Verifica tu internet';
        } else {
          errorMessage = 'Error: ${e.toString()}';
        }
        _showMessage(errorMessage, isError: true);
      } finally {
        if (mounted) setState(() => _isLoading = false);
      }
    }
  }

  void _showMessage(String message, {bool isError = false}) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Row(
          children: [
            Icon(
              isError ? Icons.error : Icons.check_circle,
              color: Colors.white,
            ),
            const SizedBox(width: 12),
            Expanded(child: Text(message)),
          ],
        ),
        backgroundColor: isError ? BioWayColors.error : BioWayColors.success,
        behavior: SnackBarBehavior.floating,
        margin: const EdgeInsets.all(20),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: BioWayColors.backgroundGrey,
      body: Container(
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [
              BioWayColors.primaryGreen,
              BioWayColors.primaryGreen.withValues(alpha: 0.8),
              BioWayColors.darkGreen,
            ],
          ),
        ),
        child: SafeArea(
          child: Column(
            children: [
              // Header
              Container(
                padding: const EdgeInsets.all(16),
                child: Row(
                  children: [
                    IconButton(
                      onPressed: () => Navigator.pop(context),
                      icon: const Icon(Icons.arrow_back_ios, color: Colors.white),
                    ),
                    const Expanded(
                      child: Text(
                        'REGISTRO TEMPORAL - CENTRO',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                          letterSpacing: 1,
                        ),
                        textAlign: TextAlign.center,
                      ),
                    ),
                    const SizedBox(width: 48),
                  ],
                ),
              ),
              
              // Warning banner
              Container(
                margin: const EdgeInsets.symmetric(horizontal: 20),
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.orange.withValues(alpha: 0.2),
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: Colors.orange, width: 1),
                ),
                child: Row(
                  children: [
                    const Icon(Icons.warning, color: Colors.orange, size: 20),
                    const SizedBox(width: 8),
                    const Expanded(
                      child: Text(
                        'Este formulario es temporal y será removido en producción',
                        style: TextStyle(color: Colors.white, fontSize: 12),
                      ),
                    ),
                  ],
                ),
              ),
              
              // Form
              Expanded(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.all(20),
                  child: Form(
                    key: _formKey,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        // Icono de centro
                        Center(
                          child: Container(
                            padding: const EdgeInsets.all(20),
                            decoration: BoxDecoration(
                              color: Colors.white.withValues(alpha: 0.1),
                              shape: BoxShape.circle,
                            ),
                            child: const Icon(
                              Icons.warehouse,
                              size: 60,
                              color: Colors.white,
                            ),
                          ),
                        ),
                        const SizedBox(height: 30),
                        
                        // Sección: Información del Centro
                        Text(
                          'INFORMACIÓN DEL CENTRO',
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.9),
                            fontSize: 14,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 1,
                          ),
                        ),
                        const SizedBox(height: 16),
                        
                        // Campo Nombre del Centro
                        TextFormField(
                          controller: _nombreCentroController,
                          style: const TextStyle(color: Colors.white),
                          decoration: _buildInputDecoration(
                            'Nombre del Centro',
                            Icons.business,
                          ),
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Por favor ingrese el nombre del centro';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 16),
                        
                        // Campo Responsable
                        TextFormField(
                          controller: _responsableController,
                          style: const TextStyle(color: Colors.white),
                          decoration: _buildInputDecoration(
                            'Nombre del Responsable',
                            Icons.person,
                          ),
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Por favor ingrese el nombre del responsable';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 16),
                        
                        // Campo Dirección
                        TextFormField(
                          controller: _direccionController,
                          style: const TextStyle(color: Colors.white),
                          maxLines: 2,
                          decoration: _buildInputDecoration(
                            'Dirección completa',
                            Icons.location_on,
                          ),
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Por favor ingrese la dirección';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 16),
                        
                        // Campos de coordenadas
                        Row(
                          children: [
                            Expanded(
                              child: TextFormField(
                                controller: _latitudController,
                                keyboardType: TextInputType.numberWithOptions(decimal: true, signed: true),
                                style: const TextStyle(color: Colors.white),
                                decoration: _buildInputDecoration(
                                  'Latitud',
                                  Icons.map,
                                ),
                                validator: (value) {
                                  if (value == null || value.isEmpty) {
                                    return 'Requerido';
                                  }
                                  if (double.tryParse(value) == null) {
                                    return 'Número inválido';
                                  }
                                  return null;
                                },
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: TextFormField(
                                controller: _longitudController,
                                keyboardType: TextInputType.numberWithOptions(decimal: true, signed: true),
                                style: const TextStyle(color: Colors.white),
                                decoration: _buildInputDecoration(
                                  'Longitud',
                                  Icons.map,
                                ),
                                validator: (value) {
                                  if (value == null || value.isEmpty) {
                                    return 'Requerido';
                                  }
                                  if (double.tryParse(value) == null) {
                                    return 'Número inválido';
                                  }
                                  return null;
                                },
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 24),
                        
                        // Sección: Información de Contacto
                        Text(
                          'INFORMACIÓN DE CONTACTO',
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.9),
                            fontSize: 14,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 1,
                          ),
                        ),
                        const SizedBox(height: 16),
                        
                        // Campo Email
                        TextFormField(
                          controller: _emailController,
                          keyboardType: TextInputType.emailAddress,
                          style: const TextStyle(color: Colors.white),
                          decoration: _buildInputDecoration(
                            'Email',
                            Icons.email,
                          ),
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Por favor ingrese el email';
                            }
                            if (!value.contains('@')) {
                              return 'Email inválido';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 16),
                        
                        // Campo Teléfono
                        TextFormField(
                          controller: _telefonoController,
                          keyboardType: TextInputType.phone,
                          style: const TextStyle(color: Colors.white),
                          decoration: _buildInputDecoration(
                            'Teléfono',
                            Icons.phone,
                          ),
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Por favor ingrese el teléfono';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 24),
                        
                        // Sección: Materiales Aceptados
                        Text(
                          'MATERIALES ACEPTADOS',
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.9),
                            fontSize: 14,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 1,
                          ),
                        ),
                        const SizedBox(height: 16),
                        
                        // Lista de materiales
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: Colors.white.withValues(alpha: 0.1),
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(
                              color: Colors.white.withValues(alpha: 0.3),
                            ),
                          ),
                          child: Column(
                            children: _materialesDisponibles.map((material) {
                              final isSelected = _materialesSeleccionados.contains(material);
                              return CheckboxListTile(
                                title: Text(
                                  material,
                                  style: const TextStyle(color: Colors.white),
                                ),
                                value: isSelected,
                                onChanged: (bool? value) {
                                  setState(() {
                                    if (value == true) {
                                      _materialesSeleccionados.add(material);
                                    } else {
                                      _materialesSeleccionados.remove(material);
                                    }
                                  });
                                },
                                activeColor: BioWayColors.turquoise,
                                checkColor: Colors.white,
                                side: BorderSide(color: Colors.white.withValues(alpha: 0.5)),
                              );
                            }).toList(),
                          ),
                        ),
                        const SizedBox(height: 24),
                        
                        // Sección: Seguridad
                        Text(
                          'SEGURIDAD',
                          style: TextStyle(
                            color: Colors.white.withValues(alpha: 0.9),
                            fontSize: 14,
                            fontWeight: FontWeight.bold,
                            letterSpacing: 1,
                          ),
                        ),
                        const SizedBox(height: 16),
                        
                        // Campo Contraseña
                        TextFormField(
                          controller: _passwordController,
                          obscureText: _obscurePassword,
                          style: const TextStyle(color: Colors.white),
                          decoration: InputDecoration(
                            labelText: 'Contraseña',
                            labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.8)),
                            prefixIcon: const Icon(Icons.lock, color: Colors.white70),
                            suffixIcon: IconButton(
                              icon: Icon(
                                _obscurePassword ? Icons.visibility : Icons.visibility_off,
                                color: Colors.white70,
                              ),
                              onPressed: () => setState(() => _obscurePassword = !_obscurePassword),
                            ),
                            enabledBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(12),
                              borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.3)),
                            ),
                            focusedBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(12),
                              borderSide: const BorderSide(color: Colors.white, width: 2),
                            ),
                            errorBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(12),
                              borderSide: const BorderSide(color: Colors.red),
                            ),
                            focusedErrorBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(12),
                              borderSide: const BorderSide(color: Colors.red, width: 2),
                            ),
                            filled: true,
                            fillColor: Colors.white.withValues(alpha: 0.1),
                          ),
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Por favor ingrese la contraseña';
                            }
                            if (value.length < 6) {
                              return 'La contraseña debe tener al menos 6 caracteres';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 16),
                        
                        // Campo Confirmar Contraseña
                        TextFormField(
                          controller: _confirmPasswordController,
                          obscureText: _obscureConfirmPassword,
                          style: const TextStyle(color: Colors.white),
                          decoration: InputDecoration(
                            labelText: 'Confirmar contraseña',
                            labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.8)),
                            prefixIcon: const Icon(Icons.lock_outline, color: Colors.white70),
                            suffixIcon: IconButton(
                              icon: Icon(
                                _obscureConfirmPassword ? Icons.visibility : Icons.visibility_off,
                                color: Colors.white70,
                              ),
                              onPressed: () => setState(() => _obscureConfirmPassword = !_obscureConfirmPassword),
                            ),
                            enabledBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(12),
                              borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.3)),
                            ),
                            focusedBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(12),
                              borderSide: const BorderSide(color: Colors.white, width: 2),
                            ),
                            errorBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(12),
                              borderSide: const BorderSide(color: Colors.red),
                            ),
                            focusedErrorBorder: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(12),
                              borderSide: const BorderSide(color: Colors.red, width: 2),
                            ),
                            filled: true,
                            fillColor: Colors.white.withValues(alpha: 0.1),
                          ),
                          validator: (value) {
                            if (value == null || value.isEmpty) {
                              return 'Por favor confirme la contraseña';
                            }
                            if (value != _passwordController.text) {
                              return 'Las contraseñas no coinciden';
                            }
                            return null;
                          },
                        ),
                        const SizedBox(height: 30),
                        
                        // Botón de registro
                        SizedBox(
                          width: double.infinity,
                          height: 56,
                          child: ElevatedButton(
                            onPressed: _isLoading ? null : _handleRegister,
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.teal,
                              foregroundColor: Colors.white,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(12),
                              ),
                              elevation: 3,
                            ),
                            child: _isLoading
                                ? const CircularProgressIndicator(color: Colors.white)
                                : const Text(
                                    'CREAR CENTRO DE ACOPIO',
                                    style: TextStyle(
                                      fontSize: 16,
                                      fontWeight: FontWeight.bold,
                                      letterSpacing: 1,
                                    ),
                                  ),
                          ),
                        ),
                        const SizedBox(height: 20),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
  
  InputDecoration _buildInputDecoration(String label, IconData icon) {
    return InputDecoration(
      labelText: label,
      labelStyle: TextStyle(color: Colors.white.withValues(alpha: 0.8)),
      prefixIcon: Icon(icon, color: Colors.white70),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: BorderSide(color: Colors.white.withValues(alpha: 0.3)),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: Colors.white, width: 2),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: Colors.red),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(12),
        borderSide: const BorderSide(color: Colors.red, width: 2),
      ),
      filled: true,
      fillColor: Colors.white.withValues(alpha: 0.1),
    );
  }
}