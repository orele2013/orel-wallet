package com.orel.wallet.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orel.wallet.domain.*
import com.orel.wallet.ui.components.*

@Composable fun HistoryScreen(transactions:List<Transaction>,onTransaction:(String)->Unit) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) }
    val filtered=transactions.filter { (it.merchant.contains(query,true) || it.last4.contains(query) || it.cardLabel.contains(query,true)) && when(filter){1->it.channel==TransactionChannel.ONLINE; 2->it.channel==TransactionChannel.IN_STORE; else->true} }
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(24.dp)) {
        item {
            Text("Tu actividad",style=MaterialTheme.typography.headlineMedium)
            Text("Cada movimiento, en un solo lugar.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=6.dp,bottom=20.dp))
            OutlinedTextField(query,{query=it},label={Text("Buscar movimientos")},leadingIcon={Icon(Icons.Outlined.Search,null)},singleLine=true,modifier=Modifier.fillMaxWidth(),shape=MaterialTheme.shapes.large)
            Row(Modifier.padding(top=12.dp,bottom=18.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) { listOf("Todos","En línea","En tienda").forEachIndexed {i,label -> FilterChip(filter==i,{filter=i},label={Text(label)}) } }
            DemoBadge(Modifier.padding(bottom=10.dp))
        }
        if(filtered.isEmpty()) item { EmptyState("Sin resultados","Prueba otra búsqueda o realiza un pago demo",Icons.Outlined.ReceiptLong) }
        items(filtered,key={it.id}) {tx -> TransactionRow(tx) {onTransaction(tx.id)}; HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.6f)) }
    }
}

@Composable fun TransactionDetailsScreen(transaction:Transaction?,onBack:()->Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Detalle del movimiento",onBack)
        if(transaction==null) EmptyState("Movimiento no disponible","Vuelve al historial")
        else Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            DemoBadge()
            Icon(Icons.Outlined.CheckCircle,null,modifier=Modifier.size(52.dp),tint=MaterialTheme.colorScheme.primary)
            Text(money(transaction.amountMinor,transaction.currency),style=MaterialTheme.typography.headlineLarge)
            Text(transaction.merchant,style=MaterialTheme.typography.titleLarge)
            SettingRow(Icons.Outlined.ReceiptLong,"Estado",when(transaction.status){TransactionStatus.COMPLETED->"Completada · Demo"; TransactionStatus.FAILED->"Fallida"; TransactionStatus.PENDING->"Pendiente"})
            SettingRow(Icons.Outlined.ReceiptLong,"Fecha",dateTime(transaction.timestamp))
            SettingRow(Icons.Outlined.ReceiptLong,"Tarjeta","${transaction.cardLabel} · •••• ${transaction.last4}")
            SettingRow(Icons.Outlined.ReceiptLong,"Canal",if(transaction.channel==TransactionChannel.ONLINE) "En línea" else "En tienda")
            Text("Referencia: ${transaction.id}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Transacción ficticia. No se ha cobrado dinero ni contactado con un banco.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
