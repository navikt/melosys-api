package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Optional;

import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringSedDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.tjenester.gui.dto.journalforing.JournalpostDto;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalfoeringTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(JournalfoeringTjenesteTest.class);
    private static final String JOURNALFOERING_SCHEMA = "journalforing-schema.json";
    private static final String JOURNALFOERING_TILORDNE_SCHEMA = "journalforing-tilordne-post-schema.json";
    private static final String JOURNALFOERING_OPPRETT_SCHEMA = "journalforing-opprett-post-schema.json";
    private static final String JOURNALFOERING_SED_SCHEMA = "journalforing-sed-post-schema.json";
    private static final String SAMPLE_ORGNR = "899655123";
    private static final String SAMPLE_FNR = "77777777772";

    private EasyRandom random;

    @Mock
    private JournalfoeringService journalføringService;
    @Mock
    private OppgaveService oppgaveService;

    private JournalfoeringTjeneste tjeneste;

    @BeforeEach
    public void setUp() {
        tjeneste = new JournalfoeringTjeneste(journalføringService, oppgaveService);

        random = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));
    }

    @Test
    void hentJournalpost_validerSchema() throws IOException {
        Journalpost journalpost = random.nextObject(Journalpost.class);
        journalpost.setAvsenderId(SAMPLE_ORGNR);
        when(journalføringService.hentJournalpost(anyString())).thenReturn(journalpost);
        when(journalføringService.finnBrukerIdent(journalpost)).thenReturn(Optional.of(SAMPLE_FNR));

        ResponseEntity<JournalpostDto> response = tjeneste.hentJournalpostOpplysninger(anyString());
        JournalpostDto journalpostDto = response.getBody();

        valider(journalpostDto, JOURNALFOERING_SCHEMA, log);
    }

    @Test
    void journalføringTilordne_validerKallOgSchema() throws IOException {
        JournalfoeringTilordneDto journalføringDto = random.nextObject(JournalfoeringTilordneDto.class);
        journalføringDto.setBrukerID(SAMPLE_FNR);
        journalføringDto.setBehandlingstypeKode(Behandlingstyper.ENDRET_PERIODE.getKode());

        tjeneste.journalførOgTilordneSak(journalføringDto);

        verify(journalføringService).journalførOgTilordneSak(journalføringDto);
        verify(oppgaveService).ferdigstillOppgave(journalføringDto.getOppgaveID());
        valider(journalføringDto, JOURNALFOERING_TILORDNE_SCHEMA, log);
    }

    @Test
    void journalføringOpprett_validerKallOgSchema() throws IOException {
        JournalfoeringOpprettDto journalføringDto = random.nextObject(JournalfoeringOpprettDto.class);
        journalføringDto.setBrukerID(SAMPLE_FNR);
        journalføringDto.setBehandlingstemaKode(Behandlingstema.ARBEID_FLERE_LAND.getKode());
        journalføringDto.setArbeidsgiverID(SAMPLE_ORGNR);
        journalføringDto.setRepresentantID(SAMPLE_ORGNR);

        tjeneste.journalførOgOpprettSak(journalføringDto);

        verify(journalføringService).journalførOgOpprettSak(journalføringDto);
        verify(oppgaveService).ferdigstillOppgave(journalføringDto.getOppgaveID());
        valider(journalføringDto, JOURNALFOERING_OPPRETT_SCHEMA, log);
    }

    @Test
    void journalføringOpprett_validerSchemaMedRepresentantIDNull() throws IOException {
        JournalfoeringOpprettDto journalføringDto = random.nextObject(JournalfoeringOpprettDto.class);
        journalføringDto.setBrukerID(SAMPLE_FNR);
        journalføringDto.setBehandlingstemaKode(Behandlingstema.ARBEID_FLERE_LAND.getKode());
        journalføringDto.setArbeidsgiverID(SAMPLE_ORGNR);
        journalføringDto.setRepresentantID(null);
        valider(journalføringDto, JOURNALFOERING_OPPRETT_SCHEMA, log);
    }

    @Test
    void journalførSed_validerSchema() throws IOException {
        JournalfoeringSedDto journalføringSedDto = new JournalfoeringSedDto();
        journalføringSedDto.setOppgaveID("123123");
        journalføringSedDto.setBrukerID(SAMPLE_FNR);
        journalføringSedDto.setJournalpostID("1231231232");

        tjeneste.journalførSed(journalføringSedDto);

        verify(journalføringService).journalførSed(journalføringSedDto);
        verify(oppgaveService).ferdigstillOppgave(journalføringSedDto.getOppgaveID());
        valider(journalføringSedDto, JOURNALFOERING_SED_SCHEMA, log);
    }
}

