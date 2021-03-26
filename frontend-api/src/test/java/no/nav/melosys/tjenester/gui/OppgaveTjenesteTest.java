package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.*;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.oppgave.OppgaveOversiktDto;
import no.nav.melosys.tjenester.gui.dto.oppgave.PlukketOppgaveDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppgaveTjenesteTest extends JsonSchemaTestParent {
    private static final Logger logger = LoggerFactory.getLogger(OppgaveTjenesteTest.class);

    private static final String OPPGAVER_OVERSIKT_SCHEMA = "oppgaver-oversikt-schema.json";
    private static final String OPPGAVER_TILBAKELEGGE_SCHEMA = "oppgaver-tilbakelegg-post-schema.json";
    private static final String OPPGAVER_SOK_SCHEMA = "oppgaver-sok-schema.json";
    private static final String OPPGAVER_PLUKK_SCHEMA = "oppgaver-plukk-schema.json";
    private static final String OPPGAVER_PLUKK_POST_SCHEMA = "oppgaver-plukk-post-schema.json";

    private OppgaveTjeneste oppgaveTjeneste;
    @Mock
    private Oppgaveplukker oppgaveplukker;
    @Mock
    private OppgaveService oppgaveService;

    @Before
    public void setUp() {
        oppgaveTjeneste = new OppgaveTjeneste(oppgaveplukker, oppgaveService);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    public void mineOppgaver() throws FunksjonellException, TekniskException, IOException {
        List<OppgaveDto> oppgaver = new ArrayList<>();
        int oppgaveNr = 1 + defaultEasyRandom().nextInt(2);
        for (int i = 0; i < oppgaveNr; i++) {
            oppgaver.add(defaultEasyRandom().nextObject(BehandlingsoppgaveDto.class));
            oppgaver.add(defaultEasyRandom().nextObject(JournalfoeringsoppgaveDto.class));
        }

        when(oppgaveService.hentOppgaverMedAnsvarlig(anyString())).thenReturn(oppgaver);
        ResponseEntity response = oppgaveTjeneste.mineOppgaver();

        OppgaveOversiktDto oppgaveOversikt = (OppgaveOversiktDto) response.getBody();
        valider(oppgaveOversikt, OPPGAVER_OVERSIKT_SCHEMA, logger);
    }

    @Test
    public void plukkOppgave() throws FunksjonellException, TekniskException, IOException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setId(1L);
        when(oppgaveService.hentSistAktiveBehandling(anyString())).thenReturn(behandling);

        PlukkOppgaveInnDto innData = new PlukkOppgaveInnDto();

        String behandlingstema = behandling.getTema().getKode();
        innData.setBehandlingstema(behandlingstema);

        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgaveBuilder.setSaksnummer("MEl-1");
        oppgaveBuilder.setJournalpostId("123");
        oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        Optional<Oppgave> plukket = Optional.of(oppgaveBuilder.build());

        when(oppgaveplukker.plukkOppgave(anyString(), eq(innData))).thenReturn(plukket);

        valider(innData, OPPGAVER_PLUKK_POST_SCHEMA, logger);

        ResponseEntity response = oppgaveTjeneste.plukkOppgave(innData);

        assertThat(response.getBody()).isExactlyInstanceOf(PlukketOppgaveDto.class);

        PlukketOppgaveDto entity = (PlukketOppgaveDto) response.getBody();
        valider(entity, OPPGAVER_PLUKK_SCHEMA, logger);

        assertThat(entity.getOppgaveID()).isEqualTo("1");
    }

    @Test
    public void søkOppgaverMedBrukerID() throws FunksjonellException, TekniskException, IOException {
        List<Oppgave> oppgaver = defaultEasyRandom().objects(Oppgave.class, 3).collect(Collectors.toList());
        when(oppgaveService.finnOppgaverMedBrukerID(anyString())).thenReturn(oppgaver);

        validerArray((List<no.nav.melosys.tjenester.gui.dto.oppgave.OppgaveDto>) oppgaveTjeneste.søkOppgaverMedBrukerID("").getBody(), OPPGAVER_SOK_SCHEMA, logger);
    }

    @Test
    public void tilbakeleggOppgave() throws Exception {
        TilbakeleggingDto tilbakelegging = defaultEasyRandom().nextObject(TilbakeleggingDto.class);

        assertThat(tilbakelegging).isNotNull();

        valider(tilbakelegging, OPPGAVER_TILBAKELEGGE_SCHEMA, logger);
    }
}