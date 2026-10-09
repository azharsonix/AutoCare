package com.haydra.autocare.data.repository

import com.haydra.autocare.data.model.User

/**
 * Contrato de autenticación de AutoCare (RF01, RF02, RF03).
 *
 * La app usa [RoomAuthRepository] (cuentas locales con contraseña cifrada) y las pruebas
 * usan [FakeAuthRepository]. En el Sprint 6 se añadirá una implementación que hable con
 * Supabase Auth sin cambiar el resto de la app, ya que todas dependen solo de esta interfaz.
 */
interface AuthRepository {
    suspend fun register(nombre: String, correo: String, password: String): Result<User>
    suspend fun login(correo: String, password: String): Result<User>
    suspend fun restoreSession(userId: String): Result<User>
    suspend fun updateProfile(userId: String, nombre: String, correo: String): Result<User>
    suspend fun changePassword(userId: String, currentPassword: String, newPassword: String): Result<Unit>
    suspend fun resetPassword(correo: String, newPassword: String): Result<Unit>
    fun logout()
    fun currentUser(): User?
}
