package no.nav.melosys.tjenester.gui.dto.converter;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.tjenester.gui.dto.SaksopplysningerDto;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;

import java.util.Set;

/**
 * Denne klassen konverterer alle SaksopplysningDokumenter til et objekt tre for frontend.
 */
public class SaksopplysningerTilDtoConverter implements Converter<Set<Saksopplysning>, SaksopplysningerDto> {

    @Override
    public SaksopplysningerDto convert(MappingContext<Set<Saksopplysning>, SaksopplysningerDto> context) {
        SaksopplysningerDto dto = new SaksopplysningerDto();

        if (context.getSource() == null) {
            // Frontend ønsker å motta et objekt, selv når saksopplysninger ikke finnes.
            return dto;
        }

        for (Saksopplysning saksopplysning : context.getSource()) {
            SaksopplysningType type = saksopplysning.getType();
            SaksopplysningDokument dokument = saksopplysning.getDokument();

            switch (type) {
                case PERSONOPPLYSNING:
                    dto.setPerson((PersonDokument)dokument);
                    break;
                case ARBEIDSFORHOLD:
                    dto.setArbeidsforhold((ArbeidsforholdDokument)dokument);
                    break;
                case ORGANISASJON:
                    dto.getOrganisasjoner().add((OrganisasjonDokument)dokument);
                    break;
                case MEDLEMSKAP:
                    dto.setMedlemskap((MedlemskapDokument)dokument);
                    break;
                case INNTEKT:
                    dto.setInntekt((InntektDokument)dokument);
                    break;
                case SØKNAD:
                    // N.B. Frontend ønsker ikke å få søknaden på /fagsaker slik at opplysninger fra registrene er adskilt
                    break;
                default:
                    throw new IllegalArgumentException("Type " + type.getKode() + " ikke støttet.");
            }
        }

        return dto;
    }
}
