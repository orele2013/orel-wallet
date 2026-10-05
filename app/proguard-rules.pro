-keepattributes Signature
# Public application contracts exercised by the separately signed release test APK.
# Keep these small types stable while UI, dependencies and unused demo code shrink.
-keep class com.orel.wallet.WalletApplication { public *; }
-keep class com.orel.wallet.domain.** { *; }
-keep interface com.orel.wallet.wallet.WalletRepository { *; }
-keep,includedescriptorclasses interface com.orel.wallet.payments.PaymentService { *; }
-keep class com.orel.wallet.payments.PaymentException { *; }
