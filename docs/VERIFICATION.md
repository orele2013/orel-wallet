# Verificación de Orel Wallet

Fecha: 5 de octubre de 2026. Versión `1.0.0-demo`, Android API 30–35. APK debug para instalar y probar; no es una publicación de producción. Firma debug comprobada con `apksigner verify`.

SHA-256 de `artifacts/orel-wallet-demo.apk`: `0af23a418210adc6d51e85332c4d0e4c0f3ee0255f3af69985006b2761864607`.

## Entorno

- macOS Apple Silicon, JDK 17, Gradle 8.12, Android SDK 35.
- Emulador `OrelWallet_API35`, imagen Google APIs ARM64 de Android 15/API 35.
- Node.js 22 o superior para el backend mock local.
- El emulador carece de NFC físico; se comprueba la interfaz de ausencia de NFC.

## Comprobaciones

| Comprobación | Resultado |
|---|---|
| Pruebas JVM de dominio, repositorio, pagos, APDU y tokens | 27 aprobadas, cero fallos |
| Backend mock, validación, confirmación e idempotencia | 17 aprobadas, cero fallos |
| Android Keystore: cifrado, lectura y borrado | 1 aprobada en API 35 |
| Compilación final y lint | BUILD SUCCESSFUL; 0 errores, 14 avisos |
| Flujos Compose sobre Room real | 6 recorridos aprobados: 5 en suite y pago repetido de forma aislada |
| PIN oficial de Android | Desbloqueo mediante BiometricPrompt y retorno a Ajustes comprobados |
| Revisión visual | Inicio claro/oscuro, tarjetas, detalles y personalización inspeccionados |

Las pruebas JVM cubren altas, borrado, tarjeta principal, orden, bloqueo, invariantes de identidad/apariencia, mapeo Room, estados de autenticación, caducidad, confirmación, cancelación, timeout, idempotencia y rechazo de tarjetas reales. El protocolo HCE se prueba como lógica pura, incluido el rechazo sin autorización y de comandos incorrectos.

Las pruebas Compose recorren bienvenida y navegación, alta demo persistente, apariencia sin cambiar identidad, búsqueda/filtros del historial, tema persistente y compra demo con un movimiento confirmado de 12,50 €.

Total: **51 pruebas aprobadas** (27 JVM + 17 backend + 6 Compose + 1 Keystore). La prueba de pago final confirmó la creación de un único movimiento en Room con comercio `DEMO STORE`, importe de 1.250 céntimos y etiqueta demo, y abrió sus detalles.

La revisión corrigió la zona táctil del acceso «Pagar demo» para incluir icono y texto. La prueba de pago también se ajustó para esperar la transición al estado listo antes de pulsar la confirmación. Los otros cinco recorridos pasaron sobre la APK final; después se recompiló únicamente el APK de pruebas y el recorrido de pago pasó de forma aislada (`OK (1 test)`). No se cambió la APK de la app durante esa última repetición.

Los avisos de lint son 11 sugerencias de actualización de versiones fijadas, dos sugerencias de uso de extensiones KTX y una migración sugerida de kapt a KSP. No son errores de compilación ni fallos de las pruebas.

Para verificar el fallback se configuró un PIN temporal exclusivamente en el emulador de pruebas, se activó la autenticación en Ajustes y se desbloqueó mediante la pantalla de credencial de `com.android.systemui`. El PIN se retiró al terminar. La app no recibe ni almacena esa credencial. Las capturas de revisión están en `artifacts/qa-*.png`; las pantallas sensibles permanecen protegidas por `FLAG_SECURE`.

## Reproducir

```sh
./gradlew assembleDebug testDebugUnitTest assembleDebugAndroidTest lintDebug --max-workers=1
(cd backend && npm test)
./gradlew connectedDebugAndroidTest
```

En este Mac de 8 GB se detuvo Gradle antes de arrancar el emulador. El primer arranque de instrumentación produjo un ANR del verificador DEX de Android por presión de memoria del entorno, antes de ejecutar los tests. La app arrancó por separado. Se completó la verificación del paquete y se repitió la instrumentación:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell cmd package compile -m verify -f com.orel.wallet
adb shell cmd package compile -m verify -f com.orel.wallet.test
adb shell am instrument -w -r com.orel.wallet.test/androidx.test.runner.AndroidJUnitRunner
```

Los resultados JVM y lint están en `app/build/reports/`. Los registros de esta ejecución se conservan localmente en `.tools/`, excluido de Git.

Registros principales: `verify-final.log`, `backend-final.log`, `security-instrumentation-verified.log`, `ui-instrumentation-final.log` (cinco recorridos aprobados y el fallo de sincronización corregido), `verify-ui-test-final.log` y `payment-ui-final.log` (pago aprobado).

## Límites comprobados

Esta demo no realiza pagos reales. La APK no solicita permiso de Internet ni transmite datos a bancos o al backend mock. `RealPaymentService` rechaza operar hasta disponer de una integración autorizada.

NFC/HCE físico requiere un Android compatible y un lector ISO-DEP de laboratorio. No se ha verificado contra un terminal bancario porque el protocolo demo no es bancario. Huella y rostro necesitan hardware/enrolamiento compatibles; no se garantiza el comportamiento ni la frecuencia de fotogramas de todos los dispositivos sin medirlos. TalkBack, pantallas y frecuencias adicionales requieren revisión en hardware representativo antes de producción.

Las preferencias de notificaciones se guardan; esta versión no envía notificaciones de un proveedor. Analíticas y crash reporting están desactivados.
