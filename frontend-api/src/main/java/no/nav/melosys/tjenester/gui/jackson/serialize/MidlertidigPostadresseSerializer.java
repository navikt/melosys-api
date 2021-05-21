package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresseUtland;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.dto.MidlertidigPostadresseDto;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.dokument.adresse.UstrukturertAdresse;

import static no.nav.melosys.domain.FellesKodeverk.POSTNUMMER;

public class MidlertidigPostadresseSerializer extends StdSerializer<no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse> {

    private transient KodeverkService kodeverkService;

    public MidlertidigPostadresseSerializer(KodeverkService kodeverkService) {
        super(no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(no.nav.melosys.domain.dokument.person.adresse.MidlertidigPostadresse midlertidigPostadresse, JsonGenerator generator, SerializerProvider provider) throws IOException {
        MidlertidigPostadresseDto dto = new MidlertidigPostadresseDto();

        if (midlertidigPostadresse instanceof MidlertidigPostadresseNorge) {
            MidlertidigPostadresseNorge adresse = (MidlertidigPostadresseNorge) midlertidigPostadresse;
            dto.strukturertAdresse = new StrukturertAdresse();

            dto.strukturertAdresse.gatenavn = adresse.gateadresse.getGatenavn();
            dto.strukturertAdresse.husnummer =
                Stream.of(
                    adresse.gateadresse.getHusbokstav(),
                    adresse.gateadresse.getHusnummer()
                ).filter(Objects::nonNull).map(Objects::toString).collect(Collectors.joining(" "));
            dto.strukturertAdresse.postnummer = adresse.poststed;
            dto.strukturertAdresse.poststed = kodeverkService.dekod(POSTNUMMER, adresse.poststed, LocalDate.now());

            if (midlertidigPostadresse.land != null) {
                dto.strukturertAdresse.landkode = midlertidigPostadresse.land.getKode();
            }
            dto.adressetype = MidlertidigPostadresseDto.Adressetype.STRUKTURERT;

        } else if (midlertidigPostadresse instanceof MidlertidigPostadresseUtland) {
            MidlertidigPostadresseUtland adresse = (MidlertidigPostadresseUtland) midlertidigPostadresse;
            dto.ustrukturertAdresse = UstrukturertAdresse.av(adresse);
            dto.adressetype = MidlertidigPostadresseDto.Adressetype.USTRUKTURERT;
        }
        generator.writeObject(dto);
    }
}
