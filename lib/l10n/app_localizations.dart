import 'package:flutter/material.dart';

class AppLocalizations {
  final Locale locale;

  AppLocalizations(this.locale);

  static AppLocalizations? of(BuildContext context) {
    return Localizations.of<AppLocalizations>(context, AppLocalizations);
  }

  static const LocalizationsDelegate<AppLocalizations> delegate = 
      _AppLocalizationsDelegate();

  // Login Screen
  String get welcomeBack => locale.languageCode == 'es' 
      ? 'Bienvenido de nuevo' 
      : 'Welcome back';
  
  String get loginToContinue => locale.languageCode == 'es'
      ? 'Inicia sesión para continuar'
      : 'Log in to continue';
      
  String get email => locale.languageCode == 'es'
      ? 'Correo electrónico'
      : 'Email';
      
  String get password => locale.languageCode == 'es'
      ? 'Contraseña'
      : 'Password';
      
  String get forgotPassword => locale.languageCode == 'es'
      ? '¿Olvidaste tu contraseña?'
      : 'Forgot your password?';
      
  String get login => locale.languageCode == 'es'
      ? 'Iniciar Sesión'
      : 'Log In';
      
  String get noAccount => locale.languageCode == 'es'
      ? '¿No tienes una cuenta?'
      : "Don't have an account?";
      
  String get register => locale.languageCode == 'es'
      ? 'Regístrate'
      : 'Sign Up';
      
  String get orContinueWith => locale.languageCode == 'es'
      ? 'O continúa con'
      : 'Or continue with';
      
  String get continueWithGoogle => locale.languageCode == 'es'
      ? 'Continuar con Google'
      : 'Continue with Google';
      
  String get invalidEmail => locale.languageCode == 'es'
      ? 'Por favor ingresa un correo válido'
      : 'Please enter a valid email';
      
  String get passwordTooShort => locale.languageCode == 'es'
      ? 'La contraseña debe tener al menos 6 caracteres'
      : 'Password must be at least 6 characters';
      
  String get loginError => locale.languageCode == 'es'
      ? 'Error al iniciar sesión'
      : 'Login error';
      
  String get checkCredentials => locale.languageCode == 'es'
      ? 'Por favor verifica tus credenciales'
      : 'Please check your credentials';

  // Dashboard Screen
  String get home => locale.languageCode == 'es'
      ? 'Inicio'
      : 'Home';
      
  String get hello => locale.languageCode == 'es'
      ? 'Hola'
      : 'Hello';
      
  String get yourImpact => locale.languageCode == 'es'
      ? 'Tu Impacto'
      : 'Your Impact';
      
  String get totalRecycled => locale.languageCode == 'es'
      ? 'Total Reciclado'
      : 'Total Recycled';
      
  String get co2Saved => locale.languageCode == 'es'
      ? 'CO₂ Ahorrado'
      : 'CO₂ Saved';
      
  String get pointsEarned => locale.languageCode == 'es'
      ? 'Puntos Ganados'
      : 'Points Earned';
      
  String get quickActions => locale.languageCode == 'es'
      ? 'Acciones Rápidas'
      : 'Quick Actions';
      
  String get schedulePickup => locale.languageCode == 'es'
      ? 'Programar Recolección'
      : 'Schedule Pickup';
      
  String get findCenter => locale.languageCode == 'es'
      ? 'Buscar Centro'
      : 'Find Center';
      
  String get scanWaste => locale.languageCode == 'es'
      ? 'Escanear Residuo'
      : 'Scan Waste';
      
  String get localCommerce => locale.languageCode == 'es'
      ? 'Comercio Local'
      : 'Local Commerce';
      
  String get recentActivity => locale.languageCode == 'es'
      ? 'Actividad Reciente'
      : 'Recent Activity';
      
  String get viewAll => locale.languageCode == 'es'
      ? 'Ver todo'
      : 'View all';
      
  String get collectionCompleted => locale.languageCode == 'es'
      ? 'Recolección completada'
      : 'Collection completed';
      
  String get pointsAdded => locale.languageCode == 'es'
      ? 'puntos agregados'
      : 'points added';
      
  String get exchangeCompleted => locale.languageCode == 'es'
      ? 'Intercambio completado'
      : 'Exchange completed';
      
  String get newBadge => locale.languageCode == 'es'
      ? 'Nueva insignia obtenida'
      : 'New badge earned';

  // Scanner Screen
  String get scanMaterial => locale.languageCode == 'es'
      ? 'Escanear Material'
      : 'Scan Material';
      
  String get aiAnalysisComplete => locale.languageCode == 'es'
      ? 'Análisis IA Completado'
      : 'AI Analysis Complete';
      
