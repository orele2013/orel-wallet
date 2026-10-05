package com.orel.wallet

import android.app.Application
import com.orel.wallet.data.RoomWalletRepository
import com.orel.wallet.payments.DemoPaymentService
import com.orel.wallet.payments.PaymentService
import com.orel.wallet.payments.RealPaymentService
import com.orel.wallet.wallet.WalletRepository

class WalletApplication : Application() {
    val repository:WalletRepository by lazy {RoomWalletRepository(this, seedDemoData=BuildConfig.DEMO_MODE)}
    val paymentService:PaymentService by lazy {if(BuildConfig.DEMO_MODE) DemoPaymentService(repository) else RealPaymentService()}
}
