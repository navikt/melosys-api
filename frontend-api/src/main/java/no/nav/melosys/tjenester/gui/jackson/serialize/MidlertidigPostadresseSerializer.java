package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseUtland;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.StrukturertAdresseDto;
import no.nav.melosys.tjenester.gui.dto.UstrukturertAdresseDto;

import static no.nav.melosys.domain.FellesKodeverk.POSTNUMMER;

public class MidlertidigPostadresseSerializer extends StdSerializer<MidlertidigPostadresse> {

    private KodeverkService kodeverkService;

    public MidlertidigPostadresseSerializer(KodeverkService kodeverkService) {
        super(MidlertidigPostadresse.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(MidlertidigPostadresse midlertidigPostadresse, JsonGenerator generator, SerializerProvider provider) throws IOException {

        if (midlertidigPostadresse instanceof MidlertidigPostadresseNorge) {
            MidlertidigPostadresseNorge adresse = (MidlertidigPostadresseNorge) midlertidigPostadresse;
            StrukturertAdresseDto adresseDto = new StrukturertAdresseDto();

            adresseDto.gatenavn = adresse.gateadresse.getGatenavn();
            adresseDto.husnummer =
                Stream.of(
                    adresse.gateadresse.getHusbokstav(),
                    adresse.gateadresse.getHusnummer()
                ).filter(Objects::nonNull).map(Objects::toString).collect(Collectors.joining(" "));
            adresseDto.postnummer = adresse.poststed;
            adresseDto.poststed = kodeverkService.dekod(POSTNUMMER, adresse.poststed, LocalDate.now());

            if (midlertidigPostadresse.land != null) {
                adresseDto.land = midlertidigPostadresse.land.getKode();
            }

            generator.writeObject(adresseDto);

        } else if (midlertidigPostadresse instanceof MidlertidigPostadresseUtland) {
            MidlertidigPostadresseUtland adresse = (MidlertidigPostadresseUtland) midlertidigPostadresse;
            UstrukturertAdresseDto adresseDto = new UstrukturertAdresseDto();

            adresseDto.adresselinje1 = adresse.adresselinje1;
            adresseDto.adresselinje2 = adresse.adresselinje2;
            adresseDto.adresselinje3 = adresse.adresselinje3;
            adresseDto.adresselinje4 = adresse.adresselinje4;

            if (midlertidigPostadresse.land != null) {
                adresseDto.land = midlertidigPostadresse.land.getKode();
            }

            generator.writeObject(adresseDto);
        }
    }
}
