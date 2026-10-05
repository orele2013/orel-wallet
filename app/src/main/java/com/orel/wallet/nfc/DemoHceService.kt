package com.orel.wallet.nfc

import android.app.KeyguardManager
import android.content.Context
import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class DemoHceService : HostApduService() {
    private val processor = DemoApduProcessor()
    override fun processCommandApdu(commandApdu: ByteArray?, extras: Bundle?): ByteArray {
        if ((getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager).isDeviceLocked) {
            DemoHceGate.disarm()
            return byteArrayOf(0x69, 0x85.toByte())
        }
        return commandApdu?.let(processor::process) ?: byteArrayOf(0x67, 0)
    }
    override fun onDeactivated(reason: Int) { processor.reset() }
}
