package com.orel.wallet.wallet

import com.orel.wallet.domain.*
import kotlinx.coroutines.flow.Flow

interface WalletRepository {
    val cards: Flow<List<Card>>
    val transactions: Flow<List<Transaction>>
    val settings: Flow<WalletSettings>
    suspend fun initialize()
    suspend fun addCard(network: CardNetwork, displayName: String): Card
    suspend fun removeCard(id: String)
    suspend fun updateCard(card: Card)
    suspend fun setDefault(id: String)
    suspend fun reorder(ids: List<String>)
    suspend fun saveAppearance(cardId: String, appearance: CardAppearance)
    suspend fun saveSettings(settings: WalletSettings)
    suspend fun recordTransaction(transaction: Transaction)
    suspend fun findCard(id: String): Card?
}
