package no.nav.melosys.melosysmock.melosyseessi

import no.nav.melosys.domain.eessi.BucInformasjon

object MelosysEessiRepo {
    val repo = mutableListOf<BucInformasjon>()


    fun clear() {
        repo.clear()
    }

    fun opprettBucinformasjon(bucInformasjon: BucInformasjon) {
        repo.add(bucInformasjon)
    }
}
