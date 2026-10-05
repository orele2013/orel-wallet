package com.orel.wallet.data

import com.orel.wallet.domain.*
import org.junit.Assert.*
import org.junit.Test

class EntityMappingTest {
    @Test fun visualChangesSurvivePersistenceWithoutChangingIdentity() {
        val card = Card("demo-id", "Personal", CardNetwork.VISA, "4821", isDefault = true,
            appearance = CardAppearance("demo-id", BackgroundType.GRADIENT, "blue", 0xFF111111, .7f, .3f,
                ChipStyle.MINIMAL, NumberPosition.TOP, false, TextStyle.MONO),
            isLocked = true, isHidden = true, contactlessEnabled = false, sortOrder = 2)
        assertEquals(card, card.toEntity().toDomain())
    }
    @Test fun historyAndSettingsRetainAllUserValues() {
        val tx = Transaction("tx", "Compra DEMO", 1290, timestamp = 100, cardId = "demo-id", cardLabel = "Personal",
            last4 = "4821", status = TransactionStatus.FAILED, channel = TransactionChannel.ONLINE)
        assertEquals(tx, tx.toEntity().toDomain())
        val settings = WalletSettings(ThemeMode.DARK, 0xFFAA3377, false, 300, false,
            false, false, true, true, true)
        assertEquals(settings, settings.toEntity().toDomain())
    }
}
