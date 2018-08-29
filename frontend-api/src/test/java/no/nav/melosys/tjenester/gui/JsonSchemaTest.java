package no.nav.melosys.tjenester.gui;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.jackson.JacksonModule;
import no.nav.melosys.tjenester.gui.util.JsonResourceLoader;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.springframework.core.io.DefaultResourceLoader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class JsonSchemaTest {

    private static final String ROOT = "schema/";

    private static ObjectMapper objectMapper;

    public abstract String schemaNavn();

    protected Schema hentSchema() throws IOException {
        return hentSchema(schemaNavn());
    }

    protected Schema hentSchema(String schemaNavn) throws IOException {
        String schemaString = JsonResourceLoader.load(new DefaultResourceLoader(), ROOT + schemaNavn);
        return lastSchema(schemaString);
    }

    protected Schema lastSchema(String schemaString) {
        JSONObject rawSchema = new JSONObject(schemaString);
        SchemaLoader loader = SchemaLoader.builder().schemaJson(rawSchema).draftV7Support().useDefaults(true).build();
        return loader.load().build();
    }

    protected ObjectMapper objectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            objectMapper.registerModule(new JavaTimeModule());
            KodeverkService kodeverkService = mock(KodeverkService.class);
            when(kodeverkService.dekod(any(),any(),any())).thenReturn("DUMMY");
            objectMapper.registerModule(new JacksonModule(kodeverkService));
        }
        return objectMapper;
    }
}
