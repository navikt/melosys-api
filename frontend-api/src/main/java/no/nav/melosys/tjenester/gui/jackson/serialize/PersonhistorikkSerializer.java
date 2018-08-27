package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.tjenester.gui.dto.PersonhistorikkDto;

public class PersonhistorikkSerializer extends StdSerializer<PersonhistorikkDokument> {

    public PersonhistorikkSerializer() {
        super(PersonhistorikkDokument.class);
    }

    @Override
    public void serialize(PersonhistorikkDokument personhistorikkDokument, JsonGenerator generator, SerializerProvider provider) throws IOException {
        PersonhistorikkDto dto = new PersonhistorikkDto();

        if (!personhistorikkDokument.statsborgerskapListe.isEmpty()) {
            dto.statsborgerskap = personhistorikkDokument.statsborgerskapListe.get(0);
        }
        if (!personhistorikkDokument.bostedsadressePeriodeListe.isEmpty()) {
            dto.bostedsadresse = personhistorikkDokument.bostedsadressePeriodeListe.get(0);
        }
        if (!personhistorikkDokument.postadressePeriodeListe.isEmpty()) {
            dto.postadresse = personhistorikkDokument.postadressePeriodeListe.get(0);
        }
        if (!personhistorikkDokument.midlertidigAdressePeriodeListe.isEmpty()) {
            dto.midlertidigPostadresse = personhistorikkDokument.midlertidigAdressePeriodeListe.get(0);
        }

        generator.writeObject(dto);
    }
}
