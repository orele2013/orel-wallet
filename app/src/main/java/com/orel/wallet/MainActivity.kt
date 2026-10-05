package com.orel.wallet

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.core.view.WindowCompat
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.orel.wallet.navigation.WalletApp
import com.orel.wallet.presentation.WalletViewModel
import com.orel.wallet.security.BiometricAuthenticator
import com.orel.wallet.security.SecurityManager

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val graph=application as WalletApplication
        val vm=ViewModelProvider(this,object:ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T:ViewModel> create(modelClass:Class<T>):T = WalletViewModel(graph.repository,graph.paymentService,applicationContext) as T
        })[WalletViewModel::class.java]
        val auth=BiometricAuthenticator(this)
        val security=SecurityManager(window)
        setContent { WalletApp(vm,auth) {sensitive,dark ->
            security.protectScreenshots(sensitive)
            WindowCompat.getInsetsController(window,window.decorView).apply {isAppearanceLightStatusBars=!dark; isAppearanceLightNavigationBars=!dark}
        } }
    }
}
