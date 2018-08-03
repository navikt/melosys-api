package no.nav.melosys.tjenester.gui;

import java.io.IOException;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import no.nav.melosys.tjenester.gui.util.JsonResourceLoader;
import org.springframework.core.io.DefaultResourceLoader;

public abstract class JsonSchemaTest {

    private static final String ROOT = "scripts/schema/";

    private static final JsonSchemaFactory factory = JsonSchemaFactory.byDefault();

    public abstract String schemaNavn();

    public JsonSchema hentSchema() throws IOException, ProcessingException {
        String schemaString = JsonResourceLoader.load(new DefaultResourceLoader(), ROOT + schemaNavn());
        JsonNode schemaNode = JsonLoader.fromString(schemaString);
        return factory.getJsonSchema(schemaNode);
    }
}
