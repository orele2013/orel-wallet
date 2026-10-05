# Privacidad — Orel Wallet 1.1.0

La edición release personal guarda nombres, red y últimos cuatro dígitos introducidos por el usuario, referencias visuales, imágenes y ajustes en el dispositivo. No solicita números completos de tarjeta, CVV ni PIN. Las imágenes elegidas se copian al almacenamiento privado; no uses fotos que contengan credenciales de tu tarjeta.

Orel no tiene permiso de Internet, no consulta bancos ni recibe datos de pagos de Google Wallet. Cuando lo solicitas, abre Google Wallet, Play Store, la ayuda oficial o los ajustes de Android. Esas aplicaciones y páginas tienen sus propias políticas y pueden conectarse a sus servicios. Orel no recibe una autorización, saldo, historial ni confirmación por abrir o volver de Wallet.

No hay analíticas ni envío de informes de fallos. Android gestiona la biometría y credencial del dispositivo; Orel no almacena huellas o PINs. El bloqueo de Orel no sustituye la verificación de Google Wallet.

Las copias de seguridad de la app están desactivadas. Puedes eliminar referencias visuales desde Orel o borrar todos los datos locales desde los ajustes de Android. Eliminar una referencia no elimina ni bloquea una tarjeta bancaria o una tarjeta de Google Wallet.

La edición debug/demo es independiente: guarda tarjetas y movimientos ficticios, nunca realiza cargos. Su HCE de laboratorio comparte solo una referencia temporal DEMO mientras existe una sesión autorizada; ese servicio está deshabilitado en release. El backend mock es una herramienta local de desarrollo independiente y no recibe datos de ninguna APK.

El contrato de almacenamiento de tokens de laboratorio cifra referencias opacas con Android Keystore y las excluye del backup. El flujo release con Google Wallet no aprovisiona ni almacena tokens bancarios.
