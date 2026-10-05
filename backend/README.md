# Backend local de demostración

Este servidor Node.js sin dependencias procesa únicamente **metadatos ficticios de tarjetas y compras DEMO**. No realiza pagos, autenticación bancaria, tokenización, operaciones EMV ni llamadas a proveedores. Su estado vive en memoria y se pierde al reiniciar. No ofrece endpoints bancarios.

La aplicación Android inicial **no consume este backend**: usa Room como almacenamiento local y `DemoPaymentService` para confirmar sus simulaciones. Arrancar el servidor no cambia los datos ni el comportamiento de la aplicación. Esta API sirve como contrato HTTP independiente para desarrollo local.

## Ejecución y pruebas

Requiere Node.js 22.10 o posterior y npm. No hace falta instalar paquetes.

```sh
cd backend
npm test
npm start
```

`npm start` carga `.env` de la raíz cuando existe. Se puede copiar `.env.example` y ajustar sus valores. Por defecto el servidor escucha en **http://127.0.0.1:8787**, con sesiones de 60 segundos. El proceso fija `127.0.0.1`; no permite configurar una dirección de red. No lo publique mediante un proxy, túnel o reenvío de puertos.

| Variable | Valor por defecto | Uso |
| --- | --- | --- |
| `PORT` | `8787` | Puerto entero entre 1 y 65535. |
| `MOCK_SESSION_TTL_SECONDS` | `60` | Caducidad entre 1 y 300 segundos. |
| `MOCK_ALLOWED_ORIGIN` | Vacío | Un único origen HTTP local exacto, con puerto si corresponde. |
| `MOCK_API_TOKEN` | Vacío | Token local opcional elegido por el desarrollador; exige `Authorization: Bearer <token>` en los endpoints. |

Los secretos opcionales se guardan en `.env`, excluido de Git. El ejemplo no contiene claves inventadas ni claves de proveedores. El servidor no imprime cuerpos, tokens ni datos enviados. La comprobación del token usa comparación de tiempo constante para valores de la misma longitud.

## Contrato HTTP

Todos los cuerpos POST deben ser objetos JSON UTF-8 con `Content-Type: application/json` y **`isDemo: true` explícito**. El límite es de 16 KiB por petición. Se rechazan propiedades desconocidas, parámetros de consulta, PAN/CVV/CVC/PIN, IBAN y otros parámetros sensibles, incluso anidados. También se rechazan secuencias de dígitos que parezcan un PAN en texto libre. Los únicos números de tarjeta admitidos son cuatro dígitos ficticios en `last4`; no son datos de una tarjeta real.

Todos los resultados JSON incluyen `isDemo: true`. Los errores tienen la forma siguiente y no repiten los valores recibidos:

```json
{"isDemo":true,"error":{"code":"DEMO_REQUIRED","message":"This local server accepts only isDemo: true."}}
```

| Método y ruta | Resultado |
| --- | --- |
| `GET /health` | `200`: estado, modo DEMO y almacenamiento en memoria. |
| `POST /wallet/cards` | `201`: `{ "card": ... }`, con ID asignado por el servidor. |
| `GET /wallet/cards` | `200`: `{ "cards": [...] }`. |
| `POST /payments/session` | `201`: `{ "session": ... }`, estado `AUTHORIZING`; aún no crea una transacción. |
| `POST /payments/authorize` | `200`: sesión `CONFIRMED`, transacción DEMO e indicador `idempotent`. |
| `GET /transactions` | `200`: `{ "transactions": [...] }`, únicamente compras demo confirmadas. |

### Tarjetas

Ejemplo completo de creación con datos ficticios:

```sh
curl http://127.0.0.1:8787/wallet/cards \
  -H 'Content-Type: application/json' \
  -d '{"isDemo":true,"displayName":"Personal demo","network":"VISA","last4":"4821","appearance":{"backgroundType":"SKIN","backgroundValue":"gold","chipStyle":"GOLD"}}'
```

`displayName` se recorta y debe tener de 1 a 40 caracteres sin controles. `network` admite `VISA`, `MASTERCARD`, `AMEX`, `LOYALTY` o `GIFT`. `last4` debe ser una cadena de exactamente cuatro dígitos. `isLocked` e `isHidden` son booleanos opcionales que valen `false` por defecto. No hay operaciones de actualización ni borrado en esta versión del contrato.

La apariencia es un objeto separado: una skin dorada puede pertenecer a una tarjeta `VISA`; cambiar sus propiedades visuales nunca selecciona ni deriva la red de pago. Sus campos son opcionales; `null` no es válido.

| Campo de apariencia | Valores | Por defecto |
| --- | --- | --- |
| `backgroundType` | `SKIN`, `COLOR`, `GRADIENT`, `IMAGE` | `SKIN` |
| `backgroundValue` | Skin/gradiente: `blue`, `graphite`, `gold`, `violet`, `mint`, `ice`, `coral`; color: `#RRGGBB`; imagen: referencia ficticia `demo-image:identificador` | `blue` |
| `textColor` | `#RRGGBB` | `#FFFFFF` |
| `brightness`, `contrast` | Número finito entre 0 y 1 | `0.5` |
| `chipStyle` | `SILVER`, `GOLD`, `MINIMAL` | `SILVER` |
| `numberPosition` | `BOTTOM`, `TOP` | `BOTTOM` |
| `textStyle` | `CLASSIC`, `MONO` | `CLASSIC` |

`IMAGE` guarda exclusivamente una referencia ficticia; no acepta rutas, URLs ni imágenes. La galería y los archivos locales pertenecen a la implementación Android.

