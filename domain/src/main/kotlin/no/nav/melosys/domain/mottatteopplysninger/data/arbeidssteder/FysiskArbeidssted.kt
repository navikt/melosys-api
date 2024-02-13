package no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder

import no.nav.melosys.domain.adresse.StrukturertAdresse


data class FysiskArbeidssted(
    var virksomhetNavn: String? = null,
    val adresse: StrukturertAdresse = StrukturertAdresse()
)


