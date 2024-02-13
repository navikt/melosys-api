package no.nav.melosys.domain.dokument.person

import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.dokument.felles.AbstraktKodeverkHjelper

class Diskresjonskode() : AbstraktKodeverkHjelper() {

    override fun hentKodeverkNavn(): FellesKodeverk {
        return FellesKodeverk.DISKRESJONSKODER
    }

    fun erKode6(): Boolean {
        return "SPSF" == kode
    }

    fun erKode7(): Boolean {
        return "SPFO" == kode
    }

    override fun toString(): String {
        return kode
    }
}
