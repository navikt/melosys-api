package no.nav.melosys.service.oppgave

import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.Oppgavetyper
import no.nav.melosys.domain.kodeverk.Sakstemaer
import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper

class OppgavetypeGammelUtleder : OppgavetypeUtleder {
    override fun utledOppgavetype(
        sakstype: Sakstyper,
        sakstema: Sakstemaer,
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ): Oppgavetyper {
        if (sakstype == Sakstyper.EU_EOS) {
            return oppgavetypeEøs(behandlingstema, behandlingstype)
        }
        if (sakstype == Sakstyper.TRYGDEAVTALE) {
            return oppgavetypeTrygdeavtale(behandlingstema, behandlingstype)
        }
        return if (behandlingstype == Behandlingstyper.HENVENDELSE) {
            Oppgavetyper.VURD_HENV
        } else Oppgavetyper.BEH_SAK_MK
    }

    private fun oppgavetypeEøs(tema: Behandlingstema, behandlingstype: Behandlingstyper): Oppgavetyper {
        if (tema == Behandlingstema.BESLUTNING_LOVVALG_NORGE && behandlingstype == Behandlingstyper.HENVENDELSE) {
            return Oppgavetyper.VURD_HENV
        }
        if (Behandling.erAnmodningOmUnntak(tema) || Behandling.erRegistreringAvUnntak(tema) ||
            listOf(
                Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET,
                Behandlingstema.TRYGDETID,
                Behandlingstema.BESLUTNING_LOVVALG_NORGE
            ).contains(tema)
        ) {
            return Oppgavetyper.BEH_SED
        }
        return if (behandlingstype == Behandlingstyper.HENVENDELSE) {
            Oppgavetyper.VURD_HENV
        } else Oppgavetyper.BEH_SAK_MK
    }

    private fun oppgavetypeTrygdeavtale(
        behandlingstema: Behandlingstema,
        behandlingstype: Behandlingstyper
    ): Oppgavetyper {
        if (behandlingstema == Behandlingstema.FORESPØRSEL_TRYGDEMYNDIGHET) {
            return Oppgavetyper.BEH_SAK_MK
        }
        return if (behandlingstype == Behandlingstyper.HENVENDELSE) {
            Oppgavetyper.VURD_HENV
        } else Oppgavetyper.BEH_SAK_MK
    }

}