  String get analyzing => locale.languageCode == 'es'
      ? 'IA Analizando Material'
      : 'AI Analyzing Material';
      
  String get processingVision => locale.languageCode == 'es'
      ? 'Procesando con visión computacional'
      : 'Processing with computer vision';
      
  String get neuralNetwork => locale.languageCode == 'es'
      ? 'Red neuronal identificando patrones...'
      : 'Neural network identifying patterns...';
      
  String get noMaterialDetected => locale.languageCode == 'es'
      ? 'No se detectó ningún material'
      : 'No material detected';
      
  String get confidenceLevel => locale.languageCode == 'es'
      ? 'Nivel de Confianza'
      : 'Confidence Level';
      
  String get potentialSavings => locale.languageCode == 'es'
      ? 'Ahorro Potencial'
      : 'Potential Savings';
      
  String get ifRecycled => locale.languageCode == 'es'
      ? 'de CO₂ si se recicla'
      : 'of CO₂ if recycled';
      
  String get pointsPerKg => locale.languageCode == 'es'
      ? 'Puntos/kg'
      : 'Points/kg';
      
  String get recyclingInstructions => locale.languageCode == 'es'
      ? 'Instrucciones de Reciclaje'
      : 'Recycling Instructions';
      
  String get scanAnother => locale.languageCode == 'es'
      ? 'Escanear Otro'
      : 'Scan Another';
      
  String get accept => locale.languageCode == 'es'
      ? 'Aceptar'
      : 'Accept';
      
  String get tapToFocus => locale.languageCode == 'es'
      ? 'Toca para enfocar'
      : 'Tap to focus';
      
  String get fromGallery => locale.languageCode == 'es'
      ? 'Desde Galería'
      : 'From Gallery';

  // Material Categories
  String get petBottles => locale.languageCode == 'es'
      ? 'Botellas PET'
      : 'PET Bottles';
      
  String get cardboard => locale.languageCode == 'es'
      ? 'Cartón'
      : 'Cardboard';
      
  String get tetraPak => locale.languageCode == 'es'
      ? 'Tetra Pak'
      : 'Tetra Pak';
      
  String get glass => locale.languageCode == 'es'
      ? 'Vidrio'
      : 'Glass';
      
  String get aluminum => locale.languageCode == 'es'
      ? 'Aluminio'
      : 'Aluminum';
      
  String get paper => locale.languageCode == 'es'
      ? 'Papel'
      : 'Paper';
      
  String get organic => locale.languageCode == 'es'
      ? 'Orgánico'
      : 'Organic';
      
  String get metal => locale.languageCode == 'es'
      ? 'Metal'
      : 'Metal';
      
  String get plasticBags => locale.languageCode == 'es'
      ? 'Bolsas Plásticas'
      : 'Plastic Bags';
      
  String get polystyrene => locale.languageCode == 'es'
      ? 'Poliestireno'
      : 'Polystyrene';
      
  String get electronics => locale.languageCode == 'es'
      ? 'Electrónicos'
      : 'Electronics';
      
  String get batteries => locale.languageCode == 'es'
      ? 'Baterías'
      : 'Batteries';
      
  String get textiles => locale.languageCode == 'es'
      ? 'Textiles'
      : 'Textiles';

  // Registration Form
  String get selectUserType => locale.languageCode == 'es'
      ? 'Selecciona tu tipo de usuario'
      : 'Select your user type';
      
  String get selectRole => locale.languageCode == 'es'
      ? 'Selecciona tu rol en la comunidad'
      : 'Select your role in the community';
      
  String get acceptTerms => locale.languageCode == 'es'
      ? 'Debes aceptar los términos y condiciones'
      : 'You must accept the terms and conditions';
      
  String get passwordsDontMatch => locale.languageCode == 'es'
      ? 'Las contraseñas no coinciden'
      : 'Passwords do not match';
      
  String get basicInfo => locale.languageCode == 'es'
      ? 'Información básica'
      : 'Basic Information';
      
  String get fullName => locale.languageCode == 'es'
      ? 'Nombre completo'
      : 'Full Name';
      
  String get enterName => locale.languageCode == 'es'
      ? 'Por favor ingresa tu nombre'
      : 'Please enter your name';
      
  String get enterEmail => locale.languageCode == 'es'
      ? 'Por favor ingresa tu correo'
      : 'Please enter your email';
      
  String get enterValidEmail => locale.languageCode == 'es'
      ? 'Ingresa un correo válido'
      : 'Enter a valid email';
      
  String get phone10Digits => locale.languageCode == 'es'
      ? 'Teléfono (10 dígitos)'
      : 'Phone (10 digits)';
      
