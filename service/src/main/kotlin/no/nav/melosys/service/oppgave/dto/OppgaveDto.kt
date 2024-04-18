package no.nav.melosys.service.oppgave.dto

import no.nav.melosys.domain.oppgave.PrioritetType
import java.time.LocalDate

open class OppgaveDto {
    //Getter brukes av Jackson for å serialisere oppgave objekter til frontend i OppgaveTjeneste
    var aktivTil: LocalDate? = null
    var ansvarligID: String? = null
    @JvmField
    var oppgaveID: String? = null
    var prioritet: PrioritetType? = null
    @JvmField
    var navn: String? = null
    @JvmField
    var hovedpartIdent: String? = null
    var versjon = 0
}
