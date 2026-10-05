package com.orel.wallet.domain

enum class CardNetwork { VISA, MASTERCARD, AMEX, LOYALTY, GIFT }
enum class BackgroundType { SKIN, COLOR, GRADIENT, IMAGE }
enum class ChipStyle { SILVER, GOLD, MINIMAL }
enum class NumberPosition { BOTTOM, TOP }
enum class TextStyle { CLASSIC, MONO }
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Visual metadata only. Neither this model nor persistence accepts PAN or CVV. */
data class CardAppearance(
    val id: String,
    val backgroundType: BackgroundType = BackgroundType.SKIN,
    val backgroundValue: String = "blue",
    val textColor: Long = 0xFFFFFFFF,
    val brightness: Float = .5f,
    val contrast: Float = .5f,
    val chipStyle: ChipStyle = ChipStyle.SILVER,
    val numberPosition: NumberPosition = NumberPosition.BOTTOM,
    val showName: Boolean = true,
    val textStyle: TextStyle = TextStyle.CLASSIC,
)

data class Card(
    val id: String,
    val displayName: String,
    val network: CardNetwork,
    val last4: String,
    val isDefault: Boolean = false,
    val isDemo: Boolean = true,
    val appearance: CardAppearance,
    val isLocked: Boolean = false,
    val isHidden: Boolean = false,
    val contactlessEnabled: Boolean = true,
    val sortOrder: Int = 0,
)

val Card.isPayableDemo: Boolean
    get() = isDemo && !isLocked && !isHidden && network in setOf(CardNetwork.VISA, CardNetwork.MASTERCARD, CardNetwork.AMEX)

enum class TransactionStatus { COMPLETED, FAILED, PENDING }
enum class TransactionChannel { IN_STORE, ONLINE }

data class Transaction(
    val id: String,
    val merchant: String,
    val amountMinor: Long,
    val currency: String = "EUR",
    val timestamp: Long,
    val cardId: String,
    val cardLabel: String,
    val last4: String,
    val status: TransactionStatus = TransactionStatus.COMPLETED,
    val isDemo: Boolean = true,
    val channel: TransactionChannel = TransactionChannel.IN_STORE,
)

data class WalletSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: Long = 0xFF0866F5,
    val requireBiometric: Boolean = true,
    val lockTimeoutSeconds: Int = 60,
    val contactlessEnabled: Boolean = true,
    val paymentNotifications: Boolean = true,
    val transactionNotifications: Boolean = true,
    val analytics: Boolean = false,
    val crashReporting: Boolean = false,
    val onboardingComplete: Boolean = false,
)

/** An opaque provider reference. Raw bank card credentials have no domain model. */
data class PaymentToken(
    val id: String,
    val providerId: String,
    val opaqueReference: String,
    val createdAt: Long,
    val expiresAt: Long? = null,
)
