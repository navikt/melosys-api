package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.benas.randombeans.EnhancedRandomBuilder;
import io.github.benas.randombeans.api.EnhancedRandom;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.kodeverk.KodeDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.jackson.JacksonModule;
import no.nav.melosys.tjenester.gui.jackson.serialize.MedlemsperiodeSerializer;
import no.nav.melosys.tjenester.gui.util.JsonResourceLoader;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.DefaultResourceLoader;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public abstract class JsonSchemaTest {

    private static final Logger log = LoggerFactory.getLogger(JsonSchemaTest.class);

    private static final String ROOT = "schema/";

    protected static final String FEILMELDING = "Schemavalidering feilet for schema {}";

    private static ObjectMapper objectMapper;

    private static ObjectMapper objectMapperMedKodeverkServiceStub;

    private static EnhancedRandom enhancedRandom;

    public Logger getLogger() {
        return log;
    }

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

    protected ObjectMapper objectMapperMedKodeverkServiceStub() {
        if (objectMapperMedKodeverkServiceStub == null) {
            objectMapperMedKodeverkServiceStub = new ObjectMapper();
            objectMapperMedKodeverkServiceStub.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapperMedKodeverkServiceStub.configure(SerializationFeature.INDENT_OUTPUT, true);
            objectMapperMedKodeverkServiceStub.registerModule(new JavaTimeModule());
            KodeverkService kodeverkService = mock(KodeverkService.class);
            when(kodeverkService.dekod(any(), any(), any())).thenReturn("DUMMY");
            when(kodeverkService.getKodeverdi(any(), any())).thenReturn(new KodeDto("DUMMY", "DUMMY"));
            objectMapperMedKodeverkServiceStub.registerModule(new JacksonModule(kodeverkService));
            SimpleModule simpleModule = new SimpleModule().addSerializer(new MedlemsperiodeSerializer(kodeverkService));
            objectMapperMedKodeverkServiceStub.registerModule(simpleModule);
        }
        return objectMapperMedKodeverkServiceStub;
    }

    protected void valider(Object o) throws IOException {
        valider(o, getLogger());
    }

    protected void valider(Object o, Logger log) throws IOException {
        String jsonInString = objectMapper().writeValueAsString(o);
        valider(jsonInString, log);
    }

    protected void valider(String s, Logger log) throws IOException {
        try {
            Schema schema = hentSchema();
            schema.validate(new JSONObject(s));
        } catch (ValidationException e) {
            formaterFeil(e, log);
        }
    }

    protected void validerListe(Collection liste) throws IOException {
        validerListe(liste, getLogger());
    }

    protected void validerListe(Collection liste, Logger log) throws IOException {
        String json = objectMapper().writeValueAsString(liste);
        try {
            Schema schema = hentSchema();
            schema.validate(new JSONArray(json));
        } catch (ValidationException e) {
            formaterFeil(e, log);
        }
    }

    private void formaterFeil(ValidationException e, Logger log) {
        log.error(FEILMELDING, schemaNavn());
        e.getCausingExceptions().stream()
            .map(ValidationException::toJSON)
            .forEach(jsonObject -> log.error(jsonObject.toString()));
        throw e;
    }
}
