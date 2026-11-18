package com.haydra.autocare.data.repository

import com.haydra.autocare.data.DemoData
import com.haydra.autocare.data.model.User
import java.util.UUID
import kotlinx.coroutines.delay

/**
 * Implementación en memoria de [AuthRepository], usada por las pruebas unitarias (la app
 * usa la versión Room, ver AppContainer). Los usuarios se pierden al cerrar la app.
 * Se precarga la cuenta de demostración (ver [DemoData]).
 */
class FakeAuthRepository : AuthRepository {

    private data class StoredUser(val user: User, val password: String)

    private val usuariosPorId = mutableMapOf<String, StoredUser>()
    private var sesionActual: User? = null

    init {
        val usuarioDemo = User(id = DemoData.USER_ID, nombre = DemoData.NOMBRE, correo = DemoData.CORREO)
        usuariosPorId[DemoData.USER_ID] = StoredUser(usuarioDemo, DemoData.PASSWORD)
    }

    private fun buscarPorCorreo(correo: String): StoredUser? {
        val correoNormalizado = correo.trim().lowercase()
        return usuariosPorId.values.find { it.user.correo == correoNormalizado }
    }

    override suspend fun register(nombre: String, correo: String, password: String): Result<User> {
        delay(300)
        val correoNormalizado = correo.trim().lowercase()
        if (buscarPorCorreo(correoNormalizado) != null) {
            return Result.failure(IllegalStateException("Ya existe una cuenta con ese correo"))
        }
        val nuevoUsuario = User(id = UUID.randomUUID().toString(), nombre = nombre.trim(), correo = correoNormalizado)
        usuariosPorId[nuevoUsuario.id] = StoredUser(nuevoUsuario, password)
        sesionActual = nuevoUsuario
        return Result.success(nuevoUsuario)
    }

    override suspend fun login(correo: String, password: String): Result<User> {
        delay(300)
        val almacenado = buscarPorCorreo(correo)
            ?: return Result.failure(IllegalStateException("No existe una cuenta con ese correo"))
        if (almacenado.password != password) {
            return Result.failure(IllegalStateException("La contraseña es incorrecta"))
        }
        sesionActual = almacenado.user
        return Result.success(almacenado.user)
    }

    override suspend fun restoreSession(userId: String): Result<User> {
        delay(150)
        val almacenado = usuariosPorId[userId]
            ?: return Result.failure(NoSuchElementException("La sesión guardada ya no es válida"))
        sesionActual = almacenado.user
        return Result.success(almacenado.user)
    }

    override suspend fun updateProfile(userId: String, nombre: String, correo: String): Result<User> {
        delay(300)
        val almacenado = usuariosPorId[userId]
            ?: return Result.failure(NoSuchElementException("El usuario no existe"))
        val correoNormalizado = correo.trim().lowercase()
        val enUsoPorOtro = buscarPorCorreo(correoNormalizado)?.user?.id?.let { it != userId } ?: false
        if (enUsoPorOtro) {
            return Result.failure(IllegalStateException("Ya existe una cuenta con ese correo"))
        }
        val actualizado = almacenado.user.copy(nombre = nombre.trim(), correo = correoNormalizado)
        usuariosPorId[userId] = almacenado.copy(user = actualizado)
        if (sesionActual?.id == userId) sesionActual = actualizado
        return Result.success(actualizado)
    }

    override suspend fun changePassword(userId: String, currentPassword: String, newPassword: String): Result<Unit> {
        delay(300)
        val almacenado = usuariosPorId[userId]
            ?: return Result.failure(NoSuchElementException("El usuario no existe"))
        if (almacenado.password != currentPassword) {
            return Result.failure(IllegalStateException("La contraseña actual es incorrecta"))
        }
        usuariosPorId[userId] = almacenado.copy(password = newPassword)
        return Result.success(Unit)
    }

    override suspend fun sendPasswordReset(correo: String): Result<Unit> {
        delay(300)
        buscarPorCorreo(correo)
            ?: return Result.failure(IllegalStateException("No existe una cuenta con ese correo"))
        return Result.success(Unit)
    }

    override suspend fun confirmPassword(password: String): Result<Unit> {
        val almacenado = sesionActual?.let { usuariosPorId[it.id] }
            ?: return Result.failure(IllegalStateException("Tu sesión expiró, vuelve a iniciar sesión"))
        return if (almacenado.password == password) Result.success(Unit)
        else Result.failure(IllegalStateException("La contraseña es incorrecta"))
    }

    override suspend fun deleteAccount(userId: String, password: String): Result<Unit> {
        delay(300)
        confirmPassword(password).onFailure { return Result.failure(it) }
        usuariosPorId.remove(userId)
        sesionActual = null
        return Result.success(Unit)
    }

    override fun logout() {
        sesionActual = null
    }

    override fun currentUser(): User? = sesionActual
}
