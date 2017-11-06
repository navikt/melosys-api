package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.melosys.domain.dokument.SaksopplysningDokument;

public class SaksopplysningSerializer extends StdSerializer<Collection<SaksopplysningDokument>> {

    public SaksopplysningSerializer() {
        super(Collection.class, true);
    }

    @Override
    public void serialize(Collection<SaksopplysningDokument> dokumenter, JsonGenerator generator, SerializerProvider sp) throws IOException {
        Map<String, List<SaksopplysningDokument>> typeMap = new LinkedHashMap<>();

        // Grupper dokumenter avhengig av typen deres
        for (SaksopplysningDokument dokument : dokumenter) {
            String key = dokument.getClass().getSimpleName();

            List<SaksopplysningDokument> opplysninger = typeMap.get(key);
            if (opplysninger == null) {
                opplysninger = new ArrayList<>();
            }
            opplysninger.add(dokument);
            typeMap.put(key, opplysninger);
        }

        // json object saksopplysninger starter her
        generator.writeStartObject();

        for (Map.Entry<String, List<SaksopplysningDokument>> typeMapEntry : typeMap.entrySet()) {
            // Obs. PersonDokument blir til person i json, InntektDokument -> inntekt, osv.
            String dokumentType = typeMapEntry.getKey().toLowerCase().replaceFirst("dokument", "");

            List<SaksopplysningDokument> opplysninger = typeMapEntry.getValue();
            tilJson(generator, dokumentType, opplysninger);
        }

        generator.writeEndObject();

    }

    private void tilJson(JsonGenerator generator, String dokumentType, List<SaksopplysningDokument> opplysninger) throws IOException {
        if (dokumentType.equalsIgnoreCase("organisasjon")) {
            generator.writeArrayFieldStart("organisasjoner");
            for (SaksopplysningDokument dokument : opplysninger) {
                generator.writeObject(dokument);
            }
            generator.writeEndArray();
            return;
        }

        for (SaksopplysningDokument dokument : opplysninger) {
            generator.writeObjectField(dokumentType, dokument);
        }
    }

}
