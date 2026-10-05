package com.orel.wallet.wallet

import com.orel.wallet.data.InMemoryWalletRepository
import com.orel.wallet.data.toDomain
import com.orel.wallet.data.toEntity
import com.orel.wallet.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class CardDesignTest {
    @Test fun exclusiveArtPersistsAndCannotChangePixpayIdentityOrAuthorizePayments() = runTest {
        val repo=InMemoryWalletRepository(seedDemoData=false)
        repo.initialize()
        val card=repo.addExternalCard(CardNetwork.VISA,"Pixpay","4821")
        val design=CardDesignCatalog.designs.single {it.id=="amex_centurion_wiley"}
        val appearance=card.appearance.copy(brightness=.9f,contrast=.2f).withDesign(design)
        repo.saveAppearance(card.id,appearance)
        val after=repo.findCard(card.id)!!.toEntity().toDomain()
        assertEquals(card.id,after.id)
        assertEquals(card.network,after.network)
        assertEquals(card.last4,after.last4)
        assertFalse(after.isDemo)
        assertFalse(after.contactlessEnabled)
        assertFalse(after.isPayableDemo)
        assertEquals(design,CardDesignCatalog.find(after.appearance))
        assertEquals(.5f,after.appearance.brightness)
        assertEquals(.5f,after.appearance.contrast)
        assertTrue(repo.transactions.first().isEmpty())
    }

    @Test fun searchCombinesNameAndCountryAndGroupAndSwitchingToAnImageClearsCatalogMode() {
        val results=CardDesignCatalog.matching(CardDesignGroup.TRAVEL,"ANA Japón")
        assertEquals(setOf("amex_ana","amex_ana_gold","amex_ana_premium"),results.map {it.id}.toSet())
        assertTrue(CardDesignCatalog.matching(CardDesignGroup.PERSONAL,"Centurion").isEmpty())
        assertTrue(CardDesignCatalog.matching(query="invented design").isEmpty())
        val design=CardDesignCatalog.designs.first()
        val appearance=CardAppearance("reference").withDesign(design)
        assertNull(CardDesignCatalog.find(appearance.copy(backgroundType=BackgroundType.IMAGE)))
        assertNull(CardDesignCatalog.find(appearance.copy(backgroundValue="violet")))
    }
}
