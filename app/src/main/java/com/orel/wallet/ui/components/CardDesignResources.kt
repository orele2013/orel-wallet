package com.orel.wallet.ui.components

import com.orel.wallet.R
import com.orel.wallet.domain.CardDesign

/** Explicit references allow resource shrinking while retaining every gallery design. */
fun cardDesignResource(design: CardDesign): Int = when (design.id) {
    "amex_centurion_black" -> R.drawable.amex_centurion_black
    "amex_centurion_koolhaas" -> R.drawable.amex_centurion_koolhaas
    "amex_centurion_wiley" -> R.drawable.amex_centurion_wiley
    "amex_platinum" -> R.drawable.amex_platinum
    "amex_platinum_mirror" -> R.drawable.amex_platinum_mirror
    "amex_platinum_wiley" -> R.drawable.amex_platinum_wiley
    "amex_platinum_mehretu" -> R.drawable.amex_platinum_mehretu
    "amex_gold_rose" -> R.drawable.amex_gold_rose
    "amex_gold" -> R.drawable.amex_gold
    "amex_green" -> R.drawable.amex_green
    "amex_blue_everyday" -> R.drawable.amex_blue_everyday
    "amex_blue_preferred" -> R.drawable.amex_blue_preferred
    "amex_delta_blue" -> R.drawable.amex_delta_blue
    "amex_delta_gold" -> R.drawable.amex_delta_gold
    "amex_delta_platinum" -> R.drawable.amex_delta_platinum
    "amex_delta_reserve" -> R.drawable.amex_delta_reserve
    "amex_hilton" -> R.drawable.amex_hilton
    "amex_hilton_surpass" -> R.drawable.amex_hilton_surpass
    "amex_hilton_aspire" -> R.drawable.amex_hilton_aspire
    "amex_marriott_bevy" -> R.drawable.amex_marriott_bevy
    "amex_marriott_brilliant" -> R.drawable.amex_marriott_brilliant
    "amex_platinum_es" -> R.drawable.amex_platinum_es
    "amex_gold_es" -> R.drawable.amex_gold_es
    "amex_green_es" -> R.drawable.amex_green_es
    "amex_gold_credit_es" -> R.drawable.amex_gold_credit_es
    "amex_renfe" -> R.drawable.amex_renfe
    "amex_melia" -> R.drawable.amex_melia
    "amex_melia_gold" -> R.drawable.amex_melia_gold
    "amex_business_gold_es" -> R.drawable.amex_business_gold_es
    "amex_business_green_es" -> R.drawable.amex_business_green_es
    "amex_corporate_gold_es" -> R.drawable.amex_corporate_gold_es
    "amex_corporate_green_es" -> R.drawable.amex_corporate_green_es
    "amex_business_platinum" -> R.drawable.amex_business_platinum
    "amex_business_gold" -> R.drawable.amex_business_gold
    "amex_business_rose" -> R.drawable.amex_business_rose
    "amex_business_white" -> R.drawable.amex_business_white
    "amex_business_green" -> R.drawable.amex_business_green
    "amex_business_cash" -> R.drawable.amex_business_cash
    "amex_business_plus" -> R.drawable.amex_business_plus
    "amex_business_graphite" -> R.drawable.amex_business_graphite
    "amex_business_marriott" -> R.drawable.amex_business_marriott
    "amex_business_delta_gold" -> R.drawable.amex_business_delta_gold
    "amex_business_delta_platinum" -> R.drawable.amex_business_delta_platinum
    "amex_business_delta_reserve" -> R.drawable.amex_business_delta_reserve
    "amex_business_hilton" -> R.drawable.amex_business_hilton
    "amex_explorer_au" -> R.drawable.amex_explorer_au
    "amex_edge_au" -> R.drawable.amex_edge_au
    "amex_essential_au" -> R.drawable.amex_essential_au
    "amex_qantas_discovery" -> R.drawable.amex_qantas_discovery
    "amex_qantas_premium" -> R.drawable.amex_qantas_premium
    "amex_qantas_ultimate" -> R.drawable.amex_qantas_ultimate
    "amex_velocity_platinum" -> R.drawable.amex_velocity_platinum
    "amex_velocity_escape" -> R.drawable.amex_velocity_escape
    "amex_qantas_business" -> R.drawable.amex_qantas_business
    "amex_velocity_business" -> R.drawable.amex_velocity_business
    "amex_ana" -> R.drawable.amex_ana
    "amex_ana_gold" -> R.drawable.amex_ana_gold
    "amex_ana_premium" -> R.drawable.amex_ana_premium
    "amex_delta_jp" -> R.drawable.amex_delta_jp
    "amex_delta_gold_jp" -> R.drawable.amex_delta_gold_jp
    "amex_hilton_jp" -> R.drawable.amex_hilton_jp
    "amex_hilton_premium_jp" -> R.drawable.amex_hilton_premium_jp
    "amex_marriott_jp" -> R.drawable.amex_marriott_jp
    "amex_marriott_premium_jp" -> R.drawable.amex_marriott_premium_jp
    "amex_gold_preferred_jp" -> R.drawable.amex_gold_preferred_jp
    "amex_business_rose_jp" -> R.drawable.amex_business_rose_jp
    "amex_business_mirror_jp" -> R.drawable.amex_business_mirror_jp
    else -> error("Unknown catalog design: ${design.id}")
}
