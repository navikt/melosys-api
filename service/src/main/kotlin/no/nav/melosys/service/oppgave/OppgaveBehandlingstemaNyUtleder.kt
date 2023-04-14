package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

class OppgaveBehandlingstemaNyUtleder : OppgaveBehandlingstemaUtleder {

    private val oppgaveGoSysMapping = OppgaveGoSysMapping()

    override fun utledOppgaveBehandlingstema(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper?
    ): OppgaveBehandlingstema =
        oppgaveGoSysMapping.finnOppgave(sakstype, sakstema, behandlingstema, behandlingstype).oppgaveBehandlingstema

}
