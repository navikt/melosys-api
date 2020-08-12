package no.nav.melosys.saksflyt.steg.iv;

import java.time.LocalDate;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class OpprettAvgiftsoppgave implements StegBehandler {
    private static final long FRIST_AVGIFTSVURDERING_MD = 1;
    static final String AVGIFTSVURDERING_BESKRIVELSE = "Vurderes for innregistrering i Avgiftssystemet";

    private final BehandlingsresultatService behandlingsresultatService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final OppgaveService oppgaveService;

    @Autowired
    public OpprettAvgiftsoppgave(BehandlingsresultatService behandlingsresultatService,
                                 LovvalgsperiodeService lovvalgsperiodeService,
                                 @Qualifier("system") OppgaveService oppgaveService) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.oppgaveService = oppgaveService;
    }

    @Override
    public ProsessSteg inngangsSteg() {
        return ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE;
    }

    @Override
    public void utfør(Prosessinstans prosessinstans) throws FunksjonellException, TekniskException {
        final Behandling behandling = prosessinstans.getBehandling();
        final long behandlingID = behandling.getId();
        final var behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);

        if (!behandlingsresultat.erAvslag()) {
            Lovvalgsperiode lovvalgsperiode = lovvalgsperiodeService.hentValidertLovvalgsperiode(behandlingID);
            if (!lovvalgsperiode.erArtikkel11() && !lovvalgsperiode.erArtikkel13()) {
                oppgaveService.opprettOppgave(lagOppgaveTilTrygdeavgift(behandling));
            }
        }

        prosessinstans.setSteg(ProsessSteg.IV_AVSLUTT_BEHANDLING);
    }

    private static Oppgave lagOppgaveTilTrygdeavgift(Behandling behandling) throws TekniskException {
        Fagsak fagsak = behandling.getFagsak();
        return new Oppgave.Builder()
            .setTema(Tema.TRY).setOppgavetype(Oppgavetyper.VUR)
            .setJournalpostId(behandling.getInitierendeJournalpostId())
            .setBehandlesAvApplikasjon(Fagsystem.INTET)
            .setAktørId(fagsak.hentBruker().getAktørId())
            .setBeskrivelse(AVGIFTSVURDERING_BESKRIVELSE)
            .setFristFerdigstillelse(LocalDate.now().plusMonths(FRIST_AVGIFTSVURDERING_MD))
            .setSaksnummer(fagsak.getSaksnummer())
            .build();
    }
}

