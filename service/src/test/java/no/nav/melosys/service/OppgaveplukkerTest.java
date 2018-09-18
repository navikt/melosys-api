package no.nav.melosys.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.Behandlingstype;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.OppgaveTilbakelegging;
import no.nav.melosys.domain.oppgave.PrioritetType;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppgaveplukkerTest {

    @Mock
    private Pep pep;

    @Mock
    private GsakFasade gsakFasade;

    @Mock
    private FagsakRepository fagsakRepository;

    @Mock
    private OppgaveTilbakeleggingRepository oppgaveTilbakkeleggingRepo;

    private Oppgaveplukker oppgaveplukker;

    @Before
    public void setUp() {
        this.oppgaveplukker = new Oppgaveplukker(gsakFasade,
                                                 fagsakRepository,
                                                 oppgaveTilbakkeleggingRepo,
                                                 new PepStub());
    }

    @Test
    public void plukkOppgave_høy_prio() throws IkkeFunnetException, SikkerhetsbegrensningException, FunksjonellException, TekniskException {
        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgaveId("1");
        oppgave1.setPrioritet(PrioritetType.LAV);
        oppgave1.setFristFerdigstillelse(LocalDate.of(2017, 8, 7));
        oppgaver.add(oppgave1);
        Oppgave oppgave2 = new Oppgave();
        oppgave2.setOppgaveId("2");
        oppgave2.setPrioritet(PrioritetType.HOY);
        oppgave2.setFristFerdigstillelse(LocalDate.of(2018, 8, 7));
        oppgaver.add(oppgave2);
        Oppgave oppgave3 = new Oppgave();
        oppgave3.setOppgaveId("3");
        oppgave3.setFristFerdigstillelse(LocalDate.of(2018, 8, 10));
        oppgave3.setPrioritet(PrioritetType.NORM);
        oppgaver.add(oppgave3);
        Oppgave oppgave4 = new Oppgave();
        oppgave4.setOppgaveId("4");
        oppgave4.setFristFerdigstillelse(LocalDate.of(2018, 8, 5));
        oppgave4.setPrioritet(PrioritetType.HOY);
        oppgaver.add(oppgave4);

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), any(), anyList(), anyList())).thenReturn(oppgaver);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(FagsakType.EU_EØS.getKode());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(Behandlingstype.SØKNAD.getKode());

        PlukkOppgaveInnDto plukkOppgaveInnDto = new PlukkOppgaveInnDto();
        plukkOppgaveInnDto.setOppgavetype("BEH_SAK");
        plukkOppgaveInnDto.setFagomrade("MED");
        plukkOppgaveInnDto.setSakstyper(sakstyper);
        plukkOppgaveInnDto.setBehandlingstyper(behandlingstyper);

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", plukkOppgaveInnDto);

        assertThat(oppgave.isPresent()).isTrue();
        oppgave.ifPresent(o -> assertThat(o.getOppgaveId()).isEqualTo("4"));
    }

    @Test
    public void plukkOppgave_1_tilbakelagt() throws IkkeFunnetException, SikkerhetsbegrensningException, FunksjonellException, TekniskException {
        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgaveId("1");
        oppgave1.setPrioritet(PrioritetType.NORM);
        oppgaver.add(oppgave1);
        Oppgave oppgave2 = new Oppgave();
        oppgave2.setOppgaveId("2");
        oppgave2.setPrioritet(PrioritetType.NORM);
        oppgaver.add(oppgave2);
        Oppgave oppgave3 = new Oppgave();
        oppgave3.setOppgaveId("3");
        oppgave3.setPrioritet(PrioritetType.NORM);
        oppgaver.add(oppgave3);

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), any(), anyList(), anyList())).thenReturn(oppgaver);

        List<OppgaveTilbakelegging> tilbakelagt = new ArrayList<>();
        tilbakelagt.add(new OppgaveTilbakelegging());
        when(oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(anyString(), eq("1"))).thenReturn(tilbakelagt);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(FagsakType.FOLKETRYGD.getKode());

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
    public void plukkOppgave_alle_tilbakelagt() throws IkkeFunnetException, SikkerhetsbegrensningException, FunksjonellException, TekniskException {
        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgaveId("1");
        oppgave1.setPrioritet(PrioritetType.LAV);
        oppgaver.add(oppgave1);
        Oppgave oppgave2 = new Oppgave();
        oppgave2.setOppgaveId("2");
        oppgave2.setPrioritet(PrioritetType.HOY);
        oppgaver.add(oppgave2);
        Oppgave oppgave3 = new Oppgave();
        oppgave3.setOppgaveId("3");
        oppgave3.setPrioritet(PrioritetType.NORM);
        oppgaver.add(oppgave3);

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), any(), anyList(), anyList())).thenReturn(oppgaver);

        List<OppgaveTilbakelegging> tilbakelagt = new ArrayList<>();
        tilbakelagt.add(new OppgaveTilbakelegging());
        when(oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(anyString(), anyString())).thenReturn(tilbakelagt);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(FagsakType.FOLKETRYGD.getKode());

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
    public void leggTilbakeOppgave() throws IkkeFunnetException, SikkerhetsbegrensningException, FunksjonellException, TekniskException {
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

        oppgaveplukker.leggTilbakeOppgave(oppgaveId, saksbehandlerID, begrunnelse);
    }
}