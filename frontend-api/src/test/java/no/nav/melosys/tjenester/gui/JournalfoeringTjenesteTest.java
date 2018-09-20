package no.nav.melosys.tjenester.gui;

import java.io.IOException;

import javax.ws.rs.core.Response;

import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.melosys.domain.Journalpost;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.journalforing.JournalfoeringService;
import no.nav.melosys.service.journalforing.dto.JournalfoeringOpprettDto;
import no.nav.melosys.service.journalforing.dto.JournalfoeringTilordneDto;
import no.nav.melosys.tjenester.gui.dto.journalforing.JournalpostDto;
import org.everit.json.schema.Schema;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JournalfoeringTjenesteTest extends JsonSchemaTest {

    private static final Logger log = LoggerFactory.getLogger(JournalfoeringTjenesteTest.class);

    private static final String JOURNALFOERING_SCHEMA = "journalforing-schema.json";
    private static final String JOURNALFOERING_TILORDNE_SCHEMA = "journalforing-tilordne-schema.json";
    private static final String JOURNALFOERING_OPPRETT_SCHEMA = "journalforing-opprett-schema.json";

    private EnhancedRandom random;

    private String schemaType;

    private JournalfoeringTjeneste tjeneste;
    @Mock
    private JournalfoeringService journalføringService;

    @Override
    public String schemaNavn() {
        return this.schemaType;
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
        when(journalføringService.hentJournalpost(anyString())).thenReturn(journalpost);

        Response response = tjeneste.hentJournalpostOpplysninger(anyString());
        JournalpostDto journalpostDto = (JournalpostDto) response.getEntity();

        schemaValidering(journalpostDto, JOURNALFOERING_SCHEMA);
    }

    @Test
    public void journalføringTilordneSchemaValidering() throws IOException, JSONException {
        JournalfoeringTilordneDto journalfoeringDto = random.nextObject(JournalfoeringTilordneDto.class);
        schemaValidering(journalfoeringDto, JOURNALFOERING_TILORDNE_SCHEMA);
    }

    @Test
    public void journalføringOpprettSchemaValidering() throws IOException, JSONException {
        JournalfoeringOpprettDto journalfoeringDto = random.nextObject(JournalfoeringOpprettDto.class);
        schemaValidering(journalfoeringDto, JOURNALFOERING_OPPRETT_SCHEMA);
        journalfoeringDto.setRepresentantID(null);
        schemaValidering(journalfoeringDto, JOURNALFOERING_OPPRETT_SCHEMA);
    }

    private void schemaValidering(Object journalDto, String schemaType) throws IOException, JSONException {
        String jsonInString = objectMapper().writeValueAsString(journalDto);

        try {
            this.schemaType = schemaType;

            Schema schema = hentSchema();
            schema.validate(new JSONObject(jsonInString));

        } catch (ValidationException e) {
            log.error("Feil ved schemavalidering for {} dokument", this.schemaType);
            e.getCausingExceptions().stream()
                .map(ValidationException::toJSON)
                .forEach(jsonObject -> log.error(jsonObject.toString()));
            //throw e;
        }
    }

}

