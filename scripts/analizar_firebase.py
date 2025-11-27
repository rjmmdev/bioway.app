#!/usr/bin/env python3
"""
Script de Análisis de Firebase - BioWay
Analiza las colecciones de Firestore y sugiere qué eliminar
"""

import json

# Este script requiere credenciales de Firebase Admin SDK
# Descarga el archivo JSON de credenciales desde:
# https://console.firebase.google.com/project/software-4e6b6/settings/serviceaccounts/adminsdk

print("📊 SCRIPT DE ANÁLISIS DE FIREBASE")
print("="*80)
print("\n⚠️  Para ejecutar este script necesitas:")
print("1. Instalar: pip install firebase-admin")
print("2. Descargar credenciales de servicio:")
print("   https://console.firebase.google.com/project/software-4e6b6/settings/serviceaccounts/adminsdk")
print("3. Guardar como: firebase-admin-key.json")
print("\nEjemplo de uso:")
print("  python3 scripts/analizar_firebase.py")
print("\n" + "="*80)

try:
    import firebase_admin
    from firebase_admin import credentials, firestore

    # Inicializar Firebase Admin
    cred = credentials.Certificate('firebase-admin-key.json')
    firebase_admin.initialize_app(cred)

    db = firestore.client()

    # Colecciones a analizar
    colecciones = ['UsersInAct', 'Recolectores', 'CentrosDeAcopio', 'Horarios']

    print("\n📋 ANÁLISIS DE COLECCIONES:")
    print("="*80)

    usuarios_mantener = ['maestro@bioway.com.mx', 'maestro@ecoce.mx']

    for coleccion in colecciones:
        print(f"\n📂 {coleccion}/")
        print("-"*80)

        docs = db.collection(coleccion).stream()
        total = 0
        a_mantener = 0
        a_eliminar = 0

        lista_eliminar = []
        lista_mantener = []

        for doc in docs:
            total += 1
            data = doc.to_dict()
            doc_id = doc.id

            # Verificar si debe mantenerse
            mantener = False

            # Verificar por email
            if 'email' in data and data['email'] in usuarios_mantener:
                mantener = True
                a_mantener += 1
                lista_mantener.append(f"{doc_id} ({data.get('email', 'sin email')})")
            else:
                a_eliminar += 1
                email = data.get('email', 'sin email')
                nombre = data.get('nombre', data.get('name', 'sin nombre'))
                lista_eliminar.append(f"{doc_id} - {nombre} ({email})")

        print(f"Total documentos: {total}")
        print(f"A mantener: {a_mantener}")
        print(f"A eliminar: {a_eliminar}")

        if lista_mantener:
            print(f"\n✅ MANTENER ({len(lista_mantener)}):")
            for item in lista_mantener[:5]:
                print(f"  - {item}")
            if len(lista_mantener) > 5:
                print(f"  ... y {len(lista_mantener) - 5} más")

        if lista_eliminar:
            print(f"\n🗑️  SUGERIDOS PARA ELIMINAR ({len(lista_eliminar)}):")
            for item in lista_eliminar[:10]:
                print(f"  - {item}")
            if len(lista_eliminar) > 10:
                print(f"  ... y {len(lista_eliminar) - 10} más")

    print("\n" + "="*80)
    print("✅ Análisis completado")
    print("\n💡 Para eliminar, usa:")
    print('   firebase firestore:delete "UsersInAct/documentId" --force')

except ImportError:
    print("\n❌ firebase-admin no está instalado")
    print("Instala con: pip3 install firebase-admin")
except FileNotFoundError:
    print("\n❌ No se encontró firebase-admin-key.json")
    print("Descarga desde Firebase Console → Project Settings → Service Accounts")
except Exception as e:
    print(f"\n❌ Error: {e}")
