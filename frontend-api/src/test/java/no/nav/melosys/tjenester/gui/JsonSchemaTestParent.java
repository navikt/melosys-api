package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.uri.URIFetcher;
import no.nav.melosys.service.JsonSchemaValidator;
import no.nav.melosys.service.kodeverk.KodeDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.jackson.MelosysModule;
import no.nav.melosys.tjenester.gui.util.NumericStringRandomizer;
import org.jeasy.random.EasyRandom;
import org.jeasy.random.EasyRandomParameters;
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
        jsonSchemaValidator(objectMapper()).valider(objectMapper().readTree(json), hentSchemaStream(schemaNavn), logger);
    }

    protected void valider(Object o, String schemaNavn, ObjectMapper objectMapper) throws IOException {
        String jsonString = objectMapper.writeValueAsString(o);
        jsonSchemaValidator(objectMapper).valider(jsonString, hentSchemaStream(schemaNavn), log);
    }

    protected <T> void validerArray(Collection<T> liste, String schemaNavn) throws IOException {
        validerArray(liste, schemaNavn, log);
    }

    protected <T> void validerArray(Collection<T> liste, String schemaNavn, Logger logger) throws IOException {
        String json = objectMapper().writeValueAsString(liste);
        jsonSchemaValidator(objectMapper()).valider(objectMapper().readTree(json), hentSchemaStream(schemaNavn), logger);
    }

    private InputStream hentSchemaStream(String schemanavn) throws IOException {
        return new DefaultResourceLoader().getResource("classpath:" + schemanavn).getInputStream();
    }

    private JsonSchemaValidator jsonSchemaValidator(ObjectMapper objectMapper) {
        return new JsonSchemaValidator(objectMapper,
            JsonSchemaFactory
                .builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
                .uriFetcher(new ClasspathURIFetcher(), "http")
                .build());
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
