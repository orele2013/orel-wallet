# Galería American Express

Orel Wallet 1.2.0 incluye **67 diseños de referencia** con imágenes públicas de los catálogos de American Express. Se eligen desde los detalles de una tarjeta → **Card Appearance → Amex**. La búsqueda admite modelo, artista o país, y los filtros separan Exclusivas, Personales, Viajes y Business. Pulsa **Guardar apariencia** para aplicar la selección.

## Colección incluida

- **Centurion:** Classic Black, Rem Koolhaas y Kehinde Wiley. Las imágenes nativas de 670 × 424 se extraen del formulario público de American Express Italia; los nombres de los artistas se contrastan con el formulario británico.
- **Platinum:** clásica, Mirror, Kehinde Wiley, Julie Mehretu y variante española.
- **Personales:** Gold, Rose Gold, Green, Blue Cash Everyday/Preferred, Gold Credit, Explorer, Platinum Edge, Essential Rewards y Gold Preferred.
- **Aerolíneas:** Delta SkyMiles Blue/Gold/Platinum/Reserve y variantes Business y japonesas; ANA/Gold/Premium; Qantas Discovery/Premium/Ultimate/Business; Velocity Escape Plus/Platinum/Business; Renfe.
- **Hoteles:** Hilton Honors/Surpass/Aspire/Business y versiones japonesas; Marriott Bonvoy Bevy/Brilliant/Business y versiones japonesas; Meliá/mas y Meliá Rewards Gold.
- **Business y Corporate:** Platinum, Gold, Rose Gold, White Gold, Green Rewards, Graphite, Blue Business Cash/Plus, variantes españolas y Platinum Mirror/Rose Gold de Japón.

Es una colección internacional de diseños documentados de España, Estados Unidos, Italia/Reino Unido, Australia y Japón, no un catálogo exhaustivo de todas las tarjetas, países o años de American Express. Las imágenes españolas de Green, Gold Credit y Meliá se identifican como históricas. El catálogo visual no acredita disponibilidad para solicitar una tarjeta ni acceso a sus prestaciones.

## Fidelidad e identidad

Se conservan los píxeles de las imágenes de catálogo mediante WebP sin pérdida, incluidas sus máscaras de transparencia. Se muestran completas, sin recortar, recolorear, aplicar brillo/contraste ni superponer el chip o logotipo de Orel. La pantalla adapta su tamaño al dispositivo; algunas fuentes tienen menos resolución que otras. Una imagen pública no reproduce el relieve, material ni reflejos físicos de la tarjeta.

Los nombres, números de muestra y fechas ya impresos en las imágenes pertenecen a las referencias públicas del diseño. No son datos introducidos por el usuario. El nombre de la referencia de Orel, su **red real indicada y sus últimos cuatro dígitos** aparecen fuera de la imagen en cada pantalla donde se muestra una tarjeta. Elegir una Centurion para una referencia Visa conserva Visa, su id y sus últimos dígitos.

La apariencia se almacena como una skin de `CardAppearance` y no necesita una migración de Room. La edición PERSONAL sigue sin almacenar PAN, CVV o PIN introducidos por el usuario; no crea una tarjeta Amex, credenciales, beneficios ni movimientos. Google Wallet conserva su propia imagen, selección y autenticación. Los estilos originales de Orel y las imágenes personales continúan disponibles en **Imagen**.

## Fuentes y atribución

Las marcas, logotipos e imágenes de American Express y sus colaboradores pertenecen a sus respectivos titulares. Orel Wallet es una aplicación independiente y no está afiliada a American Express. Estos recursos de terceros no se presentan como arte original de Orel ni como contenido de código abierto.

El inventario completo en [CARD_ART_SOURCES.json](CARD_ART_SOURCES.json) registra cada recurso, país, página oficial, URL de la imagen/PDF, dimensiones y SHA-256. Fecha de consulta: 5 de octubre de 2026. Los diseños se incluyen en la APK y funcionan sin conexión; la app no descarga imágenes.

Fuentes principales: [España](https://www.americanexpress.com/es/beneficios/infotitulares/), [EE. UU. personal](https://www.americanexpress.com/us/credit-cards/), [EE. UU. Business](https://www.americanexpress.com/us/credit-cards/business/business-credit-cards/), [Australia](https://www.americanexpress.com/au/credit-cards/all-cards/), [Japón](https://www.americanexpress.com/ja-jp/credit-cards/all-cards/), [Centurion Italia](https://www.americanexpress.com/content/dam/amex/it/staticassets/pdf/card-member/Modulo_di_richiesta_CarteSupplementare_Centurion.pdf), [Centurion Reino Unido](https://www.americanexpress.com/content/dam/amex/en-gb/benefits/centurion/supplementary-cards/Centurion_Supplementary_Card_Printable_Forms.pdf).

Para verificar integridad y correspondencia con los recursos Android: `python3 scripts/verify_card_art.py`.
