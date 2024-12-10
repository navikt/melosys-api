package no.nav.melosys.integrasjon.trygdeavgift

import no.nav.melosys.domain.kodeverk.Avgiftsdekning
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.exception.FunksjonellException

class AvgiftsdekningerFraTrygdedekning {
    companion object {
        fun avgiftsdekningerFraTrygdedekning(trygdedekning: Trygdedekninger): Set<Avgiftsdekning> {
            return when (trygdedekning) {
                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_HELSE ->
                    setOf(Avgiftsdekning.HELSEDEL_UTEN_SYKEPENGER)

                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_A_ANDRE_LEDD_HELSE_SYKE_FORELDREPENGER ->
                    setOf(Avgiftsdekning.HELSEDEL_MED_SYKEPENGER)

                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_PENSJON ->
                    setOf(Avgiftsdekning.PENSJONSDEL_UTEN_YRKESSKADETRYGD)

                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_HELSE_PENSJON ->
                    setOf(Avgiftsdekning.HELSEDEL_UTEN_SYKEPENGER, Avgiftsdekning.PENSJONSDEL_UTEN_YRKESSKADETRYGD)

                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER ->
                    setOf(Avgiftsdekning.HELSEDEL_MED_SYKEPENGER, Avgiftsdekning.PENSJONSDEL_UTEN_YRKESSKADETRYGD)

                Trygdedekninger.FULL_DEKNING_FTRL ->
                    setOf(Avgiftsdekning.HELSEDEL_MED_SYKEPENGER, Avgiftsdekning.PENSJONSDEL_MED_YRKESSKADETRYGD)

                Trygdedekninger.FTRL_2_7A_ANDRE_LEDD_B_HELSE_SYKE_FORELDREPENGER ->
                    setOf(Avgiftsdekning.HELSEDEL_MED_SYKEPENGER)

                Trygdedekninger.FTRL_2_7_TREDJE_LEDD_B_HELSE_SYKE_FORELDREPENGER ->
                    setOf(Avgiftsdekning.HELSEDEL_MED_SYKEPENGER)

                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_B_TREDJE_LEDD_PENSJON_YRKESSKADE ->
                    setOf(Avgiftsdekning.PENSJONSDEL_MED_YRKESSKADETRYGD)

                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_TREDJE_LEDD_HELSE_PENSJON_YRKESSKADE ->
                    setOf(Avgiftsdekning.HELSEDEL_UTEN_SYKEPENGER, Avgiftsdekning.PENSJONSDEL_MED_YRKESSKADETRYGD)

                Trygdedekninger.FTRL_2_9_FØRSTE_LEDD_C_ANDRE_LEDD_TREDJE_LEDD_HELSE_PENSJON_SYKE_FORELDREPENGER_YRKESSKADE ->
                    setOf(Avgiftsdekning.HELSEDEL_MED_SYKEPENGER, Avgiftsdekning.PENSJONSDEL_MED_YRKESSKADETRYGD)

                // Dette brukes ikke, men vi må mappe det siden det er samme endepunkt som frivillig medlemskap i trygdeavgiftsberegning.
                Trygdedekninger.TILLEGGSAVTALE_NATO_HELSEDEL ->
                    setOf(Avgiftsdekning.HELSEDEL_MED_SYKEPENGER)

                else -> throw FunksjonellException("Kan ikke finne avgiftsdekninger fra trygdedekning $trygdedekning")
            }
        }
    }
}
