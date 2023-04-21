package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

class OppgavetypeNyUtleder : OppgavetypeUtleder {
    private val oppgaveGosysMapping = OppgaveGosysMapping()
    override fun utledOppgavetype(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ): Oppgavetyper = oppgaveGosysMapping.finnOppgave(sakstype, sakstema, behandlingstema, behandlingstype).oppgaveType
}
