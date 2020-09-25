package no.nav.melosys.tjenester.gui;

import java.io.IOException;

import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringSedDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("resource")
class JournalfoeringTjenesteTest extends JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(JournalfoeringTjenesteTest.class);
    private static final String JOURNALFOERING_SCHEMA = "journalforing-schema.json";
    private static final String JOURNALFOERING_TILORDNE_SCHEMA = "journalforing-tilordne-post-schema.json";
    private static final String JOURNALFOERING_OPPRETT_SCHEMA = "journalforing-opprett-post-schema.json";
    private static final String JOURNALFOERING_SED_SCHEMA = "journalforing-sed-post-schema.json";
    private static final String SAMPLE_ORGNR = "899655123";
    private static final String SAMPLE_FNR = "77777777772";

    private EasyRandom random;

    private JournalfoeringTjeneste tjeneste;
    @Mock
    private JournalfoeringService journalføringService;

    @BeforeEach
    public void setUp() {
        tjeneste = new JournalfoeringTjeneste(journalføringService);

        random = new EasyRandom(new EasyRandomParameters().collectionSizeRange(1, 4));
    }

    @Test
    void hentJournalpostValidering() throws Exception {
        Journalpost journalpost = random.nextObject(Journalpost.class);
        journalpost.setBrukerId(SAMPLE_FNR);
        journalpost.setAvsenderId(SAMPLE_ORGNR);
        when(journalføringService.hentJournalpost(anyString())).thenReturn(journalpost);

        ResponseEntity<JournalpostDto> response = tjeneste.hentJournalpostOpplysninger(anyString());
        JournalpostDto journalpostDto = response.getBody();

        valider(journalpostDto, JOURNALFOERING_SCHEMA, log);
    }

    @Test
    void journalføringTilordneSchemaValidering() throws Exception {
        JournalfoeringTilordneDto journalfoeringDto = random.nextObject(JournalfoeringTilordneDto.class);
        journalfoeringDto.setBrukerID(SAMPLE_FNR);
        journalfoeringDto.setBehandlingstypeKode(Behandlingstyper.ENDRET_PERIODE.getKode());
        valider(journalfoeringDto, JOURNALFOERING_TILORDNE_SCHEMA, log);
    }

    @Test
    void journalføringOpprettSchemaValidering() throws Exception {
        JournalfoeringOpprettDto journalfoeringDto = random.nextObject(JournalfoeringOpprettDto.class);
        journalfoeringDto.setBrukerID(SAMPLE_FNR);
        journalfoeringDto.setBehandlingstemaKode(Behandlingstema.ARBEID_FLERE_LAND.getKode());
        journalfoeringDto.setArbeidsgiverID(SAMPLE_ORGNR);
        journalfoeringDto.setRepresentantID(SAMPLE_ORGNR);
        valider(journalfoeringDto, JOURNALFOERING_OPPRETT_SCHEMA, log);
    }

    @Test
    void journalføringOpprettSchemaValideringMedRepresentantIDNull() throws Exception {
        JournalfoeringOpprettDto journalfoeringDto = random.nextObject(JournalfoeringOpprettDto.class);
        journalfoeringDto.setBrukerID(SAMPLE_FNR);
        journalfoeringDto.setBehandlingstemaKode(Behandlingstema.ARBEID_FLERE_LAND.getKode());
        journalfoeringDto.setArbeidsgiverID(SAMPLE_ORGNR);
        journalfoeringDto.setRepresentantID(null);
        valider(journalfoeringDto, JOURNALFOERING_OPPRETT_SCHEMA, log);
    }

    @Test
    void journalførSedSchemaValidering() throws IOException {
        JournalfoeringSedDto journalfoeringSedDto = new JournalfoeringSedDto();
        journalfoeringSedDto.setOppgaveID("123123");
        journalfoeringSedDto.setBrukerID(SAMPLE_FNR);
        journalfoeringSedDto.setJournalpostID("1231231232");
        valider(journalfoeringSedDto, JOURNALFOERING_SED_SCHEMA, log);
    }

}

