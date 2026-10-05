package com.orel.wallet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orel.wallet.domain.Transaction
import com.orel.wallet.BuildConfig
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

fun money(minor:Long,currency:String="EUR"):String = NumberFormat.getCurrencyInstance(Locale("es","ES")).apply { this.currency=Currency.getInstance(currency) }.format(minor/100.0)
fun dateTime(timestamp:Long):String = DateTimeFormatter.ofPattern("d MMM · HH:mm",Locale("es","ES")).withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(timestamp))

@Composable fun DemoBadge(modifier:Modifier=Modifier,dark:Boolean=false) {
    InfoBadge("MODO DEMO",modifier,dark)
}
@Composable fun WalletModeBadge(modifier:Modifier=Modifier,dark:Boolean=false) {
    InfoBadge(if(BuildConfig.DEMO_MODE) "MODO DEMO" else "PERSONAL",modifier,dark)
}
@Composable fun InfoBadge(label:String,modifier:Modifier=Modifier,dark:Boolean=false) {
    Text(label,modifier.clip(RoundedCornerShape(7.dp)).background(if(dark) Color.White.copy(alpha=0.1f) else MaterialTheme.colorScheme.secondaryContainer).padding(horizontal=9.dp,vertical=5.dp),
        color=if(dark) Color(0xFFA7C5FF) else MaterialTheme.colorScheme.primary,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
}

@Composable fun ScreenHeader(title:String,onBack:()->Unit,action:(@Composable ()->Unit)?=null) {
    Row(Modifier.fillMaxWidth().padding(top=6.dp,bottom=14.dp),verticalAlignment=Alignment.CenterVertically) {
        IconButton(onClick=onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Volver") }
        Text(title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable fun SectionLabel(text:String,modifier:Modifier=Modifier) {
    Text(text.uppercase(),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=modifier.padding(top=20.dp,bottom=10.dp))
}

@Composable fun SettingRow(icon:ImageVector,title:String,subtitle:String?=null,onClick:(()->Unit)?=null,
    trailing:(@Composable ()->Unit)?=null,danger:Boolean=false) {
    Surface(color=MaterialTheme.colorScheme.surface,shape=RoundedCornerShape(16.dp),modifier=Modifier.fillMaxWidth()) {
        Row(Modifier.semantics(mergeDescendants=true) {}.then(if(onClick!=null) Modifier.clickable(onClick=onClick) else Modifier).padding(horizontal=14.dp,vertical=14.dp).heightIn(min=34.dp),verticalAlignment=Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(if(danger) Color(0xFFFFECEC) else MaterialTheme.colorScheme.surfaceVariant),contentAlignment=Alignment.Center) {
                Icon(icon,null,tint=if(danger) Color(0xFFDB4B50) else MaterialTheme.colorScheme.primary,modifier=Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f).padding(start=12.dp)) {
                Text(title,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.Medium,color=if(danger) Color(0xFFDB4B50) else MaterialTheme.colorScheme.onSurface)
                if(subtitle!=null) Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if(trailing!=null) trailing() else if(onClick!=null) Icon(Icons.Outlined.ChevronRight,null,tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(18.dp))
        }
    }
}

@Composable fun WalletSwitch(label:String,checked:Boolean,onChange:(Boolean)->Unit,enabled:Boolean=true) {
    Switch(checked,onChange,enabled=enabled,modifier=Modifier.semantics {contentDescription=label})
}

@Composable fun TransactionRow(transaction:Transaction,onClick:()->Unit) {
    val colors=listOf(Color(0xFF117869),Color(0xFF185CC1),Color(0xFF6B4FB5),Color(0xFFB77D22))
    val color=colors[(transaction.merchant.hashCode().toLong().let { if(it<0) -it else it }%colors.size).toInt()]
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=14.dp),verticalAlignment=Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(15.dp)).background(color.copy(alpha=0.10f)),contentAlignment=Alignment.Center) {
            Text(transaction.merchant.take(1).uppercase(),color=color,fontWeight=FontWeight.Bold,fontSize=18.sp)
        }
        Column(Modifier.weight(1f).padding(start=12.dp,end=6.dp)) {
            Text(transaction.merchant,style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold,maxLines=1,overflow=TextOverflow.Ellipsis)
            Text("${dateTime(transaction.timestamp)} · •••• ${transaction.last4}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment=Alignment.End) {
            Text("− ${money(transaction.amountMinor,transaction.currency)}",fontSize=14.sp,fontWeight=FontWeight.SemiBold)
            Text(if(transaction.isDemo) "Demo" else transaction.status.name,fontSize=10.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun EmptyState(title:String,body:String,icon:ImageVector=Icons.Outlined.CreditCard) {
    Column(Modifier.fillMaxWidth().padding(vertical=48.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Icon(icon,null,modifier=Modifier.size(42.dp),tint=MaterialTheme.colorScheme.primary)
        Text(title,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=18.dp))
        Text(body,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=8.dp))
    }
}
