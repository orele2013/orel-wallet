# Orel Wallet — diseño aprobado por la solicitud

La especificación detallada del usuario establece Android nativo, Kotlin, Compose Material 3, MVVM y Clean Architecture. La ejecución está autorizada expresamente; se usan decisiones reversibles sin añadir una fase de aprobación.

Identidad visual original: azul eléctrico, tipografía Android sans, tarjetas con fondos propios, esquinas suaves y superficies claras u oscuras. Interfaz en español, importes EUR y modo DEMO siempre visible. Inicio con carrusel; tarjetas, historial y ajustes en navegación inferior. Personalización con galería Android, skins originales, color, gradiente, brillo, contraste, chip y texto. Gestión persistente con Room y separación Card / CardAppearance.

BiometricPrompt con credencial del dispositivo. Simulación de autenticación exclusivamente explícita en demo y nunca etiquetada como biometría real. NFC se detecta realmente; HCE utiliza AID propietario de laboratorio en categoría other, sin EMV ni datos bancarios. La simulación de compra requiere una acción explícita y confirmación de DemoPaymentService. Estados de pago validados, autorización ligada a sesión y caducidad, cancelación sin registros, ejecución idempotente.

Room conserva únicamente metadatos y transacciones demo; referencias de token se cifran AES-GCM con Keystore y exclusión de backup. Pantallas de pago y seguridad protegidas contra capturas. Proveedores reales son interfaces sin endpoints ni credenciales inventadas. Backend local mock independiente, con confirmación de demo explícita, validación y pruebas.

Verificación: compilación progresiva, pruebas unitarias para invariantes y estados, tests Compose en emulador cuando sea posible, lint y APK debug instalable. NFC físico y proveedores reales necesitan hardware/integración externa.
