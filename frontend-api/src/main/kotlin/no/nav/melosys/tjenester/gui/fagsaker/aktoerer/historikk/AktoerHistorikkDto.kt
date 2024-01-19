package no.nav.melosys.tjenester.gui.fagsaker.aktoerer.historikk

import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.Fullmaktstype
import java.time.LocalDateTime

data class AktoerHistorikkDto(
    val registrertFra: LocalDateTime,
    val registretTil: LocalDateTime? = null,
    val aktoerID: String? = null,
    val personIdent: String? = null,
    val institusjonsID: String? = null,
    val orgnr: String? = null,
    val rolle: Aktoersroller,
    val fullmakter: Set<Fullmaktstype> = emptySet(),
)
