package no.nav.melosys.saksflyt.steg.sed.ny_sak;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("opprettoppgaveSedNySak")
public class OpprettOppgave extends AbstraktStegBehandler {

    private final OppgaveService oppgaveService;

    public OpprettOppgave(@Qualifier("system") OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @Override
    protected ProsessSteg inngangsSteg() {
        return ProsessSteg.SED_GENERELL_SAK_OPPRETT_OPPGAVE;
    }

    @Override
    protected void utfør(Prosessinstans prosessinstans) throws MelosysException {
        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
            prosessinstans.getBehandling(),
            prosessinstans.hentJournalpostID(),
            prosessinstans.getData(ProsessDataKey.AKTØR_ID),
            prosessinstans.hentSaksbehandlerHvisTilordnes()
        );

        prosessinstans.setSteg(ProsessSteg.FERDIG);
    }
}
