package no.nav.melosys.tjenester.gui.dto.converter;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.tjenester.gui.dto.SaksopplysningerDto;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;

import java.util.HashSet;
import java.util.Set;

public class DtoTilSaksopplysningerConverter implements Converter<SaksopplysningerDto, Set<Saksopplysning>> {

    @Override
    public Set<Saksopplysning> convert(MappingContext<SaksopplysningerDto, Set<Saksopplysning>> context) {
        Set opplysninger = new HashSet();

        SaksopplysningerDto saksopplysningerDto = context.getSource();
        if (saksopplysningerDto != null) {
            PersonDokument personDokument = saksopplysningerDto.getPerson();
            // fetch from DB or create
            Saksopplysning personOpplysning = new Saksopplysning(personDokument, SaksopplysningType.PERSONOPPLYSNING);

            saksopplysningerDto.getArbeidsforhold();
            saksopplysningerDto.getOrganisasjoner();
            saksopplysningerDto.getMedlemskap();
            saksopplysningerDto.getInntekt();
        }

        return opplysninger;
    }
}
