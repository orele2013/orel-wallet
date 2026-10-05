package com.orel.wallet.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

enum class AuthResult { SUCCESS, CANCELLED, UNAVAILABLE, ERROR }

class BiometricAuthenticator(private val activity: FragmentActivity) {
    private val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
    fun availability(): Boolean = BiometricManager.from(activity).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS

    suspend fun authenticate(title: String = "Desbloquear Orel Wallet"): AuthResult = withContext(Dispatchers.Main.immediate) {
        if (!availability()) return@withContext AuthResult.UNAVAILABLE
        suspendCancellableCoroutine { continuation ->
            val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (continuation.isActive) continuation.resume(AuthResult.SUCCESS)
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        val result = when (errorCode) {
                            BiometricPrompt.ERROR_USER_CANCELED, BiometricPrompt.ERROR_CANCELED,
                            BiometricPrompt.ERROR_NEGATIVE_BUTTON -> AuthResult.CANCELLED
                            BiometricPrompt.ERROR_NO_BIOMETRICS, BiometricPrompt.ERROR_HW_NOT_PRESENT,
                            BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL -> AuthResult.UNAVAILABLE
                            else -> AuthResult.ERROR
                        }
                        if (continuation.isActive) continuation.resume(result)
                    }
                })
            continuation.invokeOnCancellation { prompt.cancelAuthentication() }
            if (continuation.isActive) prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle("Confirma tu identidad con Android")
                .setAllowedAuthenticators(authenticators)
                .build())
        }
    }
}
