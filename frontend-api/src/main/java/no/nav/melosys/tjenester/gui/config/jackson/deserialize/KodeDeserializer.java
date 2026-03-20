package no.nav.melosys.tjenester.gui.config.jackson.deserialize;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import no.nav.melosys.domain.kodeverk.Kodeverk;

/**
 * Deserializes {@code Kodeverk} enums from both plain strings ({@code "FØRSTEGANGSVEDTAK"})
 * and KodeDto objects ({@code {"kode":"FØRSTEGANGSVEDTAK","term":"..."}}).
 * This enables roundtrip serialization/deserialization with {@link no.nav.melosys.tjenester.gui.config.jackson.serialize.KodeSerializer}.
 */
public class KodeDeserializer extends ValueDeserializer<Kodeverk> {

    private final Class<? extends Kodeverk> enumType;

    public KodeDeserializer(Class<? extends Kodeverk> enumType) {
        this.enumType = enumType;
    }

    @Override
    public Kodeverk deserialize(JsonParser p, DeserializationContext ctxt) {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return resolveEnum(p.getValueAsString(), ctxt);
        }

        if (p.currentToken() == JsonToken.START_OBJECT) {
            return deserializeFromObject(p, ctxt);
        }

        throw ctxt.wrongTokenException(p, ctxt.constructType(enumType), JsonToken.VALUE_STRING,
            "Expected string or object for Kodeverk enum");
    }

    private Kodeverk deserializeFromObject(JsonParser p, DeserializationContext ctxt) {
        String kode = null;
        while (p.nextToken() != JsonToken.END_OBJECT) {
            String fieldName = p.currentName();
            p.nextToken();
            if ("kode".equals(fieldName)) {
                kode = p.getValueAsString();
            }
        }
        if (kode == null) {
            return null;
        }
        return resolveEnum(kode, ctxt);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Kodeverk resolveEnum(String value, DeserializationContext ctxt) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        Class<? extends Enum> rawEnumType = (Class<? extends Enum>) enumType;

        // First try exact enum name match
        try {
            return (Kodeverk) Enum.valueOf(rawEnumType, value);
        } catch (IllegalArgumentException e) {
            // Fall through to kode-based lookup
        }

        // Then try matching by kode
        Object[] constants = enumType.getEnumConstants();
        if (constants == null) {
            throw ctxt.weirdStringException(value, enumType,
                "Type is not an enum: " + enumType.getName());
        }
        for (Object constant : constants) {
            Kodeverk kv = (Kodeverk) constant;
            if (value.equals(kv.getKode())) {
                return kv;
            }
        }

        throw ctxt.weirdStringException(value, enumType,
            "Cannot resolve Kodeverk enum value: " + value);
    }
}
