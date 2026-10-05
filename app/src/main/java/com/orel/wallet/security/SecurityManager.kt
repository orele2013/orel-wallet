package com.orel.wallet.security

import android.view.Window
import android.view.WindowManager

class SecurityManager(private val window: Window) {
    fun protectScreenshots(enabled: Boolean) {
        if (enabled) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}
