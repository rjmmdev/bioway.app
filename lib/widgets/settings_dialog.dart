import 'package:flutter/material.dart';
import 'package:easy_localization/easy_localization.dart';
import '../utils/colors.dart';

class SettingsDialog extends StatefulWidget {
  const SettingsDialog({super.key});

  @override
  State<SettingsDialog> createState() => _SettingsDialogState();
}

class _SettingsDialogState extends State<SettingsDialog> {
  late String _selectedLanguage;

  @override
  void initState() {
    super.initState();
  }
  
  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    // Get current locale after context is available
    _selectedLanguage = context.locale.languageCode;
  }

  Future<void> _changeLanguage(String langCode) async {
    if (langCode == 'en') {
      await context.setLocale(const Locale('en', 'US'));
    } else {
      await context.setLocale(const Locale('es', 'MX'));
    }
    setState(() {
      _selectedLanguage = langCode;
    });
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(20),
      ),
      child: Container(
        constraints: const BoxConstraints(maxWidth: 400),
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header
            Row(
              children: [
                Icon(
                  Icons.settings,
                  color: BioWayColors.primaryGreen,
                  size: 28,
                ),
                const SizedBox(width: 12),
                Text(
                  'settings'.tr(),
                  style: const TextStyle(
                    fontSize: 24,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Spacer(),
                IconButton(
                  icon: const Icon(Icons.close),
                  onPressed: () => Navigator.of(context).pop(),
                  color: Colors.grey,
                ),
              ],
            ),
            const SizedBox(height: 24),
            
            // Language Section
            Text(
              'language'.tr(),
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.w600,
                color: BioWayColors.textGrey,
              ),
            ),
            const SizedBox(height: 12),
            
            // Language Options
            _buildLanguageOption(
              title: 'Español',
              subtitle: 'México',
              flag: '🇲🇽',
              langCode: 'es',
              isSelected: _selectedLanguage == 'es',
            ),
            const SizedBox(height: 8),
            _buildLanguageOption(
              title: 'English',
              subtitle: 'United States',
              flag: '🇺🇸',
              langCode: 'en',
              isSelected: _selectedLanguage == 'en',
            ),
            
            const SizedBox(height: 24),
            
            // Info text
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: BioWayColors.info.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(
                  color: BioWayColors.info.withValues(alpha: 0.3),
                ),
              ),
              child: Row(
                children: [
                  Icon(
                    Icons.info_outline,
                    color: BioWayColors.info,
                    size: 20,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      'can_change_later'.tr(),
                      style: TextStyle(
                        fontSize: 13,
                        color: BioWayColors.textGrey,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: 24),
            
            // Action buttons
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: Text(
                    'close'.tr(),
                    style: TextStyle(
                      color: BioWayColors.textGrey,
                      fontSize: 16,
                    ),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
  
  Widget _buildLanguageOption({
    required String title,
    required String subtitle,
    required String flag,
    required String langCode,
    required bool isSelected,
  }) {
    return InkWell(
      onTap: () => _changeLanguage(langCode),
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.all(16),
        decoration: BoxDecoration(
          color: isSelected 
              ? BioWayColors.primaryGreen.withValues(alpha: 0.1)
              : Colors.grey.withValues(alpha: 0.05),
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: isSelected 
                ? BioWayColors.primaryGreen 
                : Colors.grey.withValues(alpha: 0.2),
            width: isSelected ? 2 : 1,
          ),
        ),
        child: Row(
          children: [
            Text(
              flag,
              style: const TextStyle(fontSize: 28),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: TextStyle(
                      fontSize: 16,
                      fontWeight: FontWeight.bold,
                      color: isSelected 
                          ? BioWayColors.primaryGreen 
                          : BioWayColors.textGrey,
                    ),
                  ),
                  Text(
                    subtitle,
                    style: TextStyle(
                      fontSize: 12,
                      color: Colors.grey.shade600,
                    ),
                  ),
                ],
              ),
            ),
            if (isSelected)
              Icon(
                Icons.check_circle,
                color: BioWayColors.primaryGreen,
                size: 24,
              ),
          ],
        ),
      ),
    );
  }
}