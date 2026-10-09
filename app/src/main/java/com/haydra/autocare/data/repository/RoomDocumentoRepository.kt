package com.haydra.autocare.data.repository

import com.haydra.autocare.data.local.db.DocumentoDao
import com.haydra.autocare.data.local.db.toEntity
import com.haydra.autocare.data.local.db.toModel
import com.haydra.autocare.data.model.Documento
import java.time.LocalDate
import java.util.UUID

/** Implementación de [DocumentoRepository] sobre Room. */
class RoomDocumentoRepository(private val documentoDao: DocumentoDao) : DocumentoRepository {

    override suspend fun getDocumentos(vehiculoId: String): Result<List<Documento>> = runCatching {
        documentoDao.getByVehiculo(vehiculoId).map { it.toModel() }
    }

    override suspend fun insertDocumento(
        vehiculoId: String,
        tipo: String,
        vence: LocalDate?,
        foto: String?,
        notas: String
    ): Result<Documento> = runCatching {
        val documento = Documento(
            id = UUID.randomUUID().toString(),
            vehiculoId = vehiculoId,
            tipo = tipo.trim(),
            vence = vence,
            foto = foto,
            notas = notas.trim()
        )
        documentoDao.insert(documento.toEntity())
        documento
    }

    override suspend fun updateDocumento(documento: Documento): Result<Documento> = runCatching {
        if (documentoDao.update(documento.toEntity()) == 0) throw NoSuchElementException("El documento no existe")
        documento
    }

    override suspend fun deleteDocumento(id: String): Result<Unit> = runCatching {
        documentoDao.deleteById(id)
    }
}
