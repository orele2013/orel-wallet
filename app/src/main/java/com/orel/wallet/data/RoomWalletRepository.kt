package com.orel.wallet.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.orel.wallet.domain.*
import com.orel.wallet.wallet.WalletRepository
import com.orel.wallet.wallet.WalletRules
import com.orel.wallet.wallet.WalletRules.eligibleDefault
import kotlinx.coroutines.flow.map

class RoomWalletRepository(context: Context) : WalletRepository {
    private val database = Room.databaseBuilder(context.applicationContext, WalletDatabase::class.java, "orel-wallet-demo.db").build()
    private val dao = database.walletDao()
    override val cards = dao.observeCards().map { values -> values.map { it.toDomain() } }
    override val transactions = dao.observeTransactions().map { values -> values.map { it.toDomain() } }
    override val settings = dao.observeSettings().map { it?.toDomain() ?: WalletSettings() }

    override suspend fun initialize() = database.withTransaction {
        // Persisted settings row is also the initialization marker, so deleted cards stay deleted.
        if (dao.settings() == null) {
            if (dao.allCards().isEmpty()) dao.insertCards(DemoSeed.cards().map { it.toEntity() })
            DemoSeed.transactions().forEach { if (dao.transaction(it.id) == null) dao.insertTransaction(it.toEntity()) }
            dao.saveSettings(WalletSettings().toEntity())
        }
    }

    private suspend fun mutate(transform: (List<Card>) -> List<Card>): List<Card> = database.withTransaction {
        val updated = transform(dao.allCards().map { it.toDomain() })
        dao.clearCards()
        dao.insertCards(updated.map { it.toEntity() })
        updated
    }

    override suspend fun addCard(network: CardNetwork, displayName: String): Card {
        var id = ""
        val updated = mutate { existing ->
            val card = WalletRules.newCard(network, displayName, existing.size)
            id = card.id
            WalletRules.normalize(existing + card)
        }
        return updated.first { it.id == id }
    }

    override suspend fun removeCard(id: String) { mutate { WalletRules.normalize(it.filterNot { card -> card.id == id }) } }
    override suspend fun updateCard(card: Card) { mutate { WalletRules.update(it, card) } }
    override suspend fun setDefault(id: String) {
        mutate { existing ->
            require(existing.any { it.id == id && it.eligibleDefault }) { "Tarjeta no elegible" }
            WalletRules.normalize(existing, id)
        }
    }
    override suspend fun reorder(ids: List<String>) { mutate { WalletRules.reorder(it, ids) } }
    override suspend fun saveAppearance(cardId: String, appearance: CardAppearance) {
        mutate { existing ->
            val card = existing.firstOrNull { it.id == cardId } ?: throw IllegalArgumentException("Tarjeta inexistente")
            WalletRules.update(existing, card.copy(appearance = appearance))
        }
    }
    override suspend fun saveSettings(settings: WalletSettings) {
        WalletRules.settings(settings)
        dao.saveSettings(settings.toEntity())
    }
    override suspend fun recordTransaction(transaction: Transaction) = database.withTransaction {
        WalletRules.transaction(transaction)
        val previous = dao.transaction(transaction.id)?.toDomain()
        require(previous == null || previous == transaction) { "Conflicto de identidad de transacción" }
        if (previous == null) dao.insertTransaction(transaction.toEntity())
    }
    override suspend fun findCard(id: String) = dao.card(id)?.toDomain()
}
