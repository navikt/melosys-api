package no.nav.melosys.domain.dokument.person.adresse

import no.nav.melosys.domain.dokument.felles.Land
import no.nav.melosys.domain.dokument.felles.Periode
import java.time.LocalDateTime

data class MidlertidigPostadresseUtland(
    val endringstidspunkt: LocalDateTime,
    val land: Land,
    val postleveringsPeriode: Periode,
    val adresselinje1: String,
    val adresselinje2: String? = null,
    val adresselinje3: String? = null,
    val adresselinje4: String? = null
)


