package com.haydra.autocare.data.repository

import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.haydra.autocare.data.local.PasswordHasher
import com.haydra.autocare.data.local.db.UserDao
import com.haydra.autocare.data.local.db.UserEntity
import com.haydra.autocare.data.local.db.toModel
import com.haydra.autocare.data.model.User
import com.haydra.autocare.data.privacidad.AvisoPrivacidad
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Implementación de [AuthRepository] con Firebase Authentication (correo y contraseña).
 *
 * - Firebase valida la cuenta y envía los correos de restablecimiento.
 * - Cloud Firestore guarda el perfil en `usuarios/{uid}` (nombre, correo y el id local).
 * - Room conserva la fila del usuario para que sus vehículos sigan ligados por llave foránea.
 *
 * Las cuentas creadas antes de Firebase (incluida la demo) se migran solas: si Firebase no
 * reconoce el correo pero la contraseña coincide con la guardada en el teléfono, se crea la
 * cuenta en Firebase y se conserva el mismo id local, así no se pierde ningún registro.
 */
class FirebaseAuthRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userDao: UserDao
) : AuthRepository {

    @Volatile
    private var sesionActual: User? = null

    init {
        auth.setLanguageCode("es")
    }

    override suspend fun register(nombre: String, correo: String, password: String): Result<User> = conFirebase {
        val correoNormalizado = normalizar(correo)
        val local = userDao.getByCorreo(correoNormalizado)
        if (local != null && !coincide(password, local.passwordHash)) {
            throw IllegalStateException("Ya existe una cuenta con ese correo")
        }
        val usuario = auth.createUserWithEmailAndPassword(correoNormalizado, password).await().user
            ?: throw IllegalStateException(MensajesFirebase.GENERICO)
        // Solo se llega aquí después de aceptar el aviso de privacidad en el formulario de registro.
        vincular(usuario, nombre.trim(), password, avisoAceptado = true)
    }

    override suspend fun login(correo: String, password: String): Result<User> = conFirebase {
        val correoNormalizado = normalizar(correo)
        val usuario = try {
            auth.signInWithEmailAndPassword(correoNormalizado, password).await().user
        } catch (e: FirebaseAuthException) {
            if (e.errorCode !in MensajesFirebase.CREDENCIALES_INVALIDAS) throw e
            migrarCuentaLocal(correoNormalizado, password) ?: throw e
        } ?: throw IllegalStateException(MensajesFirebase.GENERICO)
        vincular(usuario, nombre = null, password = password)
    }

    override suspend fun restoreSession(userId: String): Result<User> = conFirebase {
        val usuario = auth.currentUser ?: throw IllegalStateException("Tu sesión expiró, vuelve a iniciar sesión")
        val entidad = userDao.getById(userId) ?: throw NoSuchElementException("La sesión guardada ya no es válida")
        if (entidad.correo != usuario.email?.let(::normalizar)) {
            throw IllegalStateException("La sesión guardada ya no es válida")
        }
        entidad.toModel().also { sesionActual = it }
    }

    override suspend fun updateProfile(userId: String, nombre: String, correo: String): Result<User> = conFirebase {
        val usuario = usuarioFirebase()
        val entidad = userDao.getById(userId) ?: throw NoSuchElementException("El usuario no existe")
        val correoNuevo = normalizar(correo)
        if (correoNuevo != entidad.correo) {
            val otro = userDao.getByCorreo(correoNuevo)
            if (otro != null && otro.id != userId) throw IllegalStateException("Ya existe una cuenta con ese correo")
            // Firebase solo cambia el correo después de que el usuario abre el enlace de verificación.
            usuario.verifyBeforeUpdateEmail(correoNuevo).await()
            throw IllegalStateException(
                "Te enviamos un enlace a $correoNuevo. Ábrelo para confirmar el cambio y vuelve a iniciar sesión."
            )
        }
        val actualizada = entidad.copy(nombre = nombre.trim())
        userDao.update(actualizada)
        guardarPerfil(usuario.uid, actualizada)
        actualizada.toModel().also { if (sesionActual?.id == userId) sesionActual = it }
    }

    override suspend fun changePassword(userId: String, currentPassword: String, newPassword: String): Result<Unit> =
        conFirebase {
            val usuario = reautenticar(currentPassword)
            usuario.updatePassword(newPassword).await()
            userDao.getById(userId)?.let { userDao.update(it.copy(passwordHash = hashear(newPassword))) }
        }

    override suspend fun sendPasswordReset(correo: String): Result<Unit> = conFirebase {
        auth.sendPasswordResetEmail(normalizar(correo)).await()
        Unit
    }

    override suspend fun confirmPassword(password: String): Result<Unit> = conFirebase {
        reautenticar(password)
        Unit
    }

    /**
     * Firebase pide un inicio de sesión reciente para borrar una cuenta, por eso se vuelve a
     * validar la contraseña. El perfil se borra antes que la cuenta porque después las reglas
     * de Firestore ya no dejarían tocarlo.
     */
    override suspend fun deleteAccount(userId: String, password: String): Result<Unit> = conFirebase {
        val usuario = reautenticar(password)
        firestore.collection(COLECCION_USUARIOS).document(usuario.uid).delete().await()
        usuario.delete().await()
        // Room borra en cascada los vehículos y todos sus registros.
        userDao.deleteById(userId)
        sesionActual = null
    }

    override fun logout() {
        auth.signOut()
        sesionActual = null
    }

    override fun currentUser(): User? = sesionActual

    /** Crea en Firebase una cuenta que hasta ahora solo existía en el teléfono. */
    private suspend fun migrarCuentaLocal(correo: String, password: String): FirebaseUser? {
        val local = userDao.getByCorreo(correo) ?: return null
        if (!coincide(password, local.passwordHash)) return null
        return try {
            auth.createUserWithEmailAndPassword(correo, password).await().user
        } catch (_: FirebaseAuthUserCollisionException) {
            null // La cuenta ya existe en Firebase con otra contraseña.
        }
    }

    /**
     * Relaciona la cuenta de Firebase con la fila local del usuario (la crea si no existe)
     * y actualiza su perfil en Firestore.
     */
    private suspend fun vincular(
        usuario: FirebaseUser,
        nombre: String?,
        password: String,
        avisoAceptado: Boolean = false
    ): User {
        val correo = normalizar(usuario.email ?: throw IllegalStateException(MensajesFirebase.GENERICO))
        val perfil = runCatching {
            firestore.collection(COLECCION_USUARIOS).document(usuario.uid).get().await()
        }.getOrNull()
        val idLocal = perfil?.getString("localId")
        val local = idLocal?.let { userDao.getById(it) } ?: userDao.getByCorreo(correo) ?: userDao.getById(usuario.uid)
        val nombreFinal = nombre
            ?: perfil?.getString("nombre")
            ?: local?.nombre
            ?: usuario.displayName
            ?: correo.substringBefore('@')

        val entidad = if (local != null) {
            local.copy(nombre = nombreFinal, correo = correo, passwordHash = hashear(password)).also { userDao.update(it) }
        } else {
            UserEntity(id = usuario.uid, nombre = nombreFinal, correo = correo, passwordHash = hashear(password))
                .also { userDao.insert(it) }
        }
        guardarPerfil(usuario.uid, entidad, avisoAceptado)
        return entidad.toModel().also { sesionActual = it }
    }

    /**
     * Sin `await`: Firestore guarda el cambio en su caché y lo sube cuando hay conexión.
     * Con [avisoAceptado] queda constancia de qué versión del aviso de privacidad aceptó y cuándo.
     */
    private fun guardarPerfil(uid: String, entidad: UserEntity, avisoAceptado: Boolean = false) {
        val datos = mutableMapOf<String, Any>(
            "nombre" to entidad.nombre,
            "correo" to entidad.correo,
            "localId" to entidad.id,
            "actualizado" to FieldValue.serverTimestamp()
        )
        if (avisoAceptado) {
            datos["avisoPrivacidad"] = mapOf(
                "version" to AvisoPrivacidad.VERSION,
                "aceptado" to FieldValue.serverTimestamp()
            )
        }
        firestore.collection(COLECCION_USUARIOS).document(uid).set(datos, SetOptions.merge())
    }

    private fun usuarioFirebase(): FirebaseUser =
        auth.currentUser ?: throw IllegalStateException("Tu sesión expiró, vuelve a iniciar sesión")

    private suspend fun reautenticar(password: String): FirebaseUser {
        val usuario = usuarioFirebase()
        val correo = usuario.email ?: throw IllegalStateException(MensajesFirebase.GENERICO)
        usuario.reauthenticate(EmailAuthProvider.getCredential(correo, password)).await()
        return usuario
    }

    private fun normalizar(correo: String) = correo.trim().lowercase()

    // PBKDF2 es costoso a propósito: se calcula fuera del hilo principal.
    private suspend fun hashear(password: String) =
        withContext(Dispatchers.Default) { PasswordHasher.hash(password) }

    private suspend fun coincide(password: String, guardado: String) =
        guardado.isNotEmpty() && withContext(Dispatchers.Default) { PasswordHasher.verify(password, guardado) }

    /** Ejecuta [bloque] y convierte cualquier error de Firebase en un mensaje en español. */
    private inline fun <T> conFirebase(bloque: () -> T): Result<T> = try {
        Result.success(bloque())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(IllegalStateException(MensajesFirebase.de(e), e))
    }

    companion object {
        const val COLECCION_USUARIOS = "usuarios"
    }
}
