package no.nav.melosys.domain.jpa;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;

public class SaksopplysningDokumentConverter implements AttributeConverter<SaksopplysningDokument, String> {

    private final static ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    @Override
    public String convertToDatabaseColumn(SaksopplysningDokument saksopplysningDokument) {
        if (saksopplysningDokument == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(saksopplysningDokument);
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    @Override
    public SaksopplysningDokument convertToEntityAttribute(String s) {
        if (s == null) {
            return null;
        }
        try {
            return objectMapper.readValue(s, SaksopplysningDokument.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
