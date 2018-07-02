package no.nav.melosys.tjenester.gui;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import no.nav.melosys.tjenester.gui.dto.KodeverkDto;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class KodeverkTjenesteTest extends JsonSchemaTest {

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
    public void getKodeverk() throws IOException, ProcessingException {
        JsonSchema schema = hentSchema();
        KodeverkDto kodeverkDto = tjeneste.getKodeverk();
        JsonNode testNode = new ObjectMapper().valueToTree(kodeverkDto);

        ProcessingReport report = schema.validate(testNode);
        assertThat(report.isSuccess());
    }
}