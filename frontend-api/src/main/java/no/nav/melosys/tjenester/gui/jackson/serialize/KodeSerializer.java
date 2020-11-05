package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.Behandlingsgrunnlagtyper;
import no.nav.melosys.domain.kodeverk.Kodeverk;
import no.nav.melosys.service.kodeverk.KodeDto;

/**
 * Alle klasser som implementerer {@code Kodeverk} skulle serialiseres med kode og term.
 */
public class KodeSerializer extends StdSerializer<Kodeverk> {

    public KodeSerializer() {
        super(Kodeverk.class);
    }

    @Override
    public void serialize(Kodeverk value, JsonGenerator generator, SerializerProvider provider) throws IOException {
        //FIXME schema må endres til å bruke bare kode og ikke kode + term.
        if (value instanceof Avsendertyper || value instanceof Tema || value instanceof Behandlingsgrunnlagtyper) {
            generator.writeString(value.getKode());
        } else {
            KodeDto kodeDto = new KodeDto(value.getKode(), value.getBeskrivelse());
            generator.writeObject(kodeDto);
        }
    }
}
