package no.nav.melosys.domain.dokument.person.adresse

import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import java.time.LocalDateTime

data class MidlertidigPostadresseNorge(
    val endringstidspunkt: LocalDateTime,
    val land: Land,
    val postleveringsPeriode: Periode,
    val tilleggsadresse: String,
    val tilleggsadresseType: String,
    val poststed: String,
    val bolignummer: String,
    val kommunenummer: String,
    val gateadresse: Gateadresse
)
