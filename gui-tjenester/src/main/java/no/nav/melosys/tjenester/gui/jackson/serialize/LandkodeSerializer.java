package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import no.nav.melosys.domain.dokument.felles.Landkode;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class LandkodeSerializer extends StdSerializer<Landkode> {

    private final KodeverkService kodeverkService;

    public LandkodeSerializer(KodeverkService kodeverkService) {
        super(Landkode.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(Landkode landkode, JsonGenerator generator, SerializerProvider serializerProvider) throws IOException {
        // String land = kodeverkService.dekod(Kodeverk.LANDKODER, landkode.getKode(), LocalDate.now());
        String land = landkode.getKode();
        generator.writeString(land);
    }
}
