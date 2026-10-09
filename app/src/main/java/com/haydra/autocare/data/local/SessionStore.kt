package com.haydra.autocare.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionDataStore by preferencesDataStore(name = "autocare_session")

/**
 * Guarda el id del usuario con sesión iniciada en el almacenamiento local del
 * dispositivo (DataStore), para no pedir inicio de sesión cada vez que se
 * abre la app (RF02). Solo guarda el id; los datos del usuario se recuperan
 * del repositorio de autenticación al reabrir la app.
 */
class SessionStore(private val context: Context) {

    private val userIdKey = stringPreferencesKey("user_id")

    val sessionUserId: Flow<String?> = context.sessionDataStore.data.map { it[userIdKey] }

    suspend fun saveSession(userId: String) {
        context.sessionDataStore.edit { it[userIdKey] = userId }
    }

    suspend fun clearSession() {
        context.sessionDataStore.edit { it.remove(userIdKey) }
    }
}
