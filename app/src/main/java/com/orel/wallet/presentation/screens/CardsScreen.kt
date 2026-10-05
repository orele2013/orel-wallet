package com.orel.wallet.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orel.wallet.BuildConfig
import com.orel.wallet.domain.Card
import com.orel.wallet.presentation.WalletViewModel
import com.orel.wallet.ui.components.*

@Composable fun CardsScreen(cards:List<Card>,onAdd:()->Unit,onCard:(String)->Unit) {
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        item {
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Mis tarjetas",style=MaterialTheme.typography.headlineMedium); Text("${cards.size} ${if(BuildConfig.DEMO_MODE) "tarjetas · Demo Wallet" else "referencias · Orel Wallet"}",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium) }
                IconButton(onAdd) { Icon(Icons.Outlined.Add,"Añadir tarjeta") }
            }
        }
        if(cards.isEmpty()) item { EmptyState("Un lugar para tus tarjetas",if(BuildConfig.DEMO_MODE) "Empieza con una tarjeta demo" else "Añade una referencia visual de tu Pixpay") }
        items(cards,key={it.id}) { card ->
            Column {
                WalletCard(card,Modifier.clickable { onCard(card.id) },compact=true)
                Row(Modifier.fillMaxWidth().padding(top=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(card.displayName,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold,modifier=Modifier.weight(1f))
                    Text(when {card.isHidden -> "Oculta"; card.isLocked -> if(card.isDemo) "Bloqueada" else "Bloqueada en Orel"; card.isDefault -> if(card.isDemo) "Principal" else "Principal en Orel"; else -> if(card.isDemo) "Demo" else "Referencia"},style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { OutlinedButton(onAdd,Modifier.fillMaxWidth().height(54.dp)) { Icon(Icons.Outlined.Add,null); Spacer(Modifier.width(8.dp)); Text("Añadir tarjeta") } }
    }
}

@Composable fun CardDetailsScreen(card:Card?,vm:WalletViewModel,onBack:()->Unit,onAppearance:()->Unit,onPay:()->Unit) {
    if(card==null) { Column { ScreenHeader("Tarjeta",onBack); EmptyState("Tarjeta no disponible","Vuelve a tu wallet") }; return }
    var rename by remember { mutableStateOf(false) }
    var delete by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf(false) }
    var name by remember(card.id,card.displayName) { mutableStateOf(card.displayName) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Detalles de tarjeta",onBack)
        LazyColumn(contentPadding=PaddingValues(start=24.dp,end=24.dp,bottom=28.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            item {
                WalletCard(card)
                Row(Modifier.fillMaxWidth().padding(top=18.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(card.displayName,style=MaterialTheme.typography.titleLarge); Text("${card.network} · •••• ${card.last4}",color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodyMedium) }
                    if(card.isDemo) DemoBadge() else InfoBadge("REFERENCIA")
                }
            }
            item { SectionLabel("Tu tarjeta, a tu manera") }
            item { SettingRow(Icons.Outlined.Palette,"Card Appearance","Imagen, color, chip y texto",onAppearance) }
            item { SettingRow(Icons.Outlined.Info,"Información de la tarjeta","Identidad y apariencia separadas",{info=true}) }
            item { SettingRow(Icons.Outlined.Edit,"Cambiar nombre",card.displayName,{rename=true}) }
            item { SectionLabel(if(card.isDemo) "Preferencias de pago" else "Preferencias de Orel") }
            item { SettingRow(Icons.Outlined.CreditCard,if(card.isDemo) "Tarjeta principal" else "Principal en Orel",if(!card.isDemo) "Solo cambia el orden visual en Orel" else if(card.isDefault) "Seleccionada por defecto" else "Usar para pagos demo",trailing={WalletSwitch("Tarjeta principal",card.isDefault,{ if(it) vm.default(card) },enabled=!card.isLocked && !card.isHidden)}) }
            if(card.isDemo) item { SettingRow(Icons.Outlined.Contactless,"Contactless","Solo credencial de laboratorio",trailing={WalletSwitch("Contactless de la tarjeta",card.contactlessEnabled,{vm.update(card.copy(contactlessEnabled=it))})}) }
            item { SettingRow(Icons.Outlined.Lock,if(!card.isDemo) "Bloquear acceso en Orel" else if(card.isLocked) "Desbloquear tarjeta" else "Bloquear tarjeta",if(!card.isDemo) "No bloquea tu Pixpay ni Google Wallet" else if(card.isLocked) "Los pagos están desactivados" else "Pausar pagos de esta tarjeta",trailing={WalletSwitch("Bloquear tarjeta",card.isLocked,{vm.update(card.copy(isLocked=it))})}) }
            item { SettingRow(Icons.Outlined.VisibilityOff,"Ocultar en inicio","Se conserva en Mis tarjetas",trailing={WalletSwitch("Ocultar en inicio",card.isHidden,{vm.update(card.copy(isHidden=it))})}) }
            item { SectionLabel("Organizar wallet") }
            item {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    OutlinedButton({vm.move(card.id,-1)},Modifier.weight(1f)) { Icon(Icons.Outlined.ArrowUpward,null,Modifier.size(18.dp)); Text("Subir",Modifier.padding(start=6.dp)) }
                    OutlinedButton({vm.move(card.id,1)},Modifier.weight(1f)) { Icon(Icons.Outlined.ArrowDownward,null,Modifier.size(18.dp)); Text("Bajar",Modifier.padding(start=6.dp)) }
                }
            }
            item { SettingRow(Icons.Outlined.DeleteOutline,"Eliminar tarjeta","También se eliminará su apariencia",{delete=true},danger=true) }
            item { Text(if(card.isDemo) "Esta tarjeta es ficticia. Su aspecto no modifica su red ni permite realizar pagos reales." else "Esta referencia solo guarda el nombre, la red y los últimos cuatro dígitos. Añade y selecciona tu tarjeta para pagar en Google Wallet.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=16.dp)) }
            item { Button(onPay,Modifier.fillMaxWidth().height(52.dp),enabled=!card.isLocked && !card.isHidden && card.network.name in listOf("VISA","MASTERCARD","AMEX")) { Text(if(card.isDemo) "Probar pago demo" else "Preparar pago con Wallet") } }
        }
    }
    if(rename) AlertDialog(onDismissRequest={rename=false},title={Text("Nombre de la tarjeta")},text={OutlinedTextField(name,{name=it.take(40)},singleLine=true,label={Text("Nombre")})},confirmButton={TextButton({vm.update(card.copy(displayName=name.trim())); rename=false},enabled=name.isNotBlank()) { Text("Guardar") }},dismissButton={TextButton({rename=false}) { Text("Cancelar") }})
    if(delete) AlertDialog(onDismissRequest={delete=false},title={Text("¿Eliminar ${card.displayName}?")},text={Text(if(card.isDemo) "Se quitará del wallet. Tus movimientos se conservarán en el historial." else "Se eliminará esta referencia de Orel. Tu tarjeta de Pixpay y Google Wallet se conserva.")},confirmButton={TextButton({vm.remove(card) {onBack()}; delete=false}) { Text("Eliminar",color=MaterialTheme.colorScheme.error) }},dismissButton={TextButton({delete=false}) { Text("Conservar") }})
    if(info) AlertDialog(onDismissRequest={info=false},title={Text("Información de la tarjeta")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        if(card.isDemo) {
        DemoBadge(); Text("Red: ${card.network}\nÚltimos dígitos: •••• ${card.last4}\nEmisor: Orel Demo Wallet\nApariencia: ${card.appearance.backgroundType}")
        Text("Identificador ficticio: DEMO-0000-${card.last4}\nCaducidad ficticia: 12/30\nCódigo de prueba: DEMO (no es un CVV)",style=MaterialTheme.typography.bodySmall)
        Text("Estos valores solo se muestran en la demo, no son credenciales ni se pueden usar para pagar. No se guardan números completos ni CVV. Las tarjetas demo no son instrumentos emitidos por una entidad bancaria.",style=MaterialTheme.typography.bodySmall)
        } else { InfoBadge("REFERENCIA"); Text("Red indicada: ${card.network}\nÚltimos dígitos: •••• ${card.last4}\nNombre: ${card.displayName}"); Text("Datos introducidos por ti, sin comprobación bancaria. La apariencia y las preferencias de Orel no cambian la tarjeta ni la selección de Google Wallet.",style=MaterialTheme.typography.bodySmall) }
    }},confirmButton={TextButton({info=false}) {Text("Entendido")}})
}
