package no.nav.melosys.tjenester.gui.jackson.serialize;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.tjenester.gui.dto.PersonDto;
import no.nav.melosys.tjenester.gui.dto.SaksopplysningerDto;
import org.modelmapper.ModelMapper;

public class PersonSerializer extends StdSerializer<PersonDokument> {

    private ModelMapper mapper;

    public PersonSerializer() {
        super(PersonDokument.class);
        mapper = new ModelMapper();
        mapper.getConfiguration().setFieldMatchingEnabled(true);
    }

    @Override
    public void serialize(PersonDokument personDokument, JsonGenerator generator, SerializerProvider provider) throws IOException {
        SaksopplysningerDto saksopplysningerDto = (SaksopplysningerDto) generator.getCurrentValue();

        PersonDto personDto = mapper.map(personDokument, PersonDto.class);
        personDto.historikk = saksopplysningerDto.getPersonhistorikk();

        generator.writeObject(personDto);
    }
}
