package com.orel.wallet.nfc

import com.orel.wallet.data.InMemoryWalletRepository
import com.orel.wallet.domain.CardNetwork
import com.orel.wallet.payments.DemoPaymentService
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DemoApduProcessorTest {
    private val select = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x06, 0xF0.toByte(), 0x4F, 0x52, 0x45, 0x4C, 0x01)
    @Test fun noCredentialIsExposedBeforeForegroundAuthorization() {
        val gate = DemoCredentialGate()
        assertFalse(gate.arm("untrusted"))
        assertArrayEquals(byteArrayOf(0x69, 0x85.toByte()), DemoApduProcessor(gate).process(select))
    }

    @Test fun authorizedLaboratoryCredentialExpiresAndRejectsPaymentAids() {
        var now = 100L
        val gate = DemoCredentialGate(clock = { now }, ttlMillis = 1_000)
        gate.authorize("session", 1_100)
        assertTrue(gate.arm("session"))
        val processor = DemoApduProcessor(gate)
        assertArrayEquals(byteArrayOf(0x90.toByte(), 0), processor.process(select).takeLast(2).toByteArray())
        val emv = byteArrayOf(0, 0xA4.toByte(), 4, 0, 7, 0xA0.toByte(), 0, 0, 0, 3, 0x10, 0x10)
        assertArrayEquals(byteArrayOf(0x6A, 0x82.toByte()), processor.process(emv))
        assertArrayEquals(byteArrayOf(0x69, 0x85.toByte()), processor.process(byteArrayOf(0x80.toByte(), 0xCA.toByte(), 0, 0, 0)))
        processor.process(select)
        val payload = processor.process(byteArrayOf(0x80.toByte(), 0xCA.toByte(), 0, 0, 0))
        assertTrue(payload.dropLast(2).toByteArray().decodeToString().startsWith("DEMO:"))
        now = 1_101
        assertArrayEquals(byteArrayOf(0x69, 0x85.toByte()), processor.process(select))
    }

    @Test fun cancelRevokesDemoHceAuthorization() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val card = repo.addCard(CardNetwork.VISA, "Viajes")
        val service = DemoPaymentService(repo)
        val session = service.preparePayment(card.id)
        service.authenticateDemo(session)
        assertTrue(DemoHceGate.arm(session.id))
        service.cancel()
        assertNull(DemoHceGate.credential())
        assertFalse(DemoHceGate.arm(session.id))
    }

    @Test fun nfcHardwareStatusDoesNotInventCapabilities() {
        assertEquals(NfcStatus(false, false, false), NfcController.availability(false, true, true))
        assertEquals(NfcStatus(true, false, true), NfcController.availability(true, false, true))
        assertEquals(NfcStatus(true, true, false), NfcController.availability(true, true, false))
    }
}