  String get enterPhone => locale.languageCode == 'es'
      ? 'Por favor ingresa tu teléfono'
      : 'Please enter your phone';
      
  String get phoneMust10Digits => locale.languageCode == 'es'
      ? 'El teléfono debe tener 10 dígitos'
      : 'Phone must have 10 digits';
      
  String get enterPassword => locale.languageCode == 'es'
      ? 'Por favor ingresa una contraseña'
      : 'Please enter a password';
      
  String get min6Characters => locale.languageCode == 'es'
      ? 'Mínimo 6 caracteres'
      : 'Minimum 6 characters';
      
  String get confirmPassword => locale.languageCode == 'es'
      ? 'Confirmar contraseña'
      : 'Confirm Password';
      
  String get confirmPasswordPlease => locale.languageCode == 'es'
      ? 'Por favor confirma tu contraseña'
      : 'Please confirm your password';
  
  // Navigation
  String get profile => locale.languageCode == 'es'
      ? 'Perfil'
      : 'Profile';
      
  String get map => locale.languageCode == 'es'
      ? 'Mapa'
      : 'Map';
      
  String get rewards => locale.languageCode == 'es'
      ? 'Recompensas'
      : 'Rewards';
      
  String get settings => locale.languageCode == 'es'
      ? 'Configuración'
      : 'Settings';
      
  String get logout => locale.languageCode == 'es'
      ? 'Cerrar Sesión'
      : 'Log Out';
      
  String get language => locale.languageCode == 'es'
      ? 'Idioma'
      : 'Language';
      
  String get spanish => locale.languageCode == 'es'
      ? 'Español'
      : 'Spanish';
      
  String get english => locale.languageCode == 'es'
      ? 'Inglés'
      : 'English';
      
  // User Types
  String get brindador => locale.languageCode == 'es'
      ? 'Brindador'
      : 'Provider';
      
  String get recolector => locale.languageCode == 'es'
      ? 'Recolector'
      : 'Collector';
      
  String get centro => locale.languageCode == 'es'
      ? 'Centro'
      : 'Center';
      
  String get admin => locale.languageCode == 'es'
      ? 'Admin'
      : 'Admin';
      
  String get recycleFromHome => locale.languageCode == 'es'
      ? 'Recicla desde casa'
      : 'Recycle from home';
      
  String get collectMaterials => locale.languageCode == 'es'
      ? 'Recolecta materiales'
      : 'Collect materials';
      
  String get separateWaste => locale.languageCode == 'es'
      ? 'Separa residuos, agenda recolecciones y gana recompensas'
      : 'Separate waste, schedule pickups and earn rewards';
      
  String get accessPreseparated => locale.languageCode == 'es'
      ? 'Accede a materiales pre-separados y optimiza tus rutas'
      : 'Access pre-separated materials and optimize your routes';
      
  // Address
  String get collectionAddress => locale.languageCode == 'es'
      ? 'Dirección de recolección'
      : 'Collection Address';
      
  String get whereWeCollect => locale.languageCode == 'es'
      ? 'Donde recolectaremos tus materiales'
      : 'Where we will collect your materials';
      
  String get streetAndNumber => locale.languageCode == 'es'
      ? 'Calle y número'
      : 'Street and Number';
      
  String get enterAddress => locale.languageCode == 'es'
      ? 'Por favor ingresa tu dirección'
      : 'Please enter your address';
      
  String get postalCode => locale.languageCode == 'es'
      ? 'Código Postal'
      : 'Postal Code';
      
  String get neighborhood => locale.languageCode == 'es'
      ? 'Colonia'
      : 'Neighborhood';
      
  String get cityMunicipality => locale.languageCode == 'es'
      ? 'Ciudad/Municipio'
      : 'City/Municipality';
      
  String get state => locale.languageCode == 'es'
      ? 'Estado'
      : 'State';
      
  // Collector Info
  String get collectorInfo => locale.languageCode == 'es'
      ? 'Información de recolector'
      : 'Collector Information';
      
  String get companyCodeOptional => locale.languageCode == 'es'
      ? 'Código de empresa (opcional)'
      : 'Company Code (optional)';
      
  String get ifBelongCompany => locale.languageCode == 'es'
      ? 'Si perteneces a una empresa asociada'
      : 'If you belong to an associated company';
      
  String get operationZone => locale.languageCode == 'es'
      ? 'Zona de operación'
      : 'Operation Zone';
      
  String get selectZone => locale.languageCode == 'es'
      ? 'Por favor selecciona una zona'
      : 'Please select a zone';
      
  String get dontWorrySchedules => locale.languageCode == 'es'
      ? 'No te preocupes, podrás acceder a horarios fijos de recolección en tu zona'
      : "Don't worry, you'll have access to fixed collection schedules in your area";
      
