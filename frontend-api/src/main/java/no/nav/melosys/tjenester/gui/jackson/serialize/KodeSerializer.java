package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.Kodeverk;
import no.nav.melosys.service.kodeverk.KodeDto;

/**
 * Alle klasser som implementerer {@code Kodeverk} serialiseres med kode og term.
 */
public class KodeSerializer extends StdSerializer<Kodeverk> {

    public KodeSerializer() {
        super(Kodeverk.class);
    }

    @Override
    public void serialize(Kodeverk value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        KodeDto kodeDto = new KodeDto(value.getKode(), value.getBeskrivelse());
        generator.writeObject(kodeDto);
    }
}
