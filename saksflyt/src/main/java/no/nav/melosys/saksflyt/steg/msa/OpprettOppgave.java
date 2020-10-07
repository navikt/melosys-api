package no.nav.melosys.saksflyt.steg.msa;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component("MottakSoknadAltinnOpprettOppgave")
public class OpprettOppgave implements StegBehandler {
    private final OppgaveService oppgaveService;

    public OpprettOppgave(@Qualifier("system") OppgaveService oppgaveService) {
        this.oppgaveService = oppgaveService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.MSA_OPPRETT_OPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws MelosysException {

        final Behandling behandling = prosessinstans.getBehandling();

        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
            behandling,
            behandling.getInitierendeJournalpostId(),
            behandling.getFagsak().hentBruker().getAktørId(),
            null
        );

        prosessinstans.setSteg(ProsessSteg.MSA_SEND_FORVALTNINGSMELDING);
    }
}
