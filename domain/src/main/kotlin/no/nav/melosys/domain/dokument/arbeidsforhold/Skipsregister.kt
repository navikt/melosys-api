package no.nav.melosys.domain.dokument.arbeidsforhold

import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.dokument.felles.AbstraktKodeverkHjelper


class Skipsregister : AbstraktKodeverkHjelper() {
    override fun hentKodeverkNavn(): FellesKodeverk {
        return FellesKodeverk.SKIPSREGISTRE
    }
}

