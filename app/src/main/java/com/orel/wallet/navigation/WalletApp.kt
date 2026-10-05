package com.orel.wallet.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.orel.wallet.domain.ThemeMode
import com.orel.wallet.BuildConfig
import com.orel.wallet.nfc.DemoHceGate
import com.orel.wallet.payments.PaymentState
import com.orel.wallet.presentation.WalletViewModel
import com.orel.wallet.presentation.screens.*
import com.orel.wallet.security.BiometricAuthenticator
import com.orel.wallet.ui.theme.OrelTheme

@Composable fun WalletApp(vm:WalletViewModel,auth:BiometricAuthenticator,onWindowState:(sensitive:Boolean,dark:Boolean)->Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val selectedId by vm.selectedId.collectAsStateWithLifecycle()
    val nfc by vm.nfc.collectAsStateWithLifecycle()
    val message by vm.message.collectAsStateWithLifecycle()
    val nav=rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route=entry?.destination?.route ?: "home"
    val bottomRoutes=listOf("home","cards","history","settings")
    val snackbar=remember {SnackbarHostState()}
    val locked by vm.walletLocked.collectAsStateWithLifecycle()
    val lifecycle=LocalLifecycleOwner.current
    val currentSettings by rememberUpdatedState(ui.settings)
    DisposableEffect(lifecycle) {
        val observer=LifecycleEventObserver {_,event -> when(event) {
            Lifecycle.Event.ON_STOP -> {
                vm.background(); DemoHceGate.disarm()
                if(vm.paymentService.session.value?.state in listOf(PaymentState.READY_TO_PAY,PaymentState.PROCESSING)) vm.cancelPayment()
            }
            Lifecycle.Event.ON_START -> {
                vm.foreground(currentSettings)
            }
            else -> Unit
        } }
        lifecycle.lifecycle.addObserver(observer); onDispose {lifecycle.lifecycle.removeObserver(observer)}
    }
    LaunchedEffect(message) {message?.let {snackbar.showSnackbar(it); vm.clearMessage()}}
    val systemDark=isSystemInDarkTheme()
    val themeDark=ui.settings.themeMode==ThemeMode.DARK || (ui.settings.themeMode==ThemeMode.SYSTEM && systemDark)
    val specialDark=route=="pay/{id}" || (ui.loaded && !ui.settings.onboardingComplete)
    SideEffect {onWindowState(route=="pay/{id}" || route=="wallet/{id}" || (ui.settings.onboardingComplete && locked && ui.settings.requireBiometric),specialDark || themeDark)}
    OrelTheme(ui.settings) {
        Box(Modifier.fillMaxSize().background(if(specialDark) Color(0xFF090F1A) else MaterialTheme.colorScheme.background).windowInsetsPadding(WindowInsets.safeDrawing)) {
            when {
                !ui.loaded -> Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {CircularProgressIndicator()}
                !ui.settings.onboardingComplete -> WelcomeScreen(ui.cards) {vm.settings(ui.settings.copy(onboardingComplete=true))}
                locked && ui.settings.requireBiometric -> WalletLockScreen(auth,vm::unlockWallet)
                else -> Scaffold(containerColor=MaterialTheme.colorScheme.background,contentWindowInsets=WindowInsets(0),snackbarHost={SnackbarHost(snackbar)},bottomBar={
                    if(route in bottomRoutes) NavigationBar(containerColor=MaterialTheme.colorScheme.surface,tonalElevation=0.dp,windowInsets=WindowInsets(0)) {
                        val icons=listOf(Icons.Outlined.Home,Icons.Outlined.CreditCard,Icons.Outlined.History,Icons.Outlined.Settings)
                        val labels=listOf("Inicio","Tarjetas","Historial","Ajustes")
                        bottomRoutes.forEachIndexed {i,target -> NavigationBarItem(route==target,onClick={nav.navigate(target) {popUpTo("home") {saveState=true}; launchSingleTop=true; restoreState=true}},icon={Icon(icons[i],null,Modifier.size(22.dp))},label={Text(labels[i])}) }
                    }
                }) {padding ->
                    NavHost(nav,startDestination="home",modifier=Modifier.padding(padding).fillMaxSize(),enterTransition={fadeIn(tween(180))+slideInHorizontally(tween(220)) {it/12}},exitTransition={fadeOut(tween(150))},popEnterTransition={fadeIn(tween(180))},popExitTransition={fadeOut(tween(150))}) {
                        composable("home") {
                            val selected=ui.cards.firstOrNull {it.id==selectedId && !it.isHidden} ?: ui.cards.firstOrNull {it.isDefault && !it.isHidden} ?: ui.cards.firstOrNull {!it.isHidden}
                            HomeScreen(ui,selected?.id,nfc,vm::select,onPay={selected?.let {nav.navigate(paymentRoute(it.id))}},onAdd={nav.navigate("add")},onHistory={nav.navigate("history")},onDetails={nav.navigate("card/$it")},onSettings={nav.navigate("settings")},onNfc={nav.navigate("nfc")},onTransaction={nav.navigate("transaction/$it")})
                        }
                        composable("cards") {CardsScreen(ui.cards,{nav.navigate("add")}) {vm.select(it); nav.navigate("card/$it")}}
                        composable("history") {HistoryScreen(ui.transactions) {nav.navigate("transaction/$it")}}
                        composable("settings") {SettingsScreen(ui.settings,vm::settings,{nav.navigate("cards")},{nav.navigate("nfc")})}
                        composable("add") {AddCardScreen(vm,{nav.popBackStack()}) {id -> nav.navigate("card/$id") {popUpTo("add") {inclusive=true}}}}
                        composable("card/{id}") {back ->
                            val id=back.arguments?.getString("id"); CardDetailsScreen(ui.cards.firstOrNull {it.id==id},vm,{nav.popBackStack()},{nav.navigate("appearance/$id")},{id?.let {nav.navigate(paymentRoute(it))}})
                        }
                        composable("appearance/{id}") {back -> AppearanceScreen(ui.cards.firstOrNull {it.id==back.arguments?.getString("id")},vm) {nav.popBackStack()}}
                        if(BuildConfig.DEMO_MODE) composable("pay/{id}") {back -> PaymentScreen(ui.cards.firstOrNull {it.id==back.arguments?.getString("id")},vm,auth,{nav.popBackStack()}) {id -> nav.navigate("transaction/$id") {popUpTo("pay/{id}") {inclusive=true}}}}
                        composable("wallet/{id}") {back -> WalletAssistScreen(ui.cards.firstOrNull {it.id==back.arguments?.getString("id")},nfc) {nav.popBackStack()}}
                        composable("transaction/{id}") {back -> TransactionDetailsScreen(ui.transactions.firstOrNull {it.id==back.arguments?.getString("id")}) {nav.popBackStack()}}
                        composable("nfc") {NfcScreen(nfc,ui.settings,vm::settings) {nav.popBackStack()}}
                    }
                }
            }
        }
    }
}

private fun paymentRoute(id:String)=if(BuildConfig.DEMO_MODE) "pay/$id" else "wallet/$id"
