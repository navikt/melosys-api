package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

interface OppgaveBehandlingstemaFactory {

    fun utledOppgaveBehandlingstema(
        sakstype: Sakstyper, sakstema: Sakstemaer, behandlingstema: Behandlingstema, behandlingstype: Behandlingstyper?
    ): OppgaveBehandlingstema
}
