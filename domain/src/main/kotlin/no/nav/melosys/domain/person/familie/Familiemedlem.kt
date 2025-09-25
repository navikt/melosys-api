package no.nav.melosys.domain.person.familie

import no.nav.melosys.domain.person.Foedsel
import no.nav.melosys.domain.person.Folkeregisteridentifikator
import no.nav.melosys.domain.person.Navn
import no.nav.melosys.domain.person.Sivilstand

@JvmRecord
data class Familiemedlem(
    val folkeregisteridentifikator: Folkeregisteridentifikator?,
    val navn: Navn?,
    val familierelasjon: Familierelasjon?,
    val fødsel: Foedsel?,
    val folkeregisteridentAnnenForelder: Folkeregisteridentifikator?,
    val foreldreansvarstype: String?,
    val sivilstand: Sivilstand?
) {
    fun erBarn(): Boolean {
        return familierelasjon == Familierelasjon.BARN
    }

    fun erForelder(): Boolean {
        return familierelasjon == Familierelasjon.FAR || familierelasjon == Familierelasjon.MOR
    }

    fun erRelatertVedSivilstand(): Boolean {
        return familierelasjon == Familierelasjon.RELATERT_VED_SIVILSTAND
    }
}
