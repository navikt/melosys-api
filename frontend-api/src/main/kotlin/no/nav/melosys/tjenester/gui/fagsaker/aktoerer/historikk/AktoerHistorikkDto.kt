package no.nav.melosys.tjenester.gui.fagsaker.aktoerer.historikk

import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import java.time.LocalDate

data class AktoerHistorikkDto(
    val registrertFra: LocalDate,
    val registretTil: LocalDate? = null,
    val aktoerID: String? = null,
    val personIdent: String? = null,
    val institusjonsID: String? = null,
    val orgnr: String? = null,
    val rolle: Aktoersroller,
    val fullmakter: Set<Fullmaktstype> = emptySet(),
)
