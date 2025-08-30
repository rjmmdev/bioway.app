import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:camera/camera.dart';
import 'package:image_picker/image_picker.dart';
import '../../models/detected_material.dart';
import '../../services/waste_detection_service.dart';
import '../../utils/colors.dart';

class WasteScannerScreen extends StatefulWidget {
  const WasteScannerScreen({Key? key}) : super(key: key);

  @override
  State<WasteScannerScreen> createState() => _WasteScannerScreenState();
}

class _WasteScannerScreenState extends State<WasteScannerScreen> 
    with TickerProviderStateMixin {
  CameraController? _cameraController;
  final WasteDetectionService _detectionService = WasteDetectionService();
  final ImagePicker _imagePicker = ImagePicker();
  
  bool _isProcessing = false;
  bool _isCameraInitialized = false;
  bool _isFlashOn = false;
  DetectedMaterial? _detectedMaterial;
  File? _capturedImage;
  
  late AnimationController _scanAnimationController;
  late Animation<double> _scanAnimation;
  
  // Para el indicador de enfoque
  late AnimationController _focusAnimationController;
  late Animation<double> _focusAnimation;
  Offset? _focusPoint;
  bool _showFocusCircle = false;

  @override
  void initState() {
    super.initState();
    _initializeCamera();
    _initializeAnimation();
    // Inicialización del servicio se hace cuando sea necesario
  }

  void _initializeAnimation() {
    _scanAnimationController = AnimationController(
      duration: const Duration(seconds: 2),
      vsync: this,
    );
    
    _scanAnimation = Tween<double>(
      begin: 0.0,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _scanAnimationController,
      curve: Curves.easeInOut,
    ));
    
    _scanAnimationController.repeat(reverse: true);
    
    // Animación para el indicador de enfoque
    _focusAnimationController = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );
    
    _focusAnimation = Tween<double>(
      begin: 1.5,
      end: 1.0,
    ).animate(CurvedAnimation(
      parent: _focusAnimationController,
      curve: Curves.easeOut,
    ));
  }

  Future<void> _initializeCamera() async {
    try {
      final cameras = await availableCameras();
      if (cameras.isEmpty) return;

      _cameraController = CameraController(
        cameras.first,
        ResolutionPreset.high,
        enableAudio: false,
        imageFormatGroup: ImageFormatGroup.jpeg,
      );

      await _cameraController!.initialize();
      
      // Configurar el flash como apagado por defecto
      await _cameraController!.setFlashMode(FlashMode.off);
      
      if (mounted) {
        setState(() {
          _isCameraInitialized = true;
        });
      }
    } catch (e) {
      print('Error al inicializar cámara: $e');
    }
  }

  Future<void> _captureAndAnalyze() async {
    if (_cameraController == null || !_cameraController!.value.isInitialized || _isProcessing) {
      return;
    }

    print('Iniciando captura y análisis...');
    setState(() {
      _isProcessing = true;
    });

    try {
      // Apagar flash si está encendido antes de capturar
      if (_isFlashOn) {
        await _cameraController!.setFlashMode(FlashMode.off);
        setState(() {
          _isFlashOn = false;
        });
      }
      
      // Capturar la imagen SIN detener la cámara
      print('Capturando imagen...');
      final XFile photo = await _cameraController!.takePicture();
      final File imageFile = File(photo.path);
      print('Imagen capturada: ${imageFile.path}');
      
      setState(() {
        _capturedImage = imageFile;
      });

      // Solo pausar el preview, no detener la cámara
      await _cameraController!.pausePreview();
      
      // Inicializar el servicio si no está inicializado
      print('Inicializando servicio de detección...');
      await _detectionService.initialize();
      
      // Procesar la imagen
      print('Procesando imagen con IA...');
      final result = await _detectionService.detectMaterial(imageFile);
      print('Resultado obtenido: ${result?.category ?? "null"}');
      
      if (mounted) {
        setState(() {
          _detectedMaterial = result;
          _isProcessing = false;
        });
      }
    } catch (e) {
      print('Error en _captureAndAnalyze: $e');
      if (mounted) {
        setState(() {
          _isProcessing = false;
          _detectedMaterial = null;
        });
      }
      
      // Reanudar preview si hay error
      try {
        await _cameraController?.resumePreview();
      } catch (_) {}
      
      _showError('Error al procesar imagen: ${e.toString()}');
    }
  }

  Future<void> _pickFromGallery() async {
    if (_isProcessing) return;
    
    print('Seleccionando imagen de galería...');
    
    try {
      final XFile? image = await _imagePicker.pickImage(
        source: ImageSource.gallery,
        imageQuality: 85, // Reducir calidad para mejor rendimiento
      );

      if (image != null) {
        print('Imagen seleccionada: ${image.path}');
        setState(() {
          _isProcessing = true;
          _capturedImage = File(image.path);
        });
        
        // Pausar preview mientras se procesa
        if (_cameraController != null && _cameraController!.value.isInitialized) {
          await _cameraController!.pausePreview();
        }

        // Inicializar el servicio si no está inicializado
        print('Inicializando servicio de detección...');
        await _detectionService.initialize();
        
        print('Procesando imagen con IA...');
        final result = await _detectionService.detectMaterial(File(image.path));
        print('Resultado obtenido: ${result?.category ?? "null"}');
        
        if (mounted) {
          setState(() {
            _detectedMaterial = result;
            _isProcessing = false;
          });
        }
      }
    } catch (e) {
      print('Error en _pickFromGallery: $e');
      if (mounted) {
        setState(() {
          _isProcessing = false;
        });
      }
      
      // Reanudar preview si hay error
      try {
        await _cameraController?.resumePreview();
      } catch (_) {}
      
      _showError('Error al procesar imagen: ${e.toString()}');
    }
  }

  void _resetScanner() async {
    print('Reseteando escáner...');
    setState(() {
      _detectedMaterial = null;
      _capturedImage = null;
      _isProcessing = false;
    });
    
    // Reanudar el preview de la cámara
    try {
      if (_cameraController != null && _cameraController!.value.isInitialized) {
        await _cameraController!.resumePreview();
        print('Preview reanudado');
      } else {
        print('Reinicializando cámara...');
        await _initializeCamera();
      }
    } catch (e) {
      print('Error al resetear: $e');
      // Si hay error, reinicializar la cámara
      await _initializeCamera();
    }
  }

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: Colors.red,
      ),
    );
  }
  
  Future<void> _toggleFlash() async {
    if (_cameraController == null || !_cameraController!.value.isInitialized) {
      return;
    }
    
    try {
      if (_isFlashOn) {
        await _cameraController!.setFlashMode(FlashMode.off);
      } else {
        await _cameraController!.setFlashMode(FlashMode.torch);
      }
      setState(() {
        _isFlashOn = !_isFlashOn;
      });
    } catch (e) {
      print('Error al cambiar flash: $e');
    }
  }
  
  Future<void> _onTapToFocus(TapDownDetails details, BoxConstraints constraints) async {
    if (_cameraController == null || !_cameraController!.value.isInitialized) {
      return;
    }
    
    final offset = Offset(
      details.localPosition.dx / constraints.maxWidth,
      details.localPosition.dy / constraints.maxHeight,
    );
    
    try {
      await _cameraController!.setExposurePoint(offset);
      await _cameraController!.setFocusPoint(offset);
      
      setState(() {
        _focusPoint = details.localPosition;
        _showFocusCircle = true;
      });
      
      _focusAnimationController.forward().then((_) {
        Future.delayed(const Duration(milliseconds: 800), () {
          if (mounted) {
            setState(() {
              _showFocusCircle = false;
            });
            _focusAnimationController.reset();
          }
        });
      });
    } catch (e) {
      print('Error al enfocar: $e');
    }
  }

  @override
  void dispose() {
    // Detener animaciones primero
    _scanAnimationController.stop();
    _focusAnimationController.stop();
    
    // Liberar cámara
    _cameraController?.dispose();
    
    // Liberar controladores de animación
    _scanAnimationController.dispose();
    _focusAnimationController.dispose();
    
    // No llamar dispose del servicio porque es un singleton
    // _detectionService.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      extendBodyBehindAppBar: true,
      appBar: _detectedMaterial == null ? null : AppBar(
        title: Row(
          children: [
            SvgPicture.asset(
              'assets/logos/bioway_logo.svg',
              height: 24,
              width: 24,
              colorFilter: ColorFilter.mode(
                Colors.greenAccent,
                BlendMode.srcIn,
              ),
            ),
            SizedBox(width: 8),
            Text(
              'Análisis IA Completado',
              style: TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.w600,
              ),
            ),
          ],
        ),
        centerTitle: false,
        backgroundColor: Colors.black.withOpacity(0.5),
        foregroundColor: Colors.white,
        elevation: 0,
        flexibleSpace: Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: [
                Colors.black.withOpacity(0.8),
                Colors.transparent,
              ],
            ),
          ),
        ),
        leading: Container(
          margin: EdgeInsets.all(8),
          decoration: BoxDecoration(
            color: Colors.white.withOpacity(0.1),
            shape: BoxShape.circle,
            border: Border.all(
              color: Colors.white.withOpacity(0.2),
              width: 1,
            ),
          ),
          child: IconButton(
            icon: Icon(Icons.arrow_back_ios_new, size: 20),
            onPressed: () => Navigator.pop(context),
          ),
        ),
      ),
      body: Stack(
        fit: StackFit.expand,
        children: [
          if (_detectedMaterial == null) ...[
            // Vista de cámara con GestureDetector para enfoque
            if (_isCameraInitialized && _cameraController != null)
              Positioned.fill(
                child: LayoutBuilder(
                  builder: (context, constraints) {
                    final size = MediaQuery.of(context).size;
                    final cameraAspectRatio = _cameraController!.value.aspectRatio;
                    
                    // Calcular el tamaño para llenar toda la pantalla
                    double scale = 1.0;
                    if (size.aspectRatio > cameraAspectRatio) {
                      scale = size.width / (size.height / cameraAspectRatio);
                    } else {
                      scale = size.height / (size.width * cameraAspectRatio);
                    }
                    
                    return ClipRect(
                      child: Transform.scale(
                        scale: scale,
                        child: Center(
                          child: GestureDetector(
                            onTapDown: (details) => _onTapToFocus(details, constraints),
                            child: Stack(
                              children: [
                                CameraPreview(_cameraController!),
                                // Indicador de enfoque
                                if (_showFocusCircle && _focusPoint != null)
                                  Positioned(
                                    left: _focusPoint!.dx - 40,
                                    top: _focusPoint!.dy - 40,
                                    child: AnimatedBuilder(
                                      animation: _focusAnimation,
                                      builder: (context, child) {
                                        return Transform.scale(
                                          scale: _focusAnimation.value,
                                          child: Container(
                                            width: 80,
                                            height: 80,
                                            decoration: BoxDecoration(
                                              shape: BoxShape.circle,
                                              border: Border.all(
                                                color: Colors.yellow,
                                                width: 2,
                                              ),
                                            ),
                                            child: Center(
                                              child: Container(
                                                width: 5,
                                                height: 5,
                                                decoration: BoxDecoration(
                                                  shape: BoxShape.circle,
                                                  color: Colors.yellow,
                                                ),
                                              ),
                                            ),
                                          ),
                                        );
                                      },
                                    ),
                                  ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    );
                  },
                ),
              )
            else
              const Center(
                child: CircularProgressIndicator(
                  color: BioWayColors.primary,
                ),
              ),
            
            // Overlay de escaneo mejorado
            if (_isCameraInitialized)
              Positioned.fill(
                child: IgnorePointer(
                  child: Container(
                    decoration: BoxDecoration(
                      gradient: RadialGradient(
                        center: Alignment.center,
                        radius: 0.8,
                        colors: [
                          Colors.transparent,
                          Colors.black.withOpacity(0.4),
                        ],
                      ),
                    ),
                    child: Center(
                      child: AnimatedBuilder(
                        animation: _scanAnimation,
                        builder: (context, child) {
                          return Container(
                            width: 280,
                            height: 280,
                            child: Stack(
                              children: [
                                // Marco con esquinas redondeadas
                                CustomPaint(
                                  size: Size(280, 280),
                                  painter: ScannerFramePainter(
                                    color: BioWayColors.primary,
                                    strokeWidth: 3,
                                    cornerRadius: 30,
                                    animationValue: _scanAnimation.value,
                                  ),
                                ),
                                // Línea de escaneo horizontal
                                Positioned(
                                  top: _scanAnimation.value * 280,
                                  left: 20,
                                  right: 20,
                                  child: Container(
                                    height: 2,
                                    decoration: BoxDecoration(
                                      gradient: LinearGradient(
                                        colors: [
                                          Colors.transparent,
                                          BioWayColors.primary,
                                          BioWayColors.primary,
                                          Colors.transparent,
                                        ],
                                      ),
                                      boxShadow: [
                                        BoxShadow(
                                          color: BioWayColors.primary.withOpacity(0.5),
                                          blurRadius: 8,
                                          spreadRadius: 2,
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                                // Texto instructivo
                                Positioned(
                                  bottom: -40,
                                  left: 0,
                                  right: 0,
                                  child: Text(
                                    'Centra el material aquí',
                                    textAlign: TextAlign.center,
                                    style: TextStyle(
                                      color: Colors.white,
                                      fontSize: 16,
                                      fontWeight: FontWeight.w500,
                                      shadows: [
                                        Shadow(
                                          blurRadius: 10,
                                          color: Colors.black54,
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                              ],
                            ),
                          );
                        },
                      ),
                    ),
                  ),
                ),
              ),
          ] else ...[
            // Vista de resultados
            _buildResultsView(),
          ],
          
          // Controles superiores (flash y más opciones)
          if (_detectedMaterial == null && _isCameraInitialized && !_isProcessing)
            Positioned(
              top: MediaQuery.of(context).padding.top + 10,
              left: 20,
              right: 20,
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  // Botón de cerrar
                  Container(
                    decoration: BoxDecoration(
                      color: Colors.black.withOpacity(0.5),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: IconButton(
                      icon: Icon(
                        Icons.close,
                        color: Colors.white,
                        size: 24,
                      ),
                      onPressed: () => Navigator.pop(context),
                    ),
                  ),
                  // Botón de flash
                  Container(
                    decoration: BoxDecoration(
                      color: Colors.black.withOpacity(0.5),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: IconButton(
                      icon: Icon(
                        _isFlashOn ? Icons.flash_on : Icons.flash_off,
                        color: _isFlashOn ? Colors.yellow : Colors.white,
                        size: 24,
                      ),
                      onPressed: _toggleFlash,
                    ),
                  ),
                ],
              ),
            ),
          
          // Overlay de procesamiento
          if (_isProcessing)
            Container(
              color: Colors.black87,
              child: Center(
                child: Container(
                  padding: const EdgeInsets.all(30),
                  margin: const EdgeInsets.symmetric(horizontal: 40),
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(15),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black26,
                        blurRadius: 10,
                        spreadRadius: 2,
                      ),
                    ],
                  ),
                  child: Column(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Stack(
                        alignment: Alignment.center,
                        children: [
                          const CircularProgressIndicator(
                            color: BioWayColors.primary,
                            strokeWidth: 3,
                          ),
                          Icon(
                            Icons.auto_awesome,
                            color: BioWayColors.primary.withOpacity(0.6),
                            size: 24,
                          ),
                        ],
                      ),
                      const SizedBox(height: 20),
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          SvgPicture.asset(
                            'assets/logos/bioway_logo.svg',
                            height: 20,
                            width: 20,
                            colorFilter: ColorFilter.mode(
                              BioWayColors.primary,
                              BlendMode.srcIn,
                            ),
                          ),
                          SizedBox(width: 8),
                          const Text(
                            'IA Analizando Material',
                            style: TextStyle(
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                              color: BioWayColors.primary,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 10),
                      const Text(
                        'Procesando con visión computacional',
                        style: TextStyle(
                          fontSize: 14,
                          color: Colors.grey,
                        ),
                      ),
                      const SizedBox(height: 5),
                      const Text(
                        'Red neuronal identificando patrones...',
                        style: TextStyle(
                          fontSize: 12,
                          color: Colors.grey,
                          fontStyle: FontStyle.italic,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
        ],
      ),
      bottomNavigationBar: _buildBottomBar(),
    );
  }

  List<Widget> _buildCorners() {
    return [
      // Esquina superior izquierda
      Positioned(
        top: 0,
        left: 0,
        child: Container(
          width: 30,
          height: 3,
          color: BioWayColors.primary,
        ),
      ),
      Positioned(
        top: 0,
        left: 0,
        child: Container(
          width: 3,
          height: 30,
          color: BioWayColors.primary,
        ),
      ),
      // Esquina superior derecha
      Positioned(
        top: 0,
        right: 0,
        child: Container(
          width: 30,
          height: 3,
          color: BioWayColors.primary,
        ),
      ),
      Positioned(
        top: 0,
        right: 0,
        child: Container(
          width: 3,
          height: 30,
          color: BioWayColors.primary,
        ),
      ),
      // Esquina inferior izquierda
      Positioned(
        bottom: 0,
        left: 0,
        child: Container(
          width: 30,
          height: 3,
          color: BioWayColors.primary,
        ),
      ),
      Positioned(
        bottom: 0,
        left: 0,
        child: Container(
          width: 3,
          height: 30,
          color: BioWayColors.primary,
        ),
      ),
      // Esquina inferior derecha
      Positioned(
        bottom: 0,
        right: 0,
        child: Container(
          width: 30,
          height: 3,
          color: BioWayColors.primary,
        ),
      ),
      Positioned(
        bottom: 0,
        right: 0,
        child: Container(
          width: 3,
          height: 30,
          color: BioWayColors.primary,
        ),
      ),
    ];
  }

  Widget _buildResultsView() {
    if (_detectedMaterial == null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(
              Icons.search_off,
              size: 80,
              color: Colors.white38,
            ),
            SizedBox(height: 20),
            Text(
              'No se detectó ningún material',
              style: TextStyle(
                color: Colors.white70,
                fontSize: 18,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
      );
    }

    final confidenceColor = _detectedMaterial!.confidence > 0.7
        ? Colors.green
        : _detectedMaterial!.confidence > 0.4
            ? Colors.orange
            : Colors.red;
    
    // Determinar ícono según categoría
    IconData categoryIcon = Icons.recycling;
    Color categoryColor = BioWayColors.primary;
    
    if (_detectedMaterial!.category.toLowerCase().contains('pet') ||
        _detectedMaterial!.category.toLowerCase().contains('plástico')) {
      categoryIcon = Icons.local_drink;
      categoryColor = Colors.blue;
    } else if (_detectedMaterial!.category.toLowerCase().contains('tetra pak')) {
      categoryIcon = Icons.breakfast_dining;
      categoryColor = Colors.orange;
    } else if (_detectedMaterial!.category.toLowerCase().contains('cartón') ||
               _detectedMaterial!.category.toLowerCase().contains('papel')) {
      categoryIcon = Icons.inventory_2;
      categoryColor = Colors.brown;
    } else if (_detectedMaterial!.category.toLowerCase().contains('vidrio')) {
      categoryIcon = Icons.wine_bar;
      categoryColor = Colors.teal;
    } else if (_detectedMaterial!.category.toLowerCase().contains('metal') ||
               _detectedMaterial!.category.toLowerCase().contains('aluminio')) {
      categoryIcon = Icons.scatter_plot;
      categoryColor = Colors.blueGrey;
    } else if (_detectedMaterial!.category.toLowerCase().contains('orgánico')) {
      categoryIcon = Icons.eco;
      categoryColor = Colors.green;
    }

    return Container(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            Colors.black,
            Colors.grey[900]!,
          ],
        ),
      ),
      child: SingleChildScrollView(
        child: Column(
          children: [
            // Imagen capturada con overlay
            if (_capturedImage != null)
              Stack(
                children: [
                  Container(
                    height: 300,
                    width: double.infinity,
                    decoration: BoxDecoration(
                      image: DecorationImage(
                        image: FileImage(_capturedImage!),
                        fit: BoxFit.cover,
                      ),
                    ),
                  ),
                  // Gradient overlay
                  Container(
                    height: 300,
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          Colors.transparent,
                          Colors.black.withOpacity(0.7),
                        ],
                      ),
                    ),
                  ),
                  // Badge de categoría flotante
                  Positioned(
                    bottom: 20,
                    left: 20,
                    right: 20,
                    child: Container(
                      padding: EdgeInsets.symmetric(horizontal: 20, vertical: 15),
                      decoration: BoxDecoration(
                        color: Colors.white,
                        borderRadius: BorderRadius.circular(15),
                        boxShadow: [
                          BoxShadow(
                            color: Colors.black26,
                            blurRadius: 10,
                            offset: Offset(0, 5),
                          ),
                        ],
                      ),
                      child: Row(
                        children: [
                          Container(
                            padding: EdgeInsets.all(10),
                            decoration: BoxDecoration(
                              color: categoryColor.withOpacity(0.1),
                              shape: BoxShape.circle,
                            ),
                            child: Icon(
                              categoryIcon,
                              color: categoryColor,
                              size: 30,
                            ),
                          ),
                          SizedBox(width: 15),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Text(
                                  _detectedMaterial!.category,
                                  style: TextStyle(
                                    fontSize: 20,
                                    fontWeight: FontWeight.bold,
                                    color: Colors.black87,
                                  ),
                                ),
                                SizedBox(height: 3),
                                Text(
                                  _detectedMaterial!.name,
                                  style: TextStyle(
                                    fontSize: 14,
                                    color: Colors.grey[600],
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
          
            
            // Contenido principal
            Container(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  // Tarjeta de confianza
                  Container(
                    padding: EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        colors: [
                          confidenceColor.withOpacity(0.2),
                          confidenceColor.withOpacity(0.05),
                        ],
                      ),
                      borderRadius: BorderRadius.circular(15),
                      border: Border.all(
                        color: confidenceColor.withOpacity(0.3),
                        width: 1,
                      ),
                    ),
                    child: Column(
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Text(
                              'Nivel de Confianza',
                              style: TextStyle(
                                color: Colors.white70,
                                fontSize: 14,
                              ),
                            ),
                            Container(
                              padding: EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                              decoration: BoxDecoration(
                                color: confidenceColor.withOpacity(0.2),
                                borderRadius: BorderRadius.circular(20),
                                border: Border.all(color: confidenceColor),
                              ),
                              child: Text(
                                '${(_detectedMaterial!.confidence * 100).toStringAsFixed(0)}%',
                                style: TextStyle(
                                  color: confidenceColor,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 16,
                                ),
                              ),
                            ),
                          ],
                        ),
                        SizedBox(height: 15),
                        ClipRRect(
                          borderRadius: BorderRadius.circular(10),
                          child: LinearProgressIndicator(
                            value: _detectedMaterial!.confidence,
                            backgroundColor: Colors.white12,
                            valueColor: AlwaysStoppedAnimation<Color>(confidenceColor),
                            minHeight: 10,
                          ),
                        ),
                      ],
                    ),
                  ),
                  SizedBox(height: 15),
                  
                  // Información en grid
                  Row(
                    children: [
                      Expanded(
                        child: _buildInfoCard(
                          icon: Icons.eco,
                          title: 'Ahorro Potencial',
                          value: _calculateCO2Saved(),
                          color: Colors.green,
                        ),
                      ),
                      SizedBox(width: 15),
                      Expanded(
                        child: _buildInfoCard(
                          icon: Icons.stars,
                          title: 'Puntos/kg',
                          value: '${_detectedMaterial!.pointsPerKg}',
                          color: Colors.amber,
                        ),
                      ),
                    ],
                  ),
                  SizedBox(height: 15),
                  
                  // Instrucciones
                  Container(
                    padding: EdgeInsets.all(20),
                    decoration: BoxDecoration(
                      color: Colors.white10,
                      borderRadius: BorderRadius.circular(15),
                      border: Border.all(
                        color: Colors.white24,
                        width: 1,
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Icon(
                              Icons.lightbulb_outline,
                              color: Colors.yellow[700],
                              size: 24,
                            ),
                            SizedBox(width: 10),
                            Text(
                              'Instrucciones de Reciclaje',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                                color: Colors.white,
                              ),
                            ),
                          ],
                        ),
                        SizedBox(height: 15),
                        Text(
                          _detectedMaterial!.instructions,
                          style: TextStyle(
                            fontSize: 14,
                            color: Colors.white70,
                            height: 1.5,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
  
  Widget _buildInfoCard({
    required IconData icon,
    required String title,
    required String value,
    required Color color,
  }) {
    // Añadir animación especial para CO2
    final isCO2Card = title.contains('CO₂');
    
    return Container(
      padding: EdgeInsets.all(15),
      decoration: BoxDecoration(
        gradient: isCO2Card 
          ? LinearGradient(
              colors: [
                Colors.green.withOpacity(0.2),
                Colors.green.withOpacity(0.05),
              ],
              begin: Alignment.topLeft,
              end: Alignment.bottomRight,
            )
          : null,
        color: !isCO2Card ? color.withOpacity(0.1) : null,
        borderRadius: BorderRadius.circular(15),
        border: Border.all(
          color: color.withOpacity(0.3),
          width: isCO2Card ? 2 : 1,
        ),
      ),
      child: Column(
        children: [
          Icon(
            icon,
            color: color,
            size: isCO2Card ? 32 : 28,
          ),
          SizedBox(height: 8),
          Text(
            title,
            style: TextStyle(
              color: Colors.white60,
              fontSize: 12,
            ),
          ),
          SizedBox(height: 4),
          Text(
            value,
            style: TextStyle(
              color: isCO2Card ? Colors.greenAccent : Colors.white,
              fontSize: isCO2Card ? 20 : 18,
              fontWeight: FontWeight.bold,
            ),
          ),
          if (isCO2Card) ...[
            SizedBox(height: 4),
            Text(
              'de CO₂ si se recicla',
              style: TextStyle(
                color: Colors.white54,
                fontSize: 10,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ],
      ),
    );
  }

  String _calculateCO2Saved() {
    // Calcular el ahorro potencial de CO2 si el material es reciclado
    // Asumiendo 0.5 kg de peso promedio por item (puede ajustarse)
    final estimatedWeight = 0.5; // kg
    
    // Buscar la categoría correspondiente para obtener el CO2 ahorrado
    final category = RecyclableMaterials.categories.firstWhere(
      (cat) => cat.name == _detectedMaterial!.category,
      orElse: () => RecyclableMaterials.categories[0], // Default a PET
    );
    
    final co2Saved = category.co2SavedPerKg * estimatedWeight;
    
    // Formatear el resultado
    if (co2Saved < 1) {
      return '${(co2Saved * 1000).toStringAsFixed(0)}g';
    } else {
      return '${co2Saved.toStringAsFixed(1)}kg';
    }
  }
  
  Widget _buildInfoRow(IconData icon, String label, String value) {
    return Row(
      children: [
        Icon(icon, color: BioWayColors.primary, size: 20),
        const SizedBox(width: 8),
        Text(
          label,
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.w500,
          ),
        ),
        const SizedBox(width: 8),
        Text(
          value,
          style: TextStyle(
            fontSize: 16,
            fontWeight: FontWeight.bold,
            color: BioWayColors.primaryDark,
          ),
        ),
      ],
    );
  }

  Widget _buildBottomBar() {
    if (_detectedMaterial != null) {
      return Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topCenter,
            end: Alignment.bottomCenter,
            colors: [
              Colors.transparent,
              Colors.black.withOpacity(0.7),
              Colors.black.withOpacity(0.95),
            ],
          ),
        ),
        child: SafeArea(
          top: false,
          child: Row(
            children: [
              // Botón Escanear Otro
              Expanded(
                child: Container(
                  height: 56,
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [
                        Colors.white.withOpacity(0.15),
                        Colors.white.withOpacity(0.05),
                      ],
                    ),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(
                      color: Colors.white.withOpacity(0.2),
                      width: 1,
                    ),
                  ),
                  child: Material(
                    color: Colors.transparent,
                    child: InkWell(
                      onTap: _resetScanner,
                      borderRadius: BorderRadius.circular(16),
                      child: Container(
                        padding: EdgeInsets.symmetric(horizontal: 16),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              Icons.camera_alt_outlined,
                              color: Colors.white,
                              size: 22,
                            ),
                            SizedBox(width: 8),
                            Text(
                              'Escanear Otro',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 15,
                                fontWeight: FontWeight.w600,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              SizedBox(width: 12),
              // Botón Aceptar
              Expanded(
                child: Container(
                  height: 56,
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      colors: [
                        BioWayColors.primary,
                        BioWayColors.primary.withOpacity(0.8),
                      ],
                    ),
                    borderRadius: BorderRadius.circular(16),
                    boxShadow: [
                      BoxShadow(
                        color: BioWayColors.primary.withOpacity(0.3),
                        blurRadius: 12,
                        offset: Offset(0, 4),
                      ),
                    ],
                  ),
                  child: Material(
                    color: Colors.transparent,
                    child: InkWell(
                      onTap: () {
                        // Animación de confirmación
                        HapticFeedback.lightImpact();
                        Navigator.pop(context, _detectedMaterial);
                      },
                      borderRadius: BorderRadius.circular(16),
                      child: Container(
                        padding: EdgeInsets.symmetric(horizontal: 16),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              Icons.check_circle,
                              color: Colors.white,
                              size: 22,
                            ),
                            SizedBox(width: 8),
                            Text(
                              'Aceptar',
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 15,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      );
    }

    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            Colors.black.withOpacity(0.0),
            Colors.black.withOpacity(0.7),
            Colors.black.withOpacity(0.9),
          ],
        ),
      ),
      child: SafeArea(
        top: false,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            // Botón principal de captura con diseño circular
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceEvenly,
              children: [
                // Botón de galería
                Container(
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    border: Border.all(color: Colors.white30, width: 2),
                  ),
                  child: IconButton(
                    onPressed: !_isProcessing ? _pickFromGallery : null,
                    icon: Icon(
                      Icons.photo_library_rounded,
                      color: Colors.white,
                      size: 28,
                    ),
                    padding: EdgeInsets.all(12),
                  ),
                ),
                // Botón de captura central
                GestureDetector(
                  onTap: _isCameraInitialized && !_isProcessing 
                      ? _captureAndAnalyze 
                      : null,
                  child: Container(
                    width: 80,
                    height: 80,
                    decoration: BoxDecoration(
                      shape: BoxShape.circle,
                      color: BioWayColors.primary,
                      border: Border.all(
                        color: Colors.white,
                        width: 4,
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: BioWayColors.primary.withOpacity(0.3),
                          blurRadius: 15,
                          spreadRadius: 5,
                        ),
                      ],
                    ),
                    child: Icon(
                      Icons.camera_alt,
                      color: Colors.white,
                      size: 40,
                    ),
                  ),
                ),
                // Espacio para simetría
                SizedBox(width: 56),
              ],
            ),
            SizedBox(height: 10),
            Text(
              'Toca para escanear',
              style: TextStyle(
                color: Colors.white70,
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

// CustomPainter para el marco del escáner con esquinas redondeadas
class ScannerFramePainter extends CustomPainter {
  final Color color;
  final double strokeWidth;
  final double cornerRadius;
  final double animationValue;

  ScannerFramePainter({
    required this.color,
    required this.strokeWidth,
    required this.cornerRadius,
    required this.animationValue,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final paint = Paint()
      ..color = color.withOpacity(0.5 + animationValue * 0.5)
      ..strokeWidth = strokeWidth
      ..style = PaintingStyle.stroke
      ..strokeCap = StrokeCap.round;

    final glowPaint = Paint()
      ..color = color.withOpacity(0.3)
      ..strokeWidth = strokeWidth * 2
      ..style = PaintingStyle.stroke
      ..maskFilter = MaskFilter.blur(BlurStyle.outer, 8);

    final path = Path();
    final cornerLength = 40.0;

    // Esquina superior izquierda
    path.moveTo(0, cornerLength);
    path.lineTo(0, cornerRadius);
    path.quadraticBezierTo(0, 0, cornerRadius, 0);
    path.lineTo(cornerLength, 0);

    // Esquina superior derecha
    path.moveTo(size.width - cornerLength, 0);
    path.lineTo(size.width - cornerRadius, 0);
    path.quadraticBezierTo(size.width, 0, size.width, cornerRadius);
    path.lineTo(size.width, cornerLength);

    // Esquina inferior derecha
    path.moveTo(size.width, size.height - cornerLength);
    path.lineTo(size.width, size.height - cornerRadius);
    path.quadraticBezierTo(
        size.width, size.height, size.width - cornerRadius, size.height);
    path.lineTo(size.width - cornerLength, size.height);

    // Esquina inferior izquierda
    path.moveTo(cornerLength, size.height);
    path.lineTo(cornerRadius, size.height);
    path.quadraticBezierTo(0, size.height, 0, size.height - cornerRadius);
    path.lineTo(0, size.height - cornerLength);

    // Dibujar glow primero
    canvas.drawPath(path, glowPaint);
    // Dibujar el marco principal
    canvas.drawPath(path, paint);

    // Puntos decorativos en las esquinas
    final dotPaint = Paint()
      ..color = color
      ..style = PaintingStyle.fill;

    canvas.drawCircle(Offset(cornerRadius, cornerRadius), 3, dotPaint);
    canvas.drawCircle(
        Offset(size.width - cornerRadius, cornerRadius), 3, dotPaint);
    canvas.drawCircle(
        Offset(cornerRadius, size.height - cornerRadius), 3, dotPaint);
    canvas.drawCircle(Offset(size.width - cornerRadius, 
        size.height - cornerRadius), 3, dotPaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => true;
}