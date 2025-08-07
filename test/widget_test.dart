// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:bioway_nueva_version/main.dart';

void main() {
  testWidgets('BioWay app starts with splash screen', (WidgetTester tester) async {
    // Build our app and trigger a frame.
    await tester.pumpWidget(const BioWayApp());

    // Verify that the app starts with splash screen
    // The splash screen should show "BioWay" text
    expect(find.text('BioWay'), findsOneWidget);
    
    // Verify that "Iniciando..." text is present
    expect(find.text('Iniciando...'), findsOneWidget);
  });
}
