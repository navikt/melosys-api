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
        try {
            properties.store(writer, null);
        } catch (IOException e) {
            // Dette skal ikke skje, siden StringWriter
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    @Override
    public Properties convertToEntityAttribute(String dbData) {
        Properties properties = new Properties();
        if (dbData != null) {
            try {
                properties.load(new StringReader(dbData));
            } catch (IOException e) {
                // Dette skal ikke skje, siden StringReader
                throw new IllegalArgumentException("Kan ikke lese properties til string: " + properties, e);
            }
        }
        return properties;
    }
}
