package no.nav.melosys.domain.dokument.person;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersonConverter implements AttributeConverter<Person, String> {

    private static Logger logger = LoggerFactory.getLogger(PersonConverter.class);
    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper
            .registerModule(new JavaTimeModule())
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    @Override
    public String convertToDatabaseColumn(Person person) {
        try {
            return objectMapper.writeValueAsString(person);
        } catch (JsonProcessingException e) {
            logger.error("Kunne ikke serialisere person til database.");
            return null;
        }
    }

    @Override
    public Person convertToEntityAttribute(String s) {
        try {
            return objectMapper.readValue(s, Person.class);
        } catch (JsonProcessingException e) {
            logger.error("Kunne ikke deserialisere person fra database.");
            return null;
        }
    }
}
