package com.haydra.autocare.data.local

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Deriva un hash PBKDF2 con sal aleatoria para no guardar contraseñas en
 * texto plano en la base local (requisito no funcional de Seguridad).
 * Formato guardado: "iteraciones:salBase64:hashBase64".
 */
object PasswordHasher {
    private const val ALGORITMO = "PBKDF2WithHmacSHA256"
    private const val ITERACIONES = 120_000
    private const val BYTES_SAL = 16
    private const val BITS_HASH = 256

    fun hash(password: String): String {
        val sal = ByteArray(BYTES_SAL).also { SecureRandom().nextBytes(it) }
        val hash = derivar(password, sal, ITERACIONES)
        val codificador = Base64.getEncoder()
        return "$ITERACIONES:${codificador.encodeToString(sal)}:${codificador.encodeToString(hash)}"
    }

    fun verify(password: String, guardado: String): Boolean {
        val partes = guardado.split(":")
        if (partes.size != 3) return false
        val iteraciones = partes[0].toIntOrNull() ?: return false
        val decodificador = Base64.getDecoder()
        val sal = decodificador.decode(partes[1])
        val esperado = decodificador.decode(partes[2])
        return MessageDigest.isEqual(derivar(password, sal, iteraciones), esperado)
    }

    private fun derivar(password: String, sal: ByteArray, iteraciones: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), sal, iteraciones, BITS_HASH)
        return try {
            SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
