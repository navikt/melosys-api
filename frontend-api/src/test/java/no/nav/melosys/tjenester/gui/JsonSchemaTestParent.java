package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.JsonSchemaValidator;
import no.nav.melosys.service.kodeverk.KodeDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.jackson.MelosysModule;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import org.everit.json.schema.loader.SchemaClient;
import org.everit.json.schema.loader.SchemaLoader;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;

import static org.jeasy.random.FieldPredicates.named;
import static org.jeasy.random.FieldPredicates.ofType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

public class JsonSchemaTestParent {
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

    protected ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new MelosysModule(null));

        return objectMapper;
    }

    protected ObjectMapper objectMapperMedKodeverkServiceStub() {
        ObjectMapper objectMapperMedKodeverkServiceStub = new ObjectMapper();
        objectMapperMedKodeverkServiceStub.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapperMedKodeverkServiceStub.configure(SerializationFeature.INDENT_OUTPUT, true);
        objectMapperMedKodeverkServiceStub.registerModule(new JavaTimeModule());
        KodeverkService kodeverkService = mock(KodeverkService.class);
        lenient().when(kodeverkService.dekod(any(), any(), any())).thenReturn("DUMMY");
        lenient().when(kodeverkService.getKodeverdi(any(), any())).thenReturn(new KodeDto("DUMMY", "DUMMY"));
        objectMapperMedKodeverkServiceStub.registerModule(new MelosysModule(kodeverkService));

        return objectMapperMedKodeverkServiceStub;
    }

    protected void valider(Object o, String schemaNavn) throws IOException, TekniskException {
        jsonSchemaValidator(objectMapper()).valider(o, hentSchemaStream(schemaNavn));
    }

    protected void valider(Object o, String schemaNavn, Logger logger) throws IOException {
        String jsonString = objectMapper().writeValueAsString(o);
        valider(jsonString, schemaNavn, logger);
    }

    protected void valider(String json, String schemaNavn, Logger logger) throws IOException {
        jsonSchemaValidator(objectMapper()).valider(json, hentSchemaStream(schemaNavn), logger);
    }

    protected void valider(Object o, String schemaNavn, ObjectMapper objectMapper) throws IOException, TekniskException {
        jsonSchemaValidator(objectMapper).valider(o, hentSchemaStream(schemaNavn));
    }

    protected <T> void validerArray(Collection<T> liste, String schemaNavn) throws IOException, TekniskException {
        String json = objectMapper().writeValueAsString(liste);
        valider(new JSONArray(json), schemaNavn);
    }

    protected <T> void validerArray(Collection<T> liste, String schemaNavn, Logger logger) throws IOException {
        String json = objectMapper().writeValueAsString(liste);
        valider(new JSONArray(json), schemaNavn, logger);
    }

    protected void valider(JSONArray json, String schemaNavn) throws IOException {
        jsonSchemaValidator(objectMapper()).valider(json, hentSchemaStream(schemaNavn));
    }

    protected void valider(JSONArray json, String schemaNavn, Logger logger) throws IOException {
        jsonSchemaValidator(objectMapper()).valider(json, hentSchemaStream(schemaNavn), logger);
    }

    private JsonSchemaValidator jsonSchemaValidator(ObjectMapper objectMapper) {
        return new JsonSchemaValidator(objectMapper,
            SchemaLoader.builder().httpClient(new ClasspathSchemaClient()).draftV7Support().useDefaults(true));
    }

    private InputStream hentSchemaStream(String schemanavn) throws IOException {
        return new DefaultResourceLoader().getResource("classpath:" + schemanavn).getInputStream();
    }

    private static class ClasspathSchemaClient implements SchemaClient {
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
