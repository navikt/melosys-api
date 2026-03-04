package no.nav.melosys.service;

import java.io.InputStream;
import java.util.List;
import jakarta.validation.ValidationException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jayway.jsonpath.JsonPath;
import com.networknt.schema.*;
import no.nav.melosys.exception.TekniskException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonSchemaValidator {
    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private static final SchemaRegistry DEFAULT_SCHEMA_REGISTRY = SchemaRegistry
        .withDefaultDialect(SpecificationVersion.DRAFT_7);

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
        valider(json, hentSchema(schemaNavn));
    }

    public void valider(String json, InputStream schemaStream) {
        valider(json, schemaStream, log);
    }

    public void valider(String json, InputStream schemaStream, Logger logger) {
        valider(json, hentSchema(schemaStream), logger);
    }

    public void valider(Object o, InputStream schemaStream) {
        valider(objektTilString(o), schemaStream, log);
    }

    public void valider(ArrayNode arrayNode, InputStream schemaStream) {
        valider(arrayNode, schemaStream, log);
    }

    public void valider(ArrayNode arrayNode, InputStream schemaStream, Logger logger) {
        valider(nodeTilString(arrayNode), hentSchema(schemaStream), logger);
    }

    public void valider(JsonNode jsonObject, InputStream schemaStream, Logger logger) {
        valider(nodeTilString(jsonObject), hentSchema(schemaStream), logger);
    }

    private void valider(String json, Schema schema) {
        valider(json, schema, log);
    }

    private void valider(String json, Schema schema, Logger logger) {
        List<Error> errors = schema.validate(json, InputFormat.JSON);
        if (!errors.isEmpty()) {
            formaterFeil(errors, schema, json, logger);
        }
    }

    private String objektTilString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new TekniskException("Feil ved mapping av objekt til json", e);
        }
    }

    private String nodeTilString(JsonNode node) {
        try {
            return objectMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new TekniskException("Feil ved mapping av JsonNode til json", e);
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
        String schemaUri = schema.getSchemaLocation().toString();
        logger.error(FEILMELDING, schemaUri);
        errors.forEach(error -> logger.error(formaterMelding(error, json)));
        throw new ValidationException(String.format("%s: %d schema violations found",
            schemaUri, errors.size()));
    }

    private String formaterMelding(Error error, String json) {
        String sti = error.getInstanceLocation().toString();
        final Object objekt = JsonPath.read(json, sti);
        return error.getMessage().replace(sti, sti + " [" + objekt + "]");
    }
}
