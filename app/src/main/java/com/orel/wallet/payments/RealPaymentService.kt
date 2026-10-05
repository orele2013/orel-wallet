package com.orel.wallet.payments

import com.orel.wallet.domain.Transaction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fails closed until an issuer, token provider and payment processor are integrated. */
class RealPaymentService : PaymentService {
    private val mutableSession = MutableStateFlow<PaymentSession?>(null)
    override val session = mutableSession.asStateFlow()
    override suspend fun preparePayment(cardId: String): PaymentSession = unavailable()
    override suspend fun authenticate(session: PaymentSession): Unit = unavailable()
    override suspend fun executePayment(session: PaymentSession): Transaction = unavailable()
    override fun cancel() { mutableSession.value = mutableSession.value?.copy(state = PaymentState.CANCELLED) }
    private fun unavailable(): Nothing = throw PaymentException("Pagos reales no disponibles: se requiere una integración bancaria certificada")
}
