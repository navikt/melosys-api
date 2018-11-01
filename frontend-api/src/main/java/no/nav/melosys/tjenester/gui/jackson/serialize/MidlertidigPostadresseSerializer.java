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
import no.nav.melosys.tjenester.gui.MidlertidigPostadresseDto;
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
        MidlertidigPostadresseDto dto = new MidlertidigPostadresseDto();

        if (midlertidigPostadresse instanceof MidlertidigPostadresseNorge) {
            MidlertidigPostadresseNorge adresse = (MidlertidigPostadresseNorge) midlertidigPostadresse;
            dto.strukturertAdresse = new StrukturertAdresseDto();

            dto.strukturertAdresse.gatenavn = adresse.gateadresse.getGatenavn();
            dto.strukturertAdresse.husnr =
                Stream.of(
                    adresse.gateadresse.getHusbokstav(),
                    adresse.gateadresse.getHusnummer()
                ).filter(Objects::nonNull).map(Objects::toString).collect(Collectors.joining(" "));
            dto.strukturertAdresse.postnr = adresse.poststed;
            dto.strukturertAdresse.poststed = kodeverkService.dekod(POSTNUMMER, adresse.poststed, LocalDate.now());

            if (midlertidigPostadresse.land != null) {
                dto.strukturertAdresse.landKode = midlertidigPostadresse.land.getKode();
            }
            dto.adressetype = MidlertidigPostadresseDto.Adressetype.STRUKTURERT;

        } else if (midlertidigPostadresse instanceof MidlertidigPostadresseUtland) {
            MidlertidigPostadresseUtland adresse = (MidlertidigPostadresseUtland) midlertidigPostadresse;
            dto.ustrukturertAdresse = new UstrukturertAdresseDto();

            if (adresse.adresselinje1 != null) {
                dto.ustrukturertAdresse.adresselinjer.add(adresse.adresselinje1);
            }
            if (adresse.adresselinje2 != null) {
                dto.ustrukturertAdresse.adresselinjer.add(adresse.adresselinje2);
            }
            if (adresse.adresselinje3 != null) {
                dto.ustrukturertAdresse.adresselinjer.add(adresse.adresselinje3);
            }
            if (adresse.adresselinje4 != null) {
                dto.ustrukturertAdresse.adresselinjer.add(adresse.adresselinje4);
            }
            if (midlertidigPostadresse.land != null) {
                dto.ustrukturertAdresse.landKode = midlertidigPostadresse.land.getKode();
            }
            dto.adressetype = MidlertidigPostadresseDto.Adressetype.USTRUKTURERT;
        }
        generator.writeObject(dto);
    }
}
