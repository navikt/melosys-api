package no.nav.melosys.tjenester.gui.dto.oppgave

import no.nav.melosys.service.oppgave.dto.BehandlingsoppgaveDto
import no.nav.melosys.service.oppgave.dto.JournalfoeringsoppgaveDto

class OppgaveOversiktDto {
    var journalforing: List<JournalfoeringsoppgaveDto>? = null
    var saksbehandling: List<BehandlingsoppgaveDto>? = null
}