  // Buttons
  String get iAccept => locale.languageCode == 'es'
      ? 'Acepto los'
      : 'I accept the';
      
  String get previous => locale.languageCode == 'es'
      ? 'Anterior'
      : 'Previous';
      
  String get next => locale.languageCode == 'es'
      ? 'Siguiente'
      : 'Next';
      
  String get createAccount => locale.languageCode == 'es'
      ? 'Crear cuenta'
      : 'Create Account';
      
  String get termsAndConditions => locale.languageCode == 'es'
      ? 'términos y condiciones'
      : 'terms and conditions';
      
  String get couldNotOpenTerms => locale.languageCode == 'es'
      ? 'No se pudo abrir los términos y condiciones'
      : 'Could not open terms and conditions';
      
  // Centro de Acopio
  String get collectionCenter => locale.languageCode == 'es'
      ? 'Centro de Acopio'
      : 'Collection Center';
      
  String get centerBioWay => locale.languageCode == 'es'
      ? 'Centro de Acopio BioWay'
      : 'BioWay Collection Center';
      
  String get notificationsSoon => locale.languageCode == 'es'
      ? 'Notificaciones (próximamente)'
      : 'Notifications (coming soon)';
      
  String get settingsSoon => locale.languageCode == 'es'
      ? 'Configuración (próximamente)'
      : 'Settings (coming soon)';
      
  String get operations => locale.languageCode == 'es'
      ? 'Operaciones'
      : 'Operations';
      
  String get lastReceptions => locale.languageCode == 'es'
      ? 'Últimas Recepciones'
      : 'Last Receptions';
      
  String get scanQr => locale.languageCode == 'es'
      ? 'Escanear QR'
      : 'Scan QR';
      
  String get reload => locale.languageCode == 'es'
      ? 'Recargar'
      : 'Reload';
      
  String get prepaidBalance => locale.languageCode == 'es'
      ? 'Saldo Prepago'
      : 'Prepaid Balance';
      
  String get receptionsToday => locale.languageCode == 'es'
      ? 'Recepciones Hoy'
      : 'Receptions Today';
      
  String get kgToday => locale.languageCode == 'es'
      ? 'Kg Hoy'
      : 'Kg Today';
      
  String get reputation => locale.languageCode == 'es'
      ? 'Reputación'
      : 'Reputation';
      
  String get reception => locale.languageCode == 'es'
      ? 'Recepción'
      : 'Reception';
      
  String get inventory => locale.languageCode == 'es'
      ? 'Inventario'
      : 'Inventory';
      
  String get reports => locale.languageCode == 'es'
      ? 'Reportes'
      : 'Reports';
      
  String get prepaid => locale.languageCode == 'es'
      ? 'Prepago'
      : 'Prepaid';
      
  String get scanQrCode => locale.languageCode == 'es'
      ? 'Escanear Código QR'
      : 'Scan QR Code';
      
  String get placeQrCode => locale.languageCode == 'es'
      ? 'Coloca el código QR del brindador en el centro'
      : 'Place the provider\'s QR code in the center';
      
  String get designMode => locale.languageCode == 'es'
      ? '🎨 MODO DISEÑO'
      : '🎨 DESIGN MODE';
      
  String get scannerDisabled => locale.languageCode == 'es'
      ? 'Escáner QR deshabilitado'
      : 'QR Scanner disabled';
      
  String get manualEntry => locale.languageCode == 'es'
      ? 'Ingresar Manual'
      : 'Manual Entry';
      
  // Material Reception
  String get materialReception => locale.languageCode == 'es'
      ? 'Recepción de Material'
      : 'Material Reception';
      
  String get collector => locale.languageCode == 'es'
      ? 'Recolector'
      : 'Collector';
      
  String get noName => locale.languageCode == 'es'
      ? 'Sin nombre'
      : 'No name';
      
  String get receivedMaterials => locale.languageCode == 'es'
      ? 'Materiales Recibidos'
      : 'Received Materials';
      
  String get invalid => locale.languageCode == 'es'
      ? 'Inválido'
      : 'Invalid';
      
  String get bioWayCommission => locale.languageCode == 'es'
      ? 'Comisión BioWay (10%):'
      : 'BioWay Commission (10%):';
      
  String get processReception => locale.languageCode == 'es'
      ? 'Procesar Recepción'
      : 'Process Reception';
      
  String get receptionRegistered => locale.languageCode == 'es'
      ? 'Recepción registrada exitosamente'
      : 'Reception registered successfully';
      
