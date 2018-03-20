package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Response;

import no.nav.melosys.domain.Oppgave;
import no.nav.melosys.domain.gsak.Oppgavetype;
import no.nav.melosys.domain.gsak.Underkategori;
import no.nav.melosys.service.Oppgaveplukker;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.OppgaveDto;
import no.nav.melosys.tjenester.gui.dto.PlukkOppgaveInnDto;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppgaveTjenesteTest {

    @Mock
    private Oppgaveplukker oppgaveplukker;
    
    private OppgaveTjeneste tjeneste;

    @Before
    public void setUp() {
        tjeneste = new OppgaveTjeneste(oppgaveplukker);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    public void plukkOppgave() {
        PlukkOppgaveInnDto innData = new PlukkOppgaveInnDto();

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Underkategori.BOSTED_MED.toString());
        innData.setSakstyper(sakstyper);

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(Oppgavetype.JFR_MED.toString());
        innData.setBehandlingstyper(behandlingstyper);

        Oppgave oppgave = new Oppgave();
        oppgave.setOppgaveId("1");
        Optional<Oppgave> plukket = Optional.of(oppgave);

        when(oppgaveplukker.plukkOppgave(any(String.class), anyList(), anyList())).thenReturn(plukket);

        Response response = tjeneste.plukkOppgave(innData);

        assertThat(response.getEntity()).isExactlyInstanceOf(OppgaveDto.class);

        OppgaveDto entity = (OppgaveDto) response.getEntity();
        assertThat(entity.getOppgaveId()).isEqualTo("1");

    }

    public void jsonInn() {
        ObjectMapper mapper = new ObjectMapper();

        PlukkOppgaveInnDto innData = new PlukkOppgaveInnDto();

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Underkategori.BOSTED_MED.toString());
        sakstyper.add(Underkategori.MIDL_LOVVALG_MED.toString());
        innData.setSakstyper(sakstyper);

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(Oppgavetype.JFR_MED.toString());
        behandlingstyper.add(Oppgavetype.BEH_SED_MED.toString());
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

        OppgaveDto dto = new OppgaveDto();
        dto.setOppgaveId("1");
        dto.setSaksnummer("123");
        dto.setDokumentID("DOK_321");

        try {
            String json = mapper.writeValueAsString(dto);

            System.out.println(json);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}