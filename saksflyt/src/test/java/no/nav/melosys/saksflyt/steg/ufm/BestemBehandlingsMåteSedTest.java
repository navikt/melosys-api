package no.nav.melosys.saksflyt.steg.ufm;

import java.util.Set;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BestemBehandlingsMåteSedTest {

    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private OppgaveService oppgaveService;

    private BestemBehandlingsMåteSed bestemBehandlingsMåteSed;

    private final Behandling behandling = new Behandling();
    private final Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
    private final Prosessinstans prosessinstans = new Prosessinstans();

    @Before
    public void setUp() throws IkkeFunnetException {
        bestemBehandlingsMåteSed = new BestemBehandlingsMåteSed(behandlingsresultatService, oppgaveService);
        prosessinstans.setBehandling(behandling);
        behandling.setId(234L);

        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId("123");
        behandling.setFagsak(new Fagsak());
        behandling.getFagsak().getAktører().add(bruker);

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);
    }

    @Test
    public void utfør_temaRegistreringUnntakIngenTreffIRegister_prosessOpprettes() throws Exception {
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        bestemBehandlingsMåteSed.utfør(prosessinstans);
        //TODO verify prosessinstansService......
    }

    @Test
    public void utfør_temaRegistreringUnntakMedTreffIRegister_oppgaveOpprettes() throws Exception {
        behandling.setTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING);
        Kontrollresultat kontrollresultat = new Kontrollresultat();
        kontrollresultat.setBegrunnelse(Kontroll_begrunnelser.FEIL_I_PERIODEN);
        behandlingsresultat.setKontrollresultater(Set.of(kontrollresultat));

        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        bestemBehandlingsMåteSed.utfør(prosessinstans);

        verify(oppgaveService).opprettEllerGjenbrukBehandlingsoppgave(eq(behandling), any(), any(), any());
    }
}