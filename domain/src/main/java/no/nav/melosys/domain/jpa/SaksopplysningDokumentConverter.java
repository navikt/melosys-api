package no.nav.melosys.domain.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.module.kotlin.KotlinModule;
import no.nav.melosys.domain.dokument.DokumentView;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.inntekt.tillegsinfo.TilleggsinformasjonDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse;
import no.nav.melosys.domain.jpa.mixin.GeografiskAdresseMixIn;
import no.nav.melosys.domain.jpa.mixin.MidlertidigPostadresseMixIn;
import no.nav.melosys.domain.jpa.mixin.SaksopplysningDokumentMixIn;
import no.nav.melosys.domain.jpa.mixin.TilleggsinformasjonDetaljerMixIn;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.serializer.LovvalgBestemmelseDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Converter
public class SaksopplysningDokumentConverter implements AttributeConverter<SaksopplysningDokument, String> {

    private static final Logger log = LoggerFactory.getLogger(SaksopplysningDokumentConverter.class);

    private static final ObjectMapper objectMapper = JsonMapper.builder()
        .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
        .addModule(new SimpleModule()
            .addDeserializer(LovvalgBestemmelse.class, new LovvalgBestemmelseDeserializer()))
        .addModule(new KotlinModule.Builder().build())
        .addMixIn(GeografiskAdresse.class, GeografiskAdresseMixIn.class)
        .addMixIn(MidlertidigPostadresse.class, MidlertidigPostadresseMixIn.class)
        .addMixIn(SaksopplysningDokument.class, SaksopplysningDokumentMixIn.class)
        .addMixIn(TilleggsinformasjonDetaljer.class, TilleggsinformasjonDetaljerMixIn.class)
        .build();

    @Override
    public String convertToDatabaseColumn(SaksopplysningDokument saksopplysningDokument) {
        if (saksopplysningDokument == null) {
            return null;
        }
        try {
            return getObjectMapper().writerWithView(DokumentView.Database.class)
                .writeValueAsString(saksopplysningDokument);
        } catch (JacksonException e) {
            log.error("Kunne ikke skrive saksopplysning av type '{}' til database",
                saksopplysningDokument.getClass());
            return null;
        }
    }

    protected ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public SaksopplysningDokument convertToEntityAttribute(String s) {
        if (s == null) {
            return null;
        }
        try {
            return objectMapper.readValue(s, SaksopplysningDokument.class);
        } catch (JacksonException e) {
            log.error("Kunne ikke lese saksopplysning av type '{}' fra database",
                s.contains("\"type\"") ? s.substring(9, s.indexOf("\"", 9)) : "Ukjent", e);
            return null;
        }
    }
}
