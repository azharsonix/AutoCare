package com.haydra.autocare.data.repository

import com.haydra.autocare.data.model.User

/**
 * Contrato de autenticación de AutoCare (RF01, RF02, RF03).
 *
 * La app usa [FirebaseAuthRepository] (Firebase Authentication + perfil en Cloud Firestore)
 * y las pruebas usan [FakeAuthRepository]. El resto de la app depende solo de esta interfaz.
 */
interface AuthRepository {
    suspend fun register(nombre: String, correo: String, password: String): Result<User>
    suspend fun login(correo: String, password: String): Result<User>
    suspend fun restoreSession(userId: String): Result<User>
    suspend fun updateProfile(userId: String, nombre: String, correo: String): Result<User>
    suspend fun changePassword(userId: String, currentPassword: String, newPassword: String): Result<Unit>
    /** Envía al correo un enlace para elegir una contraseña nueva. */
    suspend fun sendPasswordReset(correo: String): Result<Unit>
    fun logout()
    fun currentUser(): User?
}
