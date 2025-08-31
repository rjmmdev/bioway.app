import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:url_launcher/url_launcher.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:easy_localization/easy_localization.dart';
import '../../utils/colors.dart';
import '../../models/bioway/horario.dart';
import '../../models/bioway/user_state.dart';
import 'brindador_residuos_grid_screen.dart';
import 'waste_scanner_screen.dart';
import '../../l10n/app_localizations.dart';

class BrindadorDashboardScreen extends StatefulWidget {
  const BrindadorDashboardScreen({super.key});

  @override
  State<BrindadorDashboardScreen> createState() => _BrindadorDashboardScreenState();
}

class _BrindadorDashboardScreenState extends State<BrindadorDashboardScreen> 
    with TickerProviderStateMixin {
  List<Horario> _horarios = [];
  UserState? _userState;
  late AnimationController _animationController;
  late Animation<double> _scaleAnimation;
  late AnimationController _fabAnimationController;
  late Animation<double> _fabScaleAnimation;
  late Animation<double> _fabRotationAnimation;
  
  int _bioCoins = 1250;
  int _userStatus = 0;
  int _selectedIndex = 1; // HOY por defecto
  late PageController _pageController;

  @override
  void initState() {
    super.initState();
    _pageController = PageController(viewportFraction: 0.3, initialPage: _selectedIndex);
    _animationController = AnimationController(
      duration: const Duration(seconds: 2),
      vsync: this,
    );
    _scaleAnimation = Tween<double>(
      begin: 1.0,
      end: 1.05,
    ).animate(CurvedAnimation(
      parent: _animationController,
      curve: Curves.easeInOut,
    ));
    _animationController.repeat(reverse: true);
    
    // Animación para el FAB
    _fabAnimationController = AnimationController(
      duration: const Duration(seconds: 3),
      vsync: this,
    );
    _fabScaleAnimation = Tween<double>(
      begin: 0.9,
      end: 1.1,
    ).animate(CurvedAnimation(
      parent: _fabAnimationController,
      curve: Curves.elasticInOut,
    ));
    _fabRotationAnimation = Tween<double>(
      begin: 0,
      end: 0.05,
    ).animate(CurvedAnimation(
      parent: _fabAnimationController,
      curve: Curves.easeInOut,
    ));
    _fabAnimationController.repeat(reverse: true);
    
    _initializeMockData();
  }

  void _initializeMockData() {
    // Necesitamos retrasar la inicialización hasta que tengamos contexto
    WidgetsBinding.instance.addPostFrameCallback((_) {
      setState(() {
        _horarios = Horario.getMockHorarios(context);
        _userState = UserState.getMockUserState();
        _userStatus = 0;
      });
    });
  }

  @override
  void dispose() {
    _pageController.dispose();
    _animationController.dispose();
    _fabAnimationController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final days = _getAyerHoyManana(_horarios);
    
    return Scaffold(
      backgroundColor: const Color(0xFFF8F9FA),
      body: SafeArea(
        child: Column(
          children: [
            _buildHeader(),
            Expanded(
              child: SingleChildScrollView(
                physics: const BouncingScrollPhysics(),
                padding: const EdgeInsets.only(bottom: 20),
                child: Column(
                  children: [
                    const SizedBox(height: 20),
                    _buildRecycleNowCard(days[_selectedIndex]),
                    const SizedBox(height: 24),
                    _buildScheduleSection(days),
                    const SizedBox(height: 24),
                    _buildTipsSection(),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
      floatingActionButton: _buildFloatingActionButton(),
    );
  }

  Widget _buildFloatingActionButton() {
    return AnimatedBuilder(
      animation: _fabAnimationController,
      builder: (context, child) {
        return Transform.scale(
          scale: _fabScaleAnimation.value,
          child: Transform.rotate(
            angle: _fabRotationAnimation.value,
            child: Container(
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: LinearGradient(
                  colors: [
                    BioWayColors.primary,
                    BioWayColors.primaryLight,
                  ],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                boxShadow: [
                  BoxShadow(
                    color: BioWayColors.primary.withOpacity(0.4),
                    blurRadius: 20,
                    spreadRadius: 2,
                    offset: const Offset(0, 4),
                  ),
                  BoxShadow(
                    color: BioWayColors.primary.withOpacity(0.2),
                    blurRadius: 30,
                    spreadRadius: 5,
                    offset: const Offset(0, 8),
                  ),
                ],
              ),
              child: FloatingActionButton(
                onPressed: _openScanner,
                backgroundColor: Colors.transparent,
                elevation: 0,
                child: Container(
                  width: 60,
                  height: 60,
                  decoration: BoxDecoration(
                    shape: BoxShape.circle,
                    gradient: LinearGradient(
                      colors: [
                        Colors.white.withOpacity(0.2),
                        Colors.transparent,
                      ],
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                    ),
                  ),
                  child: Icon(
                    Icons.qr_code_scanner,
                    color: Colors.white,
                    size: 28,
                  ),
                ),
              ),
            ),
          ),
        );
      },
    );
  }

  Future<void> _openScanner() async {
    final result = await Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => const WasteScannerScreen(),
      ),
    );
    
    if (result != null && mounted) {
      // Manejar el resultado del escaneo si es necesario
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('${'material_detected'.tr()}: ${result.category}'),
          backgroundColor: BioWayColors.primary,
        ),
      );
    }
  }

  Widget _buildHeader() {
    final screenWidth = MediaQuery.of(context).size.width;
    return Container(
      padding: EdgeInsets.fromLTRB(
        screenWidth * 0.04, 
        16, 
        screenWidth * 0.04, 
        0
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'hello'.tr() + ',',
                style: TextStyle(
                  fontSize: 14,
                  color: Colors.grey[600],
                ),
              ),
              const SizedBox(height: 4),
              Text(
                _userState?.nombre ?? 'Usuario',
                style: const TextStyle(
                  fontSize: 28,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1A1A1A),
                ),
              ),
            ],
          ),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: BioWayColors.navGreen.withOpacity(0.1),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Row(
              children: [
                Icon(
                  Icons.monetization_on,
                  color: BioWayColors.navGreen,
                  size: 20,
                ),
                const SizedBox(width: 8),
                Text(
                  '$_bioCoins',
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.bold,
                    color: BioWayColors.navGreen,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildRecycleNowCard(Horario? horario) {
    final canRecycle = (_userState?.puedeBrindar ?? false) && horario != null;
    final screenWidth = MediaQuery.of(context).size.width;
    
    return Container(
      margin: EdgeInsets.symmetric(horizontal: screenWidth * 0.04),
      decoration: BoxDecoration(
        gradient: LinearGradient(
          colors: canRecycle
              ? BioWayColors.backgroundGradientSoft
              : [Colors.grey[400]!.withOpacity(0.3), Colors.grey[500]!.withOpacity(0.3)],
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
        ),
        borderRadius: BorderRadius.circular(24),
        boxShadow: [
          BoxShadow(
            color: canRecycle
                ? BioWayColors.primaryGreen.withOpacity(0.3)
                : Colors.black.withOpacity(0.1),
            blurRadius: 20,
            offset: const Offset(0, 8),
          ),
        ],
      ),
      child: Material(
        color: Colors.transparent,
        borderRadius: BorderRadius.circular(24),
        child: InkWell(
          borderRadius: BorderRadius.circular(24),
          onTap: canRecycle ? () => _navigateToResiduos(horario) : null,
          child: Padding(
            padding: EdgeInsets.symmetric(
              horizontal: screenWidth * 0.05,
              vertical: 20,
            ),
            child: Column(
              children: [
                if (horario != null) ...[
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                        decoration: BoxDecoration(
                          color: Colors.white.withOpacity(0.2),
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: Text(
                          'today'.tr() + ' - ${horario.dia}',
                          style: const TextStyle(
                            color: Color(0xFF00553F),
                            fontSize: 13,
                            fontWeight: FontWeight.w600,
                            letterSpacing: 0.5,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Text(
                    horario.matinfo ?? '',
                    style: const TextStyle(
                      color: Color(0xFF00553F),
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                      letterSpacing: -0.5,
                    ),
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 6),
                  Wrap(
                    alignment: WrapAlignment.center,
                    spacing: 8,
                    children: [
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(
                            Icons.schedule,
                            color: Color(0xFF00553F),
                            size: 16,
                          ),
                          const SizedBox(width: 6),
                          Text(
                            horario.horario ?? '',
                            style: const TextStyle(
                              color: Color(0xFF00553F),
                              fontSize: 14,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ],
                      ),
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(
                            Icons.scale,
                            color: Color(0xFF00553F),
                            size: 16,
                          ),
                          const SizedBox(width: 6),
                          Text(
                            'Mín: ${horario.cantidadMinima}',
                            style: const TextStyle(
                              color: Color(0xFF00553F),
                              fontSize: 14,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),
                ],
                AnimatedBuilder(
                  animation: _scaleAnimation,
                  builder: (context, child) {
                    return Transform.scale(
                      scale: canRecycle ? _scaleAnimation.value : 1.0,
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 32, vertical: 14),
                        decoration: BoxDecoration(
                          color: Colors.white,
                          borderRadius: BorderRadius.circular(35),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withOpacity(0.15),
                              blurRadius: 15,
                              offset: const Offset(0, 8),
                            ),
                          ],
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(
                              Icons.recycling,
                              color: canRecycle
                                  ? BioWayColors.navGreen
                                  : Colors.grey,
                              size: 28,
                            ),
                            const SizedBox(width: 12),
                            Text(
                              canRecycle
                                  ? 'Reciclar ahora'
                                  : horario == null
                                      ? 'Sin recolección hoy'
                                      : 'No disponible',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.bold,
                                color: canRecycle
                                    ? BioWayColors.navGreen
                                    : Colors.grey,
                                letterSpacing: -0.5,
                              ),
                            ),
                          ],
                        ),
                      ),
                    );
                  },
                ),
                if (!canRecycle && horario == null) ...[
                  const SizedBox(height: 16),
                  Text(
                    'check_calendar'.tr(),
                    style: TextStyle(
                      color: Color(0xFF00553F),
                      fontSize: 14,
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }


  Widget _buildScheduleSection(List<Horario?> days) {
    final screenWidth = MediaQuery.of(context).size.width;
    return Container(
      margin: EdgeInsets.symmetric(horizontal: screenWidth * 0.04),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Text(
            'recycling_calendar'.tr(),
            style: TextStyle(
              fontSize: 20,
              fontWeight: FontWeight.bold,
              color: BioWayColors.textDark,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            'swipe_to_see_days'.tr(),
            style: TextStyle(
              fontSize: 13,
              color: Colors.grey[600],
            ),
          ),
          const SizedBox(height: 16),
          Container(
            height: 85,
            child: PageView.builder(
              controller: _pageController,
              itemCount: 3,
              onPageChanged: _onCardSelected,
              itemBuilder: (context, index) {
                return _buildDayCard(days[index], index, _getLabel(index));
              },
            ),
          ),
          if (days[_selectedIndex] != null) ...[
            const SizedBox(height: 20),
            _buildScheduleDetails(days[_selectedIndex]!),
          ],
        ],
      ),
    );
  }

  Widget _buildDayCard(Horario? horario, int index, String label) {
    final isSelected = index == _selectedIndex;
    return GestureDetector(
      onTap: () => _onCardSelected(index),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        margin: EdgeInsets.symmetric(
          horizontal: isSelected ? 5 : 10,
          vertical: isSelected ? 0 : 6,
        ),
        decoration: BoxDecoration(
          gradient: isSelected
              ? LinearGradient(
                  colors: BioWayColors.backgroundGradientSoft,
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                )
              : null,
          color: isSelected ? null : Colors.white,
          borderRadius: BorderRadius.circular(14),
          boxShadow: [
            BoxShadow(
              color: isSelected
                  ? BioWayColors.primaryGreen.withOpacity(0.3)
                  : Colors.black.withOpacity(0.05),
              blurRadius: isSelected ? 12 : 6,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Text(
                label,
                style: TextStyle(
                  fontSize: isSelected ? 15 : 13,
                  fontWeight: FontWeight.bold,
                  color: isSelected ? const Color(0xFF00553F) : BioWayColors.textDark,
                ),
              ),
              const SizedBox(height: 4),
              if (horario != null)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: isSelected
                        ? Colors.white.withOpacity(0.2)
                        : BioWayColors.navGreen.withOpacity(0.1),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    horario.dia?.substring(0, 3).toUpperCase() ?? '',
                    style: TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: isSelected ? const Color(0xFF00553F) : BioWayColors.navGreen,
                    ),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildScheduleDetails(Horario horario) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(20),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: BioWayColors.navGreen.withOpacity(0.1),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Icon(
                  Icons.recycling,
                  color: BioWayColors.navGreen,
                  size: 24,
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      horario.matinfo ?? '',
                      style: const TextStyle(
                        fontSize: 18,
                        fontWeight: FontWeight.bold,
                        color: Color(0xFF1A1A1A),
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '${_getLabel(_selectedIndex)} - ${horario.dia}',
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
          const SizedBox(height: 20),
          _buildDetailRow(Icons.schedule, 'schedule'.tr(), horario.horario ?? ''),
          const SizedBox(height: 12),
          _buildDetailRow(Icons.scale, 'minimum_quantity'.tr(), horario.cantidadMinima ?? ''),
          const SizedBox(height: 12),
          _buildDetailRow(Icons.not_interested, 'No se recibe', horario.qnr ?? ''),
          const SizedBox(height: 20),
          SizedBox(
            width: double.infinity,
            child: OutlinedButton.icon(
              onPressed: _openMoreInfo,
              style: OutlinedButton.styleFrom(
                padding: const EdgeInsets.symmetric(vertical: 12),
                side: BorderSide(color: BioWayColors.navGreen),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              icon: Icon(
                Icons.info_outline,
                color: BioWayColors.navGreen,
              ),
              label: Text(
                'Más información',
                style: TextStyle(
                  color: BioWayColors.navGreen,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDetailRow(IconData icon, String label, String value) {
    return Row(
      children: [
        Icon(
          icon,
          size: 20,
          color: Colors.grey[600],
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                label,
                style: TextStyle(
                  fontSize: 12,
                  color: Colors.grey[600],
                ),
              ),
              const SizedBox(height: 2),
              Text(
                value,
                style: const TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.w600,
                  color: Color(0xFF1A1A1A),
                ),
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildTipsSection() {
    final screenWidth = MediaQuery.of(context).size.width;
    return Container(
      margin: EdgeInsets.symmetric(horizontal: screenWidth * 0.04),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(
                Icons.lightbulb_outline,
                color: Colors.amber[700],
                size: 24,
              ),
              const SizedBox(width: 8),
              Text(
                'recycling_tips'.tr(),
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                  color: Color(0xFF1A1A1A),
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          _buildTipCard(
            icon: Icons.clean_hands,
            title: 'clean_waste_title'.tr(),
            description: 'clean_waste_desc'.tr(),
            color: Colors.blue,
          ),
          const SizedBox(height: 12),
          _buildTipCard(
            icon: Icons.compress,
            title: 'compact_material_title'.tr(),
            description: 'compact_material_desc'.tr(),
            color: Colors.green,
          ),
          const SizedBox(height: 12),
          _buildTipCard(
            icon: Icons.category,
            title: 'separate_correctly_title'.tr(),
            description: 'separate_correctly_desc'.tr(),
            color: Colors.purple,
          ),
        ],
      ),
    );
  }

  Widget _buildTipCard({
    required IconData icon,
    required String title,
    required String description,
    required Color color,
  }) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(16),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            blurRadius: 10,
            offset: const Offset(0, 4),
          ),
        ],
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: color.withOpacity(0.1),
              borderRadius: BorderRadius.circular(12),
            ),
            child: Icon(
              icon,
              color: color,
              size: 24,
            ),
          ),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                    color: Color(0xFF1A1A1A),
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  description,
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
    );
  }

  void _navigateToResiduos(Horario horario) {
    HapticFeedback.mediumImpact();
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => BrindadorResiduosGridScreen(
          selectedCantMin: horario.cantidadMinima ?? '',
        ),
      ),
    );
  }

  void _onCardSelected(int index) {
    setState(() {
      _selectedIndex = index;
    });
    _pageController.animateToPage(
      index, 
      duration: const Duration(milliseconds: 200), 
      curve: Curves.easeInOut,
    );
  }

  Horario? _findHorarioOrNull(List<Horario> all, int day) {
    for (final h in all) {
      if (h.numDia == day) return h;
    }
    return null;
  }

  List<Horario?> _getAyerHoyManana(List<Horario> all) {
    final now = DateTime.now();
    final hoyNum = now.weekday;
    final ayerNum = (hoyNum == 1) ? 7 : hoyNum - 1;
    final mananaNum = (hoyNum == 7) ? 1 : hoyNum + 1;

    return [
      _findHorarioOrNull(all, ayerNum),
      _findHorarioOrNull(all, hoyNum),
      _findHorarioOrNull(all, mananaNum),
    ];
  }

  String _getLabel(int index) {
    final loc = AppLocalizations.of(context);
    if (index == 0) return loc?.yesterday ?? 'AYER';
    if (index == 1) return loc?.today ?? 'HOY';
    if (index == 2) return loc?.tomorrow ?? 'MAÑANA';
    return "";
  }

  Future<void> _openMoreInfo() async {
    final Uri url = Uri.parse("https://bioway.com.mx/biowayapp.html#guia-reciclaje");
    if (!await launchUrl(url, mode: LaunchMode.externalApplication)) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('could_not_open_link'.tr())),
        );
      }
    }
  }
}