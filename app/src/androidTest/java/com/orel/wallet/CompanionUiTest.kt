package com.orel.wallet

import android.content.ComponentName
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.orel.wallet.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CompanionUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as WalletApplication
    private val repository get() = app.repository

    @Before fun reset() {
        assumeFalse(BuildConfig.DEMO_MODE)
        runBlocking {
            repository.initialize()
            assertTrue(repository.cards.first().none {it.isDemo})
            assertTrue(repository.transactions.first().isEmpty())
            repository.cards.first().forEach { repository.removeCard(it.id) }
            repository.saveSettings(WalletSettings(themeMode=ThemeMode.LIGHT,requireBiometric=false,onboardingComplete=false))
        }
        compose.waitUntil(15_000) { compose.onAllNodesWithText("Comenzar").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun home() {
        compose.onNodeWithText("Comenzar").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Tu mundo,").fetchSemanticsNodes().isNotEmpty() }
    }
    private fun addReference() {
        home()
        compose.onNodeWithText("Añadir tarjeta").performClick()
        compose.onNodeWithText("Últimos 4 dígitos").performTextInput("4821")
        compose.onNodeWithText("Guardar referencia").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Detalles de tarjeta").fetchSemanticsNodes().isNotEmpty() }
    }
    @Test fun personalReleaseStartsEmptyAndCannotActAsAnNfcPaymentService() {
        assertEquals("com.orel.wallet.companion", compose.activity.packageName)
        assertTrue(runBlocking {repository.cards.first().isEmpty()})
        assertTrue(runBlocking {repository.transactions.first().isEmpty()})
        val info=compose.activity.packageManager.getServiceInfo(ComponentName(compose.activity,"com.orel.wallet.nfc.DemoHceService"),android.content.pm.PackageManager.MATCH_DISABLED_COMPONENTS)
        assertFalse(info.enabled)
        try { runBlocking {app.paymentService.preparePayment("unused")}; fail("No direct real payments") }
        catch (_: com.orel.wallet.payments.PaymentException) { }
        home()
        compose.onAllNodesWithText("Historial").onLast().performClick()
        compose.onNodeWithText("Consulta los pagos en Pixpay").assertExists()
        compose.onNodeWithText("Pago completado").assertDoesNotExist()
        compose.onNodeWithText("Ajustes").performClick()
        compose.onNodeWithText("Preferencias de NFC").performScrollTo().performClick()
        val walletInstalled=compose.activity.packageManager.getLaunchIntentForPackage("com.google.android.apps.walletnfcrel")!=null
        compose.onNodeWithText(if(walletInstalled) "Abrir Google Wallet" else "Instalar Google Wallet").performScrollTo().assertIsDisplayed()
    }
    @Test fun referenceAndSkinPersistWithoutCreatingATransaction() {
        addReference()
        val before=runBlocking {repository.cards.first().single()}
        assertFalse(before.isDemo)
        assertFalse(before.contactlessEnabled)
        assertEquals("4821",before.last4)
        compose.onNodeWithText("Card Appearance").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Skin Aurora").performScrollTo().performClick()
        compose.onNodeWithText("Guardar apariencia").performClick()
        compose.waitUntil(10_000) {runBlocking {repository.findCard(before.id)?.appearance?.backgroundValue=="violet"}}
        val after=runBlocking {repository.findCard(before.id)}!!
        assertEquals(before.id,after.id)
        assertEquals(before.network,after.network)
        assertEquals(before.last4,after.last4)
        assertTrue(runBlocking {repository.transactions.first().isEmpty()})
        compose.waitUntil(10_000) {compose.onAllNodesWithText("Detalles de tarjeta").fetchSemanticsNodes().isNotEmpty()}
        compose.waitUntil(10_000) {compose.onAllNodesWithText("Apariencia guardada").fetchSemanticsNodes().isEmpty()}
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Preparar pago con Wallet"))
        compose.onNodeWithText("Preparar pago con Wallet").performClick()
        compose.waitUntil(10_000) {compose.onAllNodesWithText("Tu móvil. Tu Wallet.").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Tu móvil. Tu Wallet.").assertExists()
        compose.onNodeWithText("Orel no recibe el resultado del pago.",substring=true).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Pago completado").assertDoesNotExist()
    }
    @Test fun invalidFullNumberCannotCreateReference() {
        home()
        compose.onNodeWithText("Añadir tarjeta").performClick()
        compose.onNodeWithText("Últimos 4 dígitos").performTextInput("4111111111111111")
        compose.onNodeWithText("Guardar referencia").performScrollTo().assertIsNotEnabled()
        assertTrue(runBlocking {repository.cards.first().isEmpty()})
    }

    @Test fun internationalArtCanBeSearchedSelectedSavedAndReopened() {
        addReference()
        val before=runBlocking {repository.cards.first().single()}
        compose.onNodeWithText("Card Appearance").performScrollTo().performClick()
        compose.onNodeWithText("Amex").performScrollTo().performClick()
        compose.onNodeWithText("Buscar diseño o país").performScrollTo().performTextInput("ANA Japón")
        compose.onNodeWithText("3 diseños").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("Diseño ANA Premium, Japón").assertExists()
        compose.onNodeWithText("Buscar diseño o país").performTextReplacement("no existe")
        compose.onNodeWithText("No hay diseños con esa búsqueda.",substring=true).assertExists()
        compose.onNodeWithText("Buscar diseño o país").performTextReplacement("Centurion Wiley")
        compose.onNodeWithContentDescription("Diseño Centurion · Kehinde Wiley, Italia / Reino Unido")
            .performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Brillo de la tarjeta").assertDoesNotExist()
        compose.onNodeWithText("Guardar apariencia").performClick()
        compose.waitUntil(10_000) {runBlocking {repository.findCard(before.id)?.appearance?.backgroundValue=="amex_centurion_wiley"}}
        // A second Room instance reads the persisted selection, independent of editor state.
        val after=runBlocking {com.orel.wallet.data.RoomWalletRepository(compose.activity,seedDemoData=false).findCard(before.id)}!!
        assertEquals(before.id,after.id)
        assertEquals(before.network,after.network)
        assertEquals(before.last4,after.last4)
        assertFalse(after.isDemo)
        assertFalse(after.contactlessEnabled)
        assertTrue(runBlocking {repository.transactions.first().isEmpty()})
        compose.waitUntil(10_000) {compose.onAllNodesWithText("Detalles de tarjeta").fetchSemanticsNodes().isNotEmpty()}
        compose.waitUntil(10_000) {compose.onAllNodesWithText("Apariencia guardada").fetchSemanticsNodes().isEmpty()}
        compose.onNodeWithText("Card Appearance").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Diseño Centurion · Kehinde Wiley, Italia / Reino Unido")
            .performScrollTo().assertIsSelected()
        compose.onNodeWithText("VISA · •••• 4821").assertExists()
        // An existing original skin can still replace the full artwork.
        compose.onNodeWithText("Imagen").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Skin Aurora").performScrollTo().performClick()
        compose.onNodeWithText("Guardar apariencia").performClick()
        compose.waitUntil(10_000) {runBlocking {repository.findCard(before.id)?.appearance?.backgroundValue=="violet"}}
    }
}
