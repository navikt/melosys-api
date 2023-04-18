package no.nav.melosys.saksflyt.steg.oppgave;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.stereotype.Component;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.OPPRETT_OPPGAVE;

@Component
public class OpprettOppgave implements StegBehandler {
    private final OppgaveService oppgaveService;
    private final BehandlingRepository behandlingRepository;

    public OpprettOppgave(OppgaveService oppgaveService, BehandlingRepository behandlingRepository) {
        this.oppgaveService = oppgaveService;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return OPPRETT_OPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        Long behandlingID = prosessinstans.getBehandling().getId();
        Behandling behandlingWithSaksopplysninger = behandlingRepository.findWithSaksopplysningerById(behandlingID);
        oppgaveService.opprettEllerGjenbrukBehandlingsoppgave(
            behandlingWithSaksopplysninger,
            prosessinstans.hentJournalpostID(),
            prosessinstans.getBehandling().getFagsak().finnBrukersAktørID().orElse(null),
            prosessinstans.hentSaksbehandlerHvisTilordnes(),
            prosessinstans.getBehandling().getFagsak().finnVirksomhetsOrgnr().orElse(null)
        );
    }
}
