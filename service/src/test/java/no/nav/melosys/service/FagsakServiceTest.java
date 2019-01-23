package no.nav.melosys.service;

import java.time.Instant;
import java.util.Arrays;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.FagsakRepository;
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
    private TpsFasade tps;

    @Mock
    private ProsessinstansService prosessinstansService;

    @InjectMocks
    private FagsakService fagsakService;

    @Before
    public void setUp() {
        fagsakService = new FagsakService(fagsakRepo, behandlingService, behandlingsresultatRepository, tps, prosessinstansService);
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

    @Test
    public void henleggFagsakMedToBehandlingerHenterSisteBehandling() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "123456789";
        Behandling førsteBehandling = new Behandling();
        Behandling andreBehandling = new Behandling();
        long førsteBehandlingId = 999L;
        long andreBehandlingId = 234L;
        Behandlingsresultat behandlingsresultat = mock(Behandlingsresultat.class);

        initierFagsakMedToBehandlinger(fagsak, saksnummer, førsteBehandling, andreBehandling, førsteBehandlingId, andreBehandlingId, behandlingsresultat);

        fagsakService.henleggFagsak(saksnummer, "ANNET", "Fri tale");

        verify(prosessinstansService).opprettProsessinstansHenleggSak(andreBehandling);

        verify(behandlingsresultatRepository, never()).findOne(førsteBehandlingId);
        verify(prosessinstansService, never()).opprettProsessinstansHenleggSak(førsteBehandling);
    }

    @Test
    public void henleggFagsakMedToBehandlingerHenterFørsteBehandlingHvisSisteErAvsluttet() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "123456789";
        Behandling førsteBehandling = new Behandling();
        Behandling andreBehandling = new Behandling();
        andreBehandling.setStatus(Behandlingsstatus.AVSLUTTET);
        long førsteBehandlingId = 999L;
        long andreBehandlingId = 234L;
        Behandlingsresultat behandlingsresultat = mock(Behandlingsresultat.class);

        initierFagsakMedToBehandlinger(fagsak, saksnummer, førsteBehandling, andreBehandling, førsteBehandlingId, andreBehandlingId, behandlingsresultat);

        fagsakService.henleggFagsak(saksnummer, "ANNET", "Fri tale");

        verify(prosessinstansService).opprettProsessinstansHenleggSak(førsteBehandling);

        verify(behandlingsresultatRepository, never()).findOne(andreBehandlingId);
        verify(prosessinstansService, never()).opprettProsessinstansHenleggSak(andreBehandling);
    }

    @Test
    public void henleggFagsakMedGrunnAnnetLeggerTilFritekstPåBehandlingsresultat() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "123456789";
        Behandling førsteBehandling = new Behandling();
        Behandling andreBehandling = new Behandling();
        long førsteBehandlingId = 999L;
        long andreBehandlingId = 234L;
        Behandlingsresultat behandlingsresultat = mock(Behandlingsresultat.class);

        initierFagsakMedToBehandlinger(fagsak, saksnummer, førsteBehandling, andreBehandling, førsteBehandlingId, andreBehandlingId, behandlingsresultat);

        String fritekst = "Fri tale";
        fagsakService.henleggFagsak(saksnummer, "ANNET", fritekst);

        verify(behandlingsresultat).setHenleggelsesgrunn(Henleggelsesgrunner.ANNET);
        verify(behandlingsresultat).setHenleggelseFritekst(fritekst);
        verify(behandlingsresultatRepository).save(behandlingsresultat);
    }

    @Test
    public void henleggFagsakMedGrunnAvlystLeggerIkkeTilFritekstPåBehandlingsresultat() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "123456789";
        Behandling førsteBehandling = new Behandling();
        Behandling andreBehandling = new Behandling();
        long førsteBehandlingId = 999L;
        long andreBehandlingId = 234L;
        Behandlingsresultat behandlingsresultat = mock(Behandlingsresultat.class);

        initierFagsakMedToBehandlinger(fagsak, saksnummer, førsteBehandling, andreBehandling, førsteBehandlingId, andreBehandlingId, behandlingsresultat);

        fagsakService.henleggFagsak(saksnummer, "OPPHOLD_UTL_AVLYST", "Fri tale");

        verify(behandlingsresultat).setHenleggelsesgrunn(Henleggelsesgrunner.OPPHOLD_UTL_AVLYST);
        verify(behandlingsresultat, never()).setHenleggelseFritekst(anyString());
        verify(behandlingsresultatRepository).save(behandlingsresultat);
    }

    @Test
    public void henleggFagsakMedGrunnTrukketLeggerIkkeTilFritekstPåBehandlingsresultat() throws TekniskException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "123456789";
        Behandling førsteBehandling = new Behandling();
        Behandling andreBehandling = new Behandling();
        long førsteBehandlingId = 999L;
        long andreBehandlingId = 234L;
        Behandlingsresultat behandlingsresultat = mock(Behandlingsresultat.class);

        initierFagsakMedToBehandlinger(fagsak, saksnummer, førsteBehandling, andreBehandling, førsteBehandlingId, andreBehandlingId, behandlingsresultat);

        fagsakService.henleggFagsak(saksnummer, "SØKNADEN_TRUKKET", "Fri tale");

        verify(behandlingsresultat).setHenleggelsesgrunn(Henleggelsesgrunner.SØKNADEN_TRUKKET);
        verify(behandlingsresultat, never()).setHenleggelseFritekst(anyString());
        verify(behandlingsresultatRepository).save(behandlingsresultat);
    }

    private void initierFagsakMedToBehandlinger(Fagsak fagsak, String saksnummer, Behandling førsteBehandling, Behandling andreBehandling, long førsteBehandlingId, long andreBehandlingId, Behandlingsresultat behandlingsresultat) {
        førsteBehandling.setRegistrertDato(Instant.parse("2000-10-10T10:12:35Z"));
        førsteBehandling.setId(førsteBehandlingId);
        Instant registrertDatoForSisteBehandling = Instant.parse("2010-11-11T10:12:35Z");
        andreBehandling.setId(andreBehandlingId);
        andreBehandling.setRegistrertDato(registrertDatoForSisteBehandling);
        fagsak.setBehandlinger(Arrays.asList(førsteBehandling, andreBehandling));

        doReturn(behandlingsresultat).when(behandlingsresultatRepository).findOne(førsteBehandlingId);
        doReturn(behandlingsresultat).when(behandlingsresultatRepository).findOne(andreBehandlingId);
        doReturn(fagsak).when(fagsakRepo).findBySaksnummer(saksnummer);
    }
}