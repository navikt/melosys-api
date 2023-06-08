package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

class OppgaveBehandlingstemaUtleder internal constructor(
    private val oppgaveGosysMapping:OppgaveGosysMapping  = OppgaveGosysMapping()
) {

    fun utledOppgaveBehandlingstema(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper?
    ): OppgaveBehandlingstema? =
        oppgaveGosysMapping.finnOppgave(sakstype, sakstema, behandlingstema, behandlingstype).oppgaveBehandlingstema

}
