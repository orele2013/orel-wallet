package com.orel.wallet.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contactless
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.orel.wallet.R
import com.orel.wallet.domain.*

fun skinResource(skin: String): Int = when (skin) {
    "violet" -> R.drawable.skin_violet
    "gold" -> R.drawable.skin_gold
    "graphite" -> R.drawable.skin_graphite
    "mint" -> R.drawable.skin_mint
    "ice" -> R.drawable.skin_ice
    else -> R.drawable.skin_blue
}

@Composable
fun WalletCard(card: Card, modifier: Modifier = Modifier, compact: Boolean = false) {
    val a = card.appearance
    val shape = RoundedCornerShape(22.dp)
    val ink = Color(a.textColor)
    val contrast = 0.7f + a.contrast * 0.6f
    val offset = (a.brightness - 0.5f) * 130f + 128f * (1f - contrast)
    val colorFilter = ColorFilter.colorMatrix(ColorMatrix(floatArrayOf(
        contrast,0f,0f,0f,offset, 0f,contrast,0f,0f,offset,
        0f,0f,contrast,0f,offset, 0f,0f,0f,1f,0f
    )))
    Box(modifier.fillMaxWidth().aspectRatio(1.62f)
        .shadow(if (compact) 4.dp else 16.dp, shape, ambientColor = Color(0xFF033582), spotColor = Color(0xFF033582))
        .clip(shape).semantics(mergeDescendants = true) {
            contentDescription = "${card.displayName}, ${card.network}, termina en ${card.last4}, ${if(card.isDemo) "tarjeta demo" else "referencia visual"}${if(card.isLocked) ", bloqueada" else ""}"
        }.background(Color(0xFF073474))) {
        when(a.backgroundType) {
            BackgroundType.SKIN -> Image(painterResource(skinResource(a.backgroundValue)), null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, colorFilter = colorFilter)
            BackgroundType.IMAGE -> AsyncImage(a.backgroundValue, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop, colorFilter = colorFilter)
            BackgroundType.COLOR, BackgroundType.GRADIENT -> {
                val base = runCatching { Color(android.graphics.Color.parseColor(a.backgroundValue)) }.getOrDefault(Color(0xFF0866F5))
                fun adjusted(color:Color)=Color(red=(color.red*contrast+offset/255f).coerceIn(0f,1f),green=(color.green*contrast+offset/255f).coerceIn(0f,1f),blue=(color.blue*contrast+offset/255f).coerceIn(0f,1f))
                Box(Modifier.matchParentSize().background(Brush.linearGradient(if(a.backgroundType == BackgroundType.COLOR) listOf(adjusted(base), adjusted(base)) else listOf(adjusted(base), adjusted(Color(0xFF081329))))))
            }
        }
        Box(Modifier.matchParentSize().background(Brush.verticalGradient(listOf(Color.White.copy(alpha=0.03f), Color.Black.copy(alpha=0.22f)))))
        Column(Modifier.fillMaxSize().padding(if(compact) 18.dp else 24.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("orel", color=ink, fontSize=if(compact) 16.sp else 22.sp, fontWeight=FontWeight.SemiBold, letterSpacing=(-0.8).sp)
                    Text(if(card.isDemo) "DEMO CARD" else "PERSONAL", color=ink.copy(alpha=0.8f), fontSize=8.sp, letterSpacing=2.sp)
                }
                if(card.isLocked) Icon(Icons.Outlined.Lock,"Bloqueada",tint=ink,modifier=Modifier.size(22.dp))
                else if(card.isDemo) Icon(Icons.Outlined.Contactless, "Contactless${if(!card.contactlessEnabled) " desactivado" else ""}", tint=ink.copy(alpha=if(card.contactlessEnabled) 0.85f else 0.35f),modifier=Modifier.size(26.dp))
            }
            if(a.numberPosition == NumberPosition.TOP) CardNumber(card,ink,compact)
            Chip(a.chipStyle, Modifier.size(if(compact) 34.dp else 42.dp,if(compact) 25.dp else 31.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.Bottom) {
                Column {
                    if(a.numberPosition == NumberPosition.BOTTOM) CardNumber(card,ink,compact)
                    if(a.showName && !compact) Text(card.displayName, color=ink.copy(alpha=0.76f),fontSize=11.sp,modifier=Modifier.padding(top=6.dp))
                }
                when(card.network) {
                    CardNetwork.MASTERCARD -> Canvas(Modifier.size(42.dp,26.dp)) {
                        drawCircle(Color(0xFFEB4B3F),radius=size.height/2,center=Offset(size.width*0.33f,size.height/2))
                        drawCircle(Color(0xFFFFB344).copy(alpha=0.93f),radius=size.height/2,center=Offset(size.width*0.66f,size.height/2))
                    }
                    else -> Text(when(card.network){CardNetwork.VISA -> "VISA"; CardNetwork.AMEX -> "AMEX"; CardNetwork.LOYALTY -> "CLUB"; else -> "GIFT"},color=ink,fontWeight=FontWeight.Bold,fontSize=if(compact) 17.sp else 22.sp,letterSpacing=(-0.5).sp)
                }
            }
        }
    }
}

@Composable private fun CardNumber(card:Card, color:Color, compact:Boolean) {
    Text("••••  ${card.last4}", color=color,fontSize=if(compact) 12.sp else 16.sp,letterSpacing=2.sp,
        fontFamily=if(card.appearance.textStyle==TextStyle.MONO) FontFamily.Monospace else FontFamily.SansSerif)
}

@Composable fun Chip(style:ChipStyle,modifier:Modifier=Modifier) {
    Canvas(modifier) {
        val metal = when(style) {ChipStyle.GOLD -> listOf(Color(0xFFFFDF91),Color(0xFFA9813A)); ChipStyle.SILVER -> listOf(Color(0xFFE1E8EF),Color(0xFF91A6C0)); ChipStyle.MINIMAL -> listOf(Color.White.copy(alpha=0.4f),Color.White.copy(alpha=0.15f))}
        drawRoundRect(Brush.linearGradient(metal),cornerRadius=CornerRadius(5.dp.toPx()))
        val line = Color(0xFF2D445D).copy(alpha=0.45f)
        drawLine(line,Offset(size.width*0.33f,0f),Offset(size.width*0.33f,size.height),1.dp.toPx())
        drawLine(line,Offset(size.width*0.67f,0f),Offset(size.width*0.67f,size.height),1.dp.toPx())
        listOf(0.33f,0.67f).forEach { drawLine(line,Offset(0f,size.height*it),Offset(size.width,size.height*it),0.7.dp.toPx()) }
        drawRoundRect(line,topLeft=Offset(size.width*0.27f,size.height*0.25f),size=Size(size.width*0.46f,size.height*0.5f),cornerRadius=CornerRadius(3.dp.toPx()),style=androidx.compose.ui.graphics.drawscope.Stroke(0.8.dp.toPx()))
    }
}
