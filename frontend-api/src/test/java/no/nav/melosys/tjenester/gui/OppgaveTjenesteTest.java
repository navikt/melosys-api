package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Response;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.gsak.Oppgavetype;
import no.nav.melosys.service.OppgaveService;
import no.nav.melosys.service.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.SakOgOppgaveDto;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.PlukkOppgaveInnDto;
import no.nav.melosys.tjenester.gui.dto.PlukketOppgaveDto;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppgaveTjenesteTest {

    private OppgaveTjeneste tjeneste;
    @Mock
    private Oppgaveplukker oppgaveplukker;
    @Mock
    private OppgaveService oppgaveService;

    @Before
    public void setUp() {
        tjeneste = new OppgaveTjeneste(oppgaveplukker, oppgaveService);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    public void plukkOppgave() {
        PlukkOppgaveInnDto innData = new PlukkOppgaveInnDto();

        innData.setOppgavetype("BEH_SAK");

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(FagsakType.EU_EØS.getKode());
        innData.setSakstyper(sakstyper);

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(BehandlingType.SØKNAD.getKode());
        innData.setBehandlingstyper(behandlingstyper);

        Oppgave oppgave = new Oppgave();
        oppgave.setOppgaveId("1");
        oppgave.setOppgavetype(Oppgavetype.BEH_SAK_MED);
        Optional<Oppgave> plukket = Optional.of(oppgave);

        when(oppgaveplukker.plukkOppgave(anyString(), anyString(), anyList(), anyList())).thenReturn(plukket);

        Response response = tjeneste.plukkOppgave(innData);

        assertThat(response.getEntity()).isExactlyInstanceOf(PlukketOppgaveDto.class);

        PlukketOppgaveDto entity = (PlukketOppgaveDto) response.getEntity();
        assertThat(entity.getOppgaveID()).isEqualTo("1");
    }

    @Test
    public void mineSaker() {
        SakOgOppgaveDto oppgave = new SakOgOppgaveDto();
        oppgave.setOppgaveId("1");
        List<SakOgOppgaveDto> oppgaver = new ArrayList<>();
        oppgaver.add(oppgave);

        when(oppgaveService.hentMineSaker(anyString())).thenReturn(oppgaver);
        Response response = tjeneste.mineSaker();
        assertThat(response.getEntity()).isExactlyInstanceOf(ArrayList.class);
        List<SakOgOppgaveDto> entity = (List<SakOgOppgaveDto>) response.getEntity();
        assertThat(entity.get(0).getOppgaveId()).isEqualTo("1");
    }

    public void jsonInn() {
        ObjectMapper mapper = new ObjectMapper();

        PlukkOppgaveInnDto innData = new PlukkOppgaveInnDto();

        innData.setOppgavetype("BEH_SAK"); // eller JFR

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(FagsakType.EU_EØS.getKode());
        sakstyper.add(FagsakType.TRYGDEAVTALE.getKode());
        sakstyper.add(FagsakType.FOLKETRYGD.getKode());
        innData.setSakstyper(sakstyper);

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(BehandlingType.SØKNAD.getKode()); // Felleskodeverk finnes
        behandlingstyper.add(BehandlingType.KLAGE.getKode());
        innData.setBehandlingstyper(behandlingstyper);

        try {
            String json = mapper.writeValueAsString(innData);
            System.out.println(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void jsonUt() {
        ObjectMapper mapper = new ObjectMapper();
        PlukketOppgaveDto dto = new PlukketOppgaveDto();
        dto.setOppgaveID("1");
        dto.setOppgavetype("JFR");
        dto.setSaksnummer("123");
        dto.setJournalpostID("JOUR_321");
        try {
            String json = mapper.writeValueAsString(dto);
            System.out.println(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}