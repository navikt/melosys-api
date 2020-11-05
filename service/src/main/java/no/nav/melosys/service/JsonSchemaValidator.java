package no.nav.melosys.service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.exception.TekniskException;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonSchemaValidator {
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private static final SchemaLoader.SchemaLoaderBuilder DEFAULT_SCHEMA_LOADER_BUILDER = SchemaLoader.builder()
        .draftV7Support()
        .useDefaults(true);

    private static final Logger log = LoggerFactory.getLogger(JsonSchemaValidator.class);
    private static final String FEILMELDING = "Schemavalidering feilet for schema {}";

    private final ObjectMapper objectMapper;
    private final SchemaLoader.SchemaLoaderBuilder schemaLoaderBuilder;

    public JsonSchemaValidator(ObjectMapper objectMapper, SchemaLoader.SchemaLoaderBuilder schemaLoaderBuilder) {
        this.objectMapper = objectMapper;
        this.schemaLoaderBuilder = schemaLoaderBuilder;
    }

    public JsonSchemaValidator(SchemaLoader.SchemaLoaderBuilder schemaLoaderBuilder) {
        this(DEFAULT_OBJECT_MAPPER, schemaLoaderBuilder);
    }

    public JsonSchemaValidator(ObjectMapper objectMapper) {
        this(objectMapper, DEFAULT_SCHEMA_LOADER_BUILDER);
    }

    public JsonSchemaValidator() {
        this(DEFAULT_OBJECT_MAPPER, DEFAULT_SCHEMA_LOADER_BUILDER);
    }

    public void valider(Object object, String schemaNavn) throws TekniskException {
        valider(objektTilString(object), schemaNavn);
    }

    public void valider(String json, String schemaNavn) throws TekniskException {
        valider(new JSONObject(json), hentSchema(schemaNavn));
    }

    public void valider(String json, JSONObject schema) {
        valider(json, byggSchema(schema));
    }

    public void valider(Object object, JSONObject schema) throws TekniskException {
        valider(objektTilString(object), byggSchema(schema));
    }

    private String objektTilString(Object object) throws TekniskException {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new TekniskException("Feil ved mapping av objekt til json", e);
        }
    }

    private Schema hentSchema(String schemaNavn) throws TekniskException {
        try {
            final Path path = Paths.get(Objects.requireNonNull(
                getClass().getClassLoader().getResource(schemaNavn)).toURI());

            try (final Stream<String> schemaStream = Files.lines(path)) {
                return byggSchema(schemaStream.collect(Collectors.joining("\n")));
            }
        } catch (URISyntaxException | IOException e) {
            throw new TekniskException(String.format("Feil ved henting av schema %s", schemaNavn), e);
        }
    }

    private Schema byggSchema(String schemaString) throws JSONException {
        return byggSchema(new JSONObject(schemaString));
    }

    private Schema byggSchema(JSONObject rawSchema) {
        SchemaLoader loader = schemaLoaderBuilder.schemaJson(rawSchema).build();
        return loader.load().build();
    }

    private void valider(Object jsonObject, Schema schema) {
        try {
            schema.validate(jsonObject);
        } catch (ValidationException e) {
            formaterFeil(e, schema);
        }
    }

    private void formaterFeil(ValidationException e, Schema schema) {
        log.error(FEILMELDING, schema.getTitle());
        e.getCausingExceptions().stream()
            .map(ValidationException::toJSON)
            .forEach(jsonObject -> log.error(jsonObject.toString()));
        throw e;
    }
}
