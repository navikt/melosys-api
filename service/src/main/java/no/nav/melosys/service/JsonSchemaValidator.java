package no.nav.melosys.service;

import java.io.InputStream;
import javax.validation.ValidationException;

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

    private static final JsonSchemaFactory DEFAULT_JSON_SCHEMA_FACTORY = JsonSchemaFactory
        .builder(JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7))
        .build();

    private static final Logger log = LoggerFactory.getLogger(JsonSchemaValidator.class);
    private static final String FEILMELDING = "Schemavalidering feilet for schema {}";

    private final ObjectMapper objectMapper;
    private final JsonSchemaFactory jsonSchemaFactory;

    public JsonSchemaValidator(ObjectMapper objectMapper, JsonSchemaFactory jsonSchemaFactory) {
        this.objectMapper = objectMapper;
        this.jsonSchemaFactory = jsonSchemaFactory;
    }

    public JsonSchemaValidator(JsonSchemaFactory jsonSchemaFactory) {
        this(DEFAULT_OBJECT_MAPPER, jsonSchemaFactory);
    }

    public JsonSchemaValidator(ObjectMapper objectMapper) {
        this(objectMapper, DEFAULT_JSON_SCHEMA_FACTORY);
    }

    public JsonSchemaValidator() {
        this(DEFAULT_OBJECT_MAPPER, DEFAULT_JSON_SCHEMA_FACTORY);
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

    private void valider(ArrayNode arrayNode, JsonSchema schema, Logger logger) {
        ValidationResult result = schema.validateAndCollect(arrayNode);
        if (!result.getValidationMessages().isEmpty()) {
            formaterFeil(result, schema, arrayNode.toString(), logger);
        }
    }

    public void valider(JsonNode jsonObject, InputStream schemaStream, Logger logger) {
        valider(jsonObject, hentSchema(schemaStream), logger);
    }

    private void valider(JsonNode jsonNode, JsonSchema schema) {
        valider(jsonNode, schema, log);
    }

    private void valider(JsonNode jsonNode, JsonSchema schema, Logger logger) {
        ValidationResult result = schema.validateAndCollect(jsonNode);
        if (!result.getValidationMessages().isEmpty()) {
            formaterFeil(result, schema, jsonNode.toString(), logger);
        }
    }

    private String objektTilString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new TekniskException("Feil ved mapping av objekt til json", e);
        }
    }

    private JsonNode tilJsonNode(String jsonString) {
        try {
            return objectMapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            throw new TekniskException("Feil ved mapping av string til json", e);
        }
    }

    private JsonSchema hentSchema(String schemaNavn) {
        InputStream inputStream = Thread.currentThread().getContextClassLoader()
            .getResourceAsStream(schemaNavn);
        return hentSchema(inputStream);
    }

    private JsonSchema hentSchema(InputStream schemaStream) {
        return jsonSchemaFactory.getSchema(schemaStream);
    }

    private void formaterFeil(ValidationResult validationResult, JsonSchema schema, String json, Logger logger) {
        logger.error(FEILMELDING, schema.getCurrentUri().toString());
        validationResult.getValidationMessages().forEach(
            validationMessage -> logger.error(formaterMelding(validationMessage, json)));
        throw new ValidationException(String.format("%s: %d schema violations found",
            schema.getCurrentUri(), validationResult.getValidationMessages().size()));
    }

    private String formaterMelding(ValidationMessage validationMessage, String json) {
        final Object objekt = JsonPath.read(json, validationMessage.getPath());
        String sti = validationMessage.getPath();
        return validationMessage.getMessage().replace(sti, sti + " [" + objekt.toString() + "]");
    }
}
