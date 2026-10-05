package com.orel.wallet.wallet

import com.orel.wallet.data.InMemoryWalletRepository
import com.orel.wallet.domain.*
import com.orel.wallet.payments.DemoPaymentService
import com.orel.wallet.payments.PaymentException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ExternalCardTest {
    @Test fun personalWalletStartsEmptyAndOnlyStoresAnExternalReference() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        repo.initialize()
        assertTrue(repo.cards.first().isEmpty())
        assertTrue(repo.transactions.first().isEmpty())
        val card = repo.addExternalCard(CardNetwork.VISA, "Pixpay", "4821")
        assertEquals("4821", card.last4)
        assertFalse(card.isDemo)
        assertFalse(card.contactlessEnabled)
        assertFalse(card.isPayableDemo)
        repo.saveAppearance(card.id, card.appearance.copy(backgroundValue = "violet"))
        val changed = repo.findCard(card.id)!!
        assertEquals(card.id, changed.id)
        assertEquals(card.network, changed.network)
        assertEquals(card.last4, changed.last4)
        assertFalse(changed.isDemo)
    }

    @Test fun externalReferenceRejectsFullNumbersAndDemoCannotChargeIt() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        for (digits in listOf("", "123", "12345", "4111111111111111", "abcd")) {
            try { repo.addExternalCard(CardNetwork.VISA, "Pixpay", digits); fail("Must accept only last4") }
            catch (_: IllegalArgumentException) { }
        }
        try { repo.addExternalCard(CardNetwork.VISA, "4111 1111 1111 1111", "4821"); fail("A full number cannot be stored as the name") }
        catch (_: IllegalArgumentException) { }
        val card = repo.addExternalCard(CardNetwork.VISA, "Pixpay", "4821")
        try { DemoPaymentService(repo).preparePayment(card.id); fail("An external reference cannot pay in demo") }
        catch (_: PaymentException) { }
        assertTrue(repo.transactions.first().isEmpty())
    }
}
