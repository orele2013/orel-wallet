package com.orel.wallet.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import com.orel.wallet.domain.*
import com.orel.wallet.nfc.NfcStatus
import com.orel.wallet.presentation.WalletUiState
import com.orel.wallet.ui.components.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.absoluteValue

@Composable
fun HomeScreen(ui:WalletUiState,selectedId:String?,nfc:NfcStatus,onSelect:(String)->Unit,onPay:()->Unit,onAdd:()->Unit,onHistory:()->Unit,onDetails:(String)->Unit,onSettings:()->Unit,onNfc:()->Unit,onTransaction:(String)->Unit) {
    val cards=ui.cards.filterNot { it.isHidden }
    val initial=cards.indexOfFirst { it.id==(selectedId ?: cards.firstOrNull { c -> c.isDefault }?.id) }.coerceAtLeast(0)
    val pager=rememberPagerState(initialPage=initial,pageCount={cards.size})
    val scope=rememberCoroutineScope()
    LaunchedEffect(pager,cards.map { it.id }) { snapshotFlow { pager.currentPage }.distinctUntilChanged().collect { cards.getOrNull(it)?.let { c -> onSelect(c.id) } } }
    LaunchedEffect(selectedId,cards.map { it.id }) {
        val index=cards.indexOfFirst { it.id==selectedId }
        if(index>=0 && index!=pager.currentPage && !pager.isScrollInProgress) pager.scrollToPage(index)
    }
    val card=cards.getOrNull(pager.currentPage)
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=24.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                Text("orel wallet",style=MaterialTheme.typography.titleMedium,letterSpacing=(-0.4).sp,modifier=Modifier.weight(1f))
                IconButton(onSettings,Modifier.size(48.dp)) { Box(Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),contentAlignment=Alignment.Center) { Icon(Icons.Outlined.Person,"Abrir ajustes",modifier=Modifier.size(20.dp)) } }
            }
            Row(Modifier.fillMaxWidth().padding(start=24.dp,end=24.dp,bottom=24.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
                Column { Text("Tu mundo,",style=MaterialTheme.typography.headlineLarge); Text("en tu bolsillo.",style=MaterialTheme.typography.headlineLarge,color=MaterialTheme.colorScheme.primary) }
            }
        }
        item {
            if(cards.isNotEmpty()) {
                HorizontalPager(pager,contentPadding=PaddingValues(horizontal=24.dp),pageSpacing=12.dp,key={cards[it].id},modifier=Modifier.fillMaxWidth()) { page ->
                    val offset=((pager.currentPage-page)+pager.currentPageOffsetFraction).absoluteValue
                    WalletCard(cards[page],Modifier.graphicsLayer { scaleY=1f-offset.coerceIn(0f,1f)*0.045f; alpha=1f-offset.coerceIn(0f,1f)*0.2f }.clickable { onSelect(cards[page].id); onDetails(cards[page].id) })
                }
                Row(Modifier.fillMaxWidth().padding(start=26.dp,end=24.dp,top=17.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(card?.displayName.orEmpty(),style=MaterialTheme.typography.titleMedium)
                        Text(if(card?.isDefault==true) "Tarjeta principal · Demo" else "Tarjeta de demostración",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DemoBadge()
                }
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top=3.dp),horizontalArrangement=Arrangement.Center) {
                    cards.forEachIndexed { index,c -> Box(Modifier.size(48.dp).semantics {contentDescription="Seleccionar ${c.displayName}, ${index+1} de ${cards.size}"; selected=index==pager.currentPage}.clickable { scope.launch { pager.animateScrollToPage(index) } },contentAlignment=Alignment.Center) {
                        Box(Modifier.size(if(index==pager.currentPage) 7.dp else 5.dp).clip(CircleShape).background(if(index==pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                    } }
                }
            } else EmptyState("Tu wallet empieza aquí","Añade tu primera tarjeta demo")
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal=32.dp,vertical=12.dp),horizontalArrangement=Arrangement.SpaceEvenly) {
                QuickAction(Icons.Outlined.Contactless,"Pagar demo",onPay,card!=null && !card.isLocked && card.network!=CardNetwork.LOYALTY && card.network!=CardNetwork.GIFT)
                QuickAction(Icons.Outlined.AddCard,"Añadir tarjeta",onAdd)
                QuickAction(Icons.Outlined.History,"Historial",onHistory)
            }
        }
        item {
            Column(Modifier.padding(horizontal=24.dp)) {
                SettingRow(Icons.Outlined.Nfc,if(!nfc.available) "NFC no disponible" else if(nfc.enabled) "NFC activado" else "NFC desactivado",
                    if(!nfc.available) "Puedes probar los pagos demo" else "Credencial de laboratorio · sin pagos reales",onNfc)
                Row(Modifier.fillMaxWidth().padding(top=26.dp,bottom=5.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("Últimos movimientos",style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
                    TextButton(onHistory) { Text("Ver todos",fontSize=12.sp) }
                }
            }
        }
        if(ui.transactions.isEmpty()) item { EmptyState("Sin movimientos","Tus pagos demo aparecerán aquí",Icons.Outlined.ReceiptLong) }
        else items(minOf(3,ui.transactions.size)) { i -> Column(Modifier.padding(horizontal=24.dp)) {
            TransactionRow(ui.transactions[i]) { onTransaction(ui.transactions[i].id) }
            if(i<minOf(3,ui.transactions.size)-1) HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=0.6f))
        } }
        item { Text("Demo Mode does not perform real payments.",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=10.sp,modifier=Modifier.padding(horizontal=24.dp,vertical=18.dp)) }
    }
}

@Composable private fun QuickAction(icon:ImageVector,label:String,onClick:()->Unit,enabled:Boolean=true) {
    Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.widthIn(min=80.dp).clip(RoundedCornerShape(16.dp)).clickable(enabled=enabled,role=Role.Button,onClick=onClick).graphicsLayer { alpha=if(enabled) 1f else 0.4f }) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),contentAlignment=Alignment.Center) { Icon(icon,null,modifier=Modifier.size(23.dp)) }
        Text(label,style=MaterialTheme.typography.labelMedium,modifier=Modifier.padding(top=9.dp))
    }
}
