package com.haydra.autocare.data.repository

import com.haydra.autocare.data.local.PasswordHasher
import com.haydra.autocare.data.local.db.UserDao
import com.haydra.autocare.data.local.db.UserEntity
import com.haydra.autocare.data.local.db.toModel
import com.haydra.autocare.data.model.User
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementación de [AuthRepository] sobre la base local Room. Las cuentas se
 * guardan en el dispositivo con la contraseña cifrada con [PasswordHasher];
 * la sesión activa se recuerda aparte en SessionStore (DataStore).
 */
class RoomAuthRepository(private val userDao: UserDao) : AuthRepository {

    @Volatile
    private var sesionActual: User? = null

    override suspend fun register(nombre: String, correo: String, password: String): Result<User> = runCatching {
        val correoNormalizado = correo.trim().lowercase()
        if (userDao.getByCorreo(correoNormalizado) != null) {
            throw IllegalStateException("Ya existe una cuenta con ese correo")
        }
        val entidad = UserEntity(
            id = UUID.randomUUID().toString(),
            nombre = nombre.trim(),
            correo = correoNormalizado,
            passwordHash = hashear(password)
        )
        userDao.insert(entidad)
        entidad.toModel().also { sesionActual = it }
    }

    override suspend fun login(correo: String, password: String): Result<User> = runCatching {
        val entidad = userDao.getByCorreo(correo.trim().lowercase())
            ?: throw IllegalStateException("No existe una cuenta con ese correo")
        if (!verificar(password, entidad.passwordHash)) {
            throw IllegalStateException("La contraseña es incorrecta")
        }
        entidad.toModel().also { sesionActual = it }
    }

    override suspend fun restoreSession(userId: String): Result<User> = runCatching {
        val entidad = userDao.getById(userId)
            ?: throw NoSuchElementException("La sesión guardada ya no es válida")
        entidad.toModel().also { sesionActual = it }
    }

    override suspend fun updateProfile(userId: String, nombre: String, correo: String): Result<User> = runCatching {
        val entidad = userDao.getById(userId) ?: throw NoSuchElementException("El usuario no existe")
        val correoNormalizado = correo.trim().lowercase()
        val otro = userDao.getByCorreo(correoNormalizado)
        if (otro != null && otro.id != userId) {
            throw IllegalStateException("Ya existe una cuenta con ese correo")
        }
        val actualizada = entidad.copy(nombre = nombre.trim(), correo = correoNormalizado)
        userDao.update(actualizada)
        actualizada.toModel().also { if (sesionActual?.id == userId) sesionActual = it }
    }

    override suspend fun changePassword(userId: String, currentPassword: String, newPassword: String): Result<Unit> =
        runCatching {
            val entidad = userDao.getById(userId) ?: throw NoSuchElementException("El usuario no existe")
            if (!verificar(currentPassword, entidad.passwordHash)) {
                throw IllegalStateException("La contraseña actual es incorrecta")
            }
            userDao.update(entidad.copy(passwordHash = hashear(newPassword)))
        }

    override suspend fun resetPassword(correo: String, newPassword: String): Result<Unit> = runCatching {
        val entidad = userDao.getByCorreo(correo.trim().lowercase())
            ?: throw IllegalStateException("No existe una cuenta con ese correo")
        userDao.update(entidad.copy(passwordHash = hashear(newPassword)))
    }

    override fun logout() {
        sesionActual = null
    }

    override fun currentUser(): User? = sesionActual

    // PBKDF2 es costoso a propósito: se calcula fuera del hilo principal.
    private suspend fun hashear(password: String) =
        withContext(Dispatchers.Default) { PasswordHasher.hash(password) }

    private suspend fun verificar(password: String, guardado: String) =
        withContext(Dispatchers.Default) { PasswordHasher.verify(password, guardado) }
}
