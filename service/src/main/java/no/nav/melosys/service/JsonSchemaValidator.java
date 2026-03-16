package no.nav.melosys.service;

import java.io.InputStream;
import java.util.List;
import jakarta.validation.ValidationException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import com.jayway.jsonpath.InvalidPathException;
import com.jayway.jsonpath.JsonPath;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import no.nav.melosys.exception.TekniskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonSchemaValidator {
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = JsonMapper.builder().build();

    private static final SchemaRegistry DEFAULT_SCHEMA_REGISTRY =
        SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7);

    private static final Logger log = LoggerFactory.getLogger(JsonSchemaValidator.class);
    private static final String FEILMELDING = "Schemavalidering feilet for schema {}";

    private final ObjectMapper objectMapper;
    private final SchemaRegistry schemaRegistry;

    public JsonSchemaValidator(ObjectMapper objectMapper, SchemaRegistry schemaRegistry) {
        this.objectMapper = objectMapper;
        this.schemaRegistry = schemaRegistry;
    }

    public JsonSchemaValidator(SchemaRegistry schemaRegistry) {
        this(DEFAULT_OBJECT_MAPPER, schemaRegistry);
    }

    public JsonSchemaValidator(ObjectMapper objectMapper) {
        this(objectMapper, DEFAULT_SCHEMA_REGISTRY);
    }

    public JsonSchemaValidator() {
        this(DEFAULT_OBJECT_MAPPER, DEFAULT_SCHEMA_REGISTRY);
    }

    public void valider(Object object, String schemaNavn) {
        valider(objektTilString(object), schemaNavn);
    }

    public void valider(String json, String schemaNavn) {
        valider(tilJsonNode(json), hentSchema(schemaNavn));
    }

    public void valider(String json, InputStream schemaStream) {
        valider(json, schemaStream, log);
    }

    public void valider(String json, InputStream schemaStream, Logger logger) {
        valider(tilJsonNode(json), hentSchema(schemaStream), logger);
    }

    public void valider(Object o, InputStream schemaStream) {
        valider(objektTilString(o), schemaStream, log);
    }

    public void valider(ArrayNode arrayNode, InputStream schemaStream) {
        valider(arrayNode, schemaStream, log);
    }

    public void valider(ArrayNode arrayNode, InputStream schemaStream, Logger logger) {
        valider(arrayNode, hentSchema(schemaStream), logger);
    }

    private void valider(ArrayNode arrayNode, Schema schema, Logger logger) {
        List<Error> errors = schema.validate(arrayNode);
        if (!errors.isEmpty()) {
            formaterFeil(errors, schema, arrayNode.toString(), logger);
        }
    }

    public void valider(JsonNode jsonObject, InputStream schemaStream, Logger logger) {
        valider(jsonObject, hentSchema(schemaStream), logger);
    }

    private void valider(JsonNode jsonNode, Schema schema) {
        valider(jsonNode, schema, log);
    }

    private void valider(JsonNode jsonNode, Schema schema, Logger logger) {
        List<Error> errors = schema.validate(jsonNode);
        if (!errors.isEmpty()) {
            formaterFeil(errors, schema, jsonNode.toString(), logger);
        }
    }

    private String objektTilString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new TekniskException("Feil ved mapping av objekt til json", e);
        }
    }

    private JsonNode tilJsonNode(String jsonString) {
        try {
            return objectMapper.readTree(jsonString);
        } catch (JacksonException e) {
            throw new TekniskException("Feil ved mapping av string til json", e);
        }
    }

    private Schema hentSchema(String schemaNavn) {
        InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(schemaNavn);
        return hentSchema(inputStream);
    }

    private Schema hentSchema(InputStream schemaStream) {
        return schemaRegistry.getSchema(schemaStream);
    }

    private void formaterFeil(List<Error> errors, Schema schema, String json, Logger logger) {
        String schemaLocation = schema.getSchemaLocation().toString();
        logger.error(FEILMELDING, schemaLocation);
        errors.forEach(error -> logger.error(formaterMelding(error, json)));
        throw new ValidationException("%s: %d schema violations found".formatted(schemaLocation, errors.size()));
    }

    private String formaterMelding(Error error, String json) {
        String sti = error.getInstanceLocation().toString();
        try {
            final Object objekt = JsonPath.read(json, sti);
            return error.getMessage().replace(sti, sti + " [" + objekt + "]");
        } catch (InvalidPathException e) {
            return error.getMessage();
        }
    }
}
