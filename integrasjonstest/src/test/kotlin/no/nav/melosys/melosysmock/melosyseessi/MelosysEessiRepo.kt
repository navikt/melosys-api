package no.nav.melosys.melosysmock.melosyseessi

import no.nav.melosys.domain.eessi.BucInformasjon
import no.nav.melosys.domain.eessi.SedType

object MelosysEessiRepo {
    val repo = mutableListOf<BucInformasjon>()
    // Rinasaksnummer -> liste av sedtyper
    val sedRepo = mutableMapOf<String, List<SedType>>()


    fun clear() {
        repo.clear()
        sedRepo.clear()
    }

    fun opprettBucinformasjon(bucInformasjon: BucInformasjon) {
        repo.add(bucInformasjon)
    }
}
