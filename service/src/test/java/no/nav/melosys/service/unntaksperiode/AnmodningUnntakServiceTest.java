package no.nav.melosys.service.unntaksperiode;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningUnntakServiceTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private ProsessinstansService prosessinstansService;

    private AnmodningUnntakService anmodningUnntakService;

    @Before
    public void setUp() {
        anmodningUnntakService = new AnmodningUnntakService(behandlingService, oppgaveService, prosessinstansService);
    }

    @Test
    public void anmodningOmUnntak_fungerer() throws FunksjonellException, TekniskException {
        long behandlingID = 1L;
        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        behandling.setFagsak(fagsak);
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);

        anmodningUnntakService.anmodningOmUnntak(behandlingID);

        verify(behandlingService).oppdaterStatus(eq(behandlingID), eq(Behandlingsstatus.ANMODNING_UNNTAK_SENDT));
        verify(prosessinstansService).opprettProsessinstansAnmodningOmUnntak(any(Behandling.class));
        verify(oppgaveService).leggTilbakeOppgaveMedSaksnummer(any());
    }
}