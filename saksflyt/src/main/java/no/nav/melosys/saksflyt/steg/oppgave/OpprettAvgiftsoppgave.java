package no.nav.melosys.saksflyt.steg.oppgave;

import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.stereotype.Component;

@Component
public class OpprettAvgiftsoppgave implements StegBehandler {
    private static final long FRIST_AVGIFTSVURDERING_MD = 1;
    static final String AVGIFTSVURDERING_BESKRIVELSE = "Vurderes for innregistrering i Avgiftssystemet";

    private final BehandlingsresultatService behandlingsresultatService;
    private final OppgaveService oppgaveService;

    public OpprettAvgiftsoppgave(BehandlingsresultatService behandlingsresultatService,
                                 OppgaveService oppgaveService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.oppgaveService = oppgaveService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.OPPRETT_AVGIFTSOPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) {
        final Behandling behandling = prosessinstans.getBehandling();
        final long behandlingID = behandling.getId();
        final var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        if (!behandlingsresultat.erAvslag() && !behandling.getFagsak().erSakstypeFtrl()) {
            Lovvalgsperiode lovvalgsperiode = behandlingsresultat.hentLovvalgsperiode();
            if (!lovvalgsperiode.erArtikkel11() && !lovvalgsperiode.erArtikkel13()) {
                oppgaveService.opprettOppgave(lagOppgaveTilTrygdeavgift(behandling));
            }
        }
    }

    private static Oppgave lagOppgaveTilTrygdeavgift(Behandling behandling) {
        Fagsak fagsak = behandling.getFagsak();
        return new Oppgave.Builder()
            .setTema(Tema.TRY).setOppgavetype(Oppgavetyper.VUR)
            .setJournalpostId(behandling.getInitierendeJournalpostId())
            .setBehandlesAvApplikasjon(Fagsystem.INTET)
            .setAktørId(fagsak.hentBrukersAktørID())
            .setBeskrivelse(AVGIFTSVURDERING_BESKRIVELSE)
            .setFristFerdigstillelse(LocalDate.now().plusMonths(FRIST_AVGIFTSVURDERING_MD))
            .setSaksnummer(fagsak.getSaksnummer())
            .build();
    }
}

