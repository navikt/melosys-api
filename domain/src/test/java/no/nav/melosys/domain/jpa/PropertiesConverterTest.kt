package no.nav.melosys.domain.jpa;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PropertiesConverterTest {

    private PropertiesConverter propertiesConverter = new PropertiesConverter();

    @Test
    public void convert() {
        Properties properties = new Properties();
        properties.setProperty("key", "value");
        String s = propertiesConverter.convertToDatabaseColumn(properties);
        Properties converted = propertiesConverter.convertToEntityAttribute(s);
        assertThat(converted).isEqualTo(properties);
    }
}
