package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;
import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.dokument.felles.KodeverkHjelper;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.util.StringUtils;

public class FellesKodeverkSerializer extends StdSerializer<KodeverkHjelper> {

    private final KodeverkService kodeverkService;

    public FellesKodeverkSerializer(KodeverkService kodeverkService) {
        super(KodeverkHjelper.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(KodeverkHjelper kodeverkHjelper, JsonGenerator generator, SerializerProvider serializerProvider) throws IOException {
        String kode = kodeverkHjelper.getKode();
        String term = null;

        if (StringUtils.isEmpty(kode)) {
            kode = null;
        } else {
            try {
                term = kodeverkService.dekod(kodeverkHjelper.hentKodeverkNavn(), kodeverkHjelper.getKode(), LocalDate.now());
            } catch (TekniskException e) {
                // Antar at feilen allerede er logget
                // term forblir null
            }
        }

        generator.writeStartObject();
        generator.writeStringField("kode", kode);
        generator.writeStringField("term", term);
        generator.writeEndObject();
    }
}
