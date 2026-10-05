package com.orel.wallet.payments

import com.orel.wallet.domain.*
import com.orel.wallet.nfc.DemoHceGate
import com.orel.wallet.wallet.WalletRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

/** Explicit local simulation, isolated from every future real provider. */
class DemoPaymentService(
    private val repository: WalletRepository,
    private val provider: PaymentProvider = LocalDemoPaymentProvider(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val authorizationTimeoutMillis: Long = 30_000,
    private val providerTimeoutMillis: Long = 10_000,
) : PaymentService {
    init {
        require(authorizationTimeoutMillis in 1..60_000)
        require(providerTimeoutMillis in 1..60_000)
    }
    private val stateLock = Any()
    private val mutableSession = MutableStateFlow<PaymentSession?>(null)
    override val session = mutableSession.asStateFlow()
    private val completed = mutableMapOf<String, Transaction>()
    // Once a confirmed provider result enters the atomic commit, cancellation cannot undo it.
    private var committing = false

    override suspend fun preparePayment(cardId: String): PaymentSession {
        val card = repository.findCard(cardId)
        if (card?.isPayableDemo != true) throw PaymentException("Esta tarjeta no permite pagos DEMO")
        return synchronized(stateLock) {
            if (committing || mutableSession.value?.state == PaymentState.PROCESSING) throw PaymentException("Ya hay un pago en curso")
            DemoHceGate.disarm()
            val prepared = PaymentSession(UUID.randomUUID().toString(), cardId, PaymentState.SELECTING_CARD,
                expiresAt = clock() + authorizationTimeoutMillis)
            mutableSession.value = prepared
            prepared.copy(state = PaymentState.AUTHENTICATING).also { mutableSession.value = it }
        }
    }

    override suspend fun authenticate(session: PaymentSession) = authorize(session, AuthenticationMethod.BIOMETRIC)

    /** Must be invoked by a visibly labelled, explicit DEMO authentication action. */
    suspend fun authenticateDemo(session: PaymentSession) = authorize(session, AuthenticationMethod.DEMO_SIMULATION)

    private suspend fun authorize(input: PaymentSession, method: AuthenticationMethod) {
        val card = repository.findCard(input.cardId)
        val contactlessEnabled = repository.settings.first().contactlessEnabled
        synchronized(stateLock) {
            val current = current(input)
            if (current.state != PaymentState.AUTHENTICATING) throw PaymentException("La sesión no espera autenticación")
            if (expired(current) || card?.isPayableDemo != true) fail(current.id, "La autorización ha caducado o la tarjeta no está disponible")
            val authorized = current.copy(state = PaymentState.READY_TO_PAY, authenticationMethod = method,
                expiresAt = clock() + authorizationTimeoutMillis, error = null)
            mutableSession.value = authorized
            if (contactlessEnabled && card!!.contactlessEnabled) DemoHceGate.authorize(authorized.id, authorized.expiresAt!!)
        }
    }

    override suspend fun executePayment(session: PaymentSession): Transaction {
        synchronized(stateLock) {
            current(session)
            completed[session.id]?.let { return it }
        }
        val card = repository.findCard(session.cardId)
        val request = synchronized(stateLock) {
            val current = current(session)
            completed[session.id]?.let { return it }
            if (current.state != PaymentState.READY_TO_PAY || current.authenticationMethod == null) {
                throw PaymentException("Autoriza la sesión antes de simular el pago")
            }
            if (expired(current) || card?.isPayableDemo != true) fail(current.id, "La autorización ha caducado o la tarjeta no está disponible")
            mutableSession.value = current.copy(state = PaymentState.PROCESSING)
            DemoHceGate.disarm()
            PaymentRequest(current.id, current.cardId, "DEMO STORE", 1250, "EUR", isDemo = true)
        }
        try {
            val confirmation = withTimeoutOrNull(providerTimeoutMillis) { provider.authorize(request) }
                ?: throw PaymentException("El proveedor DEMO no respondió a tiempo")
            val latestCard = repository.findCard(request.cardId)
            val transaction = synchronized(stateLock) {
                val current = current(session)
                if (current.state == PaymentState.CANCELLED) throw PaymentException("Pago cancelado")
                if (current.state != PaymentState.PROCESSING) throw PaymentException("Sesión de pago inválida")
                if (expired(current) || latestCard?.isPayableDemo != true) fail(current.id, "La autorización ha caducado o la tarjeta ha sido revocada")
                if (!confirmation.approved || confirmation.sessionId != current.id || confirmation.confirmationId.isBlank()) {
                    fail(current.id, confirmation.error ?: "El proveedor DEMO no confirmó el pago")
                }
                committing = true
                Transaction("demo-payment-${current.id}", request.merchant, request.amountMinor, request.currency,
                    clock(), latestCard!!.id, latestCard.displayName, latestCard.last4,
                    status = TransactionStatus.COMPLETED, isDemo = true)
            }
            // Publish SUCCESS only after the same confirmed transaction is durably recorded.
            return withContext(NonCancellable) {
                try {
                    repository.recordTransaction(transaction)
                    synchronized(stateLock) {
                        completed[session.id] = transaction
                        mutableSession.value = current(session).copy(state = PaymentState.SUCCESS, transactionId = transaction.id, error = null)
                        committing = false
                    }
                    transaction
                } catch (error: Exception) {
                    synchronized(stateLock) { committing = false }
                    throw error
                }
            }
        } catch (error: CancellationException) {
            synchronized(stateLock) {
                if (!committing && mutableSession.value?.id == session.id && mutableSession.value?.state == PaymentState.PROCESSING) {
                    mutableSession.value = mutableSession.value!!.copy(state = PaymentState.CANCELLED, error = "Pago cancelado")
                    DemoHceGate.disarm()
                }
            }
            throw error
        } catch (error: Exception) {
            synchronized(stateLock) {
                if (mutableSession.value?.id == session.id && mutableSession.value?.state == PaymentState.PROCESSING) {
                    mutableSession.value = mutableSession.value!!.copy(state = PaymentState.FAILED, error = error.message ?: "No se pudo confirmar el pago DEMO")
                    DemoHceGate.disarm()
                }
            }
            if (error is PaymentException) throw error
            throw PaymentException("No se pudo confirmar el pago DEMO", error)
        }
    }

    override fun cancel() = synchronized(stateLock) {
        val current = mutableSession.value
        if (!committing && current != null && current.state !in terminalStates) {
            mutableSession.value = current.copy(state = PaymentState.CANCELLED, error = null)
        }
        DemoHceGate.disarm()
    }

    private fun current(input: PaymentSession): PaymentSession {
        val current = mutableSession.value
        if (current == null || current.id != input.id || current.cardId != input.cardId || !current.isDemo) {
            throw PaymentException("Sesión de pago obsoleta o inválida")
        }
        return current
    }
    private fun expired(current: PaymentSession) = current.expiresAt == null || clock() >= current.expiresAt
    private fun fail(id: String, message: String): Nothing {
        if (mutableSession.value?.id == id && mutableSession.value?.state != PaymentState.CANCELLED) {
            mutableSession.value = mutableSession.value!!.copy(state = PaymentState.FAILED, error = message)
        }
        DemoHceGate.disarm()
        throw PaymentException(message)
    }
    private val terminalStates = setOf(PaymentState.SUCCESS, PaymentState.FAILED, PaymentState.CANCELLED, PaymentState.IDLE)
}

class LocalDemoPaymentProvider : PaymentProvider {
    override suspend fun authorize(request: PaymentRequest): PaymentConfirmation {
        require(request.isDemo) { "El proveedor local solo admite DEMO" }
        require(request.amountMinor > 0 && request.currency == "EUR")
        return PaymentConfirmation(request.sessionId, "local-demo-${request.sessionId}", approved = true)
    }
}
