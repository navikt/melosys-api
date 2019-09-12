package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.service.kodeverk.KodeDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.jackson.MelosysModule;
import no.nav.melosys.tjenester.gui.util.JsonResourceLoader;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;

import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public class JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(JsonSchemaTestParent.class);

    private static final String FEILMELDING = "Schemavalidering feilet for schema {}";

    private static ObjectMapper objectMapper;
    private static ObjectMapper objectMapperMedKodeverkServiceStub;
    private static EasyRandom easyRandom;

    protected static EasyRandomParameters defaultEasyRandomParameters() {
        return new EasyRandomParameters()
            .collectionSizeRange(1, 4)
            .overrideDefaultInitialization(true)
            .stringLengthRange(2, 10)
            .randomize(named("fnr").and(ofType(String.class)), new NumericStringRandomizer(11))
            .randomize(named("orgnummer").and(ofType(String.class)), new NumericStringRandomizer(9));
    }

    protected static EasyRandom defaultEasyRandom() {
        if (easyRandom == null) {
            easyRandom = new EasyRandom(defaultEasyRandomParameters());
        }
        return easyRandom;
    }

    protected Schema hentSchema(String schemaNavn) throws IOException, JSONException {
        String schemaString = JsonResourceLoader.load(new DefaultResourceLoader(), schemaNavn);
        return lastSchema(schemaString);
    }

    protected Schema lastSchema(String schemaString) throws JSONException {
        JSONObject rawSchema = new JSONObject(schemaString);
        SchemaLoader loader = SchemaLoader.builder().schemaJson(rawSchema).httpClient(new ClasspathSchemaClient()).draftV7Support().useDefaults(true).build();
        return loader.load().build();
    }

    protected ObjectMapper objectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.registerModule(new MelosysModule(null));
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
            lenient().when(kodeverkService.dekod(any(), any(), any())).thenReturn("DUMMY");
            lenient().when(kodeverkService.getKodeverdi(any(), any())).thenReturn(new KodeDto("DUMMY", "DUMMY"));
            objectMapperMedKodeverkServiceStub.registerModule(new MelosysModule(kodeverkService));
        }
        return objectMapperMedKodeverkServiceStub;
    }

    protected void valider(Object o, String schemaNavn) throws IOException {
        String jsonString = objectMapper().writeValueAsString(o);
        valider(jsonString, schemaNavn, log);
    }

    protected void valider(Object o, String schemaNavn, Logger logger) throws IOException {
        String jsonString = objectMapper().writeValueAsString(o);
        valider(jsonString, schemaNavn, logger);
    }

    protected void valider(String json, String schemaNavn, Logger logger) throws IOException {
        valider(new JSONObject(json), hentSchema(schemaNavn), logger);
    }

    protected void validerArray(Collection liste, String schemaNavn) throws IOException {
        validerArray(liste, schemaNavn, log);
    }

    protected void validerArray(Collection liste, String schemaNavn, Logger logger) throws IOException {
        String json = objectMapper().writeValueAsString(liste);
        valider(new JSONArray(json), hentSchema(schemaNavn), logger);
    }

    private void valider(Object jsonObject, Schema schema, Logger logger) {
        try {
            schema.validate(jsonObject);
        } catch (ValidationException e) {
            formaterFeil(e, schema, logger);
        }
    }

    private void formaterFeil(ValidationException e, Schema schema, Logger logger) {
        logger.error(FEILMELDING, schema.getTitle());
        e.getCausingExceptions().stream()
            .map(ValidationException::toJSON)
            .forEach(jsonObject -> logger.error(jsonObject.toString()));
        throw e;
    }

    private class ClasspathSchemaClient implements SchemaClient {
        public InputStream get(String url) {
            try {
                url = url.replace("http://melosys.nav.no/schemas", "");
                return new ClassPathResource(url).getInputStream();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
