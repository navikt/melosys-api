package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.dokument.felles.KodeverkHjelper;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.util.StringUtils;

public class FellesKodeverkSerializer extends StdSerializer<KodeverkHjelper> {

    private final transient KodeverkService kodeverkService;

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
            term = kodeverkService.dekod(kodeverkHjelper.hentKodeverkNavn(), kodeverkHjelper.getKode());
        }

        generator.writeStartObject();
        generator.writeStringField("kode", kode);
        generator.writeStringField("term", term);
        generator.writeEndObject();
    }
}
