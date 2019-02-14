package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import javax.ws.rs.core.Response;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
import no.nav.melosys.tjenester.gui.dto.journalforing.JournalpostDto;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("resource")
public class JournalfoeringTjenesteTest extends JsonSchemaTest {

    private static final Logger log = LoggerFactory.getLogger(JournalfoeringTjenesteTest.class);

    private static final String JOURNALFOERING_SCHEMA = "journalforing-schema.json";
    private static final String JOURNALFOERING_TILORDNE_SCHEMA = "journalforing-tilordne-schema.json";
    private static final String JOURNALFOERING_OPPRETT_SCHEMA = "journalforing-opprett-schema.json";
    private static final String SAMPLE_ORGNR = "899655123";
    private static final String SAMPLE_FNR = "77777777772";

    private EnhancedRandom random;

    private String schemaType;

    private JournalfoeringTjeneste tjeneste;
    @Mock
    private JournalfoeringService journalføringService;

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    public String schemaNavn() {
        return schemaType;
    }

    @Before
    public void setUp() {
        tjeneste = new JournalfoeringTjeneste(journalføringService);

        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
            .collectionSizeRange(1, 4).build();
    }

    @Test
    public void journalPostSchemaValidering() throws IOException, MelosysException, JSONException {
        Journalpost journalpost = random.nextObject(Journalpost.class);
        journalpost.setBrukerId(SAMPLE_FNR);
        journalpost.setAvsenderId(SAMPLE_ORGNR);
        when(journalføringService.hentJournalpost(anyString())).thenReturn(journalpost);

        Response response = tjeneste.hentJournalpostOpplysninger(anyString());
        JournalpostDto journalpostDto = (JournalpostDto) response.getEntity();

        schemaType = JOURNALFOERING_SCHEMA;
        valider(journalpostDto);
    }

    @Test
    public void journalføringTilordneSchemaValidering() throws IOException, JSONException {
        JournalfoeringTilordneDto journalfoeringDto = random.nextObject(JournalfoeringTilordneDto.class);
        schemaType = JOURNALFOERING_TILORDNE_SCHEMA;
        valider(journalfoeringDto);
    }

    @Test
    public void journalføringOpprettSchemaValidering() throws IOException, JSONException {
        JournalfoeringOpprettDto journalfoeringDto = random.nextObject(JournalfoeringOpprettDto.class);
        journalfoeringDto.setArbeidsgiverID(SAMPLE_ORGNR);
        journalfoeringDto.setRepresentantID(SAMPLE_ORGNR);
        schemaType = JOURNALFOERING_OPPRETT_SCHEMA;
        valider(journalfoeringDto);
    }

    @Test
    public void journalføringOpprettSchemaValideringMedRepresentantIDNull() throws IOException, JSONException {
        JournalfoeringOpprettDto journalfoeringDto = random.nextObject(JournalfoeringOpprettDto.class);
        journalfoeringDto.setArbeidsgiverID(SAMPLE_ORGNR);
        journalfoeringDto.setRepresentantID(null);
        schemaType = JOURNALFOERING_OPPRETT_SCHEMA;
        valider(journalfoeringDto);
    }

}

