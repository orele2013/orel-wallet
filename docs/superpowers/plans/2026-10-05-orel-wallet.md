# Orel Wallet Implementation Plan

> For agentic workers: use superpowers:subagent-driven-development for independent implementation and review units.

**Goal:** Entregar una APK Android de wallet demo funcional, pulida y preparada para un proveedor autorizado.
**Architecture:** Compose → ViewModel / StateFlow → casos de uso / repositorio → Room. Identidad de tarjeta inmutable respecto a apariencia. Servicios de seguridad, NFC y pagos independientes.
**Tech Stack:** Kotlin 2.0.21, Compose, Material 3, AGP 8.9.1, Gradle 8.12, Android API 35, min API 30, Java 17, Room, coroutines.
**Spec:** ../specs/2026-10-05-orel-wallet-design.md; requisitos originales del usuario.

## Global constraints
No PAN, CVV, secretos en Room ni logs. DEMO visible. Solo confirmación del servicio permite éxito. Ningún pago bancario o integración ficticia. Interfaz en español y fondos originales.

## Secuencia de implementación
- [x] 1–3: proyecto, Gradle y Compose. `app/build.gradle.kts`, `MainActivity.kt`; comprobar `./gradlew assembleDebug`.
- [x] 4: arquitectura en `domain`, `data`, `wallet`, `payments`, `security`, `nfc`, `core`. Modelos Card, CardAppearance, WalletSettings, Transaction, PaymentSession sin datos sensibles. Repositorio observable y operaciones transaccionales.
- [x] 5–7: navegación, Home y componentes Card en `navigation`, `presentation`, `ui`. Carrusel nativo y selección por indicadores.
- [x] 8–10: apariencia, gestión y tarjetas demo. Probar que cambiar skin conserva network/id y que borrar predeterminada promueve otra.
- [x] 11: historial con búsqueda, filtros y detalles persistentes.
- [x] 12–14: BiometricPrompt, detección NFC, HCE demo. Verificar ausencia de autenticación silenciosa y denegación de APDU sin sesión autorizada.
- [x] 15–18: máquina de estados, PaymentService, implementación demo y seguridad. Tests de caminos válidos/invalidos, cancelación, idempotencia, timeout y rechazo de tarjeta real.
- [x] 19: backend mock local `backend/server.mjs`, endpoints demo y pruebas Node. Cero conexión de APK a servidor por defecto.
- [x] 20–21: pruebas unitarias/UI y README, política de privacidad y licencias.
- [x] 22–25: assembleDebug, testDebugUnitTest, lintDebug, instalación e instrumentación Android en emulador; revisión visual y APK en `artifacts/`.

## Contratos entre unidades
El repositorio expone Flow de cards/transactions/settings y mutaciones suspend. ViewModel consume estos flows y publica StateFlow. PaymentService controla preparación, autenticación y ejecución: Transaction solo tras confirmación, nunca desde UI. Apariencia contiene solo metadatos estéticos; picker copia imagen a almacenamiento privado. Biometría retorna resultado explícito y cancelable. HCE expone solo una credencial demo temporal autorizada.

## Verificación
Tests JUnit y coroutines para repositorio, valores predeterminados, límites de apariencia, estado de autenticación, disponibilidad NFC y pagos. Compose tests: bienvenida, navegación, alta demo, personalización y simulación de pago. Revisión final de seguridad y calidad independiente antes de entregar.

Resultado: APK debug compilada e instalada, 51 pruebas aprobadas, cero errores de lint, PIN oficial verificado y capturas reales. Detalles y límites en `docs/VERIFICATION.md`.
