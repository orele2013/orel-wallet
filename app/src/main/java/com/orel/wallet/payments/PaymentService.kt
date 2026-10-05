package com.orel.wallet.payments

import com.orel.wallet.domain.Transaction
import kotlinx.coroutines.flow.StateFlow

enum class PaymentState { IDLE, SELECTING_CARD, AUTHENTICATING, READY_TO_PAY, PROCESSING, SUCCESS, FAILED, CANCELLED }
enum class AuthenticationMethod { BIOMETRIC, DEMO_SIMULATION }

data class PaymentSession(
    val id: String,
    val cardId: String,
    val state: PaymentState = PaymentState.IDLE,
    val transactionId: String? = null,
    val error: String? = null,
    val isDemo: Boolean = true,
    val authenticationMethod: AuthenticationMethod? = null,
    val expiresAt: Long? = null,
)

interface PaymentService {
    val session: StateFlow<PaymentSession?>
    suspend fun preparePayment(cardId: String): PaymentSession
    /** Caller must have just received SUCCESS from the operating-system authenticator. */
    suspend fun authenticate(session: PaymentSession)
    suspend fun executePayment(session: PaymentSession): Transaction
    fun cancel()
}

class PaymentException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

data class PaymentRequest(
    val sessionId: String,
    val cardId: String,
    val merchant: String,
    val amountMinor: Long,
    val currency: String,
    val isDemo: Boolean,
)

data class PaymentConfirmation(
    val sessionId: String,
    val confirmationId: String,
    val approved: Boolean,
    val error: String? = null,
)

/** Confirmation is mandatory before any success state or transaction is published. */
interface PaymentProvider { suspend fun authorize(request: PaymentRequest): PaymentConfirmation }
interface TokenizationProvider { suspend fun provisionReference(provisioningReference: String): com.orel.wallet.domain.PaymentToken }
interface CardProvisioningProvider { suspend fun createProvisioningSession(): String }
