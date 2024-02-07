package no.nav.melosys.service.medlemskapsperiode

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.service.lovligekombinasjoner.LovligeKombinasjonerMedlemskapsperiodeRegler

object UtledMedlemskapstype {

    fun av(bestemmelse: Folketrygdloven_kap2_bestemmelser): Medlemskapstyper {
        if (bestemmelse === Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_15_ANDRE_LEDD) {
            return Medlemskapstyper.FRIVILLIG
        }
        if (LovligeKombinasjonerMedlemskapsperiodeRegler.erPliktigBestemmelse(bestemmelse)) {
            return Medlemskapstyper.PLIKTIG
        }
        return Medlemskapstyper.FRIVILLIG
    }
}
