#!/usr/bin/env python3
"""
Script para descargar y preparar modelos de clasificación de residuos
para integración en BioWay Android
"""

import os
import sys
import shutil
from pathlib import Path

def print_header(text):
    """Imprime un encabezado bonito"""
    print("\n" + "="*60)
    print(f"  {text}")
    print("="*60 + "\n")

def check_dependencies():
    """Verifica que las dependencias estén instaladas"""
    try:
        import tensorflow as tf
        print(f"✅ TensorFlow {tf.__version__} instalado")
    except ImportError:
        print("❌ TensorFlow no está instalado")
        print("   Ejecuta: pip install tensorflow")
        sys.exit(1)

    try:
        import huggingface_hub
        print(f"✅ Hugging Face Hub instalado")
    except ImportError:
        print("⚠️  Hugging Face Hub no está instalado")
        print("   Instalando...")
        os.system("pip install huggingface-hub -q")

def download_from_huggingface():
    """Descarga modelo de Hugging Face"""
    print_header("Descarga desde Hugging Face")

    print("Modelos disponibles:")
    print("1. ahmzakif/TrashNet-Classification (MobileNetV2, recomendado)")
    print("2. aculotta/Trashnet (ResNet)")
    print("3. edwinpalegre/vit-base-trashnet-demo (ViT, alta precisión)")

    choice = input("\nElige un modelo (1-3): ").strip()

    models = {
        "1": "ahmzakif/TrashNet-Classification",
        "2": "aculotta/Trashnet",
        "3": "edwinpalegre/vit-base-trashnet-demo"
    }

    repo_id = models.get(choice)
    if not repo_id:
        print("❌ Opción inválida")
        return None

    print(f"\n📥 Descargando {repo_id}...")

    try:
        from huggingface_hub import hf_hub_download, list_repo_files

        # Listar archivos disponibles
        files = list_repo_files(repo_id)
        tflite_files = [f for f in files if f.endswith('.tflite')]
        h5_files = [f for f in files if f.endswith('.h5')]
        pb_files = [f for f in files if f.endswith('.pb')]

        if tflite_files:
            print(f"✅ Modelo TFLite encontrado: {tflite_files[0]}")
            model_path = hf_hub_download(repo_id=repo_id, filename=tflite_files[0])
            return model_path

        elif h5_files:
            print(f"⚠️  Solo encontrado modelo Keras (.h5): {h5_files[0]}")
            print("   Se necesita conversión a TFLite...")
            model_path = hf_hub_download(repo_id=repo_id, filename=h5_files[0])
            return convert_keras_to_tflite(model_path)

        else:
            print("❌ No se encontró modelo compatible")
            print(f"   Archivos disponibles: {files}")
            return None

    except Exception as e:
        print(f"❌ Error al descargar: {e}")
        return None

def convert_keras_to_tflite(keras_model_path):
    """Convierte modelo Keras a TFLite"""
    print_header("Conversión Keras → TFLite")

    import tensorflow as tf

    try:
        print(f"📂 Cargando modelo desde {keras_model_path}...")
        model = tf.keras.models.load_model(keras_model_path)

        print("🔧 Convirtiendo a TFLite...")
        converter = tf.lite.TFLiteConverter.from_keras_model(model)

        # Optimización (cuantización)
        print("⚡ Aplicando cuantización para reducir tamaño...")
        converter.optimizations = [tf.lite.Optimize.DEFAULT]

        tflite_model = converter.convert()

        # Guardar
        output_path = keras_model_path.replace('.h5', '.tflite')
        with open(output_path, 'wb') as f:
            f.write(tflite_model)

        size_mb = len(tflite_model) / (1024 * 1024)
        print(f"✅ Modelo convertido: {output_path} ({size_mb:.2f} MB)")

        return output_path

    except Exception as e:
        print(f"❌ Error en conversión: {e}")
        return None

