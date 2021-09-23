package no.nav.melosys.saksflyt.steg.oppgave;

import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpprettAvgiftsoppgaveTest {
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private OppgaveService oppgaveService;
    @Captor
    private ArgumentCaptor<Oppgave> oppgave;

    private OpprettAvgiftsoppgave opprettAvgiftsoppgave;

    private static final String DUMM_ID = "DUMM_ID";
    private Behandlingsresultat behandlingsresultat;

    @BeforeEach
    public void setUp() {
        opprettAvgiftsoppgave = new OpprettAvgiftsoppgave(behandlingsresultatService, oppgaveService);
        behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setType(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
    }

    @Test
    void utfør_oppretterRiktigOppgave() {
        behandlingsresultat.getLovvalgsperioder().add(lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2));
        opprettAvgiftsoppgave.utfør(lagProsessinstans());

        verify(oppgaveService).opprettOppgave(oppgave.capture());
        assertThat(oppgave.getValue().getTema()).isEqualTo(Tema.TRY);
        assertThat(oppgave.getValue().getOppgavetype()).isEqualTo(Oppgavetyper.VUR);
        assertThat(oppgave.getValue().getJournalpostId()).isNotBlank();
        assertThat(oppgave.getValue().getBehandlesAvApplikasjon()).isEqualTo(Fagsystem.INTET);
        assertThat(oppgave.getValue().getAktørId()).isEqualTo(DUMM_ID);
        assertThat(oppgave.getValue().getBeskrivelse()).isEqualTo(OpprettAvgiftsoppgave.AVGIFTSVURDERING_BESKRIVELSE);
    }

    @Test
    void utfør_avslag_oppretterIkkeOppgave() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
        opprettAvgiftsoppgave.utfør(lagProsessinstans());

        verify(oppgaveService, never()).opprettOppgave(any());
    }

    @Test
    void utfør_saktypeErftrl_oppretterIkkeOppgave() {
        Lovvalgsperiode lovvalgsperiode = lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        behandlingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        Prosessinstans prosessinstans = new Prosessinstans();
        Fagsak fagsak = lagFagsak();
        fagsak.setType(Sakstyper.FTRL);
        prosessinstans.setBehandling(lagBehandling(fagsak));

        opprettAvgiftsoppgave.utfør(prosessinstans);

        verify(oppgaveService, never()).opprettOppgave(any());
    }

    @Test
    void utfør_art13_oppretterIkkeOppgave() {
        behandlingsresultat.getLovvalgsperioder().add(lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A));
        opprettAvgiftsoppgave.utfør(lagProsessinstans());

        verify(oppgaveService, never()).opprettOppgave(any());
    }

    private static Prosessinstans lagProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(lagBehandling(lagFagsak()));
        return prosessinstans;
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "MEL-TESTx";
        fagsak.setSaksnummer(saksnummer);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.getAktører().add(lagBruker());
        return fagsak;
    }

    private static Aktoer lagBruker() {
        Aktoer aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId(DUMM_ID);
        return aktoer;
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setFagsak(fagsak);
        behandling.setInitierendeJournalpostId("JOURNALPOSTID");
        return behandling;
    }

    private static Lovvalgsperiode lagLovvalgsperiode(LovvalgBestemmelse bestemmelse) {
        Lovvalgsperiode lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(bestemmelse);
        lovvalgsperiode.setLovvalgsland(Landkoder.NO);
        lovvalgsperiode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        return lovvalgsperiode;
    }
}
