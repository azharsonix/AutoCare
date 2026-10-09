package com.haydra.autocare.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.haydra.autocare.data.plan.PlanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.planDataStore by preferencesDataStore(name = "autocare_plan")

/** Guarda en DataStore, por cuenta, si tiene AutoCare Pro. */
class PlanStore(private val context: Context) : PlanRepository {

    private fun clave(usuarioId: String) = booleanPreferencesKey("pro_$usuarioId")

    override fun esPro(usuarioId: String): Flow<Boolean> =
        context.planDataStore.data.map { it[clave(usuarioId)] ?: false }

    override suspend fun activarPro(usuarioId: String) {
        context.planDataStore.edit { it[clave(usuarioId)] = true }
    }

    override suspend fun quitarPro(usuarioId: String) {
        context.planDataStore.edit { it.remove(clave(usuarioId)) }
    }
}
