package no.nav.melosys.tjenester.gui.config.jackson.serialize;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.adresse.UstrukturertAdresse;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.MidlertidigPostadresseDto;
import no.nav.melosys.tjenester.gui.dto.StrukturertAdresseDto;

import static no.nav.melosys.domain.FellesKodeverk.POSTNUMMER;

public class MidlertidigPostadresseSerializer extends StdSerializer<no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse> {

    private final transient KodeverkService kodeverkService;

    public MidlertidigPostadresseSerializer(KodeverkService kodeverkService) {
        super(no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse midlertidigPostadresse, JsonGenerator generator, SerializerProvider provider) throws IOException {
        MidlertidigPostadresseDto dto = new MidlertidigPostadresseDto();

        if (midlertidigPostadresse instanceof MidlertidigPostadresseNorge adresse) {
            dto.strukturertAdresse = new StrukturertAdresseDto(
                adresse.getGateadresse().getGatenavn(),
                Stream.of(
                    adresse.getGateadresse().getHusbokstav(),
                    adresse.getGateadresse().getHusnummer()
                ).filter(Objects::nonNull).map(Objects::toString).collect(Collectors.joining(" ")),
                adresse.getPoststed(),
                kodeverkService.dekod(POSTNUMMER, adresse.getPoststed()),
                null,
                midlertidigPostadresse.getLand() != null ? midlertidigPostadresse.getLand().getKode() : null
            );
            dto.adressetype = MidlertidigPostadresseDto.Adressetype.STRUKTURERT;

        } else if (midlertidigPostadresse instanceof MidlertidigPostadresseUtland adresse) {
            dto.ustrukturertAdresse = UstrukturertAdresse.av(adresse);
            dto.adressetype = MidlertidigPostadresseDto.Adressetype.USTRUKTURERT;
        }
        generator.writeObject(dto);
    }
}
