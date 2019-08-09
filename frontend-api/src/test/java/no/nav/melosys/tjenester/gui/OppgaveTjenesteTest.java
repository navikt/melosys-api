package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.core.Response;

import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.*;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.oppgave.OppgaveOversiktDto;
import no.nav.melosys.tjenester.gui.dto.oppgave.PlukketOppgaveDto;
import org.json.JSONException;
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
public class OppgaveTjenesteTest extends JsonSchemaTestParent {
    private static final Logger logger = LoggerFactory.getLogger(OppgaveTjenesteTest.class);

    private static final String OPPGAVER_OVERSIKT_SCHEMA = "oppgaver-oversikt-schema.json";
    private static final String OPPGAVER_TILBAKELEGGE_SCHEMA = "oppgaver-tilbakelegge-schema.json";
    private static final String OPPGAVER_SOK_SCHEMA = "oppgaver-sok-schema.json";
    private static final String OPPGAVER_PLUKK_POST_RESPONSE_SCHEMA = "oppgaver-plukk-post-response-schema.json";
    private static final String OPPGAVER_PLUKK_POST_SCHEMA = "oppgaver-plukk-post-schema.json";

    private String schemaType;

    private OppgaveTjeneste tjeneste;
    @Mock
    private Oppgaveplukker oppgaveplukker;
    @Mock
    private OppgaveService oppgaveService;

    @Override
    public String schemaNavn() {
        return schemaType;
    }

    @Before
    public void setUp() {
        tjeneste = new OppgaveTjeneste(oppgaveplukker, oppgaveService);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    public void mineOppgaver() throws MelosysException, IOException, JSONException {
        List<OppgaveDto> oppgaver = new ArrayList<>();
        int oppgaveNr = 1 + defaultEasyRandom().nextInt(2);
        for (int i = 0; i < oppgaveNr; i++) {
            oppgaver.add(defaultEasyRandom().nextObject(BehandlingsoppgaveDto.class));
            oppgaver.add(defaultEasyRandom().nextObject(JournalfoeringsoppgaveDto.class));
        }

        when(oppgaveService.hentOppgaverMedAnsvarlig(anyString())).thenReturn(oppgaver);
        Response response = tjeneste.mineOppgaver();

        OppgaveOversiktDto oppgaveOversikt = (OppgaveOversiktDto) response.getEntity();
        valider(OPPGAVER_OVERSIKT_SCHEMA, oppgaveOversikt, logger);
    }

    @Test
    public void plukkOppgave() throws FunksjonellException, TekniskException, IOException {
        PlukkOppgaveInnDto innData = new PlukkOppgaveInnDto();

        innData.setOppgavetype("BEH_SAK_MK");

        List<String> sakstyper = new ArrayList<>();
        sakstyper.add(Sakstyper.EU_EOS.getKode());
        innData.setSakstyper(sakstyper);

        List<String> behandlingstyper = new ArrayList<>();
        behandlingstyper.add(Behandlingstyper.SOEKNAD.getKode());
        innData.setBehandlingstyper(behandlingstyper);

        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgaveBuilder.setSaksnummer("MEl-1");
        oppgaveBuilder.setJournalpostId("123");
        oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        Optional<Oppgave> plukket = Optional.of(oppgaveBuilder.build());

        when(oppgaveplukker.plukkOppgave(anyString(), eq(innData))).thenReturn(plukket);

        schemaType = OPPGAVER_PLUKK_POST_SCHEMA;
        valider(innData);

        Response response = tjeneste.plukkOppgave(innData);

        assertThat(response.getEntity()).isExactlyInstanceOf(PlukketOppgaveDto.class);

        PlukketOppgaveDto entity = (PlukketOppgaveDto) response.getEntity();
        schemaType = OPPGAVER_PLUKK_POST_RESPONSE_SCHEMA;
        valider(entity);

        assertThat(entity.getOppgaveID()).isEqualTo("1");
    }

    @Test
    public void tilbakeleggOppgave() throws IOException {
        TilbakeleggingDto tilbakelegging = defaultEasyRandom().nextObject(TilbakeleggingDto.class);

        assertThat(tilbakelegging).isNotNull();

        schemaType = OPPGAVER_TILBAKELEGGE_SCHEMA;
        valider(tilbakelegging);
    }

    @Test
    public void sokEtterBehandlingsoppgave() throws FunksjonellException, TekniskException, IOException {
        BehandlingsoppgaveDto behandlingsoppgaveDto = defaultEasyRandom().nextObject(BehandlingsoppgaveDto.class);
        List<BehandlingsoppgaveDto> oppgaver = Arrays.asList(behandlingsoppgaveDto);

        when(oppgaveService.hentBehandlingsoppgaverMedBruker(anyString())).thenReturn(oppgaver);

        schemaType = OPPGAVER_SOK_SCHEMA;

        List<BehandlingsoppgaveDto> oppgave = (List<BehandlingsoppgaveDto>) tjeneste.hentOppgaver("").getEntity();
        assertThat(oppgave).isNotNull();
        validerListe(oppgave);
    }
}