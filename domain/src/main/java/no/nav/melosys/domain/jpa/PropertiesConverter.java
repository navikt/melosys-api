package no.nav.melosys.domain.jpa;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Properties;
import javax.persistence.AttributeConverter;

public class PropertiesConverter implements AttributeConverter<Properties, String> {

    @Override
    public String convertToDatabaseColumn(Properties properties) {
        if (properties == null || properties.isEmpty()) {
            return null;
        }
        StringWriter writer = new StringWriter(512);
        // FIXME (farjam): Her tror jeg vi må sjekke alle values og escape bl.a. '\n' og '\r'
        properties.forEach((k, v) -> {writer.append((String) k).append('=').append((String) v).append('\n');});
        return writer.toString();
    }

    @Override
    public Properties convertToEntityAttribute(String dbData) {
        Properties properties = new Properties();
        if (dbData != null) {
            try {
                properties.load(new StringReader(dbData));
            } catch (IOException e) {
                throw new IllegalArgumentException("Kan ikke lese properties til string: " + properties, e);
            }
        }
        return properties;
    }
}
