package com.orel.wallet.wallet

import com.orel.wallet.domain.*
import com.orel.wallet.data.InMemoryWalletRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class WalletRepositoryTest {
    @Test fun firstPayableCardBecomesDefaultAndLoyaltyDoesNot() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val loyalty = repo.addCard(CardNetwork.LOYALTY, "Club")
        val visa = repo.addCard(CardNetwork.VISA, "Viajes")
        assertFalse(repo.findCard(loyalty.id)!!.isDefault)
        assertTrue(repo.findCard(visa.id)!!.isDefault)
        assertTrue(visa.isDemo)
        assertTrue(visa.last4.matches(Regex("[0-9]{4}")))
    }

    @Test fun deletingDefaultSelectsNextEligibleCard() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val first = repo.addCard(CardNetwork.VISA, "Primera")
        val second = repo.addCard(CardNetwork.MASTERCARD, "Segunda")
        val locked = repo.addCard(CardNetwork.AMEX, "Bloqueada")
        repo.updateCard(locked.copy(isLocked = true))
        repo.removeCard(first.id)
        assertTrue(repo.findCard(second.id)!!.isDefault)
        assertEquals(1, repo.cards.first().count { it.isDefault })
    }

    @Test fun lockingDefaultCannotLeaveItDefault() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val first = repo.addCard(CardNetwork.VISA, "Primera")
        val second = repo.addCard(CardNetwork.MASTERCARD, "Segunda")
        repo.updateCard(first.copy(isLocked = true))
        assertFalse(repo.findCard(first.id)!!.isDefault)
        assertTrue(repo.findCard(second.id)!!.isDefault)
        assertFails { repo.setDefault(first.id) }
    }

    @Test fun appearanceCannotRewriteCardIdentityOrPaymentMetadata() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val card = repo.addCard(CardNetwork.VISA, "Personal")
        repo.saveAppearance(card.id, card.appearance.copy(backgroundValue = "gold", brightness = .7f))
        val changed = repo.findCard(card.id)!!
        assertEquals(card.last4, changed.last4)
        assertEquals(card.network, changed.network)
        assertEquals("gold", changed.appearance.backgroundValue)
        assertFails { repo.saveAppearance(card.id, card.appearance.copy(id = "different")) }
        assertFails { repo.updateCard(card.copy(last4 = "4111111111111111")) }
        assertFails { repo.updateCard(card.copy(network = CardNetwork.AMEX)) }
    }

    @Test fun reorderedCardsRetainContiguousOrderAndRejectMissingIds() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val a = repo.addCard(CardNetwork.VISA, "A")
        val b = repo.addCard(CardNetwork.MASTERCARD, "B")
        repo.reorder(listOf(b.id, a.id))
        assertEquals(listOf(b.id, a.id), repo.cards.first().map { it.id })
        assertEquals(listOf(0, 1), repo.cards.first().map { it.sortOrder })
        assertFails { repo.reorder(listOf(a.id)) }
    }

    @Test fun imageAppearanceAcceptsPrivateImportedImageAndRejectsExternalPaths() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val card = repo.addCard(CardNetwork.VISA, "Imagen")
        val privatePath = "/data/user/0/com.orel.wallet/no_backup/card-images/12345678-1234-1234-1234-123456789abc.image"
        repo.saveAppearance(card.id, card.appearance.copy(backgroundType = BackgroundType.IMAGE, backgroundValue = privatePath))
        assertEquals(privatePath, repo.findCard(card.id)!!.appearance.backgroundValue)
        for (path in listOf("https://example.org/image.png", "/sdcard/image.png", "content://untrusted/image", "/data/user/0/com.orel.wallet/no_backup/card-images/../other.image")) {
            assertFails { repo.saveAppearance(card.id, card.appearance.copy(backgroundType = BackgroundType.IMAGE, backgroundValue = path)) }
        }
    }

    @Test fun initializationIsIdempotentAndDoesNotReseedDeletedCards() = runTest {
        val repo = InMemoryWalletRepository()
        repo.initialize()
        assertEquals(3, repo.cards.first().size)
        assertEquals(6, repo.transactions.first().size)
        repo.cards.first().forEach { repo.removeCard(it.id) }
        repo.initialize()
        assertTrue(repo.cards.first().isEmpty())
    }

    @Test fun cardNamesAndTransactionIdentityAreValidated() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        assertFails { repo.addCard(CardNetwork.VISA, "   ") }
        assertFails { repo.addCard(CardNetwork.VISA, "x".repeat(41)) }
        val card = repo.addCard(CardNetwork.VISA, "  Hogar  ")
        assertEquals("Hogar", card.displayName)
        val tx = Transaction("purchase", "Café DEMO", 350, timestamp = 100, cardId = card.id, cardLabel = card.displayName, last4 = card.last4)
        repo.recordTransaction(tx)
        repo.recordTransaction(tx)
        assertEquals(1, repo.transactions.first().size)
        assertFails { repo.recordTransaction(tx.copy(amountMinor = 999)) }
    }

    private suspend fun assertFails(block: suspend () -> Unit) {
        try { block(); fail("Expected validation failure") } catch (_: IllegalArgumentException) { }
    }
}
