package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.gsak.Oppgavetype;
import no.nav.melosys.service.OppgaveService;
import no.nav.melosys.service.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.KodeverdiDto;
import no.nav.melosys.service.oppgave.dto.PeriodeDto;
import no.nav.melosys.service.oppgave.dto.SakOgOppgaveDto;
import no.nav.melosys.service.oppgave.dto.Behandling;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.PlukkOppgaveInnDto;
import no.nav.melosys.tjenester.gui.dto.PlukketOppgaveDto;
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
        oppgave.setOppgaveId("177057928");
        oppgave.setOppgavetype("journalforing");
        oppgave.setSammensattNavn("GLITRENDE HATT");
        oppgave.setSaksnummer("4");

        Behandling behandling =new Behandling();
        KodeverdiDto sakstype= new KodeverdiDto("EU_EOS","EU/EØS");
        KodeverdiDto type= new KodeverdiDto("todo0003","Påstand fra utenlandsk myndighet");
        KodeverdiDto status = new KodeverdiDto("A","Oversett Kode til Display text");
        oppgave.setSakstype(sakstype);
        behandling.setType(type);
        behandling.setStatus(status);
        oppgave.setBehandling(behandling);
        oppgave.setSakstype(sakstype);

        oppgave.setAktivTil(LocalDate.of(2016,03,30));

        oppgave.setSoknadsperiode(new PeriodeDto(LocalDate.of(2016,01,01),LocalDate.of(2020,01,01)));

        List<SakOgOppgaveDto> oppgaver = new ArrayList<>();
        oppgaver.add(oppgave);

        when(oppgaveService.hentMineSaker(anyString())).thenReturn(oppgaver);
        Response response = tjeneste.mineSaker();
        assertThat(response.getEntity()).isExactlyInstanceOf(ArrayList.class);
        List<SakOgOppgaveDto> entity = (List<SakOgOppgaveDto>) response.getEntity();
        assertThat(entity.get(0).getOppgaveID()).isEqualTo("177057928");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        try {
            assertThat(objectMapper.writeValueAsString(entity)).contains("GLITRENDE HATT");
            assertThat(objectMapper.writeValueAsString(entity)).contains("todo0003");
        } catch (IOException e) {
            e.printStackTrace();
        }
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