package com.orel.wallet.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orel.wallet.domain.Card
import com.orel.wallet.BuildConfig
import com.orel.wallet.nfc.DemoHceGate
import com.orel.wallet.payments.*
import com.orel.wallet.presentation.WalletViewModel
import com.orel.wallet.security.AuthResult
import com.orel.wallet.security.BiometricAuthenticator
import com.orel.wallet.ui.components.*
import kotlinx.coroutines.launch

@Composable fun PaymentScreen(card:Card?,vm:WalletViewModel,auth:BiometricAuthenticator,onBack:()->Unit,onDetails:(String)->Unit) {
    val session by vm.paymentService.session.collectAsStateWithLifecycle()
    var ownedSessionId by rememberSaveable(card?.id) {mutableStateOf<String?>(null)}
    var prepareError by remember {mutableStateOf<String?>(null)}
    val state=if(prepareError!=null) PaymentState.FAILED else session?.takeIf {it.cardId==card?.id && it.id==ownedSessionId}?.state
    val scope=rememberCoroutineScope()
    val haptic=LocalHapticFeedback.current
    var authenticating by remember {mutableStateOf(false)}
    var authError by remember {mutableStateOf<String?>(null)}
    val canAuth=remember {auth.availability()}
    val nfc by vm.nfc.collectAsStateWithLifecycle()
    val ui by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(card?.id) {
        if(card==null) prepareError="La tarjeta ya no está disponible"
        else if(ownedSessionId==null || vm.paymentService.session.value==null) vm.beginPayment(card.id,{ownedSessionId=it},{prepareError=it})
    }
    LaunchedEffect(state) {if(state==PaymentState.SUCCESS) haptic.performHapticFeedback(HapticFeedbackType.LongPress)}
    LaunchedEffect(state,nfc,ui.settings.contactlessEnabled,card?.contactlessEnabled) {
        if(state==PaymentState.READY_TO_PAY && nfc.available && nfc.enabled && nfc.hceSupported && ui.settings.contactlessEnabled && card?.contactlessEnabled==true) session?.id?.let {DemoHceGate.arm(it)}
        else if(state!=PaymentState.AUTHENTICATING) DemoHceGate.disarm()
    }
    DisposableEffect(Unit) {onDispose {DemoHceGate.disarm()}}
    BackHandler {vm.cancelPayment(); onBack()}
    val bg by animateColorAsState(if(state==PaymentState.SUCCESS) Color(0xFF09231D) else Color(0xFF080F1A),label="Payment background")
    Column(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(bg.copy(red=(bg.red+0.015f).coerceAtMost(1f)),Color(0xFF060B12)),radius=1400f)).verticalScroll(rememberScrollState()).padding(horizontal=28.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth().padding(top=10.dp),verticalAlignment=Alignment.CenterVertically) {
            IconButton({vm.cancelPayment(); onBack()}) {Icon(Icons.Outlined.Close,"Cerrar pago",tint=Color.White)}
            Text("orel wallet",color=Color.White,modifier=Modifier.weight(1f),textAlign=TextAlign.Center,style=MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(48.dp))
        }
        DemoBadge(Modifier.padding(top=8.dp,bottom=28.dp),dark=true)
        if(card!=null) WalletCard(card,Modifier.widthIn(max=380.dp).fillMaxWidth(0.95f))
        Spacer(Modifier.height(36.dp))
        AnimatedContent(state,label="Payment step",modifier=Modifier.fillMaxWidth()) { step ->
            Column(horizontalAlignment=Alignment.CenterHorizontally,modifier=Modifier.fillMaxWidth()) {
                when(step) {
                    PaymentState.AUTHENTICATING -> {
                        PaymentSymbol(Icons.Outlined.Fingerprint,Color(0xFF478BFF))
                        Text("Autentícate para probar",style=MaterialTheme.typography.titleLarge,color=Color.White,modifier=Modifier.padding(top=24.dp))
                        Text(if(canAuth) "Usa la biometría o el PIN de tu dispositivo." else "No hay autenticación del dispositivo configurada.\nPuedes simular este paso solo en modo demo.",color=Color(0xFF91A2BD),style=MaterialTheme.typography.bodyMedium,textAlign=TextAlign.Center,modifier=Modifier.padding(top=12.dp,bottom=20.dp))
                        if(canAuth && ui.settings.requireBiometric) Button({
                            val requestedSession=session?.takeIf {it.id==ownedSessionId}
                            authenticating=true; authError=null
                            scope.launch { val result=auth.authenticate("Autorizar pago DEMO"); authenticating=false; when(result) {AuthResult.SUCCESS->requestedSession?.let(vm::authenticated); AuthResult.CANCELLED->authError="Autenticación cancelada. Puedes volver a intentarlo."; AuthResult.UNAVAILABLE->authError="Autenticación no disponible"; AuthResult.ERROR->authError="No se pudo autenticar"} }
                        },enabled=!authenticating) {Text(if(authenticating) "Esperando autenticación…" else "Usar biometría o PIN")}
                        else OutlinedButton({session?.takeIf {it.id==ownedSessionId}?.let(vm::demoAuthenticate)},colors=ButtonDefaults.outlinedButtonColors(contentColor=Color(0xFFA4C6FF))) {Text("Simular autenticación · Demo")}
                        authError?.let {Text(it,color=Color(0xFFFFB3B3),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=10.dp))}
                    }
                    PaymentState.READY_TO_PAY -> {
                        PaymentSymbol(Icons.Outlined.Contactless,Color(0xFF478BFF))
                        Text("Listo para la demo",style=MaterialTheme.typography.titleLarge,color=Color.White,modifier=Modifier.padding(top=24.dp))
                        Text("Compra ficticia en DEMO STORE",style=MaterialTheme.typography.bodyMedium,color=Color(0xFF91A2BD),modifier=Modifier.padding(top=12.dp))
                        Text("12,50 €",style=MaterialTheme.typography.headlineLarge,color=Color.White,modifier=Modifier.padding(vertical=18.dp))
                        Button({session?.takeIf {it.id==ownedSessionId}?.let(vm::executeDemo)},Modifier.fillMaxWidth().height(52.dp)) {Text("Simular compra de 12,50 €")}
                        Text("No acerques el móvil a un terminal bancario.\nNo se realizará ningún cargo.",color=Color(0xFF91A2BD),style=MaterialTheme.typography.bodySmall,textAlign=TextAlign.Center,modifier=Modifier.padding(top=14.dp))
                    }
                    PaymentState.PROCESSING -> {
                        CircularProgressIndicator(color=Color(0xFF478BFF),modifier=Modifier.size(58.dp),strokeWidth=3.dp)
                        Text("Procesando pago demo…",color=Color.White,style=MaterialTheme.typography.titleMedium,modifier=Modifier.padding(top=28.dp))
                        Text("Esperando confirmación del servicio demo",color=Color(0xFF91A2BD),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=12.dp))
                    }
                    PaymentState.SUCCESS -> {
                        var appeared by remember {mutableStateOf(false)}
                        LaunchedEffect(Unit) {appeared=true}
                        val scale by animateFloatAsState(if(appeared) 1f else 0.82f,animationSpec=spring(dampingRatio=0.85f),label="Success")
                        Box(Modifier.size(78.dp).graphicsLayer {scaleX=scale; scaleY=scale}.clip(CircleShape).background(Color(0xFF22C989)),contentAlignment=Alignment.Center) {Icon(Icons.Outlined.Check,"Pago demo confirmado",tint=Color.White,modifier=Modifier.size(42.dp))}
                        Text("Pago demo completado",style=MaterialTheme.typography.titleLarge,color=Color.White,modifier=Modifier.padding(top=24.dp))
                        Text("12,50 €",style=MaterialTheme.typography.headlineLarge,color=Color.White,modifier=Modifier.padding(top=12.dp))
                        Text("DEMO STORE · Transacción ficticia",color=Color(0xFF91A2BD),style=MaterialTheme.typography.bodyMedium,modifier=Modifier.padding(top=8.dp,bottom=26.dp))
                        OutlinedButton({session?.transactionId?.let(onDetails)},colors=ButtonDefaults.outlinedButtonColors(contentColor=Color.White)) {Text("Ver movimiento")}
                        Text("No se ha cobrado dinero.",color=Color(0xFF91A2BD),style=MaterialTheme.typography.bodySmall,modifier=Modifier.padding(top=12.dp))
                    }
                    PaymentState.FAILED,PaymentState.CANCELLED -> {
                        PaymentSymbol(Icons.Outlined.ErrorOutline,Color(0xFFFF8593))
                        Text(if(step==PaymentState.CANCELLED) "Demo cancelada" else "No se pudo completar",color=Color.White,style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(top=24.dp))
                        Text(prepareError ?: session?.error ?: "No se ha registrado ningún cobro.",color=Color(0xFF91A2BD),textAlign=TextAlign.Center,modifier=Modifier.padding(top=14.dp))
                        TextButton({onBack()}) {Text("Volver al wallet",color=Color(0xFFA4C6FF))}
                    }
                    else -> {CircularProgressIndicator(color=Color(0xFF478BFF)); Text("Preparando demo…",color=Color.White,modifier=Modifier.padding(top=20.dp))}
                }
            }
        }
        Spacer(Modifier.height(32.dp))
        if(state!=PaymentState.SUCCESS) TextButton({vm.cancelPayment(); onBack()},Modifier.padding(bottom=20.dp)) {Text("Cancelar",color=Color(0xFF91A2BD))}
        else TextButton(onBack,Modifier.padding(bottom=20.dp)) {Text("Volver al wallet",color=Color(0xFFB7D6CD))}
    }
}

