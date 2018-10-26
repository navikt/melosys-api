package no.nav.melosys.tjenester.gui;

import java.io.IOException;

import no.nav.melosys.tjenester.gui.dto.KodeverkDto;
import org.everit.json.schema.ValidationException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KodeverkTjenesteTest extends JsonSchemaTest {

    private static final Logger logger = LoggerFactory.getLogger(KodeverkTjenesteTest.class);

    private KodeverkTjeneste tjeneste;

    @Before
    public void setUp() {
        tjeneste = new KodeverkTjeneste();
    }

    @Override
    public String schemaNavn() {
        return "kodeverk-schema.json";
    }

    @Test
    public void getKodeverk() throws IOException, JSONException {
        KodeverkDto kodeverkDto = tjeneste.getKodeverk();
        String jsonString = objectMapper().writeValueAsString(kodeverkDto);

        try {
            hentSchema().validate(new JSONObject(jsonString));
        } catch (ValidationException e) {
            logger.error(e.toJSON().toString());
            throw e;
        }
    }
}