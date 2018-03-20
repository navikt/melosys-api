package no.nav.melosys.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.OppgaveTilbakelegging;
import no.nav.melosys.domain.gsak.PrioritetType;
import no.nav.melosys.domain.gsak.Underkategori;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.OppgaveTilbakeleggingRepository;
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

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(anyString(), anyList(), anyList(), anyList())).thenReturn(oppgaver);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(FagsakType.EU_EØS.getKode());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(BehandlingType.SØKNAD.getKode());

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", "JFR", sakstyper, behandlingstyper);

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

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(anyString(), anyList(), anyList(), anyList())).thenReturn(oppgaver);

        List<OppgaveTilbakelegging> tilbakelagt = new ArrayList<>();
        tilbakelagt.add(new OppgaveTilbakelegging());
        when(oppgaveTilbakkeleggingRepo.findBySaksbehandlerAndOppgaveId(anyString(), eq("1"))).thenReturn(tilbakelagt);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(FagsakType.FOLKETRYGD.getKode());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(BehandlingType.REVURDERING.getKode());

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", "BEH_SAK", sakstyper, behandlingstyper);

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

        when(gsakFasade.finnUtildelteOppgaverEtterFrist(anyString(), anyList(), anyList(), anyList())).thenReturn(oppgaver);

        List<OppgaveTilbakelegging> tilbakelagt = new ArrayList<>();
        tilbakelagt.add(new OppgaveTilbakelegging());
        when(oppgaveTilbakkeleggingRepo.findBySaksbehandlerAndOppgaveId(anyString(), anyString())).thenReturn(tilbakelagt);

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Underkategori.MIDL_LOVVALG_MED.toString());

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(FagsakType.TRYGDEAVTALE.getKode());

        Optional<Oppgave> oppgave = oppgaveplukker.plukkOppgave("Z01234", "BEH_SAK", sakstyper, behandlingstyper);

        assertThat(oppgave.isPresent()).isFalse();
    }

    @Test
    public void leggTilbakeOppgave() {
        final Oppgave oppgave = new Oppgave();
        oppgave.setOppgaveId("7");
        oppgave.setPrioritet(PrioritetType.valueOf("HOY_MED"));
        final String saksnummer = "42";
        final String saksbehandlerID = "test";
        final String begrunnelse = "Oppgaven er kjedelig";

        when(gsakFasade.finnOppgaveMedSaksnummerOgSaksbehandler(saksnummer, saksbehandlerID)).thenReturn(oppgave);
        when(oppgaveTilbakkeleggingRepo.save(any(OppgaveTilbakelegging.class))).then(arguments -> {
            OppgaveTilbakelegging oppgaveTilbakelegging = arguments.getArgument(0);
            assertThat(oppgaveTilbakelegging.getOppgaveId()).isEqualTo("7");
            assertThat(oppgaveTilbakelegging.getSaksbehandlerId()).isEqualTo("test");
            assertThat(oppgaveTilbakelegging.getBegrunnelse()).isEqualTo("Oppgaven er kjedelig");
            return oppgaveTilbakelegging;
        }).getMock();

        oppgaveplukker.leggTilbakeOppgave(saksnummer, saksbehandlerID, begrunnelse);

        final String ugyldigSaksnummer = "13";
        final String forventetFeilmelding = String.format("Fant ikke oppgave med saksnummer %s og saksbehandlerID %s", ugyldigSaksnummer, saksbehandlerID);

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage(forventetFeilmelding);

        oppgaveplukker.leggTilbakeOppgave(ugyldigSaksnummer, saksbehandlerID, begrunnelse);
    }
}