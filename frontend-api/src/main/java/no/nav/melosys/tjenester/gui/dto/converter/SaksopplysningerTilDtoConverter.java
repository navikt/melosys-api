package no.nav.melosys.tjenester.gui.dto.converter;

import java.util.Comparator;
import java.util.Set;

import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.domain.dokument.inntekt.InntektDokument;
import no.nav.melosys.domain.dokument.medlemskap.MedlemskapDokument;
import no.nav.melosys.domain.dokument.medlemskap.Medlemsperiode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.tjenester.gui.dto.SaksopplysningerDto;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;

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
                    ArbeidsforholdDokument arbeidsforholdDokument = (ArbeidsforholdDokument) dokument;
                    arbeidsforholdDokument.getArbeidsforhold().sort(new ArbeidsforholdComparator());
                    dto.setArbeidsforhold(arbeidsforholdDokument);
                    break;
                case ORGANISASJON:
                    dto.getOrganisasjoner().add((OrganisasjonDokument)dokument);
                    break;
                case MEDLEMSKAP:
                    MedlemskapDokument medlemskapDokument = (MedlemskapDokument) dokument;
                    medlemskapDokument.getMedlemsperiode().sort(Comparator.comparing(Medlemsperiode::getType).thenComparing(new MedlemsPeriodComparator()));
                    dto.setMedlemskap(medlemskapDokument);
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

    /**
     * - Åpent arbeidsforhold uten sluttdato sorteres foran/over arbeidsforhold med sluttdato.
     * - Arbeidsforhold må ellers sorteres med nyeste fra-og-med-dato øverst.
     */
    final static class ArbeidsforholdComparator implements Comparator<Arbeidsforhold> {

        @Override
        public int compare(Arbeidsforhold a, Arbeidsforhold b) {
            if (a.getAnsettelsesPeriode().getTom() == null) {
                if (b.getAnsettelsesPeriode().getTom() == null) {
                    return b.getAnsettelsesPeriode().getFom().compareTo(a.getAnsettelsesPeriode().getFom());
                } else {
                    return -1;
                }
            } else if (b.getAnsettelsesPeriode().getTom() == null) {
                return 1;
            } else {
                return b.getAnsettelsesPeriode().getFom().compareTo(a.getAnsettelsesPeriode().getFom());
            }
        }
    }

    /**
     * Medlemsperioder sorteres fra nyest til eldst.
     */
    final static class MedlemsPeriodComparator implements Comparator<Medlemsperiode> {
        @Override
        public int compare(Medlemsperiode o1, Medlemsperiode o2) {
            return o2.getPeriode().getFom().compareTo(o1.getPeriode().getFom());
        }
    }
}
