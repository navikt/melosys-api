package no.nav.melosys.domain.dokument.person.adresse

import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import java.time.LocalDateTime

class MidlertidigPostadresseNorge(
    var tilleggsadresse: String? = null,
    var tilleggsadresseType: String? = null,
    var poststed: String? = null,
    var bolignummer: String? = null,
    var kommunenummer: String? = null,
    var gateadresse: Gateadresse? = null,
    endringstidspunkt: LocalDateTime? = null,
    land: Land? = null,
    postleveringsPeriode: Periode? = null
) : MidlertidigPostadresse(endringstidspunkt, land, postleveringsPeriode)
