package no.nav.melosys.domain.dokument.person.adresse

import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import java.time.LocalDateTime


open class MidlertidigPostadresse(
    var endringstidspunkt: LocalDateTime? = null,
    var land: Land? = null,
    var postleveringsPeriode: Periode? = null
)
