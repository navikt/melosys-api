package no.nav.melosys.saksflyt.steg.oppgave;

import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.saksflytapi.domain.ProsessSteg;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.stereotype.Component;

import static no.nav.melosys.saksflytapi.domain.ProsessSteg.OPPRETT_OPPGAVE;

@Component
public class OpprettOppgave implements StegBehandler {
    private final OppgaveService oppgaveService;

    public OpprettOppgave(OppgaveService oppgaveService) {
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
            prosessinstans.getBehandling().getFagsak().finnBrukersAktørID(),
            prosessinstans.hentSaksbehandlerHvisTilordnes(),
            prosessinstans.getBehandling().getFagsak().finnVirksomhetsOrgnr()
        );
    }
}
