package no.nav.melosys.domain.dokument.person.adresse

import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import java.time.LocalDateTime

class MidlertidigPostadresseUtland(
    var adresselinje1: String? = null,
    var adresselinje2: String? = null,
    var adresselinje3: String? = null,
    var adresselinje4: String? = null,
    endringstidspunkt: LocalDateTime? = null,
    land: Land? = null,
    postleveringsPeriode: Periode? = null
) : MidlertidigPostadresse(endringstidspunkt, land, postleveringsPeriode)


