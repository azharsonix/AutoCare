package com.haydra.autocare.data.repository

import com.haydra.autocare.data.model.Documento
import java.time.LocalDate

/** Documentos del vehículo (seguro, tarjeta de circulación, verificación...). */
interface DocumentoRepository {
    suspend fun getDocumentos(vehiculoId: String): Result<List<Documento>>
    suspend fun insertDocumento(
        vehiculoId: String,
        tipo: String,
        vence: LocalDate?,
        foto: String?,
        notas: String
    ): Result<Documento>
    suspend fun updateDocumento(documento: Documento): Result<Documento>
    suspend fun deleteDocumento(id: String): Result<Unit>
}
