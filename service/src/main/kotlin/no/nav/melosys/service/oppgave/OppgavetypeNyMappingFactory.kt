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
        if (behandlingstype == Behandlingstyper.HENVENDELSE) {
            if (behandlingstema == Behandlingstema.TRYGDETID) {
                return Oppgavetyper.BEH_SED
            }
            return Oppgavetyper.VURD_HENV
        }
        // TODO: flytt ut tableRows fra OppgaveBehandlingstemaNyMappingFactory og legg til dette og slå opp der
        return Oppgavetyper.BEH_SAK_MK
    }
}
