package no.nav.melosys.domain.dokument.arbeidsforhold

import no.nav.melosys.domain.FellesKodeverk
import no.nav.melosys.domain.dokument.felles.AbstraktKodeverkHjelper



class Arbeidstidsordning : AbstraktKodeverkHjelper() {
    override fun hentKodeverkNavn(): FellesKodeverk = FellesKodeverk.ARBEIDSTIDSORDNINGER
}
