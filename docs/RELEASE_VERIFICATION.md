# Verificación — Orel Wallet release 1.1.0

Comprobada el 5 de octubre de 2026. Esta edición es una app personal de referencias visuales con acceso a Google Wallet; no es una implementación bancaria NFC propia.

## Artefacto entregado

- APK: `artifacts/orel-wallet-release.apk`.
- Paquete: `com.orel.wallet.companion`, versión `1.1.0`, código `2`.
- Android mínimo: 11 / API 30; target y compilación: API 35.
- Tamaño: 4,408,932 bytes (aprox. 4,2 MiB).
- SHA-256: `f979b85eb99f218cfd983cc54f016ae27f6c8bf752465548af3b3ccb6c96e460`.
- Firma verificada mediante `apksigner`, esquema v2, certificado `CN=Orel Wallet`.
- Huella SHA-256 del certificado: `7bc8a6884de9f607bb15a9b394db69642781317780767c4cd7e4e773eae23f06`.
- Compilación release optimizada con R8 y reducción de recursos, sin flag debuggable. Backup deshabilitado y sin permiso de Internet.
- Manifiesto final: `DemoHceService` deshabilitado. Las rutas de pago demo no se registran en release.

## Comprobaciones ejecutadas

| Comprobación | Resultado |
|---|---|
| `assembleRelease testDebugUnitTest lintRelease` | Build correcto; 29 tests JVM aprobados |
| `assembleRelease lintRelease` tras configurar las pruebas release | Build correcto; 0 errores de lint, 17 avisos |
| `CompanionUiTest` sobre release PERSONAL firmada, sin reducción para las pruebas | 3 tests aprobados, `OK (3 tests)` |
| Instalación de la APK optimizada final sobre la edición de pruebas | Correcta; actualización con la misma firma |
| Arranque de la APK optimizada en emulador API 35 ARM64 | Correcto |
| Inspección de bienvenida, inicio sin tarjetas ni movimientos demo, formulario de referencia y guía NFC/Wallet en la APK optimizada | Correcta |
| Identidad y exclusión de credenciales bancarias | Alta con solo cuatro dígitos; rechazo de número completo en el campo; red/id/últimos dígitos conservados al cambiar skin |
| Ausencia de confirmaciones ficticias | Referencia externa no puede pagar por el servicio demo; servicio de pago directo rechaza operaciones; historial release remite a Pixpay |

Los tests Android verifican Room real, ausencia de seed demo, servicio HCE deshabilitado, rechazo de operaciones directas, alta de referencia Pixpay, persistencia de skin sin alterar identidad, rechazo de PAN en el campo de últimos dígitos, guía de Wallet y ausencia de movimientos generados.

El ejecutor Compose necesita clases que R8 elimina del código de la app al no usarlas esta directamente. Por eso las pruebas usan `-PorelTestBuildType=release -PorelReleaseUiTests=true`, conservando el modo PERSONAL, paquete, firma y política NFC, y dejando la reducción desactivada únicamente para esa ejecución. La APK entregada vuelve a compilarse sin esa opción y se revisa por separado. No se afirma que el ejecutor Compose haya probado directamente el binario reducido.

Los avisos de lint corresponden a versiones disponibles de dependencias, uso de kapt y sugerencias de extensiones KTX. No se modificó la combinación de dependencias ya validada para introducir actualizaciones de alcance mayor durante esta entrega.

## Límites de la comprobación

El emulador no tiene NFC ni Google Wallet instalado. Se verificó la guía para esas ausencias, pero no el arranque de Wallet instalado, la incorporación de tu Pixpay, su autenticación, ni un pago físico en datáfono. Esa configuración se completa en tu teléfono siguiendo [RELEASE.md](RELEASE.md).

Orel no recibe la tarjeta seleccionada, historial, autorización ni resultado de pagos de Wallet. El resultado debe comprobarse en el datáfono y Pixpay. La APK no se ha publicado en Google Play.

Los registros de desarrollo se conservan en `.tools/`: `release-final-build.log`, `release-ui-stable-tests.log`, `release-signature.log`, `release-manifest.log` y `release-guide-ui.txt`. Las claves privadas y propiedades de firma están excluidas de Git.
