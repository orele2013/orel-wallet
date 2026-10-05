package com.orel.wallet.data

import com.orel.wallet.domain.*

internal object DemoSeed {
    fun cards() = listOf(
        Card("demo-visa", "Visa personal", CardNetwork.VISA, "4821", isDefault = true,
            appearance = CardAppearance("demo-visa", backgroundValue = "blue"), sortOrder = 0),
        Card("demo-mastercard", "Mastercard viajes", CardNetwork.MASTERCARD, "9134",
            appearance = CardAppearance("demo-mastercard", backgroundValue = "graphite"), sortOrder = 1),
        Card("demo-amex", "Amex premium", CardNetwork.AMEX, "2763",
            appearance = CardAppearance("demo-amex", backgroundValue = "gold", chipStyle = ChipStyle.GOLD), sortOrder = 2),
    )

    fun transactions(now: Long = System.currentTimeMillis()): List<Transaction> {
        val cards = cards()
        val entries = listOf("Café Aurora" to 450L, "Mercado del Barrio" to 3270L, "Librería Horizonte" to 1850L,
            "Metro Ciudad" to 240L, "Estudio Norte" to 6400L, "Tienda Jardín" to 1290L)
        return entries.mapIndexed { index, entry ->
            val card = cards[index % cards.size]
            Transaction("demo-history-$index", entry.first, entry.second,
                timestamp = (now - (index + 1) * 3_600_000L).coerceAtLeast(0), cardId = card.id,
                cardLabel = card.displayName, last4 = card.last4,
                channel = if (index == 2 || index == 4) TransactionChannel.ONLINE else TransactionChannel.IN_STORE)
        }
    }
}
