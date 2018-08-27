package no.nav.melosys.tjenester.gui;

import java.io.IOException;

import no.nav.melosys.tjenester.gui.util.JsonResourceLoader;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.springframework.core.io.DefaultResourceLoader;

public abstract class JsonSchemaTest {

    private static final String ROOT = "scripts/schema/";

    public abstract String schemaNavn();

    public Schema hentSchema() throws IOException {
        String schemaString = JsonResourceLoader.load(new DefaultResourceLoader(), ROOT + schemaNavn());
        JSONObject rawSchema = new JSONObject(schemaString);

        SchemaLoader loader = SchemaLoader.builder().schemaJson(rawSchema).draftV7Support().useDefaults(true).build();

        return loader.load().build();
    }
}
