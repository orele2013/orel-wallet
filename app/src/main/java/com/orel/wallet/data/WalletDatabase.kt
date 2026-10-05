package com.orel.wallet.data

import androidx.room.*
import com.orel.wallet.domain.*
import com.orel.wallet.domain.Transaction
import kotlinx.coroutines.flow.Flow

/** Schema intentionally contains only last4 and visual metadata, never payment credentials. */
@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val displayName: String, val network: String, val last4: String,
    val isDefault: Boolean, val isDemo: Boolean, val isLocked: Boolean, val isHidden: Boolean,
    val contactlessEnabled: Boolean, val sortOrder: Int,
    val appearanceId: String, val backgroundType: String, val backgroundValue: String,
    val textColor: Long, val brightness: Float, val contrast: Float, val chipStyle: String,
    val numberPosition: String, val showName: Boolean, val textStyle: String,
)

@Entity(tableName = "transactions", indices = [Index("timestamp")])
data class TransactionEntity(
    @PrimaryKey val id: String,
    val merchant: String, val amountMinor: Long, val currency: String, val timestamp: Long,
    val cardId: String, val cardLabel: String, val last4: String, val status: String,
    val isDemo: Boolean, val channel: String,
)

@Entity(tableName = "settings")
data class SettingsEntity(
    @PrimaryKey val id: Int = 1,
    val themeMode: String, val accentColor: Long, val requireBiometric: Boolean,
    val lockTimeoutSeconds: Int, val contactlessEnabled: Boolean,
    val paymentNotifications: Boolean, val transactionNotifications: Boolean,
    val analytics: Boolean, val crashReporting: Boolean, val onboardingComplete: Boolean,
)

@Dao
interface WalletDao {
    @Query("SELECT * FROM cards ORDER BY sortOrder ASC, id ASC") fun observeCards(): Flow<List<CardEntity>>
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id ASC") fun observeTransactions(): Flow<List<TransactionEntity>>
    @Query("SELECT * FROM settings WHERE id = 1") fun observeSettings(): Flow<SettingsEntity?>
    @Query("SELECT * FROM cards ORDER BY sortOrder ASC, id ASC") suspend fun allCards(): List<CardEntity>
    @Query("SELECT * FROM cards WHERE id = :id") suspend fun card(id: String): CardEntity?
    @Query("SELECT * FROM settings WHERE id = 1") suspend fun settings(): SettingsEntity?
    @Query("SELECT * FROM transactions WHERE id = :id") suspend fun transaction(id: String): TransactionEntity?
    @Query("DELETE FROM cards") suspend fun clearCards()
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCards(cards: List<CardEntity>)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertTransaction(transaction: TransactionEntity)
    @Upsert suspend fun saveSettings(settings: SettingsEntity)
}

@Database(entities = [CardEntity::class, TransactionEntity::class, SettingsEntity::class], version = 1, exportSchema = true)
abstract class WalletDatabase : RoomDatabase() { abstract fun walletDao(): WalletDao }

internal fun Card.toEntity() = CardEntity(
    id, displayName, network.name, last4, isDefault, isDemo, isLocked, isHidden,
    contactlessEnabled, sortOrder, appearance.id, appearance.backgroundType.name,
    appearance.backgroundValue, appearance.textColor, appearance.brightness, appearance.contrast,
    appearance.chipStyle.name, appearance.numberPosition.name, appearance.showName, appearance.textStyle.name,
)
internal fun CardEntity.toDomain() = Card(
    id, displayName, CardNetwork.valueOf(network), last4, isDefault, isDemo,
    CardAppearance(appearanceId, BackgroundType.valueOf(backgroundType), backgroundValue, textColor,
        brightness, contrast, ChipStyle.valueOf(chipStyle), NumberPosition.valueOf(numberPosition),
        showName, TextStyle.valueOf(textStyle)),
    isLocked, isHidden, contactlessEnabled, sortOrder,
)
internal fun Transaction.toEntity() = TransactionEntity(id, merchant, amountMinor, currency,
    timestamp, cardId, cardLabel, last4, status.name, isDemo, channel.name)
internal fun TransactionEntity.toDomain() = Transaction(id, merchant, amountMinor, currency,
    timestamp, cardId, cardLabel, last4, TransactionStatus.valueOf(status), isDemo, TransactionChannel.valueOf(channel))
internal fun WalletSettings.toEntity() = SettingsEntity(themeMode = themeMode.name, accentColor = accentColor,
    requireBiometric = requireBiometric, lockTimeoutSeconds = lockTimeoutSeconds, contactlessEnabled = contactlessEnabled,
    paymentNotifications = paymentNotifications, transactionNotifications = transactionNotifications,
    analytics = analytics, crashReporting = crashReporting, onboardingComplete = onboardingComplete)
internal fun SettingsEntity.toDomain() = WalletSettings(ThemeMode.valueOf(themeMode), accentColor,
    requireBiometric, lockTimeoutSeconds, contactlessEnabled, paymentNotifications, transactionNotifications,
    analytics, crashReporting, onboardingComplete)
