package no.nav.melosys.service.oppgave;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.integrasjon.oppgave.OppgaveFasade;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import no.nav.melosys.service.oppgave.dto.TilbakeleggingDto;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OppgaveplukkerTest {

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

    private FakeUnleash unleash = new FakeUnleash();


    private Oppgaveplukker oppgaveplukker;

    private final static long BEHANDLING_ID = 123L;
    private final static long GSAK_SAKSNUMMER = 42L;
    private final static String SAKSNUMMER = "MOCK-1";

    @BeforeEach
    public void setUp() {
        this.oppgaveplukker = new Oppgaveplukker(oppgaveFasade, oppgaveTilbakkeleggingRepo, fagsakService, behandlingService, oppgaveService, unleash);

        unleash.enableAll();

        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.setGsakSaksnummer(GSAK_SAKSNUMMER);
        behandling.setFagsak(fagsak);

        when(behandlingService.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
    }

    @Test
    void plukkOppgave_toOppgaverMedPriHOYForskjelligFrist_plukkoppgaveHøyestFrist() {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2017, 8, 7), LocalDate.now(), "MEL-1"));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-12"));
        oppgaver.add(opprettOppgave("3", Oppgavetyper.JFR, PrioritetType.NORM, LocalDate.of(2018, 8, 10), LocalDate.now(), "MEL-123"));
        oppgaver.add(opprettOppgave("4", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 5), LocalDate.now(), "MEL-1234"));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto());

        assertThat(oppgave).isPresent();
        oppgave.ifPresent(o -> assertThat(o.getOppgaveId()).isEqualTo("4"));
    }

    @Test
    void plukkOppgave_toOppgaverMedPriHOYSammeFristForskjelligAktivDato_plukkoppgaveOpprettetSenest() {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.LAV, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-1"));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-12"));
        oppgaver.add(opprettOppgave("3", Oppgavetyper.BEH_SAK_MK, PrioritetType.NORM, LocalDate.of(2018, 8, 10), LocalDate.now(), "MEL-123"));
        oppgaver.add(opprettOppgave("4", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 7), LocalDate.now().plusDays(1L), "MEL-1234"));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto());

        assertThat(oppgave).isPresent();
        oppgave.ifPresent(o -> assertThat(o.getOppgaveId()).isEqualTo("2"));
    }

    @Test
    void plukkOppgave_avventerDokumentast_og_med_utløptsvarfrist() {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2019, 8, 7), LocalDate.now(), "MEL-1"));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-1"));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);
        behandling.setDokumentasjonSvarfristDato(Instant.now().minus(Duration.ofDays(1)));
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto());

        assertThat(oppgave)
            .isPresent()
            .get()
            .extracting(Oppgave::getOppgaveId)
            .isEqualTo("2");
        assertThat(fagsak.hentAktivBehandling().getStatus()).isEqualTo(Behandlingsstatus.AVVENT_DOK_PART);
    }

    @Test
    void oppgaveplukker_velgerIkkeSak_nårStatusErVenterPaaFagligAvklaring() {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2019, 8, 7), LocalDate.now(), "MEL-1"));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVVENT_FAGLIG_AVKLARING);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto());

        assertThat(oppgave).isNotPresent();
    }

    @Test
    void plukkOppgave_1_tilbakelagt() {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.NORM, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-1"));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.BEH_SAK_MK, PrioritetType.NORM, LocalDate.of(2018, 8, 8), LocalDate.now(), "MEL-2"));
        oppgaver.add(opprettOppgave("3", Oppgavetyper.VUR, PrioritetType.NORM, LocalDate.of(2018, 8, 9), LocalDate.now(), "MEL-3"));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        List<OppgaveTilbakelegging> tilbakelagt = new ArrayList<>();
        tilbakelagt.add(new OppgaveTilbakelegging());
        when(oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(anyString(), eq("1"))).thenReturn(tilbakelagt);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto());

        assertThat(oppgave).isPresent();
        oppgave.ifPresent(o -> assertThat(o.getOppgaveId()).isEqualTo("2"));
    }

    @Test
    void plukkOppgave_alle_tilbakelagt() {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.LAV, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-1"));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-2"));
        oppgaver.add(opprettOppgave("3", Oppgavetyper.BEH_SAK_MK, PrioritetType.NORM, LocalDate.of(2018, 8, 7), LocalDate.now(), "MEL-3"));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        List<OppgaveTilbakelegging> tilbakelagt = new ArrayList<>();
        tilbakelagt.add(new OppgaveTilbakelegging());
        when(oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(anyString(), anyString())).thenReturn(tilbakelagt);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto());

        assertThat(oppgave).isEmpty();
    }

    @Test
    void leggTilbakeOppgave_medBegrunnelse() {
        final String oppgaveId = String.valueOf(GSAK_SAKSNUMMER);
        final Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId(oppgaveId);
        oppgaveBuilder.setPrioritet(PrioritetType.valueOf("HOY"));
        final String saksbehandlerID = "test";
        final String begrunnelse = "Oppgaven er kjedelig";
        when(oppgaveService.hentÅpenOppgaveMedFagsaksnummer(SAKSNUMMER)).thenReturn(oppgaveBuilder.build());

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
    void leggTilbakeOppgave_venterPåDokumentasjon() {
        final String oppgaveId = String.valueOf(GSAK_SAKSNUMMER);
        final Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId(oppgaveId);
        oppgaveBuilder.setPrioritet(PrioritetType.valueOf("HOY"));
        final String saksbehandlerID = "test";
        when(oppgaveService.hentÅpenOppgaveMedFagsaksnummer(SAKSNUMMER)).thenReturn(oppgaveBuilder.build());

        TilbakeleggingDto tilbakelegging = new TilbakeleggingDto();
        tilbakelegging.setBehandlingID(BEHANDLING_ID);
        tilbakelegging.setVenterPåDokumentasjon(true);

        oppgaveplukker.leggTilbakeOppgave(saksbehandlerID, tilbakelegging);

        verify(oppgaveTilbakkeleggingRepo, times(0)).save(any(OppgaveTilbakelegging.class));
    }


    @Test
    void plukkOppgave_brukerBehandlingstema_finnerOppgave() {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2017, 8, 7), LocalDate.now(), "MEL-1"));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto());

        verify(oppgaveFasade).finnUtildelteOppgaverEtterFrist(anyString());
        assertThat(oppgave).isPresent();
    }

    @Test
    void plukkOppgave_behandlingSomVenterHarSvarfristSomikkeHarGåttUt_plukkerIkkeBehandlingen() {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.LAV, LocalDate.of(2017, 8, 7), LocalDate.now(), "MEL-1"));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);
        behandling.setDokumentasjonSvarfristDato(Instant.now().plus(Duration.ofDays(1)));
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto());

        assertThat(oppgave).isEmpty();
    }

    @Test
    void plukkOppgave_oppgaveSomVenterHarIkkeSvarfrist_plukkerIkkeBehandlingen() {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.LAV, LocalDate.of(2017, 8, 7), LocalDate.now(), "MEL-1"));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto());

        assertThat(oppgave).isEmpty();
    }

    @Test
    void plukkOppgave_søknadStatusSvarAou_oppdaterStatus() {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2017, 8, 7), LocalDate.now(), "MEL-1"));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setStatus(Behandlingsstatus.SVAR_ANMODNING_MOTTATT);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        ArgumentCaptor<Behandling> behandlingCaptor = ArgumentCaptor.forClass(Behandling.class);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto());

        verify(oppgaveFasade).finnUtildelteOppgaverEtterFrist(anyString());
        verify(behandlingService).lagre(behandlingCaptor.capture());
        assertThat(oppgave).isPresent();
        assertThat(behandlingCaptor.getValue().getStatus()).isEqualTo(Behandlingsstatus.UNDER_BEHANDLING);
    }

    private Oppgave opprettOppgave(String oppgaveId, Oppgavetyper oppgavetype, PrioritetType prioritet, LocalDate fristFerdigstillelse, LocalDate aktivDato, String saksnummer) {
        return new Oppgave.Builder()
            .setOppgavetype(oppgavetype)
            .setOppgaveId(oppgaveId)
            .setPrioritet(prioritet)
            .setFristFerdigstillelse(fristFerdigstillelse)
            .setSaksnummer(saksnummer)
            .build();
    }

    private PlukkOppgaveInnDto opprettPlukkOppgaveInnDto() {
        PlukkOppgaveInnDto plukkOppgaveInnDto = new PlukkOppgaveInnDto();
        plukkOppgaveInnDto.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        plukkOppgaveInnDto.setSakstema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        plukkOppgaveInnDto.setSakstype(Sakstyper.EU_EOS);
        return plukkOppgaveInnDto;
    }
}
