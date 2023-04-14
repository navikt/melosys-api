package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

class OppgaveBeskrivelseNyUtleder : OppgaveBeskrivelseUtleder {
    private val oppgaveGoSysMapping = OppgaveGoSysMapping()

    override fun utledBeskrivelse(
        oppgaveBehandlingstema: OppgaveBehandlingstema,
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ): String {
        // TODO: vi må få inn sed navn fra behehandling
        return oppgaveGoSysMapping.finnOppgave(sakstype, sakstema, behandlingstema, behandlingstype).beskrivelsefelt.name
    }
}
