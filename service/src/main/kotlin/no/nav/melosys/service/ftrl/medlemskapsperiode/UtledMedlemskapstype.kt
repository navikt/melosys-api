package no.nav.melosys.service.ftrl.medlemskapsperiode

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Medlemskapstyper

object UtledMedlemskapstype {

    fun av(bestemmelse: Folketrygdloven_kap2_bestemmelser): Medlemskapstyper {
        if (bestemmelse === Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD) {
            return Medlemskapstyper.FRIVILLIG
        }
        if (bestemmelse in PliktigeMedlemskapsbestemmelser.bestemmelser) {
            return Medlemskapstyper.PLIKTIG
        }
        return Medlemskapstyper.FRIVILLIG
    }

    fun avToggle(bestemmelse: Folketrygdloven_kap2_bestemmelser): Medlemskapstyper {
        if (bestemmelse === Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD) {
            return Medlemskapstyper.FRIVILLIG
        }
        if (bestemmelse in PliktigeMedlemskapsbestemmelser.bestemmelserNy) {
            return Medlemskapstyper.PLIKTIG
        }
        return Medlemskapstyper.FRIVILLIG
    }
}
