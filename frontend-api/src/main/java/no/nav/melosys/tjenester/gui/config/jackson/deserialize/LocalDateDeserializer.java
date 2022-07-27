package no.nav.melosys.tjenester.gui.config.jackson.deserialize;

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

public class LocalDateDeserializer extends StdDeserializer<LocalDate> {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final String[] SUPPORTED_DATE_FORMATS = new String[]{
        "dd.MM.yyyy",
        "yyyy-MM-dd",
    };

    public LocalDateDeserializer() {
        super(LocalDate.class);
    }

    @Override
    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
        String dateString = jsonNode.textValue();

        for (String dateFormat : SUPPORTED_DATE_FORMATS) {
            try {
                return LocalDate.parse(dateString, DateTimeFormatter.ofPattern(dateFormat));
            } catch (DateTimeParseException ignore) {
            }
        }
        throw new JsonParseException(jsonParser, "Cannot parse date '%s'. Supported formats: %s"
            .formatted(dateString, Arrays.toString(SUPPORTED_DATE_FORMATS)));
    }
}
