package no.nav.melosys.domain.serializer;

import java.io.IOException;
import java.io.Serial;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class MultiDateDeserializer extends StdDeserializer<LocalDate> {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final String[] DATE_FORMATS = new String[]{
        "dd.MM.yyyy",
        "yyyy-MM-dd",
    };

    public MultiDateDeserializer() { // Prevents InvalidDefinitionException
        this(null);
    }

    public MultiDateDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
        String dateString = jsonNode.textValue();

        for (String dateFormat : DATE_FORMATS) {
            try {
                return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(dateFormat));
            } catch (DateTimeParseException ignore) {
            }
        }
        throw new JsonParseException(jsonParser, "Cannot parse date '%s'. Supported formats: %s"
            .formatted(dateString, Arrays.toString(DATE_FORMATS)));
    }
}
