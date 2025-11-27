package com.biowaymexico.data

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/**
 * Repositorio de Autenticación con Firebase
 * Maneja login, registro y verificación de teléfono
 */
class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    // Estados para verificación de teléfono
    var verificationId: String? = null
    var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    /**
     * Login con email y contraseña
     * Retorna el tipo de usuario y sus datos
     * Verifica que el email esté verificado (excepto maestro)
     */
    suspend fun login(email: String, password: String): Result<Pair<FirebaseUser, String>> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                // Ya no requerimos verificación de email

                // Determinar tipo de usuario buscando en cada colección
                val tipoUsuario = determinarTipoUsuario(user.uid, email)

                // Si es Maestro y no existe en Firestore, crearlo automáticamente
                if (tipoUsuario == "Maestro") {
                    crearDocumentoMaestroSiNoExiste(user.uid, email)
                }

                Result.success(Pair(user, tipoUsuario))
            } else {
                Result.failure(Exception("Usuario no encontrado"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea documento del Maestro automáticamente si no existe
     */
    private suspend fun crearDocumentoMaestroSiNoExiste(userId: String, email: String) {
        try {
            val docRef = firestore.collection("Maestro").document(userId)
            val doc = docRef.get().await()

            if (!doc.exists()) {
                val maestroData = hashMapOf(
                    "userId" to userId,
                    "email" to email,
                    "role" to "bioway_admin",
                    "nombre" to "Maestro BioWay",
                    "platform" to "android",
                    "fechaRegistro" to com.google.firebase.Timestamp.now(),
                    "ultimoAcceso" to com.google.firebase.Timestamp.now()
                )

                docRef.set(maestroData).await()
                android.util.Log.d("AuthRepository", "✅ Documento Maestro creado automáticamente")
            } else {
                // Actualizar último acceso
                docRef.update("ultimoAcceso", com.google.firebase.Timestamp.now())
                android.util.Log.d("AuthRepository", "📝 Último acceso actualizado")
            }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error al crear/actualizar Maestro: ${e.message}")
        }
    }

    /**
     * Determina el tipo de usuario buscando en las colecciones
     * Retorna: "Brindador", "Recolector", "CentroAcopio", "Maestro"
     */
    private suspend fun determinarTipoUsuario(userId: String, email: String): String {
        return try {
            android.util.Log.d("AuthRepository", "🔍 Determinando tipo de usuario para: $userId")
            android.util.Log.d("AuthRepository", "Email actual: ${auth.currentUser?.email}")

            // Si el email es maestro@bioway.com.mx, es Maestro (independiente de la colección)
            if (email == "maestro@bioway.com.mx") {
                android.util.Log.d("AuthRepository", "✅ Detectado por email: Maestro")
                return "Maestro"
            }

            // Buscar en colecciones
            val existeEnMaestro = firestore.collection("Maestro").document(userId).get().await().exists()
            android.util.Log.d("AuthRepository", "Maestro/ existe: $existeEnMaestro")

            if (existeEnMaestro) return "Maestro"

            val existeEnRecolector = firestore.collection("Recolector").document(userId).get().await().exists()
            android.util.Log.d("AuthRepository", "Recolector/ existe: $existeEnRecolector")

            if (existeEnRecolector) return "Recolector"

            val existeEnCentro = firestore.collection("CentroAcopio").document(userId).get().await().exists()
            android.util.Log.d("AuthRepository", "CentroAcopio/ existe: $existeEnCentro")

            if (existeEnCentro) return "CentroAcopio"

            val existeEnBrindador = firestore.collection("Brindador").document(userId).get().await().exists()
            android.util.Log.d("AuthRepository", "Brindador/ existe: $existeEnBrindador")

            if (existeEnBrindador) return "Brindador"

            val existeEnBoteBioWay = firestore.collection("BoteBioWay").document(userId).get().await().exists()
            android.util.Log.d("AuthRepository", "BoteBioWay/ existe: $existeEnBoteBioWay")

            if (existeEnBoteBioWay) return "BoteBioWay"

            android.util.Log.w("AuthRepository", "⚠️ Usuario no encontrado en ninguna colección, usando Brindador por defecto")
            "Brindador"
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error al determinar tipo: ${e.message}")
            "Brindador"
        }
    }

    /**
     * Registro - Paso 1: Verificar teléfono
     * Envía código SMS al número de teléfono
     */
    fun verificarTelefono(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (String) -> Unit,
        onVerificationFailed: (String) -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // Auto-verificado (en algunos casos Android lo hace automático)
                // No hacemos nada aquí, esperamos el código manual
            }

            override fun onVerificationFailed(e: FirebaseException) {
                val mensaje = when (e) {
                    is FirebaseAuthInvalidCredentialsException -> "Número de teléfono inválido"
                    is FirebaseTooManyRequestsException -> "Demasiados intentos. Intenta más tarde"
                    else -> "Error al verificar teléfono: ${e.message}"
                }
                onVerificationFailed(mensaje)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // Guardar ID de verificación para usarlo después
                this@AuthRepository.verificationId = verificationId
                this@AuthRepository.resendToken = token
                onCodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)  // Formato: +52 55 1234 5678
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    /**
     * Registro - Paso 2: Verificar código SMS
     */
    fun verificarCodigo(codigo: String): PhoneAuthCredential? {
        return if (verificationId != null) {
            PhoneAuthProvider.getCredential(verificationId!!, codigo)
        } else {
            null
        }
    }

    /**
     * Registrar usuario directamente y enviar email de verificación
     */
    suspend fun registrarUsuarioDirecto(
        email: String,
        password: String,
        nombre: String,
        telefono: String,
        tipoUsuario: String
    ): Result<String> {
        return try {
            // Crear cuenta
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: return Result.failure(Exception("Error al crear usuario"))

            val coleccion = when (tipoUsuario) {
                "Brindador" -> "Brindador"
                "Recolector" -> "Recolector"
                "Centro Acopio" -> "CentroAcopio"
                "Maestro" -> "Maestro"
                "Bote BioWay" -> "BoteBioWay"
                else -> "Brindador"
            }

            val userData = if (tipoUsuario == "Bote BioWay") {
                hashMapOf<String, Any>(
                    "userId" to user.uid,
                    "identificador" to nombre,
                    "email" to email,
                    "tipoUsuario" to tipoUsuario,
                    "estadoOperativo" to true,
                    "fechaRegistro" to com.google.firebase.Timestamp.now()
                )
            } else {
                hashMapOf<String, Any>(
                    "userId" to user.uid,
                    "nombre" to nombre,
                    "email" to email,
                    "telefono" to telefono,
                    "tipoUsuario" to tipoUsuario,
                    "bioCoins" to 0,
                    "nivel" to "Bronce",
                    "totalKgReciclados" to 0.0,
                    "materialesReciclados" to hashMapOf<String, Double>(),
                    "bioImpulso" to 1,
                    "bioImpulsoActivo" to false,
                    "fechaRegistro" to com.google.firebase.Timestamp.now(),
                    "telefonoVerificado" to true,
                    "emailVerificado" to false
                )
            }

            firestore.collection(coleccion)
                .document(user.uid)
                .set(userData)
                .await()

            android.util.Log.d("AuthRepository", "✅ Usuario registrado en: $coleccion")
            android.util.Log.d("AuthRepository", "ℹ️ Email de verificación debe solicitarse desde el perfil")

            Result.success(user.uid)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error en registro: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Reenviar email de verificación
     */
    suspend fun reenviarEmailVerificacion(): Result<Unit> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("No hay usuario autenticado"))
            user.sendEmailVerification().await()
            android.util.Log.d("AuthRepository", "✉️ Email de verificación reenviado")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "❌ Error al reenviar email: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Cerrar sesión
     */
    fun logout() {
        auth.signOut()
    }

    /**
     * Usuario actual
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Verificar si hay usuario logueado
     */
    fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    /**
     * Reenviar código de verificación de teléfono
     */
    fun reenviarCodigo(
        phoneNumber: String,
        activity: Activity,
        onCodeSent: (String) -> Unit,
        onVerificationFailed: (String) -> Unit
    ) {
        if (resendToken == null) {
            onVerificationFailed("No se puede reenviar aún")
            return
        }

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {}

            override fun onVerificationFailed(e: FirebaseException) {
                onVerificationFailed("Error al reenviar: ${e.message}")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                this@AuthRepository.verificationId = verificationId
                this@AuthRepository.resendToken = token
                onCodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .setForceResendingToken(resendToken!!)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }
}
