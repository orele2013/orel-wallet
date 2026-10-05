package com.orel.wallet.data

import com.orel.wallet.domain.*
import com.orel.wallet.wallet.WalletRepository
import com.orel.wallet.wallet.WalletRules
import com.orel.wallet.wallet.WalletRules.eligibleDefault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Deterministic local repository for tests and previews; mirrors production invariants. */
class InMemoryWalletRepository(private val seedDemoData: Boolean = true) : WalletRepository {
    private val mutex = Mutex()
    private val mutableCards = MutableStateFlow<List<Card>>(emptyList())
    private val mutableTransactions = MutableStateFlow<List<Transaction>>(emptyList())
    private val mutableSettings = MutableStateFlow(WalletSettings())
    override val cards = mutableCards.asStateFlow()
    override val transactions = mutableTransactions.asStateFlow()
    override val settings = mutableSettings.asStateFlow()
    private var initialized = false

    override suspend fun initialize() = mutex.withLock {
        if (!initialized) {
            if (seedDemoData && mutableCards.value.isEmpty() && mutableTransactions.value.isEmpty()) {
                mutableCards.value = DemoSeed.cards()
                mutableTransactions.value = DemoSeed.transactions()
            }
            initialized = true
        }
    }

    override suspend fun addCard(network: CardNetwork, displayName: String): Card = mutex.withLock {
        val added = WalletRules.newCard(network, displayName, mutableCards.value.size)
        mutableCards.value = WalletRules.normalize(mutableCards.value + added)
        mutableCards.value.first { it.id == added.id }
    }

    override suspend fun removeCard(id: String) = mutex.withLock {
        mutableCards.value = WalletRules.normalize(mutableCards.value.filterNot { it.id == id })
    }

    override suspend fun updateCard(card: Card) = mutex.withLock {
        mutableCards.value = WalletRules.update(mutableCards.value, card)
    }

    override suspend fun setDefault(id: String) = mutex.withLock {
        require(mutableCards.value.any { it.id == id && it.eligibleDefault }) { "Tarjeta no elegible" }
        mutableCards.value = WalletRules.normalize(mutableCards.value, id)
    }

    override suspend fun reorder(ids: List<String>) = mutex.withLock {
        mutableCards.value = WalletRules.reorder(mutableCards.value, ids)
    }

    override suspend fun saveAppearance(cardId: String, appearance: CardAppearance) = mutex.withLock {
        val existing = mutableCards.value.firstOrNull { it.id == cardId } ?: throw IllegalArgumentException("Tarjeta inexistente")
        mutableCards.value = WalletRules.update(mutableCards.value, existing.copy(appearance = appearance))
    }

    override suspend fun saveSettings(settings: WalletSettings) = mutex.withLock {
        WalletRules.settings(settings)
        mutableSettings.value = settings
    }

    override suspend fun recordTransaction(transaction: Transaction) = mutex.withLock {
        WalletRules.transaction(transaction)
        val previous = mutableTransactions.value.firstOrNull { it.id == transaction.id }
        require(previous == null || previous == transaction) { "Conflicto de identidad de transacción" }
        if (previous == null) mutableTransactions.value = (mutableTransactions.value + transaction).sortedByDescending { it.timestamp }
    }

    override suspend fun findCard(id: String): Card? = mutex.withLock { mutableCards.value.firstOrNull { it.id == id } }
}
