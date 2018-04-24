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
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.gsak.Oppgavetype;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.BehandlingDto;
import no.nav.melosys.service.oppgave.dto.PeriodeDto;
import no.nav.melosys.service.oppgave.dto.OppgaveDto;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.PlukkOppgaveInnDto;
import no.nav.melosys.tjenester.gui.dto.PlukketOppgaveDto;
import no.nav.melosys.tjenester.gui.jackson.JacksonModule;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

        when(oppgaveplukker.plukkOppgave(anyString(), any(), anyList(), anyList())).thenReturn(plukket);

        Response response = tjeneste.plukkOppgave(innData);

        assertThat(response.getEntity()).isExactlyInstanceOf(PlukketOppgaveDto.class);

        PlukketOppgaveDto entity = (PlukketOppgaveDto) response.getEntity();
        assertThat(entity.getOppgaveID()).isEqualTo("1");
    }

    @Test
    public void mineOppgaver() {
        OppgaveDto oppgave = new OppgaveDto();
        oppgave.setOppgaveID("177057928");
        oppgave.setOppgavetype(no.nav.melosys.domain.Oppgavetype.JFR);
        oppgave.setSammensattNavn("GLITRENDE HATT");
        oppgave.setSaksnummer("4");

        BehandlingDto behandlingDto =new BehandlingDto();
        FagsakType sakstype= FagsakType.EU_EØS;
        BehandlingType type= BehandlingType.PÅSTAND_UTL;
        BehandlingStatus status = BehandlingStatus.FORELØPIG;
        oppgave.setSakstype(sakstype);
        behandlingDto.setType(type);
        behandlingDto.setStatus(status);
        oppgave.setBehandling(behandlingDto);
        oppgave.setSakstype(sakstype);

        oppgave.setAktivTil(LocalDate.of(2016,03,30));

        oppgave.setSoknadsperiode(new PeriodeDto(LocalDate.of(2016,01,01),LocalDate.of(2020,01,01)));

        List<OppgaveDto> oppgaver = new ArrayList<>();
        oppgaver.add(oppgave);

        when(oppgaveService.hentOppgaverMedAnsvarlig(anyString())).thenReturn(oppgaver);
        Response response = tjeneste.mineOppgaver();
        assertThat(response.getEntity()).isExactlyInstanceOf(ArrayList.class);
        List<OppgaveDto> entity = (List<OppgaveDto>) response.getEntity();
        assertThat(entity.get(0).getOppgaveID()).isEqualTo("177057928");

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.registerModule(new JacksonModule(null));

        try {
            String json = objectMapper.writeValueAsString(entity);
            assertThat(json).contains("GLITRENDE HATT");
            assertThat(json).contains("PS_U");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void jsonInn() {
        ObjectMapper mapper = new ObjectMapper();

        PlukkOppgaveInnDto innData = new PlukkOppgaveInnDto();

        innData.setOppgavetype(no.nav.melosys.domain.Oppgavetype.BEH_SAK.getKode()); // eller JFR

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
        dto.setOppgavetype(no.nav.melosys.domain.Oppgavetype.JFR.getKode());
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