package com.orel.wallet.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.orel.wallet.domain.CardNetwork
import com.orel.wallet.presentation.WalletViewModel
import com.orel.wallet.ui.components.*

@Composable fun ExternalAddCardScreen(vm: WalletViewModel, onBack: () -> Unit, onAdded: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("Pixpay") }
    var last4 by rememberSaveable { mutableStateOf("") }
    var network by rememberSaveable { mutableStateOf(CardNetwork.VISA) }
    var busy by remember { mutableStateOf(false) }
    var invalidDigits by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Añadir referencia", onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement=Arrangement.spacedBy(18.dp)) {
            Text("Tu tarjeta,\na tu manera.", style=MaterialTheme.typography.headlineLarge)
            Text("Guarda una referencia visual y personalízala. La tarjeta para pagar se añade por separado en Google Wallet.", style=MaterialTheme.typography.bodyMedium)
            OutlinedTextField(name, { name=it.take(40) }, label={Text("Nombre de la tarjeta")}, singleLine=true, modifier=Modifier.fillMaxWidth())
            Text("Red que aparece en tu tarjeta", style=MaterialTheme.typography.labelLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf(CardNetwork.VISA, CardNetwork.MASTERCARD, CardNetwork.AMEX).forEach { value ->
                    FilterChip(network==value, {network=value}, label={Text(if(value==CardNetwork.MASTERCARD) "Mastercard" else value.name)})
                }
            }
            OutlinedTextField(last4, { value ->
                if(value.length<=4 && value.all {it in '0'..'9'}) {last4=value; invalidDigits=false} else invalidDigits=true
            }, label={Text("Últimos 4 dígitos")}, supportingText={Text(if(invalidDigits) "Introduce solo cuatro dígitos, nunca el número completo." else "Solo para identificar tu tarjeta visualmente.")}, isError=invalidDigits,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number), singleLine=true, modifier=Modifier.fillMaxWidth())
            Text("No introduzcas el número completo, CVV ni PIN. Guardar esta referencia no añade una tarjeta a Google Wallet ni comprueba que sea compatible.", style=MaterialTheme.typography.bodySmall, color=MaterialTheme.colorScheme.onSurfaceVariant)
            Button({
                busy=true
                vm.addExternal(network, name.trim(), last4, {id -> busy=false; onAdded(id)}, {busy=false})
            }, enabled=!busy && name.isNotBlank() && last4.length==4 && !invalidDigits, modifier=Modifier.fillMaxWidth().height(54.dp)) {Text(if(busy) "Guardando…" else "Guardar referencia")}
        }
    }
}
