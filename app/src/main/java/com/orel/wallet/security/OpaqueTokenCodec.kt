package com.orel.wallet.security

import com.orel.wallet.domain.PaymentToken
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

internal object OpaqueTokenCodec {
    private val identifier = Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")
    private val opaqueReference = Regex("[A-Za-z][A-Za-z0-9._:-]{7,255}")
    fun validate(token: PaymentToken) {
        require(identifier.matches(token.id) && identifier.matches(token.providerId))
        require(listOf(token.id, token.providerId).none { Regex("[0-9]{13,19}").containsMatchIn(it) }) { "Los identificadores no pueden contener números bancarios" }
        require(opaqueReference.matches(token.opaqueReference)) { "Solo se admiten referencias opacas del proveedor" }
        require(!Regex("[0-9]{13,19}").containsMatchIn(token.opaqueReference)) { "No se admiten números bancarios" }
        require(token.createdAt >= 0 && (token.expiresAt == null || token.expiresAt > token.createdAt))
    }
    fun encode(token: PaymentToken): ByteArray {
        validate(token)
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use {
            it.writeInt(1)
            it.writeUTF(token.id)
            it.writeUTF(token.providerId)
            it.writeUTF(token.opaqueReference)
            it.writeLong(token.createdAt)
            it.writeBoolean(token.expiresAt != null)
            token.expiresAt?.let(it::writeLong)
        }
        return bytes.toByteArray()
    }
    fun decode(bytes: ByteArray): PaymentToken {
        require(bytes.size in 1..4096)
        return DataInputStream(ByteArrayInputStream(bytes)).use {
            require(it.readInt() == 1) { "Versión de token no admitida" }
            val token = PaymentToken(it.readUTF(), it.readUTF(), it.readUTF(), it.readLong(), if (it.readBoolean()) it.readLong() else null)
            require(it.available() == 0) { "Datos de token inesperados" }
            validate(token)
            token
        }
    }
}
