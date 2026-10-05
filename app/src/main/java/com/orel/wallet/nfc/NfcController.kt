package com.orel.wallet.nfc

import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcManager

data class NfcStatus(val available: Boolean, val enabled: Boolean, val hceSupported: Boolean)

class NfcController(private val context: Context) {
    fun status(): NfcStatus {
        val adapter = (context.getSystemService(Context.NFC_SERVICE) as? NfcManager)?.defaultAdapter
        return availability(adapter != null, adapter?.isEnabled == true,
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION))
    }
    companion object {
        fun availability(hasAdapter: Boolean, adapterEnabled: Boolean, supportsHce: Boolean): NfcStatus =
            NfcStatus(hasAdapter, hasAdapter && adapterEnabled, hasAdapter && supportsHce)
    }
}