@Composable private fun PaymentSymbol(icon:androidx.compose.ui.graphics.vector.ImageVector,color:Color) {
    Surface(shape=CircleShape,color=color.copy(alpha=0.07f),border=androidx.compose.foundation.BorderStroke(1.dp,color.copy(alpha=0.65f)),modifier=Modifier.size(80.dp)) {Box(contentAlignment=Alignment.Center) {Icon(icon,null,tint=color,modifier=Modifier.size(40.dp))}}
}

@Composable fun WalletLockScreen(auth:BiometricAuthenticator,onUnlocked:()->Unit) {
    val scope=rememberCoroutineScope()
    var error by remember {mutableStateOf(false)}
    Column(Modifier.fillMaxSize().padding(28.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
        Icon(Icons.Outlined.Lock,"Wallet bloqueada",modifier=Modifier.size(52.dp),tint=MaterialTheme.colorScheme.primary)
        Text("Tu wallet está bloqueada",style=MaterialTheme.typography.titleLarge,modifier=Modifier.padding(vertical=24.dp))
        WalletModeBadge()
        Spacer(Modifier.height(20.dp))
        if(auth.availability()) Button({scope.launch {if(auth.authenticate()==AuthResult.SUCCESS) onUnlocked() else error=true}}) {Text("Desbloquear con biometría o PIN")}
        else Button(onUnlocked) {Text(if(BuildConfig.DEMO_MODE) "Desbloquear demo sin biometría" else "Abrir Orel sin bloqueo")}
        if(error) Text("Autenticación no completada",color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=16.dp))
    }
}
