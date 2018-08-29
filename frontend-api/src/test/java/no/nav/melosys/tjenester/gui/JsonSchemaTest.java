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

    public abstract String schemaNavn();

    protected Schema hentSchema() throws IOException {
        String schemaString = JsonResourceLoader.load(new DefaultResourceLoader(), ROOT + schemaNavn());
        JSONObject rawSchema = new JSONObject(schemaString);

        SchemaLoader loader = SchemaLoader.builder().schemaJson(rawSchema).draftV7Support().useDefaults(true).build();

        return loader.load().build();
    }

    protected ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.registerModule(new JavaTimeModule());
        KodeverkService kodeverkService = mock(KodeverkService.class);
        when(kodeverkService.dekod(any(),any(),any())).thenReturn("DUMMY");
        mapper.registerModule(new JacksonModule(kodeverkService));
        return mapper;
    }
}
