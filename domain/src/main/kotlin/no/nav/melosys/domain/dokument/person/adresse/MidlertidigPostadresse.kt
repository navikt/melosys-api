package no.nav.melosys.domain.dokument.person.adresse

import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import java.time.LocalDateTime


data class MidlertidigPostadresse(
    val endringstidspunkt: LocalDateTime? = null,
    val land: Land? = null,
    val postleveringsPeriode: Periode? = null
)
