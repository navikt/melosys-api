package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.core.Response;

import no.nav.melosys.domain.Behandlingstype;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.domain.oppgave.Oppgavetype;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.OppgaveDto;
import no.nav.melosys.service.oppgave.dto.PlukkOppgaveInnDto;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.OppgaveOversiktDto;
import no.nav.melosys.tjenester.gui.dto.PlukketOppgaveDto;
import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("resource")
public class OppgaveTjenesteTest extends JsonSchemaTest {

    private static final Logger logger = LoggerFactory.getLogger(FagsakTjenesteTest.class);

    private OppgaveTjeneste tjeneste;
    @Mock
    private Oppgaveplukker oppgaveplukker;
    @Mock
    private OppgaveService oppgaveService;

    @Override
    public String schemaNavn() {
        return "oppgaver-schema.json";
    }

    @Before
    public void setUp() {
        tjeneste = new OppgaveTjeneste(oppgaveplukker, oppgaveService);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    public void mineOppgaver() throws MelosysException, IOException, JSONException {
        List<OppgaveDto> oppgaver = new ArrayList<>();
        int oppgaveNr = 1 + defaultEnhancedRandom().nextInt(3);
        for (int i = 0; i < oppgaveNr; i++) {
            OppgaveDto oppgaveDto = defaultEnhancedRandom().nextObject(OppgaveDto.class);
            oppgaver.add(oppgaveDto);
        }

        when(oppgaveService.hentOppgaverMedAnsvarlig(anyString())).thenReturn(oppgaver);
        Response response = tjeneste.mineOppgaver();

        OppgaveOversiktDto oppgaveOversikt = (OppgaveOversiktDto) response.getEntity();

        String jsonString = objectMapper().writeValueAsString(oppgaveOversikt);

        try {
            hentSchema().validate(new JSONObject(jsonString));
        } catch (ValidationException e) {
            logger.error(e.toJSON().toString());
            throw e;
        }
    }

    @Test
    public void plukkOppgave() throws IkkeFunnetException, SikkerhetsbegrensningException, FunksjonellException, TekniskException {
        PlukkOppgaveInnDto innData = new PlukkOppgaveInnDto();

        innData.setOppgavetype("BEH_SAK");

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(FagsakType.EU_EØS.getKode());
        innData.setSakstyper(sakstyper);

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(Behandlingstype.SØKNAD.getKode());
        innData.setBehandlingstyper(behandlingstyper);

        Oppgave oppgave = new Oppgave();
        oppgave.setOppgaveId("1");
        oppgave.setOppgavetype(Oppgavetype.BEH_SAK);
        Optional<Oppgave> plukket = Optional.of(oppgave);

        when(oppgaveplukker.plukkOppgave(anyString(), eq(innData))).thenReturn(plukket);

        Response response = tjeneste.plukkOppgave(innData);

        assertThat(response.getEntity()).isExactlyInstanceOf(PlukketOppgaveDto.class);

        PlukketOppgaveDto entity = (PlukketOppgaveDto) response.getEntity();
        assertThat(entity.getOppgaveID()).isEqualTo("1");
    }
}