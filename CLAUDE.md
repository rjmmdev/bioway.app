# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

BioWay is a Flutter application for circular economy and recycling management in Mexico. It connects citizens, collectors, companies, collection centers, and administrators in an organized recycling ecosystem.

## Development Commands

```bash
# Install dependencies
flutter pub get

# Run the application
flutter run

# Build for Android
flutter build apk

# Build for iOS
flutter build ios

# Run tests
flutter test

# Analyze code
flutter analyze

# Format code
flutter format lib/

# Configure Firebase (when needed)
flutterfire configure --project=bioway-mexico
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
- `lib/services/user_session_service.dart` - Session management and user state
- `lib/config/app_environment.dart` - Environment configuration and feature toggles

### Database Structure
Firestore collections:
- `empresas` - Partner companies with materials and zones
- `usuarios` - Multi-role users with profiles
- `materiales` - Recyclable materials catalog
- `horarios_recoleccion` - Collection schedules
- `zonas_habilitadas` - Geographic zones
- `solicitudes_recoleccion` - Collection requests

### Design Mode
The app currently runs in design mode (`designMode: true` in `app_environment.dart`):
- Firebase operations are temporarily disabled
- Free navigation without authentication
- All modules accessible for testing
- Toggle to production mode by setting `designMode: false`

## Code Patterns

### Widget Organization
- Reusable components in `lib/widgets/bioway/`
- Screen-specific widgets in respective screen folders
- Common UI elements use `BioWayColors` from `lib/utils/bioway_colors.dart`

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
4. **Min SDK**: Android 23+ (API level 23)
5. **Color System**: Use predefined colors from `BioWayColors` class
6. **Navigation**: Platform selector supports BioWay and future ECOCE integration

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

### Switch Between Design/Production Mode
Edit `lib/config/app_environment.dart`:
```dart
static const bool designMode = false; // Set to false for production
```

### Add New User Type
1. Add role constant in user model
2. Create screen folder in `lib/screens/`
3. Update navigation logic in login/register screens
4. Add role-specific Firebase rules