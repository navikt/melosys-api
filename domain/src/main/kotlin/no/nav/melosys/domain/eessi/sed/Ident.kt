package no.nav.melosys.domain.eessi.sed

import no.nav.melosys.domain.mottatteopplysninger.data.UtenlandskIdent

data class Ident(
    var ident: String? = null,
    var landkode: String? = null
) {
    fun tilUtenlandskIdent() = UtenlandskIdent(
        ident = ident,
        landkode = landkode
    )
}
