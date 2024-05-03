package no.nav.melosys.tjenester.gui.dto.oppgave

import no.nav.melosys.service.oppgave.dto.BehandlingsoppgaveDto
import no.nav.melosys.service.oppgave.dto.JournalfoeringsoppgaveDto

data class OppgaveOversiktDto(
    val journalforing: List<JournalfoeringsoppgaveDto>,
    val saksbehandling: List<BehandlingsoppgaveDto>,
)