  String get invalidQrCode => locale.languageCode == 'es'
      ? 'Código QR inválido'
      : 'Invalid QR code';
      
  String get collectorNotFound => locale.languageCode == 'es'
      ? 'Recolector no encontrado'
      : 'Collector not found';
      
  String get requestNotFound => locale.languageCode == 'es'
      ? 'Solicitud no encontrada'
      : 'Request not found';
      
  String get inventoryScreen => locale.languageCode == 'es'
      ? 'Pantalla de Inventario\n(En desarrollo)'
      : 'Inventory Screen\n(In development)';
      
  String get reportsScreen => locale.languageCode == 'es'
      ? 'Pantalla de Reportes\n(En desarrollo)'
      : 'Reports Screen\n(In development)';
      
  String get reloadBalance => locale.languageCode == 'es'
      ? 'Recargar Saldo'
      : 'Reload Balance';
      
  String get currentBalance => locale.languageCode == 'es'
      ? 'Saldo Actual'
      : 'Current Balance';
      
  String get prepaidSystem => locale.languageCode == 'es'
      ? 'Sistema de Prepago\n(En desarrollo)'
      : 'Prepaid System\n(In development)';

  // Brindador Screens
  String get selectYourWaste => locale.languageCode == 'es'
      ? 'Selecciona tus Residuos'
      : 'Select Your Waste';
      
  String get excellentWork => locale.languageCode == 'es'
      ? '¡Excelente trabajo!'
      : 'Excellent work!';
      
  String get placeYourWaste => locale.languageCode == 'es'
      ? 'Coloca tus residuos'
      : 'Place your waste';
      
  String get collectionAndReward => locale.languageCode == 'es'
      ? 'Recolección y recompensa'
      : 'Collection and reward';
      
  String get wasteRegistry => locale.languageCode == 'es'
      ? 'Registro de Residuos'
      : 'Waste Registry';
      
  String get searchStores => locale.languageCode == 'es'
      ? 'Buscar tiendas, restaurantes...'
      : 'Search stores, restaurants...';
      
  String get allStates => locale.languageCode == 'es'
      ? 'Todos los estados'
      : 'All states';
      
  String get municipality => locale.languageCode == 'es'
      ? 'Municipio'
      : 'Municipality';
      
  String get allMunicipalities => locale.languageCode == 'es'
      ? 'Todos los municipios'
      : 'All municipalities';
      
  String get seeAll => locale.languageCode == 'es'
      ? 'Ver todas'
      : 'See all';
      
  String get deleteAccount => locale.languageCode == 'es'
      ? 'Eliminar Cuenta'
      : 'Delete Account';
      
  String get cancel => locale.languageCode == 'es'
      ? 'Cancelar'
      : 'Cancel';
      
  // Recolector Screens
  String get certifiedCollector => locale.languageCode == 'es'
      ? 'Recolector Certificado'
      : 'Certified Collector';
      
  String get bioCoins => locale.languageCode == 'es'
      ? 'BioCoins'
      : 'BioCoins';
      
  String get level => locale.languageCode == 'es'
      ? 'Nivel'
      : 'Level';
      
  String get todayActivity => locale.languageCode == 'es'
      ? 'Actividad de Hoy'
      : 'Today\'s Activity';
      
  String get visitedPoints => locale.languageCode == 'es'
      ? 'Puntos\nvisitados'
      : 'Points\nvisited';
      
  String get collectedKilos => locale.languageCode == 'es'
      ? 'Kilos\nrecolectados'
      : 'Kilos\ncollected';
      
  String get myTotalImpact => locale.languageCode == 'es'
      ? 'Mi Impacto Total'
      : 'My Total Impact';
      
  String get totalCollected => locale.languageCode == 'es'
      ? 'Total recolectado'
      : 'Total collected';
      
  String get co2Avoided => locale.languageCode == 'es'
      ? 'CO₂ evitado'
      : 'CO₂ avoided';
      
  String get collectedMaterials => locale.languageCode == 'es'
      ? 'Materiales Recolectados'
      : 'Collected Materials';
      
  String get areYouSureDelete => locale.languageCode == 'es'
      ? '¿Estás seguro de que deseas eliminar tu cuenta? Esta acción no se puede deshacer.'
      : 'Are you sure you want to delete your account? This action cannot be undone.';
      
  String get delete => locale.languageCode == 'es'
      ? 'Eliminar'
      : 'Delete';
      
  String get accountDeleted => locale.languageCode == 'es'
      ? 'Cuenta eliminada'
      : 'Account deleted';
      
  String get sessionClosed => locale.languageCode == 'es'
      ? 'Sesión cerrada'
      : 'Session closed';
      
  String get navigatingTo => locale.languageCode == 'es'
      ? 'Navegando a'
      : 'Navigating to';
      
