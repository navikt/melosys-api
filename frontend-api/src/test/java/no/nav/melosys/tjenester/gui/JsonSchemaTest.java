package no.nav.melosys.tjenester.gui;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.jackson.JacksonModule;
import no.nav.melosys.tjenester.gui.util.JsonResourceLoader;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.core.io.DefaultResourceLoader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class JsonSchemaTest {

    private static final String ROOT = "schema/";

    private static ObjectMapper objectMapper;

    private static ObjectMapper objectMapperMedKodeverkServiceStub;

    private static EnhancedRandom enhancedRandom;

    public abstract String schemaNavn();

    protected static EnhancedRandom defaultEnhancedRandom() {
        if (enhancedRandom == null) {
            enhancedRandom =  EnhancedRandomBuilder.aNewEnhancedRandomBuilder()
                .collectionSizeRange(1, 4)
                .build();
        }
        return enhancedRandom;
    }

    protected Schema hentSchema() throws IOException, JSONException {
        return hentSchema(schemaNavn());
    }

    protected Schema hentSchema(String schemaNavn) throws IOException, JSONException {
        String schemaString = JsonResourceLoader.load(new DefaultResourceLoader(), ROOT + schemaNavn);
        return lastSchema(schemaString);
    }

    protected Schema lastSchema(String schemaString) throws JSONException {
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
        }
        return objectMapper;
    }

    protected ObjectMapper objectMapperMedKodeverkServiceStub() throws TekniskException {
        if (objectMapperMedKodeverkServiceStub == null) {
            objectMapperMedKodeverkServiceStub = new ObjectMapper();
            objectMapperMedKodeverkServiceStub.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapperMedKodeverkServiceStub.configure(SerializationFeature.INDENT_OUTPUT, true);
            objectMapperMedKodeverkServiceStub.registerModule(new JavaTimeModule());
            KodeverkService kodeverkService = mock(KodeverkService.class);
            when(kodeverkService.dekod(any(),any(),any())).thenReturn("DUMMY");
            objectMapperMedKodeverkServiceStub.registerModule(new JacksonModule(kodeverkService));
        }
        return objectMapperMedKodeverkServiceStub;
    }
}
