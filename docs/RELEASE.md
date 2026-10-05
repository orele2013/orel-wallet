# Orel Wallet 1.1.0 — release personal con Google Wallet

La APK `artifacts/orel-wallet-release.apk` es una compilación release firmada de Orel Wallet. Permite guardar referencias visuales de tus tarjetas, personalizarlas y abrir Google Wallet. Los pagos contactless los realiza Google Wallet con la tarjeta seleccionada allí. Orel no tiene credenciales bancarias ni una API para autorizar cada pago o recibir su resultado.

## Instalar y pagar con Pixpay

1. Copia la APK al móvil Android 11 o superior y ábrela. Autoriza a la aplicación desde la que la abres para instalar esta APK cuando Android lo solicite.
2. Instala [Google Wallet oficial](https://play.google.com/store/apps/details?id=com.google.android.apps.walletnfcrel). Añade tu Pixpay siguiendo la aplicación Pixpay o Wallet y completa las verificaciones solicitadas por el emisor. La compatibilidad depende de tu tarjeta, cuenta y dispositivo; guardar una referencia en Orel no la comprueba.
3. Activa NFC y configura Google Wallet como aplicación de pago predeterminada en Android.
4. En Orel, pulsa **Comenzar → Añadir tarjeta**. Introduce el nombre, la red que aparece en tu tarjeta y solo los últimos cuatro dígitos. Nunca el número completo, CVV o PIN.
5. Personaliza en los detalles → **Card Appearance**. Pulsa **Preparar pago con Wallet** para consultar las instrucciones o abrir Wallet y seleccionar la tarjeta real.
6. Desbloquea el móvil y acerca su parte trasera al datáfono. Puedes mantener Orel abierta cuando Wallet sea la aplicación predeterminada y el dispositivo lo permita. Android o Google pueden pedir su propia autenticación; Orel no puede sustituirla ni asegurar que no aparezca.
7. Comprueba la confirmación del datáfono y los movimientos en Pixpay. Volver de Wallet no confirma una compra en Orel.

Google explica los [requisitos y pasos de pago contactless](https://support.google.com/wallet/answer/12060043?hl=es) y su [verificación de identidad](https://support.google.com/wallet/answer/12059519?hl=es). Pixpay ofrece sus [instrucciones de activación](https://pixpay.zendesk.com/hc/es/articles/19770952967442--C%C3%B3mo-activar-y-usar-Apple-Pay-Google-Pay).

## Qué controla Orel

- Apariencia, nombre, orden, referencia principal y visibilidad locales.
- Bloqueo del acceso a Orel mediante Android. Bloquear una referencia en Orel no bloquea Pixpay ni Google Wallet.
- Apertura de la aplicación oficial de Wallet, Play Store y ajustes del dispositivo mediante intents de Android.

Orel no cambia la tarjeta predeterminada de Google Wallet, no consulta su saldo/historial y no registra cargos ni confirmaciones. La edición release empieza sin tarjetas ni movimientos ficticios. Su servicio HCE de laboratorio está deshabilitado y las rutas de pago demo no se registran.

Esta APK usa `com.orel.wallet.companion`, por lo que convive con la edición demo `com.orel.wallet`. La instalación nueva no importa los datos ficticios de la demo. No se ha publicado en Google Play. La comprobación de pagos físicos requiere tu teléfono compatible, tu Pixpay habilitada y un datáfono; el emulador no valida ese intercambio.

## Firma y futuras actualizaciones

La identidad privada se guarda **fuera de Git** en `.tools/signing/orel-wallet-release.p12` y `.tools/signing/release-signing.properties`. Conserva una copia privada de ambos archivos en un lugar seguro. No los publiques ni compartas. Android exige la misma clave para actualizar esta instalación conservando sus datos.

Para reproducir el build, con JDK 17 y SDK Android 35 configurados:

```sh
python3 scripts/create_release_key.py
./gradlew assembleRelease testDebugUnitTest lintRelease --max-workers=1 --console=plain
cp app/build/outputs/apk/release/app-release.apk artifacts/orel-wallet-release.apk
```

El script conserva la identidad existente y rechaza estados incompletos; no la sobrescribe. `OREL_SIGNING_PROPERTIES` permite indicar otro archivo de propiedades de firma. Sin propiedades, Gradle produce un release sin firma para desarrollo, que no debe entregarse como instalable.

Las pruebas Android de esta edición se compilan con `./gradlew -PorelTestBuildType=release -PorelReleaseUiTests=true assembleRelease assembleReleaseAndroidTest`; ejecuta exclusivamente `com.orel.wallet.CompanionUiTest` en un dispositivo de pruebas, ya que prepara y borra referencias locales de la instalación de prueba.

La opción `orelReleaseUiTests` conserva las APIs que necesita el ejecutor Compose; mantiene el modo PERSONAL, la firma release y el servicio HCE deshabilitado. No uses esa opción para la APK final optimizada. Después de las pruebas, recompila con `./gradlew assembleRelease` y comprueba el arranque y navegación de esa APK.
