package com.orel.wallet.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.orel.wallet.domain.PaymentToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** AES-GCM storage for opaque token references. noBackupFilesDir excludes ciphertext from backup. */
class CardTokenStore(context: Context) {
    private val directory = File(context.applicationContext.noBackupFilesDir, "opaque-token-references")
    private val lock = Any()

    suspend fun save(token: PaymentToken) = withContext(Dispatchers.IO) {
        OpaqueTokenCodec.validate(token)
        synchronized(lock) {
            check(directory.isDirectory || directory.mkdirs()) { "No se puede crear el almacén de tokens" }
            val plaintext = OpaqueTokenCodec.encode(token)
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
                val ciphertext = cipher.doFinal(plaintext)
                val payload = byteArrayOf(1, cipher.iv.size.toByte()) + cipher.iv + ciphertext
                val atomicFile = AtomicFile(file(token.id))
                val stream = atomicFile.startWrite()
                try { stream.write(payload); atomicFile.finishWrite(stream) }
                catch (error: Exception) { atomicFile.failWrite(stream); throw error }
            } finally { plaintext.fill(0) }
        }
    }

    suspend fun load(id: String): PaymentToken? = withContext(Dispatchers.IO) {
        synchronized(lock) {
            val stored = AtomicFile(file(id))
            if (!stored.baseFile.exists()) return@synchronized null
            val payload = stored.openRead().use { stream ->
                require(stream.available() in 30..8192) { "Token cifrado inválido" }
                stream.readBytes()
            }
            require(payload[0] == 1.toByte() && payload[1] == 12.toByte()) { "Formato de cifrado inválido" }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey(), GCMParameterSpec(128, payload.copyOfRange(2, 14)))
            val plaintext = cipher.doFinal(payload.copyOfRange(14, payload.size))
            try {
                val token = OpaqueTokenCodec.decode(plaintext)
                require(token.id == id) { "Referencia de token incorrecta" }
                if (token.expiresAt != null && System.currentTimeMillis() >= token.expiresAt) {
                    stored.delete()
                    null
                } else token
            } finally { plaintext.fill(0) }
        }
    }

    suspend fun remove(id: String) = withContext(Dispatchers.IO) { synchronized(lock) { AtomicFile(file(id)).delete() } }

    private fun file(id: String): File {
        require(id.matches(Regex("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}")))
        val digest = MessageDigest.getInstance("SHA-256").digest(id.encodeToByteArray())
        val name = digest.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
        return File(directory, "$name.enc")
    }
    private fun encryptionKey(): SecretKey = synchronized(keyLock) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build())
        }.generateKey()
    }
    companion object {
        private const val KEY_ALIAS = "com.orel.wallet.opaque.references.v1"
        private val keyLock = Any()
    }
}
