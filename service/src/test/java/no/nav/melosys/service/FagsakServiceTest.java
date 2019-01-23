package no.nav.melosys.service;

import java.time.Instant;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FagsakServiceTest {

    @Mock
    private FagsakRepository fagsakRepo;

    @Mock
    private BehandlingService behandlingService;

    @Mock
    private BehandlingsresultatRepository behandlingsresultatRepository;

    @Mock
    private ProsessinstansRepository prosessinstansRepo;

    @Mock
    private TpsFasade tps;

    @Mock
    private Binge binge;

    @InjectMocks
    private FagsakService fagsakService;

    @Before
    public void setUp() {
        fagsakService = new FagsakService(fagsakRepo, behandlingService, behandlingsresultatRepository, tps, prosessinstansRepo, binge);
    }

    @Test
    public void hentFagsak() {
        String saksnummer = "saksnummer";
        fagsakService.hentFagsak(saksnummer);
        verify(fagsakRepo).findBySaksnummer(eq(saksnummer));
    }

    @Test
    public void hentFagsakerMedAktør() throws IkkeFunnetException {
        when(tps.hentAktørIdForIdent(any())).thenReturn("AKTOER_ID");
        fagsakService.hentFagsakerMedAktør(RolleType.BRUKER, "FNR");
        verify(fagsakRepo).findByRolleAndAktør(eq(RolleType.BRUKER), eq("AKTOER_ID"));
    }

    @Test
    public void lagre() {
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        fagsak.setStatus(Fagsaksstatus.OPPRETTET);
        fagsak.setType(Fagsakstype.EU_EØS);
        fagsak.setRegistrertDato(Instant.now());
        fagsakService.lagre(fagsak);
        verify(fagsakRepo).save(fagsak);
        assertThat(fagsak).isNotNull();
        assertThat(fagsak.getSaksnummer()).isNotEmpty();
    }

    @Test
    public void nyFagsakOgBehandling() {
        Behandling behandling = mock(Behandling.class);
        doReturn(behandling).when(behandlingService).nyBehandling(any(), any(), any());

        Fagsak fagsak = fagsakService.nyFagsakOgBehandling("AKTOER_ID", "123456789", "", Behandlingstype.SØKNAD);
        verify(fagsakRepo).save(any(Fagsak.class));
        verify(behandlingService).nyBehandling(any(), eq(Behandlingsstatus.OPPRETTET), eq(Behandlingstype.SØKNAD));
        assertThat(fagsak.getBehandlinger()).isNotEmpty();
    }
}