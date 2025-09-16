package no.nav.melosys.domain.person.adresse

import no.nav.melosys.domain.person.adresse.AdressebeskyttelseGradering.STRENGT_FORTROLIG
import no.nav.melosys.domain.person.adresse.AdressebeskyttelseGradering.STRENGT_FORTROLIG_UTLAND

@JvmRecord
data class Adressebeskyttelse(
    val gradering: AdressebeskyttelseGradering,
    val master: String
) {
    fun erStrengtFortrolig(): Boolean =
        gradering == STRENGT_FORTROLIG || gradering == STRENGT_FORTROLIG_UTLAND
}