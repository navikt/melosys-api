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
    private final static String SAKSNUMMER_1 = "MEL-1111";
    private final static String SAKSNUMMER_2 = "MEL-2222";
    private final static String SAKSNUMMER_3 = "MEL-3333";

    @BeforeEach
    public void setUp() {
        this.oppgaveplukker = new Oppgaveplukker(oppgaveFasade, oppgaveTilbakkeleggingRepo, fagsakService, behandlingService, oppgaveService, unleash);

        unleash.enableAll();

        Behandling behandling = opprettBehandling();
        Fagsak fagsak = opprettFagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.setGsakSaksnummer(GSAK_SAKSNUMMER);
        behandling.setFagsak(fagsak);

        when(behandlingService.hentBehandling(BEHANDLING_ID)).thenReturn(behandling);
    }

    @Test
    void plukkOppgave_toOppgaverMedPriHOYForskjelligFrist_plukkoppgaveHøyestFrist() {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2017, 8, 7), SAKSNUMMER_1));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 7), "MEL-12"));
        oppgaver.add(opprettOppgave("3", Oppgavetyper.JFR, PrioritetType.NORM, LocalDate.of(2018, 8, 10), "MEL-123"));
        oppgaver.add(opprettOppgave("4", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 5), "MEL-1234"));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = opprettFagsak();
        Behandling behandling = opprettBehandling();
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
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.LAV, LocalDate.of(2018, 8, 7), SAKSNUMMER_1));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 7), "MEL-12"));
        oppgaver.add(opprettOppgave("3", Oppgavetyper.BEH_SAK_MK, PrioritetType.NORM, LocalDate.of(2018, 8, 10), "MEL-123"));
        oppgaver.add(opprettOppgave("4", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 7), "MEL-1234"));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = opprettFagsak();
        Behandling behandling = opprettBehandling();
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
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2019, 8, 7), SAKSNUMMER_1));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2018, 8, 7), SAKSNUMMER_1));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = opprettFagsak();
        Behandling behandling = opprettBehandling();
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
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2019, 8, 7), SAKSNUMMER_1));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = opprettFagsak();
        Behandling behandling = opprettBehandling();
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
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.NORM, LocalDate.of(2018, 8, 7), SAKSNUMMER_1));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.BEH_SAK_MK, PrioritetType.NORM, LocalDate.of(2018, 8, 8), SAKSNUMMER_2));
        oppgaver.add(opprettOppgave("3", Oppgavetyper.VUR, PrioritetType.NORM, LocalDate.of(2018, 8, 9), SAKSNUMMER_3));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = opprettFagsak();
        Behandling behandling = opprettBehandling();
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
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.LAV, LocalDate.of(2018, 8, 7), SAKSNUMMER_1));
        oppgaver.add(opprettOppgave("2", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 7), SAKSNUMMER_2));
        oppgaver.add(opprettOppgave("3", Oppgavetyper.BEH_SAK_MK, PrioritetType.NORM, LocalDate.of(2018, 8, 7), SAKSNUMMER_3));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = opprettFagsak();
        Behandling behandling = opprettBehandling();
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
        when(oppgaveService.hentÅpenBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER)).thenReturn(oppgaveBuilder.build());

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
        when(oppgaveService.hentÅpenBehandlingsoppgaveMedFagsaksnummer(SAKSNUMMER)).thenReturn(oppgaveBuilder.build());

        TilbakeleggingDto tilbakelegging = new TilbakeleggingDto();
        tilbakelegging.setBehandlingID(BEHANDLING_ID);
        tilbakelegging.setVenterPåDokumentasjon(true);

        oppgaveplukker.leggTilbakeOppgave(saksbehandlerID, tilbakelegging);

        verify(oppgaveTilbakkeleggingRepo, times(0)).save(any(OppgaveTilbakelegging.class));
    }

    @Test
    void plukkOppgave_brukerBehandlingstema_finnerOppgave() {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2017, 8, 7), SAKSNUMMER_1));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = opprettFagsak();
        Behandling behandling = opprettBehandling();
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
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.LAV, LocalDate.of(2017, 8, 7), SAKSNUMMER_1));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = opprettFagsak();
        Behandling behandling = opprettBehandling();
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
        oppgaver.add(opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.LAV, LocalDate.of(2017, 8, 7), SAKSNUMMER_1));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = opprettFagsak();
        Behandling behandling = opprettBehandling();
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
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2017, 8, 7), SAKSNUMMER_1));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = opprettFagsak();
        Behandling behandling = opprettBehandling();
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

    @Test
    void plukkOppgave_kombinasjonFlereBehandlingstema_sokerOppgaveToGanger() {
        List<Oppgave> oppgaver = new ArrayList<>();
        oppgaver.add(opprettOppgave("1", Oppgavetyper.VUR, PrioritetType.LAV, LocalDate.of(2017, 8, 7), SAKSNUMMER_1));
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);

        Fagsak fagsak = opprettFagsak();
        Behandling behandling = opprettBehandling();
        behandling.setStatus(Behandlingsstatus.OPPRETTET);
        behandling.setTema(Behandlingstema.PENSJONIST);
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        when(fagsakService.hentFagsak(anyString())).thenReturn(fagsak);

        var plukkOppgaveInnDto = new PlukkOppgaveInnDto(Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.PENSJONIST);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        verify(oppgaveFasade, times(2)).finnUtildelteOppgaverEtterFrist(anyString());
        assertThat(oppgave).isPresent();
    }

    @Test
    void plukkOppgave_fagsakerUlikQuery_blirIgnorert() {
        List<Oppgave> oppgaver = List.of(
            opprettOppgave("1", Oppgavetyper.BEH_SAK_MK, PrioritetType.LAV, LocalDate.of(2018, 8, 7), SAKSNUMMER_1),
            opprettOppgave("2", Oppgavetyper.BEH_SAK_MK, PrioritetType.HOY, LocalDate.of(2018, 8, 7), SAKSNUMMER_2),
            opprettOppgave("3", Oppgavetyper.BEH_SAK_MK, PrioritetType.NORM, LocalDate.of(2018, 8, 7), SAKSNUMMER_3)
        );
        when(oppgaveFasade.finnUtildelteOppgaverEtterFrist(anyString())).thenReturn(oppgaver);
        when(fagsakService.hentFagsak(SAKSNUMMER_1)).thenReturn(opprettFagsakMedBehandling(Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.YRKESAKTIV));
        when(fagsakService.hentFagsak(SAKSNUMMER_2)).thenReturn(opprettFagsakMedBehandling(Sakstyper.EU_EOS, Sakstemaer.TRYGDEAVGIFT, Behandlingstema.UTSENDT_ARBEIDSTAKER));
        when(fagsakService.hentFagsak(SAKSNUMMER_3)).thenReturn(opprettFagsakMedBehandling(Sakstyper.TRYGDEAVTALE, Sakstemaer.UNNTAK, Behandlingstema.UTSENDT_ARBEIDSTAKER));

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", opprettPlukkOppgaveInnDto());

        assertThat(oppgave).isEmpty();
    }

    private Oppgave opprettOppgave(String oppgaveId, Oppgavetyper oppgavetype, PrioritetType prioritet, LocalDate fristFerdigstillelse, String saksnummer) {
        return new Oppgave.Builder()
            .setOppgavetype(oppgavetype)
            .setOppgaveId(oppgaveId)
            .setPrioritet(prioritet)
            .setFristFerdigstillelse(fristFerdigstillelse)
            .setSaksnummer(saksnummer)
            .build();
    }

    private PlukkOppgaveInnDto opprettPlukkOppgaveInnDto() {
        return new PlukkOppgaveInnDto(Sakstyper.EU_EOS, Sakstemaer.MEDLEMSKAP_LOVVALG, Behandlingstema.UTSENDT_ARBEIDSTAKER);
    }

    private Behandling opprettBehandling() {
        var behandling = new Behandling();
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        return behandling;
    }

    private Fagsak opprettFagsak() {
        var fagsak = new Fagsak();
        fagsak.setTema(Sakstemaer.MEDLEMSKAP_LOVVALG);
        fagsak.setType(Sakstyper.EU_EOS);
        return fagsak;
    }

    private Fagsak opprettFagsakMedBehandling(Sakstyper sakstype, Sakstemaer sakstema, Behandlingstema behandlingstema) {
        var fagsak = new Fagsak();
        var behandling = new Behandling();
        behandling.setType(Behandlingstyper.FØRSTEGANG);
        behandling.setTema(behandlingstema);
        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);
        behandling.setDokumentasjonSvarfristDato(Instant.now().plus(Duration.ofDays(1)));
        behandling.setFagsak(fagsak);
        fagsak.setType(sakstype);
        fagsak.setTema(sakstema);
        fagsak.setBehandlinger(Collections.singletonList(behandling));
        return fagsak;
    }
}
