package no.nav.melosys.service.oppgave.dto

import no.nav.melosys.domain.oppgave.PrioritetType
import java.time.LocalDate

interface OppgaveDto {
    val aktivTil: LocalDate?
    val ansvarligID: String?
    val oppgaveID: String
    val prioritet: PrioritetType
    val navn: String
    val hovedpartIdent: String
    val versjon: Int
}
