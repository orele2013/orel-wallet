package com.orel.wallet

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orel.wallet.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class WalletUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val repository get() = (compose.activity.application as WalletApplication).repository
    private lateinit var sample:Card

    @Before fun resetPreferences() {
        runBlocking {
            repository.initialize()
            sample=repository.cards.first().first {it.network==CardNetwork.VISA}
            repository.updateCard(sample.copy(isHidden=false,isLocked=false))
            repository.setDefault(sample.id)
            repository.saveSettings(WalletSettings(themeMode=ThemeMode.LIGHT,onboardingComplete=false,requireBiometric=false))
        }
        compose.waitUntil(15_000) {compose.onAllNodesWithText("Comenzar").fetchSemanticsNodes().isNotEmpty()}
    }
    private fun home() {
        compose.onNodeWithText("Comenzar").performScrollTo().performClick()
        compose.waitUntil(10_000) {compose.onAllNodesWithText("Tu mundo,").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Tu mundo,").assertIsDisplayed()
    }

    @Test fun onboardingAndMainNavigation() {
        compose.onNodeWithText("Tu wallet.\nTu estilo.").assertExists()
        home()
        compose.onNodeWithText("MODO DEMO").assertExists()
        compose.onNodeWithText("Tarjetas").performClick()
        compose.onNodeWithText("Mis tarjetas").assertIsDisplayed()
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("A tu manera").assertIsDisplayed()
    }

    @Test fun addDemoCardPersistsAndOpensDetails() {
        home()
        compose.onNodeWithText("Tarjetas").performClick()
        compose.onNodeWithContentDescription("Añadir tarjeta").performClick()
        compose.onNodeWithText("Tarjeta de prueba").performClick()
        compose.onNodeWithText("Nombre opcional").performTextInput("UI Demo")
        compose.onNodeWithText("Crear tarjeta demo").performClick()
        compose.waitUntil(10_000) {compose.onAllNodesWithText("Detalles de tarjeta").fetchSemanticsNodes().isNotEmpty()}
        assertTrue(compose.onAllNodesWithText("UI Demo").fetchSemanticsNodes().isNotEmpty())
        val created=runBlocking {repository.cards.first().first {it.displayName=="UI Demo"}}
        assertTrue(created.isDemo)
        assertEquals(4,created.last4.length)
        runBlocking {repository.removeCard(created.id)}
    }

    @Test fun appearanceSavePreservesCardIdentity() {
        home()
        compose.onNodeWithText("Tarjetas").performClick()
        compose.onNode(hasContentDescription(sample.displayName,substring=true)).performClick()
        compose.onNodeWithText("Card Appearance").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Skin Aurora").performScrollTo().performClick()
        compose.onNodeWithText("Guardar apariencia").performClick()
        compose.waitUntil(10_000) {runBlocking {repository.findCard(sample.id)?.appearance?.backgroundValue=="violet"}}
        val updated=runBlocking {repository.findCard(sample.id)}!!
        assertEquals(sample.network,updated.network)
        assertEquals(sample.last4,updated.last4)
        assertEquals(sample.id,updated.id)
    }

    @Test fun historySearchAndChannelFilter() {
        home()
        compose.onAllNodesWithText("Historial").onLast().performClick()
        compose.onNodeWithText("Buscar movimientos").performTextInput("ZZ_no_merchant")
        compose.onNodeWithText("Sin resultados").assertExists()
        compose.onNodeWithText("Buscar movimientos").performTextClearance()
        compose.onNodeWithText("En línea").performClick()
        compose.onNodeWithText("Tu actividad").assertIsDisplayed()
    }

    @Test fun themeSelectionPersists() {
        home()
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Oscuro").performClick()
        compose.waitUntil(10_000) {runBlocking {repository.settings.first().themeMode==ThemeMode.DARK}}
        assertEquals(ThemeMode.DARK,runBlocking {repository.settings.first().themeMode})
    }

    @Test fun explicitDemoPaymentCreatesConfirmedTransaction() {
        home()
        val before=runBlocking {repository.transactions.first().size}
        compose.onNodeWithText("Pagar demo").performClick()
        compose.waitUntil(10_000) {compose.onAllNodesWithText("Simular autenticación · Demo").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Simular autenticación · Demo").performScrollTo().performClick()
        compose.onNodeWithText("Simular compra de 12,50 €").performScrollTo().performClick()
        compose.waitUntil(10_000) {compose.onAllNodesWithText("Pago demo completado").fetchSemanticsNodes().isNotEmpty()}
        val tx=runBlocking {repository.transactions.first()}
        assertEquals(before+1,tx.size)
        assertEquals(1250L,tx.first().amountMinor)
        assertEquals("DEMO STORE",tx.first().merchant)
        assertTrue(tx.first().isDemo)
        compose.onNodeWithText("Ver movimiento").performScrollTo().performClick()
        compose.onNodeWithText("Detalle del movimiento").assertIsDisplayed()
    }
}
