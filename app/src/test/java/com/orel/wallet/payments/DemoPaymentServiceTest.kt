package com.orel.wallet.payments

import com.orel.wallet.data.InMemoryWalletRepository
import com.orel.wallet.domain.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class DemoPaymentServiceTest {
    @Test fun successfulPaymentRequiresExplicitAuthenticationAndIsIdempotent() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val card = repo.addCard(CardNetwork.VISA, "Viajes")
        val service = DemoPaymentService(repo)
        val session = service.preparePayment(card.id)
        assertEquals(PaymentState.AUTHENTICATING, service.session.value!!.state)
        assertPaymentFails { service.executePayment(session) }
        service.authenticateDemo(session)
        assertEquals(AuthenticationMethod.DEMO_SIMULATION, service.session.value!!.authenticationMethod)
        assertEquals(PaymentState.READY_TO_PAY, service.session.value!!.state)
        val tx = service.executePayment(session)
        assertEquals("DEMO STORE", tx.merchant)
        assertEquals(1250L, tx.amountMinor)
        assertEquals(PaymentState.SUCCESS, service.session.value!!.state)
        assertEquals(tx, service.executePayment(session))
        assertEquals(1, repo.transactions.first().size)
        assertTrue(tx.isDemo)
    }

    @Test fun loyaltyLockedHiddenAndRealCardsCannotPay() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val loyalty = repo.addCard(CardNetwork.LOYALTY, "Club")
        val card = repo.addCard(CardNetwork.VISA, "Personal")
        val service = DemoPaymentService(repo)
        assertPaymentFails { service.preparePayment(loyalty.id) }
        repo.updateCard(card.copy(isLocked = true))
        assertPaymentFails { service.preparePayment(card.id) }
        repo.updateCard(repo.findCard(card.id)!!.copy(isLocked = false, isHidden = true))
        assertPaymentFails { service.preparePayment(card.id) }
        assertFalse(card.copy(isDemo = false).isPayableDemo)
    }

    @Test fun revokedCardAndExpiredAuthorizationCannotExecute() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val card = repo.addCard(CardNetwork.VISA, "Personal")
        var now = 1_000L
        val service = DemoPaymentService(repo, clock = { now }, authorizationTimeoutMillis = 1_000)
        val prepared = service.preparePayment(card.id)
        service.authenticate(prepared)
        now = 2_001
        assertPaymentFails { service.executePayment(prepared) }
        assertTrue(repo.transactions.first().isEmpty())
        val next = service.preparePayment(card.id)
        service.authenticateDemo(next)
        repo.updateCard(card.copy(isLocked = true))
        assertPaymentFails { service.executePayment(next) }
        assertEquals(PaymentState.FAILED, service.session.value!!.state)
        assertTrue(repo.transactions.first().isEmpty())
    }

    @Test fun cancelBeforeProviderConfirmationNeverWritesTransaction() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val card = repo.addCard(CardNetwork.VISA, "Personal")
        val called = CompletableDeferred<PaymentRequest>()
        val release = CompletableDeferred<Unit>()
        val provider = object : PaymentProvider {
            override suspend fun authorize(request: PaymentRequest): PaymentConfirmation {
                called.complete(request)
                release.await()
                return PaymentConfirmation(request.sessionId, "confirmation", approved = true)
            }
        }
        val service = DemoPaymentService(repo, provider)
        val prepared = service.preparePayment(card.id)
        service.authenticateDemo(prepared)
        val operation = async { runCatching { service.executePayment(prepared) } }
        called.await()
        assertEquals(PaymentState.PROCESSING, service.session.value!!.state)
        assertTrue(repo.transactions.first().isEmpty())
        service.cancel()
        release.complete(Unit)
        assertTrue(operation.await().isFailure)
        assertEquals(PaymentState.CANCELLED, service.session.value!!.state)
        assertTrue(repo.transactions.first().isEmpty())
    }

    @Test fun rejectedAndMismatchedProviderConfirmationsFailClosed() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val card = repo.addCard(CardNetwork.VISA, "Personal")
        for (confirmation in listOf(
            PaymentConfirmation("other-session", "confirmation", true),
            PaymentConfirmation("placeholder", "confirmation", false, "Rejected")
        )) {
            val provider = object : PaymentProvider {
                override suspend fun authorize(request: PaymentRequest) = if (!confirmation.approved) confirmation.copy(sessionId = request.sessionId) else confirmation
            }
            val service = DemoPaymentService(repo, provider)
            val prepared = service.preparePayment(card.id)
            service.authenticateDemo(prepared)
            assertPaymentFails { service.executePayment(prepared) }
            assertEquals(PaymentState.FAILED, service.session.value!!.state)
        }
        assertTrue(repo.transactions.first().isEmpty())
    }

    @Test fun staleSessionCannotAuthenticateOrCancelNewSession() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val card = repo.addCard(CardNetwork.VISA, "Personal")
        val service = DemoPaymentService(repo)
        val old = service.preparePayment(card.id)
        service.cancel()
        val current = service.preparePayment(card.id)
        assertPaymentFails { service.authenticateDemo(old) }
        assertEquals(current.id, service.session.value!!.id)
    }

    @Test fun unresponsiveProviderTimesOutAsFailureWithoutWritingHistory() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val card = repo.addCard(CardNetwork.VISA, "Personal")
        val provider = object : PaymentProvider {
            override suspend fun authorize(request: PaymentRequest): PaymentConfirmation {
                CompletableDeferred<Unit>().await()
                error("Unreachable")
            }
        }
        val service = DemoPaymentService(repo, provider, providerTimeoutMillis = 100)
        val prepared = service.preparePayment(card.id)
        service.authenticateDemo(prepared)
        assertPaymentFails { service.executePayment(prepared) }
        assertEquals(PaymentState.FAILED, service.session.value!!.state)
        assertTrue(repo.transactions.first().isEmpty())
    }

    @Test fun providerErrorDoesNotInventSuccess() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val card = repo.addCard(CardNetwork.VISA, "Personal")
        val provider = object : PaymentProvider {
            override suspend fun authorize(request: PaymentRequest): PaymentConfirmation = throw IllegalStateException("Disconnected")
        }
        val service = DemoPaymentService(repo, provider)
        val prepared = service.preparePayment(card.id)
        service.authenticateDemo(prepared)
        assertPaymentFails { service.executePayment(prepared) }
        assertEquals(PaymentState.FAILED, service.session.value!!.state)
        assertTrue(repo.transactions.first().isEmpty())
    }

    @Test fun callerCancellationKeepsCancellationSemanticsAndNeverWrites() = runTest {
        val repo = InMemoryWalletRepository(seedDemoData = false)
        val card = repo.addCard(CardNetwork.VISA, "Personal")
        val provider = object : PaymentProvider {
            override suspend fun authorize(request: PaymentRequest): PaymentConfirmation {
                CompletableDeferred<Unit>().await()
                error("Unreachable")
            }
        }
        val service = DemoPaymentService(repo, provider)
        val prepared = service.preparePayment(card.id)
        service.authenticateDemo(prepared)
        val error = runCatching { withTimeout(50) { service.executePayment(prepared) } }.exceptionOrNull()
        assertTrue(error is CancellationException)
        assertEquals(PaymentState.CANCELLED, service.session.value!!.state)
        assertTrue(repo.transactions.first().isEmpty())
    }

    private suspend fun assertPaymentFails(block: suspend () -> Any?) {
        try { block(); fail("Expected payment failure") } catch (_: PaymentException) { }
    }
}
