import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:camera/camera.dart';
import 'package:image_picker/image_picker.dart';
import 'package:permission_handler/permission_handler.dart';
import '../../services/ai/waste_detection_service.dart';
import '../../utils/colors.dart';

class WasteScannerScreen extends StatefulWidget {
  const WasteScannerScreen({super.key});

  @override
  State<WasteScannerScreen> createState() => _WasteScannerScreenState();
}

class _WasteScannerScreenState extends State<WasteScannerScreen>
    with TickerProviderStateMixin {
  // Servicios
  final WasteDetectionService _detectionService = WasteDetectionService();
  final ImagePicker _imagePicker = ImagePicker();
  
  // Controladores de cámara
  CameraController? _cameraController;
  List<CameraDescription>? _cameras;
  
  // Estados
  bool _isInitializing = true;
  bool _isProcessing = false;
  bool _isRealTimeMode = false;
  WasteDetectionResult? _lastResult;
  
  // Animaciones
  late AnimationController _scanAnimationController;
  late AnimationController _resultAnimationController;
  late Animation<double> _scanAnimation;
  late Animation<double> _resultScaleAnimation;
  late Animation<Offset> _resultSlideAnimation;
  
  @override
  void initState() {
    super.initState();
    _setupAnimations();
    _initializeCamera();
    _initializeDetectionService();
  }
  
  void _setupAnimations() {
    // Animación de escaneo
    _scanAnimationController = AnimationController(
      duration: const Duration(seconds: 2),
      vsync: this,
    )..repeat();
    
    _scanAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _scanAnimationController,
      curve: Curves.linear,
    ));
    
    // Animación de resultados
    _resultAnimationController = AnimationController(
      duration: const Duration(milliseconds: 500),
      vsync: this,
    );
    
    _resultScaleAnimation = Tween<double>(
      begin: 0.8,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _resultAnimationController,
      curve: Curves.elasticOut,
    ));
    
    _resultSlideAnimation = Tween<Offset>(
      begin: const Offset(0, 1),
      end: Offset.zero,
    ).animate(CurvedAnimation(
      parent: _resultAnimationController,
      curve: Curves.easeOutCubic,
    ));
  }
  
  Future<void> _initializeCamera() async {
    try {
      // Verificar permisos
      final status = await Permission.camera.request();
      if (!status.isGranted) {
        _showPermissionError();
        return;
      }
      
      // Obtener cámaras disponibles
      _cameras = await availableCameras();
      if (_cameras == null || _cameras!.isEmpty) {
        _showCameraError();
        return;
      }
      
      // Inicializar con cámara trasera
      await _setupCamera(_cameras!.first);
      
    } catch (e) {
      print('Error inicializando cámara: $e');
      _showCameraError();
    }
  }
  
  Future<void> _setupCamera(CameraDescription camera) async {
    _cameraController = CameraController(
      camera,
      ResolutionPreset.high, // Alta resolución para mejor calidad
      enableAudio: false,
      imageFormatGroup: ImageFormatGroup.jpeg, // JPEG para mejor calidad
    );
    
    try {
      await _cameraController!.initialize();
      if (mounted) {
        setState(() {
          _isInitializing = false;
        });
      }
    } catch (e) {
      print('Error configurando cámara: $e');
      _showCameraError();
    }
  }
  
  Future<void> _initializeDetectionService() async {
    final success = await _detectionService.initialize();
    if (!success && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Error inicializando servicio de detección'),
          backgroundColor: BioWayColors.error,
        ),
      );
    }
  }
  
  Future<void> _captureAndAnalyze() async {
    if (_isProcessing || _cameraController == null) return;
    
    setState(() => _isProcessing = true);
    HapticFeedback.mediumImpact();
    
    try {
      // Configurar flash si es necesario
      if (_cameraController!.value.flashMode != FlashMode.off) {
        await _cameraController!.setFlashMode(FlashMode.off);
      }
      
      // Capturar imagen con calidad alta
      final XFile imageFile = await _cameraController!.takePicture();
      
      // Analizar
      final result = await _detectionService.classifyImage(File(imageFile.path));
      
      // Mostrar resultado
      _showResult(result);
      
    } catch (e) {
      print('Error capturando/analizando: $e');
      _showError('Error al procesar imagen');
    } finally {
      setState(() => _isProcessing = false);
    }
  }
  
  Future<void> _pickFromGallery() async {
    final XFile? image = await _imagePicker.pickImage(
      source: ImageSource.gallery,
      maxWidth: 1024,
      maxHeight: 1024,
      imageQuality: 85,
    );
    
    if (image != null) {
      setState(() => _isProcessing = true);
      
      final result = await _detectionService.classifyImage(File(image.path));
      _showResult(result);
      
      setState(() => _isProcessing = false);
    }
  }
  
  void _showResult(WasteDetectionResult result) {
    if (!result.success || result.primaryClassification == null) {
      _showError('No se pudo identificar el material');
      return;
    }
    
    setState(() {
      _lastResult = result;
    });
    
    _resultAnimationController.forward();
    
    // Vibración de éxito
    HapticFeedback.lightImpact();
  }
  
  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: BioWayColors.error,
      ),
    );
  }
  
  void _showPermissionError() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Permiso de cámara requerido'),
        content: Text('Por favor habilita el acceso a la cámara para escanear materiales.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text('Cancelar'),
          ),
          TextButton(
            onPressed: () {
              Navigator.pop(context);
              openAppSettings();
            },
            child: Text('Abrir configuración'),
          ),
        ],
      ),
    );
  }
  
  void _showCameraError() {
    if (mounted) {
      setState(() {
        _isInitializing = false;
      });
    }
  }
  
  @override
  void dispose() {
    _cameraController?.dispose();
    _scanAnimationController.dispose();
    _resultAnimationController.dispose();
    _detectionService.dispose();
    super.dispose();
  }
  
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Stack(
        children: [
          // Vista de cámara con aspect ratio correcto
          if (!_isInitializing && _cameraController != null)
            Positioned.fill(
              child: AspectRatio(
                aspectRatio: _cameraController!.value.aspectRatio,
                child: CameraPreview(_cameraController!),
              ),
            )
          else
            Center(
              child: CircularProgressIndicator(
                color: BioWayColors.primaryGreen,
              ),
            ),
          
          // UI superpuesta
          SafeArea(
            child: Column(
              children: [
                // Header
                _buildHeader(),
                
                // Área de escaneo
                Expanded(
                  child: Stack(
                    alignment: Alignment.center,
                    children: [
                      // Marco de escaneo
                      _buildScanFrame(),
                      
                      // Indicador de procesamiento
                      if (_isProcessing)
                        _buildProcessingIndicator(),
                    ],
                  ),
                ),
                
                // Resultados
                if (_lastResult != null)
                  _buildResultCard(),
                
                // Controles
                _buildControls(),
              ],
            ),
          ),
        ],
      ),
    );
  }
  
  Widget _buildHeader() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            Colors.black.withValues(alpha: 0.8),
            Colors.black.withValues(alpha: 0.0),
          ],
        ),
      ),
      child: Row(
        children: [
          IconButton(
            onPressed: () => Navigator.pop(context),
            icon: Icon(Icons.arrow_back_ios, color: Colors.white),
          ),
          Expanded(
            child: Column(
              children: [
                Text(
                  'Escáner Inteligente',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                Text(
                  'Detecta: Plástico, Cartón, Vidrio, Metal y más',
                  style: TextStyle(
                    color: Colors.white70,
                    fontSize: 12,
                  ),
                ),
              ],
            ),
          ),
          // Botón de configuración de cámara
          IconButton(
            onPressed: () => _toggleCameraSettings(),
            icon: Icon(Icons.settings, color: Colors.white),
          ),
          IconButton(
            onPressed: () => _showInfoDialog(),
            icon: Icon(Icons.info_outline, color: Colors.white),
          ),
        ],
      ),
    );
  }
  
  Widget _buildScanFrame() {
    return Container(
      width: 280,
      height: 280,
      child: Stack(
        children: [
          // Esquinas del marco
          ...List.generate(4, (index) {
            final isTop = index < 2;
            final isLeft = index % 2 == 0;
            
            return Positioned(
              top: isTop ? 0 : null,
              bottom: !isTop ? 0 : null,
              left: isLeft ? 0 : null,
              right: !isLeft ? 0 : null,
              child: Container(
                width: 60,
                height: 60,
                decoration: BoxDecoration(
                  border: Border(
                    top: isTop ? BorderSide(color: BioWayColors.primaryGreen, width: 3) : BorderSide.none,
                    bottom: !isTop ? BorderSide(color: BioWayColors.primaryGreen, width: 3) : BorderSide.none,
                    left: isLeft ? BorderSide(color: BioWayColors.primaryGreen, width: 3) : BorderSide.none,
                    right: !isLeft ? BorderSide(color: BioWayColors.primaryGreen, width: 3) : BorderSide.none,
                  ),
                ),
              ),
            );
          }),
          
          // Línea de escaneo animada
          AnimatedBuilder(
            animation: _scanAnimation,
            builder: (context, child) {
              return Positioned(
                top: _scanAnimation.value * 280,
                left: 0,
                right: 0,
                child: Container(
                  height: 2,
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [
                        Colors.transparent,
                        BioWayColors.primaryGreen,
                        Colors.transparent,
                      ],
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: BioWayColors.primaryGreen.withValues(alpha: 0.5),
                        blurRadius: 10,
                        spreadRadius: 2,
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        ],
      ),
    );
  }
  
  Widget _buildProcessingIndicator() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.8),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          CircularProgressIndicator(
            color: BioWayColors.primaryGreen,
          ),
          const SizedBox(height: 16),
          Text(
            'Analizando material...',
            style: TextStyle(
              color: Colors.white,
              fontSize: 16,
            ),
          ),
        ],
      ),
    );
  }
  
  Widget _buildResultCard() {
    final primary = _lastResult!.primaryClassification!;
    
    return AnimatedBuilder(
      animation: _resultAnimationController,
      builder: (context, child) {
        return SlideTransition(
          position: _resultSlideAnimation,
          child: ScaleTransition(
            scale: _resultScaleAnimation,
            child: Container(
              margin: const EdgeInsets.all(16),
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: Colors.white,
                borderRadius: BorderRadius.circular(20),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.2),
                    blurRadius: 10,
                    offset: Offset(0, 5),
                  ),
                ],
              ),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  // Icono y nombre
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.all(12),
                        decoration: BoxDecoration(
                          color: Color(primary.category.color).withValues(alpha: 0.2),
                          shape: BoxShape.circle,
                        ),
                        child: Icon(
                          _getIconForCategory(primary.category.id),
                          color: Color(primary.category.color),
                          size: 32,
                        ),
                      ),
                      const SizedBox(width: 16),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              primary.category.name,
                              style: TextStyle(
                                fontSize: 20,
                                fontWeight: FontWeight.bold,
                                color: BioWayColors.textDark,
                              ),
                            ),
                            Text(
                              'Código: ${primary.category.code}',
                              style: TextStyle(
                                fontSize: 14,
                                color: BioWayColors.textGrey,
                              ),
                            ),
                          ],
                        ),
                      ),
                      // Confianza
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                        decoration: BoxDecoration(
                          color: _getConfidenceColor(primary.confidence),
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: Text(
                          '${(primary.confidence * 100).toInt()}%',
                          style: TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                  
                  const SizedBox(height: 16),
                  
                  // Instrucciones
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: BioWayColors.backgroundGrey,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Row(
                      children: [
                        Icon(
                          Icons.info_outline,
                          color: BioWayColors.primaryGreen,
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: Text(
                            primary.category.recyclingInstructions,
                            style: TextStyle(
                              fontSize: 14,
                              color: BioWayColors.textDark,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                  
                  const SizedBox(height: 12),
                  
                  // Valor
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'Valor estimado:',
                        style: TextStyle(
                          color: BioWayColors.textGrey,
                        ),
                      ),
                      Text(
                        '${primary.category.value} puntos/kg',
                        style: TextStyle(
                          color: BioWayColors.primaryGreen,
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
  
  Widget _buildControls() {
    return Container(
      padding: const EdgeInsets.all(24),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.bottomCenter,
          end: Alignment.topCenter,
          colors: [
            Colors.black.withValues(alpha: 0.8),
            Colors.black.withValues(alpha: 0.0),
          ],
        ),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          // Galería
          Container(
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.2),
              shape: BoxShape.circle,
            ),
            child: IconButton(
              onPressed: _pickFromGallery,
              icon: Icon(Icons.photo_library, color: Colors.white),
              iconSize: 28,
            ),
          ),
          
          // Botón de captura
          GestureDetector(
            onTap: _isProcessing ? null : _captureAndAnalyze,
            child: Container(
              width: 80,
              height: 80,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                border: Border.all(
                  color: Colors.white,
                  width: 4,
                ),
                color: _isProcessing 
                    ? Colors.grey 
                    : BioWayColors.primaryGreen,
              ),
              child: Icon(
                Icons.camera,
                color: Colors.white,
                size: 40,
              ),
            ),
          ),
          
          // Modo tiempo real
          Container(
            decoration: BoxDecoration(
              color: _isRealTimeMode 
                  ? BioWayColors.primaryGreen.withValues(alpha: 0.3)
                  : Colors.white.withValues(alpha: 0.2),
              shape: BoxShape.circle,
            ),
            child: IconButton(
              onPressed: () {
                setState(() {
                  _isRealTimeMode = !_isRealTimeMode;
                });
                HapticFeedback.selectionClick();
              },
              icon: Icon(
                Icons.videocam,
                color: _isRealTimeMode ? BioWayColors.primaryGreen : Colors.white,
              ),
              iconSize: 28,
            ),
          ),
        ],
      ),
    );
  }
  
  IconData _getIconForCategory(String categoryId) {
    switch (categoryId) {
      case 'PET':
      case 'LDPE':
        return Icons.local_drink;
      case 'CARDBOARD':
        return Icons.inventory_2;
      case 'GLASS':
        return Icons.wine_bar;
      case 'ALU':
      case 'METAL':
        return Icons.settings_input_component;
      case 'PAPER':
        return Icons.description;
      case 'ORGANIC':
        return Icons.eco;
      case 'PS':
        return Icons.takeout_dining;
      case 'EWASTE':
        return Icons.computer;
      case 'BATTERY':
        return Icons.battery_full;
      case 'TEXTILE':
        return Icons.checkroom;
      default:
        return Icons.delete;
    }
  }
  
  Color _getConfidenceColor(double confidence) {
    if (confidence >= 0.8) return Colors.green;
    if (confidence >= 0.6) return Colors.orange;
    return Colors.red;
  }
  
  void _showInfoDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Cómo usar el escáner'),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Instrucciones:', style: TextStyle(fontWeight: FontWeight.bold)),
              SizedBox(height: 8),
              _buildInfoItem(Icons.camera, 'Apunta la cámara al material'),
              _buildInfoItem(Icons.center_focus_strong, 'Centra el objeto en el marco'),
              _buildInfoItem(Icons.lightbulb, 'Asegúrate de tener buena iluminación'),
              _buildInfoItem(Icons.touch_app, 'Toca el botón para escanear'),
              SizedBox(height: 16),
              Text('Materiales detectables:', style: TextStyle(fontWeight: FontWeight.bold)),
              SizedBox(height: 8),
              _buildInfoItem(Icons.local_drink, 'Botellas PET y plásticos'),
              _buildInfoItem(Icons.inventory_2, 'Cartón y cajas'),
              _buildInfoItem(Icons.wine_bar, 'Vidrio y cristal'),
              _buildInfoItem(Icons.settings_input_component, 'Metales y aluminio'),
              _buildInfoItem(Icons.description, 'Papel y documentos'),
              _buildInfoItem(Icons.eco, 'Residuos orgánicos'),
              _buildInfoItem(Icons.computer, 'Electrónicos'),
              _buildInfoItem(Icons.checkroom, 'Textiles y ropa'),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text('Entendido'),
          ),
        ],
      ),
    );
  }
  
  Widget _buildInfoItem(IconData icon, String text) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        children: [
          Icon(icon, size: 20, color: BioWayColors.primaryGreen),
          const SizedBox(width: 12),
          Expanded(
            child: Text(text, style: TextStyle(fontSize: 14)),
          ),
        ],
      ),
    );
  }
  
  void _toggleCameraSettings() {
    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) => Container(
        padding: EdgeInsets.all(20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(
              'Configuración de Cámara',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            SizedBox(height: 20),
            ListTile(
              leading: Icon(Icons.hd, color: BioWayColors.primaryGreen),
              title: Text('Calidad Alta'),
              subtitle: Text('Mejor detección, mayor uso de recursos'),
              trailing: Radio<bool>(
                value: true,
                groupValue: true,
                onChanged: (value) {},
                activeColor: BioWayColors.primaryGreen,
              ),
            ),
            ListTile(
              leading: Icon(Icons.flash_on),
              title: Text('Flash automático'),
              subtitle: Text('Activa el flash en ambientes oscuros'),
              trailing: Switch(
                value: false,
                onChanged: (value) {
                  // Implementar control de flash
                },
                activeColor: BioWayColors.primaryGreen,
              ),
            ),
            ListTile(
              leading: Icon(Icons.grid_on),
              title: Text('Mostrar cuadrícula'),
              subtitle: Text('Ayuda a centrar objetos'),
              trailing: Switch(
                value: false,
                onChanged: (value) {
                  // Implementar cuadrícula
                },
                activeColor: BioWayColors.primaryGreen,
              ),
            ),
            SizedBox(height: 20),
          ],
        ),
      ),
    );
  }
}