package com.orel.wallet.presentation.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.orel.wallet.BuildConfig
import com.orel.wallet.domain.*
import com.orel.wallet.nfc.NfcStatus
import com.orel.wallet.ui.components.*

@Composable fun SettingsScreen(settings:WalletSettings,save:(WalletSettings)->Unit,onCards:()->Unit,onNfc:()->Unit) {
    var info by remember { mutableStateOf<String?>(null) }
    var privacy by remember {mutableStateOf(false)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item { Text("A tu manera",style=MaterialTheme.typography.headlineMedium); Text("Ajustes de Orel Wallet",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=6.dp,bottom=12.dp)); DemoBadge() }
        item { SectionLabel("Apariencia") }
        item { Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) { ThemeMode.entries.forEach { mode -> FilterChip(settings.themeMode==mode,{save(settings.copy(themeMode=mode))},label={Text(when(mode){ThemeMode.SYSTEM->"Sistema"; ThemeMode.LIGHT->"Claro"; ThemeMode.DARK->"Oscuro"})}) } } }
        item { Row(Modifier.fillMaxWidth().padding(vertical=10.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) { listOf(0xFF0866F5,0xFF6952CE,0xFF087F72,0xFF95611F).forEach {accent -> ColorSwatch(androidx.compose.ui.graphics.Color(accent),"Acento ${accent.toString(16)}",settings.accentColor==accent) {save(settings.copy(accentColor=accent))} } } }
        item { SectionLabel("Seguridad") }
        item { SettingRow(Icons.Outlined.Fingerprint,"Solicitar autenticación","Biometría o credencial del dispositivo",trailing={WalletSwitch("Solicitar autenticación",settings.requireBiometric,{save(settings.copy(requireBiometric=it))})}) }
        item { SettingRow(Icons.Outlined.Timer,"Bloqueo al volver a la app","Después de ${settings.lockTimeoutSeconds} segundos",{
            val next=when(settings.lockTimeoutSeconds){30->60;60->300;else->30}; save(settings.copy(lockTimeoutSeconds=next))
        }) }
        item { SectionLabel("Wallet") }
        item { SettingRow(Icons.Outlined.CreditCard,"Tarjeta principal","Gestionar tus tarjetas",onCards) }
        item { SettingRow(Icons.Outlined.Nfc,"Preferencias de NFC","Disponibilidad y contactless",onNfc) }
        item { SectionLabel("Notificaciones") }
        item { SettingRow(Icons.Outlined.NotificationsNone,"Avisos de pagos","Preferencia para el proveedor futuro",trailing={WalletSwitch("Avisos de pagos",settings.paymentNotifications,{save(settings.copy(paymentNotifications=it))})}) }
        item { SettingRow(Icons.Outlined.ReceiptLong,"Avisos de movimientos","Preferencia para el proveedor futuro",trailing={WalletSwitch("Avisos de movimientos",settings.transactionNotifications,{save(settings.copy(transactionNotifications=it))})}) }
        item { SectionLabel("Privacidad") }
        item { SettingRow(Icons.Outlined.Shield,"Privacidad y datos","Almacenamiento local, sin seguimiento",{privacy=true}) }
        item { SettingRow(Icons.Outlined.Analytics,"Analíticas","Desactivadas en esta versión",trailing={WalletSwitch("Analíticas",false,{},enabled=false)}) }
        item { SettingRow(Icons.Outlined.BugReport,"Informes de errores","Desactivados en esta versión",trailing={WalletSwitch("Informes de errores",false,{},enabled=false)}) }
        item { SectionLabel("Acerca de") }
        item { SettingRow(Icons.Outlined.Info,"Orel Wallet","Versión ${BuildConfig.VERSION_NAME}",{info="about"}) }
        item { SettingRow(Icons.Outlined.Code,"Licencias de código abierto",onClick={info="licenses"}) }
        item { Text("Demo Mode does not perform real payments.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=18.dp,bottom=24.dp)) }
    }
    if(privacy) AlertDialog(onDismissRequest={privacy=false},title={Text("Tus datos se quedan contigo")},text={Text("Las tarjetas y movimientos demo se guardan en este dispositivo. No se recopilan analíticas ni informes de errores. Las imágenes elegidas permanecen en almacenamiento privado. No se guardan PAN completos ni CVV. Las referencias de tokens usan Android Keystore y no se incluyen en copias de seguridad. Puedes borrar todos los datos desde los ajustes de Android. Esta versión no conecta con bancos ni servidores externos.")},confirmButton={TextButton({privacy=false}) {Text("Cerrar")}})
    if(info!=null) AlertDialog(onDismissRequest={info=null},title={Text(if(info=="licenses") "Código abierto" else "Orel Wallet")},text={Text(if(info=="licenses") "AndroidX / Jetpack Compose / Room / Navigation / Biometric — Apache License 2.0\n\nKotlin y Kotlin Coroutines — Apache License 2.0\n\nCoil — Apache License 2.0\n\nJUnit — Eclipse Public License 1.0\n\nLos avisos y enlaces completos están en OPEN_SOURCE_NOTICES.md del proyecto." else "Versión ${BuildConfig.VERSION_NAME}\n\nUna wallet de demostración con diseño original y personalización visual. No realiza pagos reales.\n\nNecesita un proveedor de pagos autorizado para incorporar tarjetas bancarias.")},confirmButton={TextButton({info=null}) {Text("Cerrar")}})
}

@Composable fun NfcScreen(status:NfcStatus,settings:WalletSettings,save:(WalletSettings)->Unit,onBack:()->Unit) {
    val context=LocalContext.current
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Contactless",onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.Nfc,null,modifier=Modifier.size(58.dp),tint=MaterialTheme.colorScheme.primary)
            Text(when { !status.available -> "NFC no disponible"; status.enabled -> "NFC activado"; else -> "NFC desactivado" },style=MaterialTheme.typography.headlineMedium)
            Text("NFC disponible: ${if(status.available) "Sí" else "No"}\nNFC activado: ${if(status.enabled) "Sí" else "No"}\nCompatibilidad HCE: ${if(status.hceSupported) "Sí" else "No"}",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            SettingRow(Icons.Outlined.Contactless,"Credencial demo contactless","Solo con sesión autorizada en primer plano",trailing={WalletSwitch("Credencial demo contactless",settings.contactlessEnabled,{save(settings.copy(contactlessEnabled=it))},enabled=status.available && status.hceSupported)})
            if(status.available) OutlinedButton({context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))},Modifier.fillMaxWidth()) {Text("Abrir ajustes NFC de Android")}
            DemoBadge()
            Text("Este modo HCE usa una credencial de laboratorio. No funciona con terminales bancarios y no transmite números de tarjeta. La simulación de compra es independiente del lector NFC.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Puedes completar una compra demo incluso si tu dispositivo no tiene NFC.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
