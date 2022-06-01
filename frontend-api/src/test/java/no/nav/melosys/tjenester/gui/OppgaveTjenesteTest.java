package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.oppgave.Oppgaveplukker;
import no.nav.melosys.service.oppgave.dto.*;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import no.nav.melosys.tjenester.gui.dto.oppgave.OppgaveOversiktDto;
import no.nav.melosys.tjenester.gui.dto.oppgave.PlukketOppgaveDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OppgaveTjenesteTest extends JsonSchemaTestParent {
    private static final Logger logger = LoggerFactory.getLogger(OppgaveTjenesteTest.class);

    private static final String OPPGAVER_OVERSIKT_SCHEMA = "oppgaver-oversikt-schema.json";
    private static final String OPPGAVER_TILBAKELEGGE_SCHEMA = "oppgaver-tilbakelegg-post-schema.json";
    private static final String OPPGAVER_SOK_SCHEMA = "oppgaver-sok-schema.json";
    private static final String OPPGAVER_PLUKK_SCHEMA = "oppgaver-plukk-schema.json";

    private OppgaveTjeneste oppgaveTjeneste;
    @Mock
    private Oppgaveplukker oppgaveplukker;
    @Mock
    private OppgaveService oppgaveService;

    @BeforeEach
    public void setUp() {
        oppgaveTjeneste = new OppgaveTjeneste(oppgaveplukker, oppgaveService);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    void mineOppgaver() throws IOException {
        List<OppgaveDto> oppgaver = new ArrayList<>();
        int oppgaveNr = 1 + defaultEasyRandom().nextInt(2);
        for (int i = 0; i < oppgaveNr; i++) {
            oppgaver.add(defaultEasyRandom().nextObject(BehandlingsoppgaveDto.class));
            oppgaver.add(defaultEasyRandom().nextObject(JournalfoeringsoppgaveDto.class));
        }
        when(oppgaveService.hentOppgaverMedAnsvarlig(anyString())).thenReturn(oppgaver);

        ResponseEntity<?> response = oppgaveTjeneste.mineOppgaver();

        OppgaveOversiktDto oppgaveOversikt = (OppgaveOversiktDto) response.getBody();
        valider(oppgaveOversikt, OPPGAVER_OVERSIKT_SCHEMA, logger);
    }

    @Test
    void plukkOppgave() throws IOException {
        Behandling behandling = new Behandling();
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        behandling.setId(1L);
        when(oppgaveService.hentSistAktiveBehandling(anyString())).thenReturn(behandling);

        Oppgave.Builder oppgaveBuilder = new Oppgave.Builder();
        oppgaveBuilder.setOppgaveId("1");
        oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        oppgaveBuilder.setSaksnummer("MEl-1");
        oppgaveBuilder.setJournalpostId("123");
        oppgaveBuilder.setOppgavetype(Oppgavetyper.BEH_SAK_MK);
        Optional<Oppgave> plukket = Optional.of(oppgaveBuilder.build());
        PlukkOppgaveInnDto innData = new PlukkOppgaveInnDto();
        innData.setBehandlingstema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
        when(oppgaveplukker.plukkOppgave(anyString(), eq(innData))).thenReturn(plukket);


        ResponseEntity<?> response = oppgaveTjeneste.plukkOppgave(innData);

        assertThat(response.getBody()).isExactlyInstanceOf(PlukketOppgaveDto.class);
        PlukketOppgaveDto entity = (PlukketOppgaveDto) response.getBody();
        valider(entity, OPPGAVER_PLUKK_SCHEMA, logger);
        assertThat(entity.getOppgaveID()).isEqualTo("1");
    }

    @Test
    void søkOppgaverMedBrukerID() throws IOException {
        List<Oppgave> oppgaver = defaultEasyRandom().objects(Oppgave.class, 3).collect(Collectors.toList());
        when(oppgaveService.finnOppgaverMedBrukerID(anyString())).thenReturn(oppgaver);

        validerArray(oppgaveTjeneste.søkOppgaverMedBrukerID("").getBody(), OPPGAVER_SOK_SCHEMA, logger);
    }

    @Test
    void tilbakeleggOppgave() throws Exception {
        TilbakeleggingDto tilbakelegging = defaultEasyRandom().nextObject(TilbakeleggingDto.class);

        assertThat(tilbakelegging).isNotNull();
        valider(tilbakelegging, OPPGAVER_TILBAKELEGGE_SCHEMA, logger);
    }
}