  String get centeringLocation => locale.languageCode == 'es'
      ? 'Centrando en tu ubicación'
      : 'Centering on your location';
      
  // Maestro/Admin Screens
  String get masterPanel => locale.languageCode == 'es'
      ? 'Panel Maestro BioWay'
      : 'BioWay Master Panel';
      
  String get adminBioWay => locale.languageCode == 'es'
      ? 'Administrador BioWay'
      : 'BioWay Administrator';
      
  String get welcome => locale.languageCode == 'es'
      ? 'Bienvenido'
      : 'Welcome';
      
  String get systemManagement => locale.languageCode == 'es'
      ? 'Gestión del Sistema'
      : 'System Management';
      
  String get companies => locale.languageCode == 'es'
      ? 'Empresas'
      : 'Companies';
      
  String get manageCompanies => locale.languageCode == 'es'
      ? 'Gestionar empresas'
      : 'Manage companies';
      
  String get materials => locale.languageCode == 'es'
      ? 'Materiales'
      : 'Materials';
      
  String get manageRecyclables => locale.languageCode == 'es'
      ? 'Gestionar reciclables'
      : 'Manage recyclables';
      
  String get users => locale.languageCode == 'es'
      ? 'Usuarios'
      : 'Users';
      
  String get manageUsers => locale.languageCode == 'es'
      ? 'Administrar usuarios'
      : 'Manage users';
      
  String get schedules => locale.languageCode == 'es'
      ? 'Horarios'
      : 'Schedules';
      
  String get collectionSchedules => locale.languageCode == 'es'
      ? 'Horarios de recolección'
      : 'Collection schedules';
      
  String get availability => locale.languageCode == 'es'
      ? 'Disponibilidad'
      : 'Availability';
      
  String get statesAndMunicipalities => locale.languageCode == 'es'
      ? 'Estados y municipios'
      : 'States and municipalities';
      
  String get configuration => locale.languageCode == 'es'
      ? 'Configuración'
      : 'Configuration';
      
  String get systemSettings => locale.languageCode == 'es'
      ? 'Ajustes del sistema'
      : 'System settings';
      
  String get cleanInactiveUsers => locale.languageCode == 'es'
      ? 'Limpiar usuarios inactivos'
      : 'Clean inactive users';
      
  String get deleteInactiveAccounts => locale.languageCode == 'es'
      ? 'Eliminar cuentas con 3+ meses de inactividad'
      : 'Delete accounts with 3+ months of inactivity';
      
  String get dataBackup => locale.languageCode == 'es'
      ? 'Respaldo de datos'
      : 'Data backup';
      
  String get exportSystemInfo => locale.languageCode == 'es'
      ? 'Exportar información del sistema'
      : 'Export system information';
      
  String get sureDeleteInactive => locale.languageCode == 'es'
      ? '¿Está seguro de eliminar todas las cuentas con más de 3 meses de inactividad?\n\nEsta acción no se puede deshacer.'
      : 'Are you sure you want to delete all accounts with more than 3 months of inactivity?\n\nThis action cannot be undone.';
      
  String get cleaningStarted => locale.languageCode == 'es'
      ? 'Limpieza de usuarios inactivos iniciada...'
      : 'Inactive user cleanup started...';
      
  String get searchByNameOrCode => locale.languageCode == 'es'
      ? 'Buscar por nombre o código...'
      : 'Search by name or code...';
      
  String get edit => locale.languageCode == 'es'
      ? 'Editar'
      : 'Edit';
      
  String get copyCode => locale.languageCode == 'es'
      ? 'Copiar Código'
      : 'Copy Code';
      
  String get codeCopied => locale.languageCode == 'es'
      ? 'Código copiado'
      : 'Code copied';
      
  String get deleteCompany => locale.languageCode == 'es'
      ? 'Eliminar Empresa'
      : 'Delete Company';
      
  String get companyDeleted => locale.languageCode == 'es'
      ? 'Empresa eliminada correctamente'
      : 'Company deleted successfully';
      
  String get companyActivated => locale.languageCode == 'es'
      ? 'Empresa activada'
      : 'Company activated';
      
  String get companyPaused => locale.languageCode == 'es'
      ? 'Empresa pausada'
      : 'Company paused';
      
  String get companyManagement => locale.languageCode == 'es'
      ? 'Gestión de Empresas'
      : 'Company Management';
      
  String get associatedCompaniesSystem => locale.languageCode == 'es'
      ? 'Sistema de Empresas Asociadas'
      : 'Associated Companies System';
      
  String get understood => locale.languageCode == 'es'
      ? 'Entendido'
      : 'Understood';
      
