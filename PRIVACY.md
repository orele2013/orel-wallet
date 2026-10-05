# Privacidad — Orel Wallet Demo 1.0

Esta versión es una demostración local y no realiza pagos reales.

Se guardan en el dispositivo nombres de tarjetas ficticias, últimos cuatro dígitos ficticios, preferencias visuales, ajustes y movimientos demo. Las imágenes seleccionadas se copian al directorio privado de la aplicación. No se guardan PAN completos, CVV, PINs ni huellas.

La app no tiene permiso de Internet, no envía datos a bancos, no incluye analíticas ni envía informes de fallos. El backend mock es una herramienta de desarrollo independiente y no recibe datos de la APK.

Android gestiona la biometría y credencial del dispositivo. Las referencias de token, si se prueban mediante el contrato de seguridad, se cifran con Android Keystore y se excluyen de backup. Las tarjetas y movimientos demo son metadatos no sensibles guardados mediante Room; las copias de seguridad de la aplicación están desactivadas.

El HCE de laboratorio puede compartir únicamente una referencia temporal etiquetada DEMO con un lector compatible mientras la sesión está autorizada y en primer plano. No transmite números de tarjetas ni confirma pagos.

Se pueden eliminar tarjetas desde la app. El historial de demostración se conserva para mantener la referencia de movimientos; borrar los datos de la aplicación desde los ajustes de Android elimina tarjetas, historial, preferencias, imágenes y referencias locales.

Una versión con pagos reales requiere una política de privacidad específica de la integración antes de su publicación.
