package no.nav.melosys.saksflyt.steg.oppgave;

import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_OPPGAVE;

@Component
public class OpprettOppgave implements StegBehandler {
    private final OppgaveService oppgaveService;

    @Autowired
    public OpprettOppgave(@Qualifier("system") OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_OPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
            prosessinstans.getBehandling(),
            prosessinstans.hentJournalpostID(),
            prosessinstans.getBehandling().getFagsak().hentAktørID(),
            prosessinstans.hentSaksbehandlerHvisTilordnes()
        );
    }
}
