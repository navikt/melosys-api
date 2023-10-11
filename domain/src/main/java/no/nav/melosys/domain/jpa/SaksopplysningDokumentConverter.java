package no.nav.melosys.domain.jpa;

import javax.persistence.AttributeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.inntekt.Inntekt;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.jpa.mixin.*;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.serializer.LovvalgBestemmelseDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.module.kotlin.KotlinModule;

public class SaksopplysningDokumentConverter implements AttributeConverter<SaksopplysningDokument, String> {

    private static final Logger log = LoggerFactory.getLogger(SaksopplysningDokumentConverter.class);

    private static final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .registerModule(new SimpleModule()
            .addDeserializer(LovvalgBestemmelse.class, new LovvalgBestemmelseDeserializer()))
        .registerModule(new KotlinModule.Builder().build())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true)
        .addMixIn(GeografiskAdresse.class, GeografiskAdresseMixIn.class)
        .addMixIn(Inntekt.class, InntektMixin.class)
        .addMixIn(MidlertidigPostadresse.class, MidlertidigPostadresseMixIn.class)
        .addMixIn(SaksopplysningDokument.class, SaksopplysningDokumentMixIn.class)
        .addMixIn(TilleggsinformasjonDetaljer.class, TilleggsinformasjonDetaljerMixIn.class);

    @Override
    public String convertToDatabaseColumn(SaksopplysningDokument saksopplysningDokument) {
        if (saksopplysningDokument == null) {
            return null;
        }
        try {
            return objectMapper.writerWithView(DokumentView.Database.class)
                .writeValueAsString(saksopplysningDokument);
        } catch (JsonProcessingException e) {
            log.error("Kunne ikke skrive saksopplysning av type '{}' til database",
                saksopplysningDokument.getClass());
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
            log.error("Kunne ikke lese saksopplysning av type '{}' fra database",
                s.contains("\"type\"") ? s.substring(9, s.indexOf("\"", 9)) : "Ukjent", e);
            return null;
        }
    }
}
