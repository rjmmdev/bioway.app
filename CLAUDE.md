# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BioWay is a Flutter application for circular economy and recycling management in Mexico. It connects citizens, collectors, companies, collection centers, and administrators in an organized recycling ecosystem.

**Key Features:**
- Multi-role authentication system with 5 user types
- AI-powered waste detection using Google ML Kit
- Real-time location tracking and route optimization
- Gamification with points and rewards system
- Multi-language support (Spanish primary, English fallback)

## Development Commands

```bash
# Install dependencies
flutter pub get

# Run the application
flutter run

# Build for Android
flutter build apk
flutter build appbundle  # For Play Store

# Build for iOS
flutter build ios

# Run tests
flutter test
flutter test test/specific_test.dart  # Run specific test

# Analyze code
flutter analyze

# Format code
flutter format lib/

# Clean build artifacts
flutter clean

# Configure Firebase (when needed)
flutterfire configure --project=bioway-mexico

# Deploy Firebase rules
firebase deploy --only firestore
```

## Architecture

### User Types & Modules
The app supports 5 distinct user types, each with dedicated screens in `lib/screens/`:
- **brindador/** - Citizens/waste providers
- **recolector/** - Waste collectors
- **empresa/** - Partner companies
- **centro_acopio/** - Collection centers
- **maestro/** - Admin panel

### Key Services
- `lib/services/firebase/firebase_manager.dart` - Main Firebase integration (singleton pattern)
- `lib/services/user_session_service.dart` - Session management using SharedPreferences
- `lib/services/bioway/` - BioWay-specific business logic services

### Database Structure
Firestore collections:
- `empresas` - Partner companies with materials and zones
- `usuarios` - Multi-role users with profiles
- `materiales` - Recyclable materials catalog
- `horarios_recoleccion` - Collection schedules
- `zonas_habilitadas` - Geographic zones
- `solicitudes_recoleccion` - Collection requests

### Environment Configuration
The app supports different configuration modes for development and production:
- Test mode allows navigation without authentication
- All user modules can be accessed for testing
- Firebase operations can be toggled based on environment

## Code Patterns

### Widget Organization
- Reusable components in `lib/widgets/bioway/`
- Screen-specific widgets in respective screen folders
- Common UI elements use `BioWayColors` from `lib/utils/colors.dart`

### State Management
Uses Provider pattern for state management. Key providers:
- User session state
- Firebase data providers
- Navigation state

### Firebase Integration
All Firebase operations go through `FirebaseManager` singleton:
```dart
final firebaseManager = FirebaseManager();
// Use firebaseManager.db for Firestore
// Use firebaseManager.auth for Authentication
```

### Error Handling
Spanish error messages for user-facing errors. Firebase auth errors are mapped to Spanish in `firebase_manager.dart`.

## Testing

Run tests with:
```bash
flutter test
```

Currently minimal test coverage. Focus testing on:
- Authentication flows
- Firebase operations
- Critical business logic in services

## Important Notes

1. **Language**: All user-facing text must be in Spanish (Mexican Spanish)
2. **Firebase Project**: `bioway-mexico` - ensure Firebase CLI is configured
3. **Platform Support**: iOS and Android only
4. **Min SDK**: Android 23+ (API level 23), Flutter SDK ^3.8.1
5. **Color System**: Use predefined colors from `BioWayColors` class in `lib/utils/colors.dart`
6. **Navigation**: Platform selector supports BioWay and future ECOCE integration
7. **AI Features**: Google ML Kit for image labeling and text recognition
8. **Localization**: Translation files in `assets/translations/` (es-MX primary, en-US fallback)

## Common Tasks

### Add New Screen
1. Create screen file in appropriate module folder under `lib/screens/`
2. Add navigation route in parent screen or navigation manager
3. Use consistent BioWay styling and colors

### Modify Firebase Schema
1. Update Firestore rules in `firestore.rules`
2. Update indexes in `firestore.indexes.json`
3. Deploy with `firebase deploy --only firestore`
4. Update models in `lib/models/`

### Configure Test/Production Mode
Update environment configuration to switch between test and production modes for authentication and Firebase operations.

### Add New User Type
1. Add role constant in user model
2. Create screen folder in `lib/screens/`
3. Update navigation logic in login/register screens
4. Add role-specific Firebase rules