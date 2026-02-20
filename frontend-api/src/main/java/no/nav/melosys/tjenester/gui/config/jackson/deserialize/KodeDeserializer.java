package no.nav.melosys.tjenester.gui.config.jackson.deserialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import no.nav.melosys.domain.kodeverk.Kodeverk;

/**
 * Deserializes {@code Kodeverk} enums from both plain strings ({@code "FØRSTEGANGSVEDTAK"})
 * and KodeDto objects ({@code {"kode":"FØRSTEGANGSVEDTAK","term":"..."}}).
 * This enables roundtrip serialization/deserialization with {@link no.nav.melosys.tjenester.gui.config.jackson.serialize.KodeSerializer}.
 */
public class KodeDeserializer extends JsonDeserializer<Kodeverk> {

    private final Class<? extends Kodeverk> enumType;

    public KodeDeserializer(Class<? extends Kodeverk> enumType) {
        this.enumType = enumType;
    }

    @Override
    public Kodeverk deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            return resolveEnum(p.getValueAsString(), ctxt);
        }

        if (p.currentToken() == JsonToken.START_OBJECT) {
            return deserializeFromObject(p, ctxt);
        }

        throw ctxt.wrongTokenException(p, ctxt.constructType(enumType), JsonToken.VALUE_STRING,
            "Expected string or object for Kodeverk enum");
    }

    private Kodeverk deserializeFromObject(JsonParser p, DeserializationContext ctxt) throws IOException {
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
    private Kodeverk resolveEnum(String value, DeserializationContext ctxt) throws IOException {
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
        for (Object constant : enumType.getEnumConstants()) {
            Kodeverk kv = (Kodeverk) constant;
            if (value.equals(kv.getKode())) {
                return kv;
            }
        }

        throw ctxt.weirdStringException(value, enumType,
            "Cannot resolve Kodeverk enum value: " + value);
    }
}
