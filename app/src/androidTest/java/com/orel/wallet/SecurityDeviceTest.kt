package com.orel.wallet

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.orel.wallet.domain.PaymentToken
import com.orel.wallet.security.CardTokenStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class SecurityDeviceTest {
    @Test fun keystoreRoundTripIsEncryptedAndRemovable() = runBlocking {
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val store=CardTokenStore(context)
        val token=PaymentToken("test-${UUID.randomUUID()}","lab-provider","demo-opaque-reference-${UUID.randomUUID()}",System.currentTimeMillis())
        store.save(token)
        assertEquals(token,store.load(token.id))
        val files=File(context.noBackupFilesDir,"opaque-token-references").listFiles().orEmpty()
        assertTrue(files.isNotEmpty())
        assertFalse(files.any {it.readBytes().toString(Charsets.ISO_8859_1).contains(token.opaqueReference)})
        store.remove(token.id)
        assertNull(store.load(token.id))
    }
}
