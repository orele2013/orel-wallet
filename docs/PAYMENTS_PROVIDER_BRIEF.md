# Orel Wallet: solicitud de integración de pagos reales

Preparado el 5 de octubre de 2026. País de la tarjeta y carácter personal/comercial del proyecto: por confirmar.

## Objetivo que debe aprobar el proveedor

Permitir pagos contactless en comercios desde una wallet Android propia con tarjetas elegibles. La app tendrá su propia interfaz y utilizará las APIs oficiales de Android para la autenticación, con autorización y credenciales gestionadas según las exigencias del proveedor.

La app actual es una demo Kotlin/Compose (`com.orel.wallet`, Android 11 o superior). No contiene credenciales bancarias ni realiza cargos. Su HCE es un protocolo de laboratorio; tendrá que sustituirse por la solución autorizada del proveedor. La apariencia visual no modifica la identidad ni la red de una tarjeta.

No buscamos únicamente un botón para añadir tarjetas a Google Wallet. Tampoco buscamos una API de checkout online o una API de transferencias. Google Wallet no ofrece una API pública documentada para aceptar una confirmación de otra app que sustituya su verificación en cada pago NFC. La experiencia solicitada requiere estudiar una solución de wallet propia, sin prometer previamente que se podrán eliminar todos los diálogos de Android o del proveedor.

## Mensaje para enviar a Pixpay

> Hola. Estoy desarrollando Orel Wallet, una aplicación Android de wallet con interfaz propia, personalización estética de tarjetas y autenticación mediante las APIs oficiales de Android. La versión actual es una demo y no realiza pagos reales.
>
> Quiero saber si mi tarjeta Pixpay puede utilizarse en una wallet NFC de terceros autorizada, para pagar en comercios desde esa wallet. No me refiero a añadirla a Google Wallet ni a aceptar pagos online.
>
> ¿Pixpay y el emisor permiten este tipo de integración? ¿Podéis remitirme al equipo de integraciones o al proveedor de tokenización/HCE correspondiente?
>
> Si es viable, necesitaría conocer: elegibilidad para un proyecto particular o empresarial, países y tarjetas compatibles, SDK Android, documentación, entorno de pruebas, requisitos de autenticación, confirmaciones de transacciones, certificaciones y condiciones comerciales. ¿Qué información debo aportar para solicitar acceso?

Añadir al mensaje el país de la cuenta y si el proyecto será personal o un servicio público. No adjuntar PAN, CVV, PIN, códigos SMS ni claves privadas.

## Reparto de tareas

| Tú | Trabajo técnico que puede preparar Codex |
|---|---|
| Contactar a Pixpay y conseguir confirmación de elegibilidad y contacto técnico. | Revisar la respuesta y comprobar si la oferta cubre pagos NFC desde una wallet propia. |
| Completar la identificación y los contratos que exija el proveedor; decidir costes y alcance. | Preparar el diseño de integración y la relación entre app, backend y proveedor. |
| Conseguir acceso autorizado al SDK, documentación, sandbox y credenciales de prueba. Configurar secretos por un canal seguro. | Integrar los contratos oficiales, backend, provisioning, ciclo de vida de tokens y pruebas, manteniendo separada la demo. |
| Proporcionar un móvil NFC compatible y realizar los acercamientos al lector en el entorno de pruebas aprobado. | Compilar e instalar, recoger diagnósticos y corregir problemas de integración. |
| Conseguir las aprobaciones exigidas y controlar cuentas de distribución, facturación y firma. | Preparar configuración de producción, build firmado y documentación para la revisión. |

## Acceso técnico que necesitamos para empezar

- Confirmación del emisor sobre tarjetas y países admitidos; autorización del programa correspondiente.
- SDK Android y documentación de pagos contactless, provisioning, autenticación y almacenamiento seguro.
- Sandbox con tarjetas/credenciales de prueba, identidad de aplicación autorizada y certificados necesarios.
- Contrato para recibir estados oficiales de pago: eventos del SDK, APIs o webhooks; significado de aprobación, rechazo, reverso y liquidación.
- Requisitos de backend, identificación de usuarios, gestión de dispositivos y revocación de tokens.
- Plan de pruebas y aprobaciones/certificaciones necesarias para acceder a producción.

Obtener acceso de sandbox permite desarrollar y probar; no habilita cargos en producción. Las pruebas actuales de la demo no certifican una integración bancaria futura.

## Contactos y fuentes verificadas

- [Contacto oficial de Pixpay España](https://www.pixpay.es/contacto/): el canal indicado es el chat de la aplicación, en configuración. La página identifica a IDT Services Limited como emisor de la tarjeta Visa española. Si la cuenta es de otro país, confirmar su emisor y canal local.
- [Visa Digital Wallet Enabler](https://developer.visa.com/capabilities/token-gateway): SDK y servicios para wallets; el acceso debe solicitarse y no está garantizado para este proyecto.
- [Acceso restringido a VDWE](https://developer.visa.com/capabilities/token-gateway/docs-how-to-use-token-requestor-onboarding).
- [Visa Token Service: participación y aprobación del emisor](https://developer.visa.com/capabilities/token-service-provisioning).
- [Verificación de identidad en Google Wallet](https://support.google.com/wallet/answer/12059519?hl=es).

Si Pixpay no admite el caso, la siguiente decisión será estudiar un proveedor/programa que sí lo admita y confirmar sus tarjetas elegibles. Abrir una cuenta de desarrollador o publicar una APK no sustituye esa autorización.
