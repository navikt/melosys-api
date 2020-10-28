package no.nav.melosys.domain.jpa;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.serializer.LovvalgBestemmelseDeserializer;

public class SaksopplysningDokumentConverter implements AttributeConverter<SaksopplysningDokument, String> {

    private final static ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .registerModule(new SimpleModule()
            .addDeserializer(LovvalgBestemmelse.class, new LovvalgBestemmelseDeserializer()))
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        .addMixIn(SaksopplysningDokument.class, SaksopplysningDokumentMixIn.class);

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
