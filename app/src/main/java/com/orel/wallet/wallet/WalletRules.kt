package com.orel.wallet.wallet

import com.orel.wallet.domain.*
import java.util.UUID

/** Invariants shared by Room and the in-memory implementation. */
internal object WalletRules {
    fun name(value: String): String = value.trim().also {
        require(it.length in 1..40) { "El nombre debe tener entre 1 y 40 caracteres" }
        require(!Regex("(?:[0-9][ -]?){12,19}").containsMatchIn(it)) { "No introduzcas números completos de tarjeta" }
    }

    fun appearance(value: CardAppearance) {
        require(value.id.isNotBlank())
        require(value.brightness.isFinite() && value.brightness in 0f..1f)
        require(value.contrast.isFinite() && value.contrast in 0f..1f)
        require(value.backgroundValue.length in 1..2048)
        require(value.textColor in 0..0xFFFFFFFF)
        if (value.backgroundType == BackgroundType.IMAGE) {
            require(privateImagePath.matches(value.backgroundValue)) { "La imagen debe ser una copia privada importada por Orel Wallet" }
        }
    }

    fun newCard(network: CardNetwork, displayName: String, order: Int): Card {
        val id = UUID.randomUUID().toString()
        val skin = when (network) {
            CardNetwork.VISA -> "blue"
            CardNetwork.MASTERCARD -> "graphite"
            CardNetwork.AMEX -> "gold"
            CardNetwork.LOYALTY -> "mint"
            CardNetwork.GIFT -> "violet"
        }
        // Synthetic display digits are generated locally; they are never a bank credential.
        val digits = (UUID.randomUUID().leastSignificantBits and Long.MAX_VALUE).rem(10_000).toString().padStart(4, '0')
        return Card(id, name(displayName), network, digits, appearance = CardAppearance(id, backgroundValue = skin),
            contactlessEnabled = network in payableNetworks, sortOrder = order)
    }

    fun normalize(cards: List<Card>, preferredDefault: String? = null): List<Card> {
        require(cards.map { it.id }.distinct().size == cards.size)
        cards.forEach {
            require(it.id.isNotBlank())
            require(it.last4.matches(Regex("[0-9]{4}")))
            require(it.displayName == name(it.displayName))
            require(it.appearance.id == it.id)
            appearance(it.appearance)
        }
        val eligible = cards.filter { it.eligibleDefault }
        val chosen = preferredDefault?.let { id -> eligible.firstOrNull { it.id == id } }
            ?: eligible.firstOrNull { it.isDefault } ?: eligible.firstOrNull()
        return cards.mapIndexed { index, card -> card.copy(sortOrder = index, isDefault = card.id == chosen?.id) }
    }

    fun newExternalCard(network: CardNetwork, displayName: String, last4: String, order: Int): Card {
        require(network in payableNetworks) { "Selecciona la red que aparece en tu tarjeta" }
        require(last4.matches(Regex("[0-9]{4}"))) { "Introduce únicamente los últimos cuatro dígitos" }
        return newCard(network, displayName, order).copy(last4 = last4, isDemo = false, contactlessEnabled = false)
    }

    fun update(current: List<Card>, changed: Card): List<Card> {
        val existing = current.firstOrNull { it.id == changed.id } ?: throw IllegalArgumentException("Tarjeta inexistente")
        require(existing.network == changed.network && existing.last4 == changed.last4 && existing.isDemo == changed.isDemo) {
            "La identidad de pago no puede modificarse"
        }
        require(existing.appearance.id == changed.appearance.id)
        require(existing.sortOrder == changed.sortOrder) { "Usa reorder para cambiar el orden" }
        require(!changed.isDefault || changed.eligibleDefault || existing.isDefault) { "Tarjeta no elegible" }
        val result = current.map { if (it.id == changed.id) changed.copy(displayName = name(changed.displayName)) else it }
        return normalize(result, changed.id.takeIf { changed.isDefault && changed.eligibleDefault })
    }

    fun reorder(current: List<Card>, ids: List<String>): List<Card> {
        require(ids.size == current.size && ids.toSet() == current.map { it.id }.toSet()) { "El orden debe incluir cada tarjeta una vez" }
        val byId = current.associateBy { it.id }
        return normalize(ids.map { byId.getValue(it) })
    }

    fun settings(value: WalletSettings) {
        require(value.lockTimeoutSeconds in 15..600)
        require(value.accentColor in 0..0xFFFFFFFF)
    }

    fun transaction(value: Transaction) {
        require(value.id.isNotBlank() && value.cardId.isNotBlank())
        require(value.merchant.trim().length in 1..120)
        require(value.cardLabel.trim().length in 1..40)
        require(value.last4.matches(Regex("[0-9]{4}")))
        require(value.amountMinor > 0 && value.amountMinor <= 100_000_000)
        require(value.currency.matches(Regex("[A-Z]{3}")))
        require(value.timestamp >= 0)
        require(value.isDemo) { "La demo no puede guardar pagos reales" }
    }

    val payableNetworks = setOf(CardNetwork.VISA, CardNetwork.MASTERCARD, CardNetwork.AMEX)
    private val privateImagePath = Regex("/data/(?:user(?:_de)?/[0-9]+|data)/com\\.orel\\.wallet(?:\\.companion)?/no_backup/card-images/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.image")
    val Card.eligibleDefault: Boolean get() = !isLocked && !isHidden && network in payableNetworks
}
