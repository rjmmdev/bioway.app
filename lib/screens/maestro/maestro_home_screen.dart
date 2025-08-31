import 'package:flutter/material.dart';
import 'package:firebase_auth/firebase_auth.dart';
import '../../../utils/colors.dart';
import '../../../services/user_session_service.dart';
import '../../../l10n/app_localizations.dart';
import 'empresas/empresas_list_screen.dart';
import 'materiales/materiales_list_screen.dart';
import 'usuarios/usuarios_list_screen.dart';
import 'configuracion/configuracion_screen.dart';
import 'horarios/horarios_screen.dart';
import 'disponibilidad/disponibilidad_screen.dart';

class MaestroHomeScreen extends StatefulWidget {
  const MaestroHomeScreen({Key? key}) : super(key: key);

  @override
  State<MaestroHomeScreen> createState() => _MaestroHomeScreenState();
}

class _MaestroHomeScreenState extends State<MaestroHomeScreen> {
  final String _adminName = 'Administrador BioWay';

  Widget _buildMenuCard({
    required String title,
    required String subtitle,
    required IconData icon,
    required Color color,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(20),
          boxShadow: [
            BoxShadow(
              color: Colors.grey.withValues(alpha: 0.1),
              spreadRadius: 1,
              blurRadius: 10,
              offset: const Offset(0, 3),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: color.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(12),
              ),
              child: Icon(
                icon,
                color: color,
                size: 30,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              title,
              style: const TextStyle(
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 4),
            Text(
              subtitle,
              style: TextStyle(
                fontSize: 14,
                color: Colors.grey[600],
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFFF5F5F5),
      appBar: AppBar(
        backgroundColor: BioWayColors.navGreen,
        elevation: 0,
        title: Text(
          AppLocalizations.of(context)?.masterPanel ?? 'Panel Maestro BioWay',
          style: TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            shadows: [
              Shadow(
                color: Colors.black54,
                offset: Offset(1, 1),
                blurRadius: 2,
              ),
            ],
          ),
        ),
        actions: [
          IconButton(
            icon: const Icon(Icons.notifications_outlined, color: Colors.white),
            onPressed: () {
              // TODO: Implementar notificaciones
            },
          ),
          IconButton(
            icon: const Icon(Icons.logout, color: Colors.white),
            onPressed: () async {
              UserSessionService().clearSession();
              await FirebaseAuth.instance.signOut();
              Navigator.pushNamedAndRemoveUntil(
                context,
                '/',
                (route) => false,
              );
            },
          ),
        ],
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Header
              Container(
                padding: const EdgeInsets.all(20),
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    colors: BioWayColors.backgroundGradientSoft,
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  ),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.black.withValues(alpha: 0.1),
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Icon(
                        Icons.admin_panel_settings,
                        color: Color(0xFF00553F),
                        size: 30,
                      ),
                    ),
                    const SizedBox(width: 16),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            AppLocalizations.of(context)?.welcome ?? 'Bienvenido',
                            style: TextStyle(
                              color: Color(0xFF00553F),
                              fontSize: 14,
                            ),
                          ),
                          Text(
                            AppLocalizations.of(context)?.adminBioWay ?? _adminName,
                            style: const TextStyle(
                              color: Color(0xFF00553F),
                              fontSize: 20,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 24),

              // Menu Grid
              Text(
                AppLocalizations.of(context)?.systemManagement ?? 'Gestión del Sistema',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 16),
              GridView.count(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                crossAxisCount: 2,
                mainAxisSpacing: 16,
                crossAxisSpacing: 16,
                childAspectRatio: 1,
                children: [
                  _buildMenuCard(
                    title: AppLocalizations.of(context)?.companies ?? 'Empresas',
                    subtitle: AppLocalizations.of(context)?.manageCompanies ?? 'Gestionar empresas',
                    icon: Icons.business,
                    color: Colors.blue,
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const EmpresasListScreen(),
                        ),
                      );
                    },
                  ),
                  _buildMenuCard(
                    title: AppLocalizations.of(context)?.materials ?? 'Materiales',
                    subtitle: AppLocalizations.of(context)?.manageRecyclables ?? 'Gestionar reciclables',
                    icon: Icons.recycling,
                    color: Colors.green,
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const MaterialesListScreen(),
                        ),
                      );
                    },
                  ),
                  _buildMenuCard(
                    title: AppLocalizations.of(context)?.users ?? 'Usuarios',
                    subtitle: AppLocalizations.of(context)?.manageUsers ?? 'Administrar usuarios',
                    icon: Icons.people,
                    color: Colors.orange,
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const UsuariosListScreen(),
                        ),
                      );
                    },
                  ),
                  _buildMenuCard(
                    title: AppLocalizations.of(context)?.schedules ?? 'Horarios',
                    subtitle: AppLocalizations.of(context)?.collectionSchedules ?? 'Horarios de recolección',
                    icon: Icons.schedule,
                    color: Colors.purple,
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const HorariosScreen(),
                        ),
                      );
                    },
                  ),
                  _buildMenuCard(
                    title: AppLocalizations.of(context)?.availability ?? 'Disponibilidad',
                    subtitle: AppLocalizations.of(context)?.statesAndMunicipalities ?? 'Estados y municipios',
                    icon: Icons.map,
                    color: Colors.teal,
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const DisponibilidadScreen(),
                        ),
                      );
                    },
                  ),
                  _buildMenuCard(
                    title: AppLocalizations.of(context)?.configuration ?? 'Configuración',
                    subtitle: AppLocalizations.of(context)?.systemSettings ?? 'Ajustes del sistema',
                    icon: Icons.settings,
                    color: Colors.grey,
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => const ConfiguracionScreen(),
                        ),
                      );
                    },
                  ),
                ],
              ),
              const SizedBox(height: 24),

              // Acciones rápidas
              Text(
                AppLocalizations.of(context)?.quickActions ?? 'Acciones Rápidas',
                style: TextStyle(
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.grey.withValues(alpha: 0.1),
                      spreadRadius: 1,
                      blurRadius: 5,
                      offset: const Offset(0, 3),
                    ),
                  ],
                ),
                child: Column(
                  children: [
                    ListTile(
                      leading: Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: Colors.red.withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: const Icon(
                          Icons.cleaning_services,
                          color: Colors.red,
                        ),
                      ),
                      title: Text(AppLocalizations.of(context)?.cleanInactiveUsers ?? 'Limpiar usuarios inactivos'),
                      subtitle: Text(AppLocalizations.of(context)?.deleteInactiveAccounts ?? 'Eliminar cuentas con 3+ meses de inactividad'),
                      trailing: const Icon(Icons.arrow_forward_ios),
                      onTap: () {
                        _showInactivityCleanupDialog();
                      },
                    ),
                    const Divider(),
                    ListTile(
                      leading: Container(
                        padding: const EdgeInsets.all(8),
                        decoration: BoxDecoration(
                          color: Colors.blue.withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: const Icon(
                          Icons.backup,
                          color: Colors.blue,
                        ),
                      ),
                      title: Text(AppLocalizations.of(context)?.dataBackup ?? 'Respaldo de datos'),
                      subtitle: Text(AppLocalizations.of(context)?.exportSystemInfo ?? 'Exportar información del sistema'),
                      trailing: const Icon(Icons.arrow_forward_ios),
                      onTap: () {
                        // TODO: Implementar respaldo
                      },
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  void _showInactivityCleanupDialog() {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(AppLocalizations.of(context)?.cleanInactiveUsers ?? 'Limpiar usuarios inactivos'),
        content: Text(
          AppLocalizations.of(context)?.sureDeleteInactive ?? '¿Está seguro de eliminar todas las cuentas con más de 3 meses de inactividad?\n\nEsta acción no se puede deshacer.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: Text(AppLocalizations.of(context)?.cancel ?? 'Cancelar'),
          ),
          ElevatedButton(
            onPressed: () async {
              Navigator.pop(context);
              _performInactivityCleanup();
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red,
            ),
            child: Text(
              AppLocalizations.of(context)?.delete ?? 'Eliminar',
              style: TextStyle(color: Colors.white),
            ),
          ),
        ],
      ),
    );
  }

  void _performInactivityCleanup() async {
    // TODO: Implementar limpieza de usuarios inactivos
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(AppLocalizations.of(context)?.cleaningStarted ?? 'Limpieza de usuarios inactivos iniciada...'),
      ),
    );
  }
}