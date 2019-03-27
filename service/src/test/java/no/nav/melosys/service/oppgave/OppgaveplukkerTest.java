package no.nav.melosys.service.oppgave;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.oppgave.Behandlingstema;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import no.nav.melosys.service.oppgave.dto.TilbakeleggingDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppgaveplukkerTest {

    @Mock
    private GsakFasade gsakFasade;

    @Mock
    private OppgaveTilbakeleggingRepository oppgaveTilbakkeleggingRepo;

    @Mock
    private FagsakRepository fagsakRepository;

    private Oppgaveplukker oppgaveplukker;

    private final static long BEHANDLING_ID = 123L;
    private final static long GSAK_SAKSNUMMER = 42L;
    private final static String SAKSNUMMER = "MOCK-1";

    @Before
    public void setUp() {
        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
        this.oppgaveplukker = new Oppgaveplukker(gsakFasade, oppgaveTilbakkeleggingRepo, fagsakRepository, behandlingRepository);

        Behandling behandling = new Behandling();
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.setGsakSaksnummer(GSAK_SAKSNUMMER);
        behandling.setFagsak(fagsak);

        when(behandlingRepository.findById(BEHANDLING_ID)).thenReturn(Optional.of(behandling));
    }

    @Test
    public void plukkOppgave_høy_prio() throws MelosysException {
        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgaveId("1");
        oppgave1.setPrioritet(PrioritetType.LAV);
        oppgave1.setFristFerdigstillelse(LocalDate.of(2017, 8, 7));
        oppgave1.setSaksnummer("MEL-1");
        oppgaver.add(oppgave1);

        Oppgave oppgave2 = new Oppgave();
        oppgave2.setOppgaveId("2");
        oppgave2.setPrioritet(PrioritetType.HOY);
        oppgave2.setFristFerdigstillelse(LocalDate.of(2018, 8, 7));
        oppgave2.setSaksnummer("MEL-12");
        oppgaver.add(oppgave2);

        Oppgave oppgave3 = new Oppgave();
        oppgave3.setOppgaveId("3");
        oppgave3.setFristFerdigstillelse(LocalDate.of(2018, 8, 10));
        oppgave3.setPrioritet(PrioritetType.NORM);
        oppgave3.setSaksnummer("MEL-123");
        oppgaver.add(oppgave3);

        Oppgave oppgave4 = new Oppgave();
        oppgave4.setOppgaveId("4");
        oppgave4.setFristFerdigstillelse(LocalDate.of(2018, 8, 5));
        oppgave4.setPrioritet(PrioritetType.HOY);
        oppgave4.setSaksnummer("MEL-1234");
        oppgaver.add(oppgave4);

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), any(), anyList(), anyList(), anyList())).thenReturn(oppgaver);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Sakstyper.EU_EOS.getKode());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(Behandlingstyper.SOEKNAD.getKode());

        PlukkOppgaveInnDto plukkOppgaveInnDto = new PlukkOppgaveInnDto();
        plukkOppgaveInnDto.setOppgavetype("BEH_SAK_MK");
        plukkOppgaveInnDto.setFagomrade("MED");
        plukkOppgaveInnDto.setSakstyper(sakstyper);
        plukkOppgaveInnDto.setBehandlingstyper(behandlingstyper);

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakRepository.findBySaksnummer(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isTrue();
        oppgave.ifPresent(o -> assertThat(o.getOppgaveId()).isEqualTo("4"));
        assertThat(fagsak.getAktivBehandling().getStatus()).isEqualTo(Behandlingsstatus.UNDER_BEHANDLING);
    }

    @Test
    public void plukkOppgave_avventerDokumentast_og_med_utløptsvarfrist() throws MelosysException {
        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgaveId("1");
        oppgave1.setPrioritet(PrioritetType.LAV);
        oppgave1.setFristFerdigstillelse(LocalDate.of(2019, 8, 7));
        oppgave1.setSaksnummer("MEL-1");
        oppgaver.add(oppgave1);

        Oppgave oppgave2 = new Oppgave();
        oppgave2.setOppgaveId("2");
        oppgave2.setPrioritet(PrioritetType.LAV);
        oppgave2.setFristFerdigstillelse(LocalDate.of(2018, 8, 7));
        oppgave2.setSaksnummer("MEL-1");
        oppgaver.add(oppgave2);

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), any(), anyList(), anyList(), anyList())).thenReturn(oppgaver);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Sakstyper.EU_EOS.getKode());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(Behandlingstyper.SOEKNAD.getKode());

        PlukkOppgaveInnDto plukkOppgaveInnDto = new PlukkOppgaveInnDto();
        plukkOppgaveInnDto.setOppgavetype("BEH_SAK_MK");
        plukkOppgaveInnDto.setFagomrade("MED");
        plukkOppgaveInnDto.setSakstyper(sakstyper);
        plukkOppgaveInnDto.setBehandlingstyper(behandlingstyper);

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);
        behandling.setDokumentasjonSvarfristDato(Instant.now().minus(Duration.ofDays(1)));
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakRepository.findBySaksnummer(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isTrue();
        oppgave.ifPresent(o -> assertThat(o.getOppgaveId()).isEqualTo("2"));
        assertThat(fagsak.getAktivBehandling().getStatus()).isEqualTo(Behandlingsstatus.AVVENT_DOK_PART);
    }

    @Test
    public void plukkOppgave_1_tilbakelagt() throws MelosysException {
        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgaveId("1");
        oppgave1.setPrioritet(PrioritetType.NORM);
        oppgave1.setSaksnummer("MEL-1");
        oppgave1.setFristFerdigstillelse(LocalDate.of(2018, 8, 7));
        oppgaver.add(oppgave1);

        Oppgave oppgave2 = new Oppgave();
        oppgave2.setOppgaveId("2");
        oppgave2.setPrioritet(PrioritetType.NORM);
        oppgave2.setSaksnummer("MEL-2");
        oppgave2.setFristFerdigstillelse(LocalDate.of(2018, 8, 8));
        oppgaver.add(oppgave2);
        Oppgave oppgave3 = new Oppgave();
        oppgave3.setOppgaveId("3");
        oppgave3.setPrioritet(PrioritetType.NORM);
        oppgave3.setSaksnummer("MEL-3");
        oppgave3.setFristFerdigstillelse(LocalDate.of(2018, 8, 9));
        oppgaver.add(oppgave3);

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakRepository.findBySaksnummer(anyString())).thenReturn(fagsak);

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), any(), anyList(), anyList(), anyList())).thenReturn(oppgaver);

        List<OppgaveTilbakelegging> tilbakelagt = new ArrayList<>();
        tilbakelagt.add(new OppgaveTilbakelegging());
        when(oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(anyString(), eq("1"))).thenReturn(tilbakelagt);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Sakstyper.FTRL.getKode());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(Behandlingstyper.NY_VURDERING.getKode());

        PlukkOppgaveInnDto plukkOppgaveInnDto = new PlukkOppgaveInnDto();
        plukkOppgaveInnDto.setOppgavetype("BEH_SAK_MK");
        plukkOppgaveInnDto.setSakstyper(sakstyper);
        plukkOppgaveInnDto.setBehandlingstyper(behandlingstyper);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isTrue();
        oppgave.ifPresent(o -> assertThat(o.getOppgaveId()).isEqualTo("2"));
        assertThat(fagsak.getAktivBehandling().getStatus()).isEqualTo(Behandlingsstatus.UNDER_BEHANDLING);
    }

    @Test
    public void plukkOppgave_alle_tilbakelagt() throws MelosysException{
        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgaveId("1");
        oppgave1.setPrioritet(PrioritetType.LAV);
        oppgave1.setSaksnummer("MEL-1");
        oppgave1.setFristFerdigstillelse(LocalDate.of(2018, 8, 7));
        oppgaver.add(oppgave1);

        Oppgave oppgave2 = new Oppgave();
        oppgave2.setOppgaveId("2");
        oppgave2.setPrioritet(PrioritetType.HOY);
        oppgave2.setSaksnummer("MEL-2");
        oppgave2.setFristFerdigstillelse(LocalDate.of(2018, 8, 7));
        oppgaver.add(oppgave2);

        Oppgave oppgave3 = new Oppgave();
        oppgave3.setOppgaveId("3");
        oppgave3.setPrioritet(PrioritetType.NORM);
        oppgave3.setSaksnummer("MEL-3");
        oppgave3.setFristFerdigstillelse(LocalDate.of(2018, 8, 7));
        oppgaver.add(oppgave3);

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakRepository.findBySaksnummer(anyString())).thenReturn(fagsak);

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), any(), anyList(), anyList(), anyList())).thenReturn(oppgaver);

        List<OppgaveTilbakelegging> tilbakelagt = new ArrayList<>();
        tilbakelagt.add(new OppgaveTilbakelegging());
        when(oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(anyString(), anyString())).thenReturn(tilbakelagt);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Sakstyper.FTRL.getKode());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(Behandlingstyper.SOEKNAD.getKode());

        PlukkOppgaveInnDto plukkOppgaveInnDto = new PlukkOppgaveInnDto();
        plukkOppgaveInnDto.setOppgavetype("BEH_SAK_MK");
        plukkOppgaveInnDto.setSakstyper(sakstyper);
        plukkOppgaveInnDto.setBehandlingstyper(behandlingstyper);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isFalse();
        assertThat(fagsak.getAktivBehandling().getStatus()).isEqualTo(Behandlingsstatus.OPPRETTET);
    }

    @Test
    public void leggTilbakeOppgave_medBegrunnelse() throws MelosysException {
        final String oppgaveId = String.valueOf(GSAK_SAKSNUMMER);
        final Oppgave oppgave = new Oppgave();
        oppgave.setOppgaveId(oppgaveId);
        oppgave.setPrioritet(PrioritetType.valueOf("HOY"));
        final String saksbehandlerID = "test";
        final String begrunnelse = "Oppgaven er kjedelig";

        when(gsakFasade.finnOppgaveMedSaksnummer(SAKSNUMMER)).thenReturn(Optional.of(oppgave));

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
        final Oppgave oppgave = new Oppgave();
        oppgave.setOppgaveId(oppgaveId);
        oppgave.setPrioritet(PrioritetType.valueOf("HOY"));
        final String saksbehandlerID = "test";

        when(gsakFasade.finnOppgaveMedSaksnummer(SAKSNUMMER)).thenReturn(Optional.of(oppgave));

        TilbakeleggingDto tilbakelegging = new TilbakeleggingDto();
        tilbakelegging.setBehandlingID(BEHANDLING_ID);
        tilbakelegging.setVenterPåDokumentasjon(true);

        oppgaveplukker.leggTilbakeOppgave(saksbehandlerID, tilbakelegging);

        verify(oppgaveTilbakkeleggingRepo, times(0)).save(any(OppgaveTilbakelegging.class));
    }


    @Test
    public void plukkOppgave_brukerBehandlingstema_finnerOppgave() throws MelosysException {
        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgaveId("1");
        oppgave1.setPrioritet(PrioritetType.LAV);
        oppgave1.setFristFerdigstillelse(LocalDate.of(2017, 8, 7));
        oppgave1.setSaksnummer("MEL-1");
        oppgaver.add(oppgave1);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Sakstyper.EU_EOS.getKode());

        List<String> behandlingstemaer = new ArrayList<>();
        behandlingstemaer.add(Behandlingstema.EU_EOS.name());

        ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), any(), anyList(), any(), captor.capture())).thenReturn(oppgaver);

        PlukkOppgaveInnDto plukkOppgaveInnDto = new PlukkOppgaveInnDto();
        plukkOppgaveInnDto.setOppgavetype("BEH_SAK_MK");
        plukkOppgaveInnDto.setFagomrade("MED");
        plukkOppgaveInnDto.setSakstyper(sakstyper);

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakRepository.findBySaksnummer(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isTrue();
        assertThat(captor.getValue()).containsExactly(Behandlingstema.EU_EOS);
    }

    @Test
    public void hentBehandlingstema_støtterAlleSakstyper() {
        List<Sakstyper> sakstyper = Arrays.asList(Sakstyper.EU_EOS, Sakstyper.TRYGDEAVTALE, Sakstyper.FTRL);

        List<Behandlingstema> behandlingstemaList = oppgaveplukker.hentBehandlingstema(sakstyper);

        assertThat(behandlingstemaList).containsExactlyInAnyOrder(Behandlingstema.values());
    }
}