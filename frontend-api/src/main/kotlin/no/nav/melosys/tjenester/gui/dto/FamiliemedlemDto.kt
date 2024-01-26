package no.nav.melosys.tjenester.gui.dto

import no.nav.melosys.domain.dokument.person.Familiemedlem
import no.nav.melosys.domain.dokument.person.Familierelasjon
import no.nav.melosys.domain.dokument.person.Sivilstand
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class FamiliemedlemDto(familiemedlem: Familiemedlem) {
    var fnr: String = familiemedlem.fnr
    var sammensattNavn: String = familiemedlem.navn
    var relasjonstype: Familierelasjon = familiemedlem.familierelasjon
    var alder: Long? = if (familiemedlem.fødselsdato == null) null else ChronoUnit.YEARS.between(familiemedlem.fødselsdato, LocalDate.now())
    var borMedBruker: Boolean = familiemedlem.borMedBruker
    var sivilstand: Sivilstand = familiemedlem.sivilstand
    var sivilstandGyldighetsperiodeFom: LocalDate = familiemedlem.sivilstandGyldighetsperiodeFom
    var fnrAnnenForelder: String = familiemedlem.fnrAnnenForelder

    companion object {
        @JvmStatic
        fun avFamiliemedlemmer(familiemedlemmer: List<Familiemedlem>): List<FamiliemedlemDto> {
            return familiemedlemmer.stream()
                .map { familiemedlem: Familiemedlem -> FamiliemedlemDto(familiemedlem) }
                .toList()
        }
    }
}
