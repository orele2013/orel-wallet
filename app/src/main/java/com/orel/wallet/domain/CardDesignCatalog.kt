package com.orel.wallet.domain

enum class CardDesignGroup(val label: String) {
    EXCLUSIVE("Exclusivas"), PERSONAL("Personales"), TRAVEL("Viajes"), BUSINESS("Business")
}

data class CardDesign(val id: String, val name: String, val region: String, val group: CardDesignGroup, val edition: String = "")

/** Public reference artwork. Identifiers are persisted as skins, never as payment identity. */
object CardDesignCatalog {
    val designs = listOf(
        CardDesign("amex_centurion_black", "Centurion Black", "Italia / Reino Unido", CardDesignGroup.EXCLUSIVE, "Classic Black"),
        CardDesign("amex_centurion_koolhaas", "Centurion · Rem Koolhaas", "Italia / Reino Unido", CardDesignGroup.EXCLUSIVE, "Art Card"),
        CardDesign("amex_centurion_wiley", "Centurion · Kehinde Wiley", "Italia / Reino Unido", CardDesignGroup.EXCLUSIVE, "Art Card"),
        CardDesign("amex_platinum", "Platinum", "EE. UU.", CardDesignGroup.EXCLUSIVE, ""),
        CardDesign("amex_platinum_mirror", "Platinum Mirror", "EE. UU.", CardDesignGroup.EXCLUSIVE, ""),
        CardDesign("amex_platinum_wiley", "Platinum · Kehinde Wiley", "EE. UU.", CardDesignGroup.EXCLUSIVE, ""),
        CardDesign("amex_platinum_mehretu", "Platinum · Julie Mehretu", "EE. UU.", CardDesignGroup.EXCLUSIVE, ""),
        CardDesign("amex_gold_rose", "Gold Rose", "EE. UU.", CardDesignGroup.PERSONAL, ""),
        CardDesign("amex_gold", "Gold", "EE. UU.", CardDesignGroup.PERSONAL, ""),
        CardDesign("amex_green", "Green", "EE. UU.", CardDesignGroup.PERSONAL, ""),
        CardDesign("amex_blue_everyday", "Blue Cash Everyday", "EE. UU.", CardDesignGroup.PERSONAL, ""),
        CardDesign("amex_blue_preferred", "Blue Cash Preferred", "EE. UU.", CardDesignGroup.PERSONAL, ""),
        CardDesign("amex_delta_blue", "Delta SkyMiles Blue", "EE. UU.", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_delta_gold", "Delta SkyMiles Gold", "EE. UU.", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_delta_platinum", "Delta SkyMiles Platinum", "EE. UU.", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_delta_reserve", "Delta SkyMiles Reserve", "EE. UU.", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_hilton", "Hilton Honors", "EE. UU.", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_hilton_surpass", "Hilton Honors Surpass", "EE. UU.", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_hilton_aspire", "Hilton Honors Aspire", "EE. UU.", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_marriott_bevy", "Marriott Bonvoy Bevy", "EE. UU.", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_marriott_brilliant", "Marriott Bonvoy Brilliant", "EE. UU.", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_platinum_es", "Platinum · España", "España", CardDesignGroup.EXCLUSIVE, ""),
        CardDesign("amex_gold_es", "Gold · España", "España", CardDesignGroup.PERSONAL, ""),
        CardDesign("amex_green_es", "Green · España", "España", CardDesignGroup.PERSONAL, "Diseño histórico"),
        CardDesign("amex_gold_credit_es", "Gold Credit", "España", CardDesignGroup.PERSONAL, "Diseño histórico"),
        CardDesign("amex_renfe", "Renfe", "España", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_melia", "Meliá / mas", "España", CardDesignGroup.TRAVEL, "Diseño histórico"),
        CardDesign("amex_melia_gold", "Meliá Rewards Gold", "España", CardDesignGroup.TRAVEL, "Diseño histórico"),
        CardDesign("amex_business_gold_es", "Business Gold · España", "España", CardDesignGroup.BUSINESS, "Diseño de catálogo"),
        CardDesign("amex_business_green_es", "Business Green · España", "España", CardDesignGroup.BUSINESS, "Diseño de catálogo"),
        CardDesign("amex_corporate_gold_es", "Corporate Gold · España", "España", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_corporate_green_es", "Corporate Green · España", "España", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_business_platinum", "Business Platinum", "EE. UU.", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_business_gold", "Business Gold", "EE. UU.", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_business_rose", "Business Rose Gold", "EE. UU.", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_business_white", "Business White Gold", "EE. UU.", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_business_green", "Business Green Rewards", "EE. UU.", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_business_cash", "Blue Business Cash", "EE. UU.", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_business_plus", "Blue Business Plus", "EE. UU.", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_business_graphite", "Business Graphite", "EE. UU.", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_business_marriott", "Marriott Bonvoy Business", "EE. UU.", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_business_delta_gold", "Delta SkyMiles Gold Business", "EE. UU.", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_business_delta_platinum", "Delta SkyMiles Platinum Business", "EE. UU.", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_business_delta_reserve", "Delta SkyMiles Reserve Business", "EE. UU.", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_business_hilton", "Hilton Honors Business", "EE. UU.", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_explorer_au", "Explorer", "Australia", CardDesignGroup.PERSONAL, ""),
        CardDesign("amex_edge_au", "Platinum Edge", "Australia", CardDesignGroup.PERSONAL, ""),
        CardDesign("amex_essential_au", "Essential Rewards", "Australia", CardDesignGroup.PERSONAL, ""),
        CardDesign("amex_qantas_discovery", "Qantas Discovery", "Australia", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_qantas_premium", "Qantas Premium", "Australia", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_qantas_ultimate", "Qantas Ultimate", "Australia", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_velocity_platinum", "Velocity Platinum", "Australia", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_velocity_escape", "Velocity Escape Plus", "Australia", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_qantas_business", "Qantas Business Rewards", "Australia", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_velocity_business", "Velocity Business", "Australia", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_ana", "ANA", "Japón", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_ana_gold", "ANA Gold", "Japón", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_ana_premium", "ANA Premium", "Japón", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_delta_jp", "Delta SkyMiles · Japón", "Japón", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_delta_gold_jp", "Delta SkyMiles Gold · Japón", "Japón", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_hilton_jp", "Hilton Honors · Japón", "Japón", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_hilton_premium_jp", "Hilton Honors Premium", "Japón", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_marriott_jp", "Marriott Bonvoy · Japón", "Japón", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_marriott_premium_jp", "Marriott Bonvoy Premium", "Japón", CardDesignGroup.TRAVEL, ""),
        CardDesign("amex_gold_preferred_jp", "Gold Preferred", "Japón", CardDesignGroup.PERSONAL, ""),
        CardDesign("amex_business_rose_jp", "Business Rose Gold · Japón", "Japón", CardDesignGroup.BUSINESS, ""),
        CardDesign("amex_business_mirror_jp", "Business Platinum Mirror", "Japón", CardDesignGroup.BUSINESS, ""),
    )
    fun find(appearance: CardAppearance): CardDesign? =
        designs.firstOrNull { appearance.backgroundType == BackgroundType.SKIN && it.id == appearance.backgroundValue }

    fun matching(group: CardDesignGroup? = null, query: String = ""): List<CardDesign> {
        val terms = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return designs.filter { design ->
            (group == null || design.group == group) && terms.all {
                it in "${design.name} ${design.region} ${design.edition}".lowercase()
            }
        }
    }
}

fun CardAppearance.withDesign(design: CardDesign): CardAppearance =
    copy(backgroundType = BackgroundType.SKIN, backgroundValue = design.id, brightness = .5f, contrast = .5f)