  String get companyName => locale.languageCode == 'es'
      ? 'Nombre de la Empresa'
      : 'Company Name';
      
  String get description => locale.languageCode == 'es'
      ? 'Descripción'
      : 'Description';
      
  String get brieflyDescribe => locale.languageCode == 'es'
      ? 'Describe brevemente la empresa...'
      : 'Briefly describe the company...';
      
  String get maxRange => locale.languageCode == 'es'
      ? 'Rango máximo (km)'
      : 'Maximum range (km)';
      
  String get restrictByDistance => locale.languageCode == 'es'
      ? 'Restringir por rango de distancia'
      : 'Restrict by distance range';
      
  String get selectAtLeastOneMaterial => locale.languageCode == 'es'
      ? 'Selecciona al menos un material'
      : 'Select at least one material';
      
  String get selectAtLeastOneState => locale.languageCode == 'es'
      ? 'Selecciona al menos un estado'
      : 'Select at least one state';
      
  String get companyUpdated => locale.languageCode == 'es'
      ? 'Empresa actualizada correctamente'
      : 'Company updated successfully';
      
  String get companyCreated => locale.languageCode == 'es'
      ? 'Empresa creada correctamente'
      : 'Company created successfully';
      
  String get userAdministration => locale.languageCode == 'es'
      ? 'Administración de Usuarios'
      : 'User Administration';
      
  String get inDevelopment => locale.languageCode == 'es'
      ? '(En desarrollo)'
      : '(In development)';
      
  // Platform Selection
  String get selectPlatform => locale.languageCode == 'es'
      ? 'Seleccionar Plataforma'
      : 'Select Platform';
      
  String get ecoceSoon => locale.languageCode == 'es'
      ? 'ECOCE próximamente disponible'
      : 'ECOCE coming soon';
      
  String get mexico => locale.languageCode == 'es'
      ? 'MÉXICO'
      : 'MEXICO';
      
  String get unitedStates => locale.languageCode == 'es'
      ? 'Estados Unidos'
      : 'United States';
      
  // Additional keys for missing translations
  String get rememberMe => locale.languageCode == 'es'
      ? 'Recordarme'
      : 'Remember me';
      
  String get success => locale.languageCode == 'es'
      ? 'Éxito'
      : 'Success';
      
  String get quickAccessDev => locale.languageCode == 'es'
      ? '🎨 ACCESO RÁPIDO (DESARROLLO)'
      : '🎨 QUICK ACCESS (DEV)';
      
  String get accessingAs => locale.languageCode == 'es'
      ? 'Accediendo como'
      : 'Accessing as';
      
  String get yourTotalImpact => locale.languageCode == 'es'
      ? 'Tu Impacto Total'
      : 'Your Total Impact';
      
  String get aiEstimations => locale.languageCode == 'es'
      ? 'Estimaciones basadas en IA'
      : 'AI-based estimations';
      
  String get ranking => locale.languageCode == 'es'
      ? 'Ranking'
      : 'Ranking';
      
  String get achievements => locale.languageCode == 'es'
      ? 'Logros'
      : 'Achievements';
      
  String get exclusiveDiscounts => locale.languageCode == 'es'
      ? 'Descuentos exclusivos para ti'
      : 'Exclusive discounts for you';
      
  String get filterByLocation => locale.languageCode == 'es'
      ? 'Filtrar por ubicación'
      : 'Filter by location';
      
  String get noStoresFound => locale.languageCode == 'es'
      ? 'No se encontraron comercios'
      : 'No stores found';
      
  String get tryOtherFilters => locale.languageCode == 'es'
      ? 'Intenta con otros filtros'
      : 'Try other filters';
      
  String get dailyOffers => locale.languageCode == 'es'
      ? 'Ofertas del día'
      : 'Daily offers';
      
  String get storesNearYou => locale.languageCode == 'es'
      ? 'Comercios cerca de ti'
      : 'Stores near you';
      
  String get offers => locale.languageCode == 'es'
      ? 'ofertas'
      : 'offers';
      
  String get discountCode => locale.languageCode == 'es'
      ? 'Tu código de descuento'
      : 'Your discount code';
      
  String get showAtStore => locale.languageCode == 'es'
      ? 'Muéstralo en el comercio'
      : 'Show it at the store';
      
  String get redeemDiscount => locale.languageCode == 'es'
      ? 'Canjear descuento'
      : 'Redeem discount';
      
  String get aboutThisOffer => locale.languageCode == 'es'
      ? 'Acerca de esta oferta'
      : 'About this offer';
      
  String get discountRedeemed => locale.languageCode == 'es'
      ? '¡Descuento canjeado exitosamente!'
      : 'Discount redeemed successfully!';
      
