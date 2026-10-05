package com.orel.wallet

import android.app.Application
import com.orel.wallet.data.RoomWalletRepository
import com.orel.wallet.payments.DemoPaymentService
import com.orel.wallet.wallet.WalletRepository

class WalletApplication : Application() {
    val repository:WalletRepository by lazy {RoomWalletRepository(this)}
    val paymentService:DemoPaymentService by lazy {DemoPaymentService(repository)}
}
