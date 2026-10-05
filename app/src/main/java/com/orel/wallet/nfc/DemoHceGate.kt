package com.orel.wallet.nfc

/** Memory only; process death, screen lock, backgrounding or expiration removes access. */
class DemoCredentialGate(
    private val clock: () -> Long = System::currentTimeMillis,
    private val ttlMillis: Long = 30_000,
) {
    init { require(ttlMillis in 1..60_000) }
    private var authorizedId: String? = null
    private var authorizationExpiresAt = 0L
    private var armedId: String? = null
    private var armedExpiresAt = 0L

    @Synchronized internal fun authorize(sessionId: String, expiresAt: Long) {
        require(sessionId.isNotBlank())
        disarm()
        authorizedId = sessionId
        authorizationExpiresAt = expiresAt
    }
    @Synchronized fun arm(sessionId: String): Boolean {
        if (sessionId != authorizedId || clock() >= authorizationExpiresAt) { disarm(); return false }
        armedId = sessionId
        armedExpiresAt = minOf(clock() + ttlMillis, authorizationExpiresAt)
        return true
    }
    @Synchronized fun credential(): String? {
        if (clock() >= armedExpiresAt || clock() >= authorizationExpiresAt) { disarm(); return null }
        return armedId?.let { "DEMO:$it" }
    }
    @Synchronized fun disarm() {
        authorizedId = null
        armedId = null
        authorizationExpiresAt = 0
        armedExpiresAt = 0
    }
}

object DemoHceGate {
    private val gate = DemoCredentialGate()
    internal fun authorize(sessionId: String, expiresAt: Long) = gate.authorize(sessionId, expiresAt)
    fun arm(sessionId: String): Boolean = gate.arm(sessionId)
    fun disarm() = gate.disarm()
    fun credential(): String? = gate.credential()
    internal val credentialGate: DemoCredentialGate get() = gate
}
