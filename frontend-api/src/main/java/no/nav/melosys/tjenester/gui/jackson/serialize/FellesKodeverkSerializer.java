package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;
import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.dokument.felles.KodeverkHjelper;
import no.nav.melosys.domain.dokument.felles.Land;
import no.nav.melosys.domain.util.FellesKodeverk;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class FellesKodeverkSerializer extends StdSerializer<KodeverkHjelper> {

    private final KodeverkService kodeverkService;

    public FellesKodeverkSerializer(KodeverkService kodeverkService) {
        super(KodeverkHjelper.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(KodeverkHjelper kodeverkHjelper, JsonGenerator generator, SerializerProvider serializerProvider) throws IOException {
        String landTerm = kodeverkService.dekod(kodeverkHjelper.hentKodeverkNavn(), kodeverkHjelper.getKode(), LocalDate.now());
        String landKode = kodeverkHjelper.getKode();
        generator.writeStartObject();
        generator.writeStringField("kode", landKode);
        generator.writeStringField("term", landTerm);
        generator.writeEndObject();
    }
}
