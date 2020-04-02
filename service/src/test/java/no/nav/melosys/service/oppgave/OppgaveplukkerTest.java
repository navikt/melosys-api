package no.nav.melosys.service.oppgave;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import no.nav.melosys.service.oppgave.dto.TilbakeleggingDto;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppgaveplukkerTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private OppgaveFasade oppgaveFasade;

    @Mock
    private OppgaveTilbakeleggingRepository oppgaveTilbakkeleggingRepo;
    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private OppgaveService oppgaveService;


    private Oppgaveplukker oppgaveplukker;

    private final static long BEHANDLING_ID = 123L;
    private final static long GSAK_SAKSNUMMER = 42L;
    private final static String SAKSNUMMER = "MOCK-1";

    @Before
    public void setUp() throws IkkeFunnetException {
        this.oppgaveplukker = new Oppgaveplukker(oppgaveFasade, oppgaveTilbakkeleggingRepo, fagsakService, behandlingService, oppgaveService);

        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.setGsakSaksnummer(GSAK_SAKSNUMMER);
        behandling.setFagsak(fagsak);

        when(behandlingService.hentBehandlingUtenSaksopplysninger(BEHANDLING_ID)).thenReturn(behandling);
    }

    @Test
    public void plukkOppgave_toOppgaverMedPriHOYForskjelligFrist_plukkoppgaveHøyestFrist() throws MelosysException {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2017, 8, 7), LocalDate.now(), "MEL-1"));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-12"));
        oppgaver.add(opprettOppgave("3", Oppgavetyper.JFR, PrioritetType.NORM, LocalDate.of(2018, 8, 10), LocalDate.now(), "MEL-123"));
        oppgaver.add(opprettOppgave("4", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 5), LocalDate.now(), "MEL-1234"));

        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(any(Behandlingstema.class))).thenReturn(oppgaver);

        PlukkOppgaveInnDto plukkOppgaveInnDto = opprettPlukkOppgaveInnDto(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isTrue();
        oppgave.ifPresent(o -> assertThat(o.getOppgaveId()).isEqualTo("4"));
    }

    @Test
    public void plukkOppgave_toOppgaverMedPriHOYSammeFristForskjelligAktivDato_plukkoppgaveOpprettetSenest() throws MelosysException {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.LAV,  LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-1"));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-12"));
        oppgaver.add(opprettOppgave("3", Oppgavetyper.BEH_SAK_MK, PrioritetType.NORM, LocalDate.of(2018, 8, 10), LocalDate.now(), "MEL-123"));
        oppgaver.add(opprettOppgave("4", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 7), LocalDate.now().plusDays(1L), "MEL-1234"));

        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(any(Behandlingstema.class))).thenReturn(oppgaver);

        PlukkOppgaveInnDto plukkOppgaveInnDto = opprettPlukkOppgaveInnDto(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isTrue();
        oppgave.ifPresent(o -> assertThat(o.getOppgaveId()).isEqualTo("2"));
    }

    @Test
    public void plukkOppgave_avventerDokumentast_og_med_utløptsvarfrist() throws MelosysException {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2019, 8, 7), LocalDate.now(), "MEL-1"));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-1"));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(any(Behandlingstema.class))).thenReturn(oppgaver);

        PlukkOppgaveInnDto plukkOppgaveInnDto = opprettPlukkOppgaveInnDto(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);
        behandling.setDokumentasjonSvarfristDato(Instant.now().minus(Duration.ofDays(1)));
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isTrue();
        oppgave.ifPresent(o -> assertThat(o.getOppgaveId()).isEqualTo("2"));
        assertThat(fagsak.getAktivBehandling().getStatus()).isEqualTo(Behandlingsstatus.AVVENT_DOK_PART);
    }

    @Test
    public void plukkOppgave_1_tilbakelagt() throws MelosysException {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.NORM, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-1"));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.BEH_SAK_MK, PrioritetType.NORM, LocalDate.of(2018, 8, 8), LocalDate.now(), "MEL-2"));
        oppgaver.add(opprettOppgave("3", Oppgavetyper.VUR, PrioritetType.NORM, LocalDate.of(2018, 8, 9), LocalDate.now(), "MEL-3"));

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(any(Behandlingstema.class))).thenReturn(oppgaver);

        List<OppgaveTilbakelegging> tilbakelagt = new ArrayList<>();
        tilbakelagt.add(new OppgaveTilbakelegging());
        when(oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(anyString(), eq("1"))).thenReturn(tilbakelagt);

        PlukkOppgaveInnDto plukkOppgaveInnDto = opprettPlukkOppgaveInnDto(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isTrue();
        oppgave.ifPresent(o -> assertThat(o.getOppgaveId()).isEqualTo("2"));
    }

    @Test
    public void plukkOppgave_alle_tilbakelagt() throws MelosysException{
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.LAV, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-1"));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-2"));
        oppgaver.add(opprettOppgave("3", Oppgavetyper.BEH_SAK_MK, PrioritetType.NORM, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-3"));

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(any(Behandlingstema.class))).thenReturn(oppgaver);

        List<OppgaveTilbakelegging> tilbakelagt = new ArrayList<>();
        tilbakelagt.add(new OppgaveTilbakelegging());
        when(oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(anyString(), anyString())).thenReturn(tilbakelagt);

        PlukkOppgaveInnDto plukkOppgaveInnDto = opprettPlukkOppgaveInnDto(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isFalse();
    }

    @Test
    public void leggTilbakeOppgave_medBegrunnelse() throws MelosysException {
        final String oppgaveId = String.valueOf(GSAK_SAKSNUMMER);
        final Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId(oppgaveId);
        oppgaveBuilder.setPrioritet(PrioritetType.valueOf("HOY"));
        final String saksbehandlerID = "test";
        final String begrunnelse = "Oppgaven er kjedelig";

        when(oppgaveService.hentOppgaveMedFagsaksnummer(SAKSNUMMER)).thenReturn(oppgaveBuilder.build());

        when(oppgaveTilbakkeleggingRepo.save(any(OppgaveTilbakelegging.class))).then(arguments -> {
            OppgaveTilbakelegging oppgaveTilbakelegging = arguments.getArgument(0);
            assertThat(oppgaveTilbakelegging.getOppgaveId()).isEqualTo(oppgaveId);
            assertThat(oppgaveTilbakelegging.getSaksbehandlerId()).isEqualTo(saksbehandlerID);
            assertThat(oppgaveTilbakelegging.getBegrunnelse()).isEqualTo(begrunnelse);
            return oppgaveTilbakelegging;
        }).getMock();

        TilbakeleggingDto tilbakelegging = new TilbakeleggingDto();
        tilbakelegging.setBehandlingID(BEHANDLING_ID);
        tilbakelegging.setBegrunnelse(begrunnelse);

        oppgaveplukker.leggTilbakeOppgave(saksbehandlerID, tilbakelegging);

        verify(oppgaveTilbakkeleggingRepo, times(1)).save(any(OppgaveTilbakelegging.class));
    }

    @Test
    public void leggTilbakeOppgave_venterPåDokumentasjon() throws MelosysException {
        final String oppgaveId = String.valueOf(GSAK_SAKSNUMMER);
        final Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId(oppgaveId);
        oppgaveBuilder.setPrioritet(PrioritetType.valueOf("HOY"));
        final String saksbehandlerID = "test";

        when(oppgaveService.hentOppgaveMedFagsaksnummer(SAKSNUMMER)).thenReturn(oppgaveBuilder.build());

        TilbakeleggingDto tilbakelegging = new TilbakeleggingDto();
        tilbakelegging.setBehandlingID(BEHANDLING_ID);
        tilbakelegging.setVenterPåDokumentasjon(true);

        oppgaveplukker.leggTilbakeOppgave(saksbehandlerID, tilbakelegging);

        verify(oppgaveTilbakkeleggingRepo, times(0)).save(any(OppgaveTilbakelegging.class));
    }


    @Test
    public void plukkOppgave_brukerBehandlingstema_finnerOppgave() throws MelosysException {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2017, 8, 7), LocalDate.now(), "MEL-1"));

        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(any(Behandlingstema.class))).thenReturn(oppgaver);

        PlukkOppgaveInnDto plukkOppgaveInnDto = opprettPlukkOppgaveInnDto(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        verify(oppgaveFasade).finnUtildelteOppgaverEtterFrist(any(Behandlingstema.class));

        assertThat(oppgave.isPresent()).isTrue();
    }

    @Test
    public void plukkOppgave_behandlingSomVenterHarSvarfristSomikkeHarGåttUt_plukkerIkkeBehandlingen() throws MelosysException {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.LAV, LocalDate.of(2017, 8, 7), LocalDate.now(), "MEL-1"));

        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(any(Behandlingstema.class))).thenReturn(oppgaver);

        PlukkOppgaveInnDto plukkOppgaveInnDto = opprettPlukkOppgaveInnDto(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);
        behandling.setDokumentasjonSvarfristDato(Instant.now().plus(Duration.ofDays(1)));

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isFalse();
    }

    @Test
    public void plukkOppgave_oppgaveSomVenterHarIkkeSvarfrist_plukkerIkkeBehandlingen() throws MelosysException {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.LAV, LocalDate.of(2017, 8, 7), LocalDate.now(), "MEL-1"));

        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(any(Behandlingstema.class))).thenReturn(oppgaver);

        PlukkOppgaveInnDto plukkOppgaveInnDto = opprettPlukkOppgaveInnDto(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);
        assertThat(oppgave.isPresent()).isFalse();
    }

    @Test
    public void plukkOppgave_søknadStatusSvarAou_oppdaterStatus() throws FunksjonellException, TekniskException {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2017, 8, 7), LocalDate.now(), "MEL-1"));

        ArgumentCaptor<Behandling> behandlingCaptor = ArgumentCaptor.forClass(Behandling.class);

        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(any(Behandlingstema.class))).thenReturn(oppgaver);

        PlukkOppgaveInnDto plukkOppgaveInnDto = opprettPlukkOppgaveInnDto(Behandlingstema.UTSENDT_ARBEIDSTAKER.getKode());

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setStatus(Behandlingsstatus.SVAR_ANMODNING_MOTTATT);

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        verify(oppgaveFasade).finnUtildelteOppgaverEtterFrist(any(Behandlingstema.class));
        verify(behandlingService).lagre(behandlingCaptor.capture());

        assertThat(oppgave.isPresent()).isTrue();
        assertThat(behandlingCaptor.getValue().getStatus()).isEqualTo(Behandlingsstatus.UNDER_BEHANDLING);
    }

    @Test(expected = FunksjonellException.class)
    public void plukkOppgave_utenBehandlingstype_forventException() throws FunksjonellException, TekniskException {
        PlukkOppgaveInnDto plukkOppgaveInnDto = opprettPlukkOppgaveInnDto("");
        oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);
    }

    private Oppgave opprettOppgave(String oppgaveId, Oppgavetyper oppgavetype, PrioritetType prioritet, LocalDate fristFerdigstillelse, LocalDate aktivDato, String saksnummer) {
        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgavetype(oppgavetype);
        oppgaveBuilder.setOppgaveId(oppgaveId);
        oppgaveBuilder.setPrioritet(prioritet);
        oppgaveBuilder.setFristFerdigstillelse(fristFerdigstillelse);
        oppgaveBuilder.setSaksnummer(saksnummer);
        return oppgaveBuilder.build();
    }

    private PlukkOppgaveInnDto opprettPlukkOppgaveInnDto(String behandlingstema) {
        PlukkOppgaveInnDto plukkOppgaveInnDto = new PlukkOppgaveInnDto();
        plukkOppgaveInnDto.setBehandlingstema(behandlingstema);
        return plukkOppgaveInnDto;
    }
}