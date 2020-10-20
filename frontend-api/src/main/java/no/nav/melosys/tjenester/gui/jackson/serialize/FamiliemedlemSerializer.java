package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.person.Familiemedlem;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.FamiliemedlemDto;

public class FamiliemedlemSerializer extends StdSerializer<Familiemedlem> {

    private transient KodeverkService kodeverkService;

    public FamiliemedlemSerializer(KodeverkService kodeverkService) {
        super(Familiemedlem.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(Familiemedlem familiemedlem, JsonGenerator generator, SerializerProvider provider) throws IOException {
        FamiliemedlemDto familiemedlemDto = new FamiliemedlemDto();
        familiemedlemDto.fnr = familiemedlem.fnr;
        familiemedlemDto.sammensattNavn = familiemedlem.navn;
        familiemedlemDto.relasjonstype = kodeverkService.getKodeverdi(FellesKodeverk.FAMILIERELASJONER, familiemedlem.familierelasjon.getKode());
        generator.writeObject(familiemedlemDto);
    }
}
