package no.nav.melosys.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import no.nav.melosys.service.oppgave.dto.TilbakeleggingDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppgaveplukkerTest {

    @Mock
    private GsakFasade gsakFasade;

    @Mock
    private OppgaveTilbakeleggingRepository oppgaveTilbakkeleggingRepo;

    @Mock
    private FagsakRepository fagsakRepository;

    private Oppgaveplukker oppgaveplukker;

    @Before
    public void setUp() {
        this.oppgaveplukker = new Oppgaveplukker(gsakFasade, oppgaveTilbakkeleggingRepo, fagsakRepository);
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

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), any(), anyList(), anyList())).thenReturn(oppgaver);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Fagsakstype.EU_EØS.getKode());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(Behandlingstype.SØKNAD.getKode());

        PlukkOppgaveInnDto plukkOppgaveInnDto = new PlukkOppgaveInnDto();
        plukkOppgaveInnDto.setOppgavetype("BEH_SAK");
        plukkOppgaveInnDto.setFagomrade("MED");
        plukkOppgaveInnDto.setSakstyper(sakstyper);
        plukkOppgaveInnDto.setBehandlingstyper(behandlingstyper);

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstype.SØKNAD);
        behandling.setStatus(Behandlingsstatus.OPPRETTET);

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakRepository.findBySaksnummer(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isTrue();
        oppgave.ifPresent(o -> assertThat(o.getOppgaveId()).isEqualTo("4"));
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

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), any(), anyList(), anyList())).thenReturn(oppgaver);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Fagsakstype.EU_EØS.getKode());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(Behandlingstype.SØKNAD.getKode());

        PlukkOppgaveInnDto plukkOppgaveInnDto = new PlukkOppgaveInnDto();
        plukkOppgaveInnDto.setOppgavetype("BEH_SAK");
        plukkOppgaveInnDto.setFagomrade("MED");
        plukkOppgaveInnDto.setSakstyper(sakstyper);
        plukkOppgaveInnDto.setBehandlingstyper(behandlingstyper);

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstype.SØKNAD);
        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);
        behandling.setDokumentasjonSvarfristDato(Instant.now().minus(Duration.ofDays(1)));
        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakRepository.findBySaksnummer(anyString())).thenReturn(fagsak);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isTrue();
        oppgave.ifPresent(o -> assertThat(o.getOppgaveId()).isEqualTo("2"));
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
        behandling.setType(Behandlingstype.SØKNAD);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakRepository.findBySaksnummer(anyString())).thenReturn(fagsak);

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), any(), anyList(), anyList())).thenReturn(oppgaver);

        List<OppgaveTilbakelegging> tilbakelagt = new ArrayList<>();
        tilbakelagt.add(new OppgaveTilbakelegging());
        when(oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(anyString(), eq("1"))).thenReturn(tilbakelagt);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Fagsakstype.FOLKETRYGD.getKode());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(Behandlingstype.REVURDERING.getKode());

        PlukkOppgaveInnDto plukkOppgaveInnDto = new PlukkOppgaveInnDto();
        plukkOppgaveInnDto.setOppgavetype("BEH_SAK");
        plukkOppgaveInnDto.setSakstyper(sakstyper);
        plukkOppgaveInnDto.setBehandlingstyper(behandlingstyper);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isTrue();
        oppgave.ifPresent(o -> assertThat(o.getOppgaveId()).isEqualTo("2"));
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
        behandling.setType(Behandlingstype.SØKNAD);
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        behandling.setFagsak(fagsak);
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakRepository.findBySaksnummer(anyString())).thenReturn(fagsak);

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), any(), anyList(), anyList())).thenReturn(oppgaver);

        List<OppgaveTilbakelegging> tilbakelagt = new ArrayList<>();
        tilbakelagt.add(new OppgaveTilbakelegging());
        when(oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(anyString(), anyString())).thenReturn(tilbakelagt);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Fagsakstype.FOLKETRYGD.getKode());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(Behandlingstype.SØKNAD.getKode());

        PlukkOppgaveInnDto plukkOppgaveInnDto = new PlukkOppgaveInnDto();
        plukkOppgaveInnDto.setOppgavetype("BEH_SAK");
        plukkOppgaveInnDto.setSakstyper(sakstyper);
        plukkOppgaveInnDto.setBehandlingstyper(behandlingstyper);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isFalse();
    }

    @Test
    public void leggTilbakeOppgave_medBegrunnelse() throws MelosysException {
        final String oppgaveId = "42";
        final Oppgave oppgave = new Oppgave();
        oppgave.setOppgaveId(oppgaveId);
        oppgave.setPrioritet(PrioritetType.valueOf("HOY"));
        final String saksbehandlerID = "test";
        final String begrunnelse = "Oppgaven er kjedelig";

        when(gsakFasade.hentOppgave(oppgaveId)).thenReturn(oppgave);

        when(oppgaveTilbakkeleggingRepo.save(any(OppgaveTilbakelegging.class))).then(arguments -> {
            OppgaveTilbakelegging oppgaveTilbakelegging = arguments.getArgument(0);
            assertThat(oppgaveTilbakelegging.getOppgaveId()).isEqualTo("42");
            assertThat(oppgaveTilbakelegging.getSaksbehandlerId()).isEqualTo("test");
            assertThat(oppgaveTilbakelegging.getBegrunnelse()).isEqualTo("Oppgaven er kjedelig");
            return oppgaveTilbakelegging;
        }).getMock();

        TilbakeleggingDto tilbakelegging = new TilbakeleggingDto();
        tilbakelegging.setOppgaveId(oppgaveId);
        tilbakelegging.setBegrunnelse(begrunnelse);

        oppgaveplukker.leggTilbakeOppgave(saksbehandlerID, tilbakelegging);

        verify(oppgaveTilbakkeleggingRepo, times(1)).save(any(OppgaveTilbakelegging.class));
    }

    @Test
    public void leggTilbakeOppgave_venterPåDokumentasjon() throws MelosysException {
        final String oppgaveId = "42";
        final Oppgave oppgave = new Oppgave();
        oppgave.setOppgaveId(oppgaveId);
        oppgave.setPrioritet(PrioritetType.valueOf("HOY"));
        final String saksbehandlerID = "test";

        when(gsakFasade.hentOppgave(oppgaveId)).thenReturn(oppgave);

        TilbakeleggingDto tilbakelegging = new TilbakeleggingDto();
        tilbakelegging.setOppgaveId(oppgaveId);
        tilbakelegging.setVenterPåDokumentasjon(true);

        oppgaveplukker.leggTilbakeOppgave(saksbehandlerID, tilbakelegging);

        verify(oppgaveTilbakkeleggingRepo, times(0)).save(any(OppgaveTilbakelegging.class));
    }
}