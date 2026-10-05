# Verificación — Orel Wallet release 1.2.0

Comprobada el 5 de octubre de 2026. Esta edición añade la galería internacional Amex a la app PERSONAL de referencias visuales con acceso a Google Wallet.

## Artefacto

- APK: `artifacts/orel-wallet-release.apk`.
- Paquete: `com.orel.wallet.companion`, versión `1.2.0`, código `3`.
- Android mínimo: 11/API 30; target: API 35.
- Tamaño: 13.040.019 bytes (12,4 MiB).
- SHA-256: `c66dfe085faec48afb32530f547ddf2a2ba7718de1120e6d126d6b431538fe9c`.
- Firma v2 verificada con `apksigner`, certificado `CN=Orel Wallet`.
- Huella SHA-256 del certificado: `7bc8a6884de9f607bb15a9b394db69642781317780767c4cd7e4e773eae23f06`, conservada desde 1.1.0.
- Release optimizada con R8 y reducción de recursos. Sin flag debuggable, backup deshabilitado, sin permiso de Internet y HCE de demo deshabilitado.

## Comprobaciones

| Comprobación | Resultado |
|---|---|
| `testDebugUnitTest` | 31 pruebas JVM aprobadas; 0 errores/fallos |
| `assembleRelease assembleReleaseAndroidTest` con opciones de QA | Build correcto |
| `CompanionUiTest` en release PERSONAL firmada sin reducción para el ejecutor | `OK (4 tests)`, 62,856 s |
| `assembleRelease lintRelease` sin opciones de QA | Build correcto; 0 errores de lint, 17 avisos |
| `python3 scripts/verify_card_art.py` | 67 diseños; hashes, fuentes y mapeos de recursos verificados |
| Comparación de cada imagen original con su WebP sin pérdida | Píxeles RGBA idénticos en los 67 diseños |
| Comparación de los recursos WebP incluidos en la APK final con el inventario | Los 67 recursos conservados byte por byte después de reducir/optimizar |
| Instalación/arranque de la APK optimizada sobre la versión de QA | Correctos, misma firma y datos conservados |
| Revisión manual de galería, selección de Centurion Black, guardado y reinicio completo de la APK optimizada | Correcta; imagen conservada e identidad Visa/4821 visible |

Las pruebas Android usan Room real y verifican búsquedas de ANA/Japón y Centurion/Wiley, ausencia de resultados, selección accesible, guardado y relectura mediante otra instancia de Room, conservación de id/red/últimos dígitos, ausencia de transacciones, recuperación de selección al reabrir el editor y retorno a Aurora. También mantienen las comprobaciones de alta con cuatro dígitos, ausencia de seed demo y guía de Wallet.

La instrumentación Compose usa `-PorelTestBuildType=release -PorelReleaseUiTests=true` para conservar APIs de AndroidX que R8 elimina del código de la aplicación. Conserva el modo PERSONAL, paquete, firma y política NFC. La APK entregada se recompila sin esa opción; no se afirma que el ejecutor Compose haya probado directamente el binario reducido.

Capturas reales de la APK optimizada: [galería](../artifacts/qa-amex-gallery.png), [vista previa de Centurion](../artifacts/qa-amex-centurion.png) e [inicio después del reinicio](../artifacts/qa-amex-home.png).

La fidelidad se refiere a las imágenes públicas documentadas; su resolución varía y no reproduce materiales físicos. La colección no es exhaustiva de todos los países/años. Fuentes y atribuciones en [CARD_DESIGNS.md](CARD_DESIGNS.md).

Orel mantiene su papel de referencia visual: no convierte Pixpay en Amex ni modifica la apariencia o autenticación de Google Wallet. El emulador carece de NFC y Google Wallet; no se verificó un pago físico. La comprobación anterior se conserva en [RELEASE_VERIFICATION_1.1.0.md](RELEASE_VERIFICATION_1.1.0.md).

Registros locales de esta entrega en `.tools/amex/`: `build-tests.log`, `ui-tests.log`, `final-build.log`, `signature.log`, `badging.log` y `manifest.log`. Las claves privadas y propiedades de firma permanecen excluidas de Git.
