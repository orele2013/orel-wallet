package com.orel.wallet.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.runtime.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orel.wallet.domain.Card
import com.orel.wallet.BuildConfig
import com.orel.wallet.ui.components.*

@Composable fun WelcomeScreen(cards:List<Card>,onStart:()->Unit) {
    var entered by remember {mutableStateOf(false)}
    LaunchedEffect(Unit) {entered=true}
    val reveal by animateFloatAsState(if(entered) 1f else 0f,tween(600),label="Welcome cards")
    Column(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF132C59),Color(0xFF090F1A)),radius=1100f)).verticalScroll(rememberScrollState()).padding(horizontal=28.dp,vertical=20.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("orel wallet",color=Color.White,fontWeight=FontWeight.SemiBold,fontSize=18.sp,letterSpacing=(-0.5).sp)
            WalletModeBadge(dark=true)
        }
        Spacer(Modifier.height(32.dp))
        Box(Modifier.fillMaxWidth().heightIn(min=240.dp).graphicsLayer {alpha=reveal; translationY=24f*(1f-reveal)},contentAlignment=Alignment.Center) {
            if(cards.isEmpty()) Icon(Icons.Outlined.AccountBalanceWallet,null,tint=Color(0xFF478BFF),modifier=Modifier.size(132.dp))
            cards.getOrNull(2)?.let { WalletCard(it,Modifier.fillMaxWidth(0.77f).graphicsLayer { rotationZ=10f; translationY=-30f; alpha=0.66f },compact=true) }
            cards.getOrNull(1)?.let { WalletCard(it,Modifier.fillMaxWidth(0.84f).graphicsLayer { rotationZ=-8f; translationY=4f; alpha=0.8f },compact=true) }
            cards.firstOrNull()?.let { WalletCard(it,Modifier.fillMaxWidth(0.90f).graphicsLayer { rotationZ=2f; translationY=42f },compact=true) }
        }
        Spacer(Modifier.height(36.dp))
        Text("Tu wallet.\nTu estilo.",style=MaterialTheme.typography.headlineLarge.copy(fontSize=46.sp,lineHeight=49.sp),color=Color.White)
        Text(if(BuildConfig.DEMO_MODE) "Un lugar para tus tarjetas.\nInfinitas formas de hacerlas tuyas." else "Personaliza tus tarjetas en Orel.\nPaga con Google Wallet.",color=Color(0xFF9EAEC6),style=MaterialTheme.typography.bodyLarge,modifier=Modifier.padding(top=18.dp,bottom=32.dp))
        Button(onStart,Modifier.fillMaxWidth().height(56.dp),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Color(0xFF111C30))) {Text("Comenzar")}
        Text(if(BuildConfig.DEMO_MODE) "Demostración sin banco ni pagos reales." else "Orel no emite tarjetas ni confirma pagos.",style=MaterialTheme.typography.bodySmall,color=Color(0xFF8A9AB4),modifier=Modifier.align(Alignment.CenterHorizontally).padding(top=16.dp,bottom=14.dp))
    }
}
