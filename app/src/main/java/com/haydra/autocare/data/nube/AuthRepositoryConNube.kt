package com.haydra.autocare.data.nube

import com.haydra.autocare.data.model.User
import com.haydra.autocare.data.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Decorador de [AuthRepository]: al iniciar sesión o registrarse espera a que los datos de la nube
 * bajen al teléfono (máximo [ESPERA_MS], para no dejar al usuario esperando sin conexión). Al
 * reabrir la app con la sesión guardada, la sincronización corre en segundo plano.
 */
class AuthRepositoryConNube(
    private val auth: AuthRepository,
    private val nube: SincronizadorNube,
    private val alcance: CoroutineScope,
    private val borrarFotos: suspend (List<String>) -> Unit = {}
) : AuthRepository by auth {

    /**
     * Borrar la cuenta va en este orden: se revisa la contraseña, se borran los datos de la nube
     * (mientras las reglas todavía dejan entrar) y al final la cuenta. Las notificaciones y las
     * fotos se quitan solo si todo salió bien.
     */
    override suspend fun deleteAccount(userId: String, password: String): Result<Unit> {
        auth.confirmPassword(password).onFailure { return Result.failure(it) }
        val locales = nube.datosLocales(userId)
        nube.borrarDeLaNube().onFailure {
            return Result.failure(IllegalStateException(ERROR_NUBE, it))
        }
        return auth.deleteAccount(userId, password).onSuccess {
            nube.cancelarAvisos(locales)
            borrarFotos(locales.fotos)
        }
    }

    override suspend fun login(correo: String, password: String): Result<User> =
        auth.login(correo, password).onSuccess { esperarSincronizacion(it) }

    override suspend fun register(nombre: String, correo: String, password: String): Result<User> =
        auth.register(nombre, correo, password).onSuccess { esperarSincronizacion(it) }

    override suspend fun restoreSession(userId: String): Result<User> =
        auth.restoreSession(userId).onSuccess { usuario -> alcance.launch { nube.sincronizar(usuario.id) } }

    private suspend fun esperarSincronizacion(usuario: User) {
        withTimeoutOrNull(ESPERA_MS) { nube.sincronizar(usuario.id) }
    }

    private companion object {
        const val ESPERA_MS = 10_000L
        const val ERROR_NUBE = "No pudimos borrar tus datos de la nube. Revisa tu conexión e inténtalo de nuevo."
    }
}
