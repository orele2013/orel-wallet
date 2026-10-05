package com.orel.wallet.nfc

/** Proprietary laboratory protocol. No PPSE, EMV commands, network AIDs or payment confirmation. */
class DemoApduProcessor(private val gate: DemoCredentialGate = DemoHceGate.credentialGate) {
    private var selected = false

    fun process(command: ByteArray): ByteArray {
        val credential = gate.credential() ?: run { selected = false; return status(0x69, 0x85) }
        if (command.size >= 4 && command[0] == 0.toByte() && command[1] == 0xA4.toByte()) {
            selected = command.contentEquals(SELECT) || command.contentEquals(SELECT + byteArrayOf(0))
            return if (selected) "OREL DEMO LAB".encodeToByteArray() + status(0x90, 0x00) else status(0x6A, 0x82)
        }
        if (!selected) return status(0x69, 0x85)
        return if (command.contentEquals(READ_CREDENTIAL)) credential.encodeToByteArray() + status(0x90, 0x00)
            else status(0x6D, 0x00)
    }

    fun reset() { selected = false }
    private fun status(a: Int, b: Int) = byteArrayOf(a.toByte(), b.toByte())
    companion object {
        private val SELECT = byteArrayOf(0x00, 0xA4.toByte(), 0x04, 0x00, 0x06, 0xF0.toByte(), 0x4F, 0x52, 0x45, 0x4C, 0x01)
        private val READ_CREDENTIAL = byteArrayOf(0x80.toByte(), 0xCA.toByte(), 0x00, 0x00, 0x00)
    }
}
