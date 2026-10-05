package com.orel.wallet.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.orel.wallet.domain.Card
import com.orel.wallet.nfc.NfcStatus
import com.orel.wallet.ui.components.*
import com.orel.wallet.wallet.GoogleWalletLauncher

@Composable fun WalletAssistScreen(card: Card?, nfc: NfcStatus, onBack: () -> Unit) {
    val context = LocalContext.current
    val launcher = remember(context) { GoogleWalletLauncher(context) }
    var installed by remember { mutableStateOf(launcher.installed()) }
    var error by remember { mutableStateOf<String?>(null) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, launcher) {
        val observer = LifecycleEventObserver { _, event -> if(event == Lifecycle.Event.ON_RESUME) installed = launcher.installed() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    fun launch(action: () -> Boolean) { if(!action()) error = "No se pudo abrir. Comprueba las aplicaciones instaladas y los ajustes de Android." }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Preparar pago con Wallet", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
            card?.let { WalletCard(it); Text("${it.displayName} · referencia visual en Orel", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant) }
            Text("Tu móvil. Tu Wallet.", style=MaterialTheme.typography.headlineMedium)
            Text("Google Wallet realiza el pago con su tarjeta seleccionada. La tarjeta que ves en Orel no cambia esa selección.", style=MaterialTheme.typography.bodyMedium)
            SettingRow(Icons.Outlined.AccountBalanceWallet, "Google Wallet", if(installed) "Aplicación disponible" else "Instala la aplicación oficial")
            SettingRow(Icons.Outlined.Nfc, "NFC", when { !nfc.available -> "Este dispositivo no tiene NFC"; nfc.enabled -> "Activado"; else -> "Actívalo en los ajustes de Android" })
            if(nfc.available && !nfc.enabled) OutlinedButton({ launch(launcher::openNfcSettings) }, Modifier.fillMaxWidth()) { Text("Activar NFC en Android") }
            Text("Antes de acercar el móvil", style=MaterialTheme.typography.titleMedium)
            Text("1. Añade tu Pixpay en Google Wallet.\n2. Elige allí la tarjeta que quieres usar.\n3. Configura Google Wallet como wallet NFC predeterminada.\n4. Desbloquea el teléfono y acerca su parte trasera al datáfono.", style=MaterialTheme.typography.bodyMedium)
            if(nfc.available) OutlinedButton({ launch(launcher::openDefaultWalletSettings) }, Modifier.fillMaxWidth()) { Text("Elegir wallet NFC predeterminada") }
            Button({ launch(if(installed) launcher::openWallet else launcher::openInstallPage) }, Modifier.fillMaxWidth().heightIn(min=54.dp)) {
                Text(if(installed) "Abrir Google Wallet" else "Instalar Google Wallet")
            }
            Text("Puedes mantener Orel abierta para pagar con la tarjeta predeterminada de Wallet. Android o Google pueden solicitar su propia verificación; el desbloqueo de Orel no la sustituye.", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Orel no recibe el resultado del pago. Confirma la compra en el datáfono y consulta los movimientos en Pixpay. Volver de Wallet no indica que se haya pagado.", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton({ launch(launcher::openHelp) }) { Text("Ver requisitos oficiales") }
            error?.let { Text(it, color=MaterialTheme.colorScheme.error, style=MaterialTheme.typography.bodySmall) }
        }
    }
}