### Sesión y confirmación demo

Copie el `card.id` recibido al crear la tarjeta:

```sh
curl http://127.0.0.1:8787/payments/session \
  -H 'Content-Type: application/json' \
  -d '{"isDemo":true,"cardId":"<card.id>","amountMinor":1299,"currency":"EUR","merchant":"Comercio demo","channel":"IN_STORE"}'
```

`amountMinor` es un entero seguro positivo en céntimos: `1299` representa 12,99 EUR. No se aceptan decimales ni números en cadenas. La moneda admitida es `EUR`; el canal es `IN_STORE` u `ONLINE`; el comercio tiene de 1 a 80 caracteres sin controles. Solo las tarjetas `VISA`, `MASTERCARD` o `AMEX` visibles y desbloqueadas pueden simular compras.

La sesión fija tarjeta, importe, moneda, comercio y canal. Devuelve `createdAt` y `expiresAt` en milisegundos Unix, con estado `AUTHORIZING`. Crear o abandonar una sesión no escribe ninguna transacción. Una sesión caducada no puede confirmarse.

La confirmación exige una acción explícita del cliente. Copie el `session.id` recibido y envíe los tres indicadores de demostración:

```sh
curl http://127.0.0.1:8787/payments/authorize \
  -H 'Content-Type: application/json' \
  -d '{"isDemo":true,"sessionId":"<session.id>","authenticationMethod":"DEMO_SIMULATION","confirmed":true}'
```

Esta llamada **simula** una autorización; no acredita una autenticación biométrica ni bancaria. El servidor rechaza `BIOMETRIC`, cualquier método de proveedor, `confirmed: false` y campos que intenten cambiar tarjeta o importe. Solo entonces cambia la sesión a `CONFIRMED` y crea una transacción con estado `COMPLETED`, `isDemo: true` y `authenticationMethod: "DEMO_SIMULATION"`.

Los reintentos de autorización de la misma sesión devuelven la misma transacción, con `idempotent: true`, incluso después del vencimiento de una sesión ya confirmada. Las confirmaciones concurrentes también crean una sola transacción. **Crear otra sesión es otra compra demo**: la creación de sesiones no comparte una clave de idempotencia. Reiniciar el proceso borra el historial y la información de reintentos. El contrato no proporciona una ruta de cancelación; para abandonar una compra no envíe la confirmación.

### Errores y acceso local

| HTTP | Código representativo | Causa |
| --- | --- | --- |
| `400` | `DEMO_REQUIRED` | Falta `isDemo: true` o se intenta modo real. |
| `400` | `SENSITIVE_DATA_REJECTED` | Datos de pago sensibles o texto que parece un PAN. |
| `400` | `INVALID_INPUT`, `INVALID_JSON` | Campo, enum, importe, estructura o codificación inválidos. |
| `400` | `DEMO_CONFIRMATION_REQUIRED` | Falta confirmación explícita de simulación. |
| `401` | `UNAUTHORIZED` | Token local configurado, ausente o incorrecto. |
| `403` | `ORIGIN_DENIED`, `LOCAL_HOST_REQUIRED` | Origen de navegador o Host no autorizado. |
| `404` | `CARD_NOT_FOUND`, `SESSION_NOT_FOUND`, `NOT_FOUND` | Referencia o ruta inexistente. |
| `405` | `METHOD_NOT_ALLOWED` | Método HTTP no admitido por la ruta. |
| `409` | `CARD_LOCKED`, `CARD_HIDDEN`, `CARD_NOT_PAYABLE` | Tarjeta no habilitada para la simulación. |
| `410` | `SESSION_EXPIRED` | Sesión sin confirmar ya caducada. |
| `413` | `BODY_TOO_LARGE` | Cuerpo superior al límite. |
| `415` | `JSON_REQUIRED` | Tipo de contenido o compresión no admitidos. |

CORS está desactivado por defecto para peticiones que llevan `Origin`. El cliente de terminal puede usar la API sin ese encabezado. Si se configura `MOCK_ALLOWED_ORIGIN`, solo ese origen HTTP de `localhost`, `127.0.0.1` o `[::1]` recibe permiso; no hay comodines ni credenciales CORS. Los preflight admiten únicamente métodos de la ruta y encabezados `Content-Type` y `Authorization`. Los encabezados Host deben identificar loopback. Las respuestas deshabilitan caché.

## Integración futura

Para conectar Android a este mock, hace falta implementar de forma explícita un cliente HTTP y decidir cómo intercambia sus identificadores y estado con el repositorio local. Los modelos JSON de esta API son un contrato independiente, no entidades Room ni una API ya conectada a la app. No desactive controles de seguridad Android para tratar este mock HTTP local como un backend de producción.

Para pagos reales, sustituya las interfaces de proveedor pendientes por una integración oficial de tokenización y pagos, con SDK/APIs admitidos, configuración y credenciales suministradas por el proveedor, autenticación verificable en servidor y resultados confirmados por ese proveedor. La provisión de una tarjeta y el pago NFC requieren una integración autorizada específica; este servidor y el HCE de laboratorio no implementan esos protocolos. Conservar tokens, validar eventos del proveedor, coordinar idempotencia y persistencia, y verificar el flujo en su entorno de pruebas forman parte de ese trabajo separado.

Las pruebas incluidas ejecutan peticiones HTTP reales contra loopback y cubren el contrato demo, datos sensibles anidados, UTF-8, límites de cuerpo, enums, tarjetas bloqueadas, caducidad y reintentos concurrentes. No validan pagos físicos ni proveedores reales.
