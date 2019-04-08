package no.nav.melosys.service.sak;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FagsakServiceTest {
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private FagsakRepository fagsakRepo;

    @Mock
    private BehandlingService behandlingService;

    @Mock
    private KontaktopplysningService kontaktopplysningService;

    @Mock
    private OppgaveService oppgaveService;

    @Mock
    private TpsFasade tps;

    @Mock
    private ProsessinstansService prosessinstansService;

    @InjectMocks
    private FagsakService fagsakService;

    private static final String SAKSBEHANDLER = "Z990007";


    @Before
    public void setUp() {
        fagsakService = new FagsakService(fagsakRepo, behandlingService, kontaktopplysningService, oppgaveService, tps, prosessinstansService);
    }

    @Test
    public void hentFagsak() throws IkkeFunnetException {
        String saksnummer = "saksnummer";
        when(fagsakRepo.findBySaksnummer(anyString())).thenReturn(new Fagsak());
        fagsakService.hentFagsak(saksnummer);
        verify(fagsakRepo).findBySaksnummer(eq(saksnummer));
    }

    @Test
    public void hentFagsakerMedAktør() throws IkkeFunnetException {
        when(tps.hentAktørIdForIdent(any())).thenReturn("AKTOER_ID");
        fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, "FNR");
        verify(fagsakRepo).findByRolleAndAktør(eq(Aktoersroller.BRUKER), eq("AKTOER_ID"));
    }

    @Test
    public void lagre() {
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        fagsak.setStatus(Saksstatuser.OPPRETTET);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setRegistrertDato(Instant.now());
        fagsakService.lagre(fagsak);
        verify(fagsakRepo).save(fagsak);
        assertThat(fagsak).isNotNull();
        assertThat(fagsak.getSaksnummer()).isNotEmpty();
    }

    @Test
    public void nyFagsakOgBehandling() throws FunksjonellException {
        Behandling behandling = mock(Behandling.class);
        String initierendeJournalpostId = "234";
        String initierendeDokumentId = "221234";
        doReturn(behandling).when(behandlingService).nyBehandling(any(), any(), any(), anyString(), anyString());

        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder().medAktørID("AKTOER_ID").medAktørID("123456789")
            .medBehandlingstype(Behandlingstyper.SOEKNAD).medInitierendeJournalpostId(initierendeJournalpostId).medInitierendeDokumentId(initierendeDokumentId).build();
        Fagsak fagsak = fagsakService.nyFagsakOgBehandling(opprettSakRequest);
        verify(fagsakRepo).save(any(Fagsak.class));
        verify(behandlingService).nyBehandling(any(), eq(Behandlingsstatus.OPPRETTET), eq(Behandlingstyper.SOEKNAD), eq(initierendeJournalpostId), eq(initierendeDokumentId));
        assertThat(fagsak.getBehandlinger()).isNotEmpty();
    }

    @Test
    public void nyFagsakOgBehandling_kontaktPersonFinnes_KontaktOpplysningOpprettes() throws FunksjonellException {
        OpprettSakRequest opprettSakRequest = new OpprettSakRequest.Builder().medAktørID("AKTOER_ID").medAktørID("123456789")
            .medBehandlingstype(Behandlingstyper.SOEKNAD).medRepresentant("RepresentantOrgnr").medRepresentantKontaktperson("Kontaktperson").build();

        fagsakService.nyFagsakOgBehandling(opprettSakRequest);

        verify(kontaktopplysningService).lagEllerOppdaterKontaktopplysning(any(), eq("RepresentantOrgnr"), eq(null), eq("Kontaktperson"));
    }

    @Test
    public void henleggFagsakMedToBehandlingerHenterSisteBehandling() throws TekniskException, FunksjonellException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "123456789";
        Behandling førsteBehandling = new Behandling();
        Behandling andreBehandling = new Behandling();
        Fagsak andreBehandlingFagsak = new Fagsak();
        andreBehandlingFagsak.setSaksnummer("987654321");
        andreBehandling.setFagsak(andreBehandlingFagsak);
        long førsteBehandlingId = 999L;
        long andreBehandlingId = 234L;

        initierFagsakMedToBehandlinger(fagsak, saksnummer, førsteBehandling, andreBehandling, førsteBehandlingId, andreBehandlingId);

        String fritekst = "Fri tale";
        fagsakService.henleggFagsak(saksnummer, "ANNET", fritekst);

        verify(prosessinstansService).opprettProsessinstansHenleggSak(andreBehandling, Henleggelsesgrunner.ANNET, fritekst);

        verify(prosessinstansService, never()).opprettProsessinstansHenleggSak(eq(førsteBehandling), any(), anyString());

        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(eq(andreBehandlingFagsak.getSaksnummer()));
    }

    @Test
    public void henleggFagsakMedToBehandlingerHenterFørsteBehandlingHvisSisteErAvsluttet() throws TekniskException, FunksjonellException {
        Fagsak fagsak = new Fagsak();
        String saksnummer = "123456789";
        Behandling førsteBehandling = new Behandling();
        Fagsak førsteBehandlingFagsak = new Fagsak();
        førsteBehandlingFagsak.setSaksnummer("987654321");
        førsteBehandling.setFagsak(førsteBehandlingFagsak);
        Behandling andreBehandling = new Behandling();
        andreBehandling.setStatus(Behandlingsstatus.AVSLUTTET);
        long førsteBehandlingId = 999L;
        long andreBehandlingId = 234L;
        Behandlingsresultat behandlingsresultat = mock(Behandlingsresultat.class);

        initierFagsakMedToBehandlinger(fagsak, saksnummer, førsteBehandling, andreBehandling, førsteBehandlingId, andreBehandlingId);

        String fritekst = "Fri tale";
        fagsakService.henleggFagsak(saksnummer, "ANNET", fritekst);

        verify(prosessinstansService).opprettProsessinstansHenleggSak(førsteBehandling, Henleggelsesgrunner.ANNET, fritekst);

        verify(prosessinstansService, never()).opprettProsessinstansHenleggSak(eq(andreBehandling), any(), anyString());

        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(eq(førsteBehandlingFagsak.getSaksnummer()));
    }

    @Test
    public void henleggFagsakMedToBehandlingerKasterExceptionNårIkkeGyldigHenleggelsesgrunn() throws TekniskException, FunksjonellException {
        String saksnummer = "123456789";
        initierFagsakMedToBehandlinger(new Fagsak(), saksnummer, new Behandling(), new Behandling(), 999L, 234L);

        expectedException.expect(TekniskException.class);

        fagsakService.henleggFagsak(saksnummer, "UGYLDIGKODE", "Fri tale");

        verify(prosessinstansService, never()).opprettProsessinstansHenleggSak(any(), any(), anyString());

        verify(oppgaveService, never()).ferdigstillOppgaveMedSaksnummer(anyString());
    }

    private void initierFagsakMedToBehandlinger(Fagsak fagsak, String saksnummer, Behandling førsteBehandling, Behandling andreBehandling, long førsteBehandlingId, long andreBehandlingId) {
        førsteBehandling.setRegistrertDato(Instant.parse("2000-10-10T10:12:35Z"));
        førsteBehandling.setId(førsteBehandlingId);
        Instant registrertDatoForSisteBehandling = Instant.parse("2010-11-11T10:12:35Z");
        andreBehandling.setId(andreBehandlingId);
        andreBehandling.setRegistrertDato(registrertDatoForSisteBehandling);
        fagsak.setBehandlinger(Arrays.asList(førsteBehandling, andreBehandling));

        doReturn(fagsak).when(fagsakRepo).findBySaksnummer(saksnummer);
    }

    @Test
    public final void testErBehandlingRedigerBar() throws FunksjonellException, TekniskException {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("12345678901");
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        Oppgave oppgave = new Oppgave();
        oppgave.setTilordnetRessurs(SAKSBEHANDLER);

        when(oppgaveService.hentOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer())).thenReturn(Optional.of(oppgave));
        assertThat(fagsakService.finnRedigerbarBehandling(SAKSBEHANDLER, fagsak).filter(behandling1 -> behandling1 == behandling).isPresent())
            .isEqualTo(true);

        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        assertThat(fagsakService.finnRedigerbarBehandling(SAKSBEHANDLER, fagsak)).isEqualTo(Optional.empty());

        behandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        assertThat(fagsakService.finnRedigerbarBehandling(SAKSBEHANDLER, fagsak)).isEqualTo(Optional.empty());

        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        assertThat(fagsakService.finnRedigerbarBehandling("", fagsak)).isEqualTo(Optional.empty());

        when(oppgaveService.hentOppgaveMedFagsaksnummer(behandling.getFagsak().getSaksnummer())).thenReturn(Optional.empty());
        assertThat(fagsakService.finnRedigerbarBehandling(SAKSBEHANDLER, fagsak)).isEqualTo(Optional.empty());

        fagsak.setBehandlinger(null);
        assertThat(fagsakService.finnRedigerbarBehandling(SAKSBEHANDLER, fagsak)).isEqualTo(Optional.empty());


    }
}