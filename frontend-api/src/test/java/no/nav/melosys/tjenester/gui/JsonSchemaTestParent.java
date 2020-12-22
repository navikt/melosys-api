package no.nav.melosys.tjenester.gui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.JsonPath;
import com.networknt.schema.*;
import com.networknt.schema.uri.URIFetcher;
import no.nav.melosys.service.kodeverk.KodeDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.jackson.MelosysModule;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.validation.ValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Collection;

import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public class JsonSchemaTestParent {
    private static final Logger log = LoggerFactory.getLogger(JsonSchemaTestParent.class);

    private static final String FEILMELDING = "Schemavalidering feilet for schema {}";
    private static final JsonSchemaFactory jsonSchemaFactory = JsonSchemaFactory
        .builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
        .uriFetcher(new ClasspathURIFetcher(), "http")
        .build();

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

    protected JsonSchema hentSchema(String schemaNavn) throws JSONException {
       InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(schemaNavn);
        return jsonSchemaFactory.getSchema(inputStream);
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
        valider(objectMapper().readTree(json), hentSchema(schemaNavn), logger);
    }

    protected void valider(Object o, String schemaNavn, ObjectMapper objectMapper) throws IOException {
        String jsonString = objectMapper.writeValueAsString(o);
        valider(jsonString, schemaNavn, log);
    }

    protected void validerArray(Collection liste, String schemaNavn) throws IOException {
        validerArray(liste, schemaNavn, log);
    }

    protected void validerArray(Collection liste, String schemaNavn, Logger logger) throws IOException {
        String json = objectMapper().writeValueAsString(liste);
        valider(objectMapper().readTree(json), hentSchema(schemaNavn), logger);
    }

    private void valider(JsonNode jsonNode, JsonSchema schema, Logger logger) {
        ValidationResult result = schema.validateAndCollect(jsonNode);
        if (!result.getValidationMessages().isEmpty()){
            formaterFeil(result, schema, jsonNode.toString(), logger);
        }
    }

    private void formaterFeil(ValidationResult validationResult, JsonSchema schema, String json, Logger logger) {
        logger.error(FEILMELDING, schema.getCurrentUri().toString());
        validationResult.getValidationMessages().forEach(
            validationMessage -> logger.error(formaterMelding(validationMessage, json)));
        throw new ValidationException(String.format("%s: %d schema violations found",
            schema.getCurrentUri(), validationResult.getValidationMessages().size()));
    }

    private String formaterMelding(ValidationMessage validationMessage, String json) {
        String verdi = JsonPath.read(json, validationMessage.getPath());
        String sti = validationMessage.getPath();
        return validationMessage.getMessage().replace(sti, sti + " [" + verdi + "]");
    }

    private static class ClasspathURIFetcher implements URIFetcher {
        @Override
        public InputStream fetch(URI uri) {
            try {
                String url = uri.toString().replace("http://melosys.nav.no/schemas/", "");
                return new ClassPathResource(url).getInputStream();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
