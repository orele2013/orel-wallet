package com.orel.wallet.wallet

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Opens official external surfaces. It cannot select a bank card, authorize or confirm a payment. */
class GoogleWalletLauncher(context: Context) {
    private val application = context.applicationContext
    fun installed(): Boolean = application.packageManager.getLaunchIntentForPackage(PACKAGE) != null
    fun openWallet(): Boolean = application.packageManager.getLaunchIntentForPackage(PACKAGE)?.let(::open) ?: false
    fun openInstallPage(): Boolean = open(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$PACKAGE"))) ||
        open(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$PACKAGE")))
    fun openNfcSettings(): Boolean = open(Intent(Settings.ACTION_NFC_SETTINGS))
    fun openDefaultWalletSettings(): Boolean = open(Intent(Settings.ACTION_NFC_PAYMENT_SETTINGS)) || openNfcSettings()
    fun openHelp(): Boolean = open(Intent(Intent.ACTION_VIEW, Uri.parse("https://support.google.com/wallet/answer/12060043?hl=es")))
    private fun open(intent: Intent): Boolean = try {
        application.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) { false } catch (_: SecurityException) { false }
    companion object { const val PACKAGE = "com.google.android.apps.walletnfcrel" }
}