  String get alreadyUsed => locale.languageCode == 'es'
      ? 'Ya lo usé'
      : 'Already used';
      
  String get haveEnoughCoins => locale.languageCode == 'es'
      ? 'Tienes suficientes BioCoins'
      : 'You have enough BioCoins';
      
  String get needMoreCoins => locale.languageCode == 'es'
      ? 'Te faltan'
      : 'You need';
      
  String get moreCoins => locale.languageCode == 'es'
      ? 'BioCoins más'
      : 'more BioCoins';
      
  // Additional recolector screen keys
  String get plastic => locale.languageCode == 'es'
      ? 'Plástico'
      : 'Plastic';
      
  String get paperCardboard => locale.languageCode == 'es'
      ? 'Papel y Cartón'
      : 'Paper and Cardboard';
      
  String get organicWaste => locale.languageCode == 'es'
      ? 'Orgánico'
      : 'Organic';
      
  String get electronicWaste => locale.languageCode == 'es'
      ? 'Electrónico'
      : 'Electronic';
      
  String get areYouSureDeleteAccount => locale.languageCode == 'es'
      ? '¿Estás seguro de que deseas eliminar tu cuenta? Esta acción no se puede deshacer.'
      : 'Are you sure you want to delete your account? This action cannot be undone.';
      
  String get warningText => locale.languageCode == 'es'
      ? 'Advertencia'
      : 'Warning';
      
  // Days of the week
  String get monday => locale.languageCode == 'es'
      ? 'Lunes'
      : 'Monday';
      
  String get tuesday => locale.languageCode == 'es'
      ? 'Martes'
      : 'Tuesday';
      
  String get wednesday => locale.languageCode == 'es'
      ? 'Miércoles'
      : 'Wednesday';
      
  String get thursday => locale.languageCode == 'es'
      ? 'Jueves'
      : 'Thursday';
      
  String get friday => locale.languageCode == 'es'
      ? 'Viernes'
      : 'Friday';
      
  String get saturday => locale.languageCode == 'es'
      ? 'Sábado'
      : 'Saturday';
      
  String get sunday => locale.languageCode == 'es'
      ? 'Domingo'
      : 'Sunday';
      
  String get yesterday => locale.languageCode == 'es'
      ? 'AYER'
      : 'YESTERDAY';
      
  String get today => locale.languageCode == 'es'
      ? 'HOY'
      : 'TODAY';
      
  String get tomorrow => locale.languageCode == 'es'
      ? 'MAÑANA'
      : 'TOMORROW';
      
  // Additional Materials
  String get allMaterials => locale.languageCode == 'es'
      ? 'Todos los materiales'
      : 'All materials';
      
  String get mixedPlastics => locale.languageCode == 'es'
      ? 'Plásticos mixtos'
      : 'Mixed plastics';
      
  String get bags => locale.languageCode == 'es'
      ? 'Bolsas'
      : 'Bags';
      
  String get wrappers => locale.languageCode == 'es'
      ? 'Envolturas'
      : 'Wrappers';
      
  String get cans => locale.languageCode == 'es'
      ? 'Latas'
      : 'Cans';
      
  String get steel => locale.languageCode == 'es'
      ? 'Acero'
      : 'Steel';
      
  String get newspaper => locale.languageCode == 'es'
      ? 'Periódico'
      : 'Newspaper';
      
  String get magazines => locale.languageCode == 'es'
      ? 'Revistas'
      : 'Magazines';
      
  String get hdpe => locale.languageCode == 'es'
      ? 'HDPE'
      : 'HDPE';
      
  String get minimumQuantity => locale.languageCode == 'es'
      ? 'Cantidad mínima'
      : 'Minimum quantity';
      
  String get notAccepted => locale.languageCode == 'es'
      ? 'No aceptamos'
      : 'Not accepted';
      
  String get organics => locale.languageCode == 'es'
      ? 'Orgánicos'
      : 'Organics';
      
  String get hazardousWaste => locale.languageCode == 'es'
      ? 'Residuos peligrosos'
      : 'Hazardous waste';
      
  String get noSchedule => locale.languageCode == 'es'
      ? 'Sin horario'
      : 'No schedule';
}

class _AppLocalizationsDelegate extends LocalizationsDelegate<AppLocalizations> {
  const _AppLocalizationsDelegate();

  @override
  bool isSupported(Locale locale) {
    return ['es', 'en'].contains(locale.languageCode);
  }

  @override
  Future<AppLocalizations> load(Locale locale) async {
    return AppLocalizations(locale);
  }

  @override
  bool shouldReload(_AppLocalizationsDelegate old) => false;
}