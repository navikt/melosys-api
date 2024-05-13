package no.nav.melosys.service.oppgave.dto

import no.nav.melosys.domain.oppgave.PrioritetType
import java.time.LocalDate

data class JournalfoeringsoppgaveDto(
    override val aktivTil: LocalDate?,
    override val ansvarligID: String?,
    override val oppgaveID: String,
    override val prioritet: PrioritetType,
    override val navn: String,
    override val hovedpartIdent: String,
    override val versjon: Int,
    val journalpostID: String,
) : OppgaveDto
