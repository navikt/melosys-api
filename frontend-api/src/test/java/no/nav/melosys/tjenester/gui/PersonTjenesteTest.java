package no.nav.melosys.tjenester.gui;

import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.melosys.tjenester.gui.dto.PersonDto;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
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
public class PersonTjenesteTest extends JsonSchemaTestParent {

    private static final Logger log = LoggerFactory.getLogger(PersonTjenesteTest.class);

    private static final String schemaType = "person-schema.json";

    private EnhancedRandom random;

    @Mock
    private PersonTjeneste personTjeneste;

    @Before
    public void setUp() throws Exception {
        random = EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .overrideDefaultInitialization(true)
                .collectionSizeRange(1, 4)
                .build();

        PersonDto person = random.nextObject(PersonDto.class);
        when(personTjeneste.getPerson(anyString())).thenReturn(Response.ok(person).build());
    }

    @Override
    public String schemaNavn() {
        return schemaType;
    }

    @Test
    public void personSchemaValidering() throws Exception {
        Response person = personTjeneste.getPerson("12345678910");
        ObjectMapper mapper = objectMapperMedKodeverkServiceStub();
        String jsonInString = mapper.writeValueAsString(person.getEntity());

        try {
            Schema schema = hentSchema();
            schema.validate(new JSONObject(jsonInString));

        } catch (ValidationException e) {
            log.error("Feil ved validering schema for person");
            e.getCausingExceptions().stream()
                    .map(ValidationException::toJSON)
                    .forEach(jsonObject -> log.error(jsonObject.toString()));
            throw e;
        }
    }
}
