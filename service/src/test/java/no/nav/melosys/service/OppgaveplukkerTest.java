package no.nav.melosys.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.OppgaveTilbakelegging;
import no.nav.melosys.domain.Oppgavetype;
import no.nav.melosys.domain.gsak.PrioritetType;
import no.nav.melosys.domain.gsak.Underkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppgaveplukkerTest {

    @Mock
    private GsakFasade gsakFasade;

    @Mock
    private OppgaveTilbakeleggingRepository oppgaveTilbakkeleggingRepo;

    private Oppgaveplukker oppgaveplukker;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        this.oppgaveplukker = new Oppgaveplukker(gsakFasade, oppgaveTilbakkeleggingRepo);
    }

    @Test
    public void plukkOppgave_høy_prio() {
        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgaveId("1");
        oppgave1.setPrioritet(PrioritetType.LAV_MED);
        oppgaver.add(oppgave1);
        Oppgave oppgave2 = new Oppgave();
        oppgave2.setOppgaveId("2");
        oppgave2.setPrioritet(PrioritetType.HOY_MED);
        oppgaver.add(oppgave2);
        Oppgave oppgave3 = new Oppgave();
        oppgave3.setOppgaveId("3");
        oppgave3.setPrioritet(PrioritetType.NORM_MED);
        oppgaver.add(oppgave3);

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), anyList(), anyList(), anyList())).thenReturn(oppgaver);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(FagsakType.EU_EØS.getKode());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(BehandlingType.SØKNAD.getKode());

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", Oppgavetype.JFR, sakstyper, behandlingstyper);

        assertThat(oppgave.isPresent()).isTrue();
        assertThat(oppgave.get().getOppgaveId()).isEqualTo("2");
    }

    @Test
    public void plukkOppgave_1_tilbakelagt() {
        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgaveId("1");
        oppgave1.setPrioritet(PrioritetType.NORM_MED);
        oppgaver.add(oppgave1);
        Oppgave oppgave2 = new Oppgave();
        oppgave2.setOppgaveId("2");
        oppgave2.setPrioritet(PrioritetType.NORM_MED);
        oppgaver.add(oppgave2);
        Oppgave oppgave3 = new Oppgave();
        oppgave3.setOppgaveId("3");
        oppgave3.setPrioritet(PrioritetType.NORM_MED);
        oppgaver.add(oppgave3);

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), anyList(), anyList(), anyList())).thenReturn(oppgaver);

        List<OppgaveTilbakelegging> tilbakelagt = new ArrayList<>();
        tilbakelagt.add(new OppgaveTilbakelegging());
        when(oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(anyString(), eq("1"))).thenReturn(tilbakelagt);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(FagsakType.FOLKETRYGD.getKode());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(BehandlingType.REVURDERING.getKode());

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", Oppgavetype.BEH_SAK, sakstyper, behandlingstyper);

        assertThat(oppgave.isPresent()).isTrue();
        assertThat(oppgave.get().getOppgaveId()).isEqualTo("2");
    }

    @Test
    public void plukkOppgave_alle_tilbakelagt() {
        List<Oppgave> oppgaver = new ArrayList<>();
        Oppgave oppgave1 = new Oppgave();
        oppgave1.setOppgaveId("1");
        oppgave1.setPrioritet(PrioritetType.LAV_MED);
        oppgaver.add(oppgave1);
        Oppgave oppgave2 = new Oppgave();
        oppgave2.setOppgaveId("2");
        oppgave2.setPrioritet(PrioritetType.HOY_MED);
        oppgaver.add(oppgave2);
        Oppgave oppgave3 = new Oppgave();
        oppgave3.setOppgaveId("3");
        oppgave3.setPrioritet(PrioritetType.NORM_MED);
        oppgaver.add(oppgave3);

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(any(), anyList(), anyList(), anyList())).thenReturn(oppgaver);

        List<OppgaveTilbakelegging> tilbakelagt = new ArrayList<>();
        tilbakelagt.add(new OppgaveTilbakelegging());
        when(oppgaveTilbakkeleggingRepo.findBySaksbehandlerIdAndOppgaveId(anyString(), anyString())).thenReturn(tilbakelagt);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Underkategori.MIDL_LOVVALG_MED.toString());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(FagsakType.TRYGDEAVTALE.getKode());

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", Oppgavetype.BEH_SAK, sakstyper, behandlingstyper);

        assertThat(oppgave.isPresent()).isFalse();
    }

    @Test
    public void leggTilbakeOppgave() {
        final String oppgaveId = "42";
        final Oppgave oppgave = new Oppgave();
        oppgave.setOppgaveId(oppgaveId);
        oppgave.setPrioritet(PrioritetType.valueOf("HOY_MED"));
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

        final String ugyldigOppgaveId = "13";
        final String forventetFeilmelding = String.format("Fant ikke oppgave med oppgaveId %s", ugyldigOppgaveId);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(forventetFeilmelding);

        oppgaveplukker.leggTilbakeOppgave(ugyldigOppgaveId, saksbehandlerID, begrunnelse);
    }
}