package no.nav.melosys.domain.dokument.person

import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.dokument.felles.KodeverkHjelper


enum class Familierelasjon(private val kode: String) : KodeverkHjelper {
    EKTE("EKTE"),
    SAM("SAM"),
    FARA("FARA"),
    REPA("REPA"),
    BARN("BARN"),
    MORA("MORA");

    override fun getKode(): String {
        return kode
    }

    override fun hentKodeverkNavn(): FellesKodeverk {
        return FellesKodeverk.FAMILIERELASJONER
    }
}
