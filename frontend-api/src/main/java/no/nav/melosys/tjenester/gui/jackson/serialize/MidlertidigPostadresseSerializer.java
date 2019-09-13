package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseNorge;
import no.nav.melosys.domain.dokument.person.MidlertidigPostadresseUtland;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.domain.dokument.felles.MidlertidigPostadresse;
import no.nav.melosys.domain.dokument.felles.StrukturertAdresse;
import no.nav.melosys.domain.dokument.felles.UstrukturertAdresse;

import static no.nav.melosys.domain.FellesKodeverk.POSTNUMMER;

public class MidlertidigPostadresseSerializer extends StdSerializer<no.nav.melosys.domain.dokument.person.MidlertidigPostadresse> {

    private transient KodeverkService kodeverkService;

    public MidlertidigPostadresseSerializer(KodeverkService kodeverkService) {
        super(no.nav.melosys.domain.dokument.person.MidlertidigPostadresse.class);
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void serialize(no.nav.melosys.domain.dokument.person.MidlertidigPostadresse midlertidigPostadresse, JsonGenerator generator, SerializerProvider provider) throws IOException {
        MidlertidigPostadresse dto = new MidlertidigPostadresse();

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
            dto.adressetype = MidlertidigPostadresse.Adressetype.STRUKTURERT;

        } else if (midlertidigPostadresse instanceof MidlertidigPostadresseUtland) {
            MidlertidigPostadresseUtland adresse = (MidlertidigPostadresseUtland) midlertidigPostadresse;
            dto.ustrukturertAdresse = UstrukturertAdresse.av(adresse);
            dto.adressetype = MidlertidigPostadresse.Adressetype.USTRUKTURERT;
        }
        generator.writeObject(dto);
    }
}
