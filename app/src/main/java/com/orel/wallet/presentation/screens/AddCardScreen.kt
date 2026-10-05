package com.orel.wallet.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orel.wallet.domain.CardNetwork
import com.orel.wallet.presentation.WalletViewModel
import com.orel.wallet.ui.components.*

@Composable fun AddCardScreen(vm:WalletViewModel,onBack:()->Unit,onAdded:(String)->Unit) {
    var kind by remember { mutableStateOf<CardNetwork?>(null) }
    var demo by remember { mutableStateOf(false) }
    var provider by remember { mutableStateOf(false) }
    var network by remember { mutableStateOf(CardNetwork.VISA) }
    var name by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Añadir tarjeta",onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("Haz espacio para\nalgo tuyo.",style=MaterialTheme.typography.headlineLarge)
            Text("Elige qué quieres llevar en tu wallet.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(bottom=22.dp))
            SettingRow(Icons.Outlined.CreditCard,"Tarjeta de pago","Requiere proveedor de pagos autorizado",{provider=true})
            SettingRow(Icons.Outlined.Science,"Tarjeta de prueba","Explora todas las funciones · DEMO",{demo=true; kind=null; name=""})
            SettingRow(Icons.Outlined.Loyalty,"Tarjeta de fidelidad","Tarjeta local de demostración",{kind=CardNetwork.LOYALTY; demo=true; name="Mi club demo"})
            SettingRow(Icons.Outlined.CardGiftcard,"Tarjeta regalo","Tarjeta local de demostración",{kind=CardNetwork.GIFT; demo=true; name="Mi regalo demo"})
            Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=MaterialTheme.shapes.large,modifier=Modifier.padding(top=16.dp)) {
                Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    DemoBadge(); Text("Prueba con tranquilidad",style=MaterialTheme.typography.titleMedium)
                    Text("Añade tarjetas ficticias y personalízalas. No necesitas un banco ni introducir datos de una tarjeta real.",style=MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    if(provider) AlertDialog(onDismissRequest={provider=false},icon={Icon(Icons.Outlined.VerifiedUser,null)},title={Text("Pagos reales")},text={Text("Esta versión funciona en modo demo. Añadir una tarjeta bancaria requiere integrar un proveedor autorizado de emisión y tokenización. No introduzcas datos de tarjetas reales.")},confirmButton={TextButton({provider=false}) {Text("Entendido")}})
    if(demo) AlertDialog(onDismissRequest={if(!busy) demo=false},title={Text(if(kind==null) "Nueva tarjeta demo" else "Nueva tarjeta local")},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        DemoBadge()
        if(kind==null) Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) { listOf(CardNetwork.VISA,CardNetwork.MASTERCARD,CardNetwork.AMEX).forEach { type -> FilterChip(network==type,{network=type},label={Text(when(type){CardNetwork.MASTERCARD -> "MC"; else -> type.name})}) } }
        OutlinedTextField(name,{name=it.take(40)},label={Text("Nombre opcional")},singleLine=true)
        Text("Solo se crearán metadatos ficticios. No se emite una tarjeta bancaria.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }},confirmButton={TextButton({
        busy=true
        val selected=kind ?: network
        vm.add(selected,name.trim().ifBlank { when(selected){CardNetwork.VISA -> "Visa Demo"; CardNetwork.MASTERCARD -> "Mastercard Demo"; CardNetwork.AMEX -> "Amex Demo"; CardNetwork.LOYALTY -> "Club Demo"; CardNetwork.GIFT -> "Regalo Demo"} }) { id -> busy=false; demo=false; onAdded(id) }
    },enabled=!busy) {Text(if(busy) "Creando…" else "Crear tarjeta demo")}},dismissButton={TextButton({demo=false},enabled=!busy) {Text("Cancelar")}})
}