def copy_to_android_project(model_path):
    """Copia el modelo al proyecto Android"""
    print_header("Integración en Proyecto Android")

    # Detectar ruta del proyecto
    current_dir = Path.cwd()
    assets_dir = current_dir / "app" / "src" / "main" / "assets"

    if not assets_dir.exists():
        print(f"❌ No se encontró el directorio assets en: {assets_dir}")
        print(f"   Asegúrate de ejecutar este script desde la raíz del proyecto")
        return False

    # Hacer backup del modelo anterior
    target_path = assets_dir / "modelo_residuos.tflite"
    if target_path.exists():
        backup_path = assets_dir / "modelo_residuos.tflite.backup"
        print(f"💾 Creando backup del modelo anterior...")
        shutil.copy(target_path, backup_path)

    # Copiar nuevo modelo
    print(f"📋 Copiando modelo a {target_path}...")
    shutil.copy(model_path, target_path)

    size_mb = target_path.stat().st_size / (1024 * 1024)
    print(f"✅ Modelo copiado exitosamente ({size_mb:.2f} MB)")

    return True

def generate_labels_file(labels):
    """Genera archivo labels.txt"""
    labels_path = Path("labels_sugeridas.txt")
    with open(labels_path, 'w') as f:
        for i, label in enumerate(labels):
            f.write(f"{i} {label}\n")

    print(f"📝 Archivo de etiquetas creado: {labels_path}")
    return labels_path

def show_integration_instructions(model_path, labels):
    """Muestra instrucciones de integración"""
    print_header("Instrucciones de Integración")

    print("📝 Actualiza el código en ClasificadorResiduos.kt (línea 22):\n")
    print("```kotlin")
    print("private val etiquetas = listOf(")
    for i, label in enumerate(labels):
        print(f'    "{label}",      // {i}')
    print(")")
    print("```\n")

    print("🔄 Compilar y probar:")
    print("   ./gradlew clean assembleDebug")
    print("   ./gradlew installDebug")
    print("\n✅ Listo para probar en la app!")

def main():
    """Función principal"""
    print_header("🚀 Descargador de Modelos BioWay")

    print("Opciones:")
    print("1. Descargar modelo pre-entrenado de Hugging Face")
    print("2. Convertir modelo Keras local a TFLite")
    print("3. Usar Teachable Machine (manual)")
    print("4. Salir")

    choice = input("\nElige una opción (1-4): ").strip()

    if choice == "1":
        check_dependencies()
        model_path = download_from_huggingface()

        if model_path:
            # Etiquetas comunes de TrashNet
            labels = ["Cardboard", "Glass", "Metal", "Paper", "Plastic", "Trash"]

            if copy_to_android_project(model_path):
                generate_labels_file(labels)
                show_integration_instructions(model_path, labels)

    elif choice == "2":
        check_dependencies()
        keras_path = input("Ruta al modelo Keras (.h5): ").strip()

        if not os.path.exists(keras_path):
            print(f"❌ Archivo no encontrado: {keras_path}")
            return

        tflite_path = convert_keras_to_tflite(keras_path)

        if tflite_path:
            labels = input("Etiquetas separadas por coma: ").strip().split(',')
            labels = [l.strip() for l in labels]

            if copy_to_android_project(tflite_path):
                generate_labels_file(labels)
                show_integration_instructions(tflite_path, labels)

    elif choice == "3":
        print_header("Teachable Machine")
        print("📖 Lee la guía completa en: TEACHABLE_MACHINE_GUIA.md")
        print("\n1. Ve a: https://teachablemachine.withgoogle.com/train/image")
        print("2. Entrena tu modelo")
        print("3. Exporta a TensorFlow Lite")
        print("4. Copia model.tflite a app/src/main/assets/modelo_residuos.tflite")
        print("5. Actualiza las etiquetas según labels.txt")
        print("\n✅ ¡Es la opción más fácil y rápida!")

    elif choice == "4":
        print("👋 ¡Hasta luego!")

    else:
        print("❌ Opción inválida")

if __name__ == "__main__":
    main()
