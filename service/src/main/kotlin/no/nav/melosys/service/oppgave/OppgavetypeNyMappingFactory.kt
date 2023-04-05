package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

class OppgavetypeNyMappingFactory : OppgavetypeFactory {
    override fun utledOppgavetype(
        sakstype: Sakstyper,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ): Oppgavetyper {
        TODO("Not yet implemented")
    }
}
