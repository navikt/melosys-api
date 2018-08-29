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
import no.nav.melosys.domain.dokument.sakogbehandling.SobSakDokument;
import no.nav.melosys.domain.dokument.person.PersonhistorikkDokument;
import no.nav.melosys.tjenester.gui.dto.PersonDto;
import no.nav.melosys.tjenester.gui.dto.PersonhistorikkDto;
import no.nav.melosys.tjenester.gui.dto.SaksopplysningerDto;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;

/**
 * Denne klassen konverterer alle SaksopplysningDokumenter til et objekt tre for frontend.
 */
public class SaksopplysningerTilDtoConverter implements Converter<Set<Saksopplysning>, SaksopplysningerDto> {


    //Medlemsperioder sorteres fra nyest til eldst.
     static final Comparator<Medlemsperiode> medlemsperiodeKomparator =
            (o1, o2) -> o2.getPeriode().getFom().compareTo(o1.getPeriode().getFom());

    @Override
    public SaksopplysningerDto convert(MappingContext<Set<Saksopplysning>, SaksopplysningerDto> context) {
        SaksopplysningerDto dto = new SaksopplysningerDto();

        if (context.getSource() == null) {
            // Frontend ønsker å motta et objekt, selv når saksopplysninger ikke finnes.
            return dto;
        }

        PersonhistorikkDto personhistorikk = null;

        for (Saksopplysning saksopplysning : context.getSource()) {
            SaksopplysningType type = saksopplysning.getType();
            SaksopplysningDokument dokument = saksopplysning.getDokument();

            switch (type) {
                case PERSONOPPLYSNING:
                    dto.setPerson(tilPersonDto((PersonDokument) dokument));
                    if (personhistorikk != null) {
                        dto.getPerson().historikk = personhistorikk;
                    }
                    break;
                case ARBEIDSFORHOLD:
                    ArbeidsforholdDokument arbeidsforholdDokument = (ArbeidsforholdDokument) dokument;
                    if (arbeidsforholdDokument != null && arbeidsforholdDokument.getArbeidsforhold() != null) {
                        arbeidsforholdDokument.getArbeidsforhold().sort(new ArbeidsforholdComparator());
                    }
                    dto.setArbeidsforhold(arbeidsforholdDokument);
                    break;
                case ORGANISASJON:
                    dto.getOrganisasjoner().add((OrganisasjonDokument)dokument);
                    break;
                case MEDLEMSKAP:
                    MedlemskapDokument medlemskapDokument = (MedlemskapDokument) dokument;
                    if (medlemskapDokument != null && medlemskapDokument.getMedlemsperiode() != null) {
                        medlemskapDokument.getMedlemsperiode().sort(Comparator.comparing(Medlemsperiode::getType).thenComparing(medlemsperiodeKomparator));
                    }
                    dto.setMedlemskap(medlemskapDokument);
                    break;
                case INNTEKT:
                    dto.setInntekt((InntektDokument)dokument);
                    break;
                case SOB_SAK:
                    dto.setSakOgBehandling((SobSakDokument) dokument);
                    break;
                case PERSONHISTORIKK:
                    personhistorikk = tilPersonhistorikkDto((PersonhistorikkDokument) dokument);
                    dto.getPerson().historikk = personhistorikk;
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

    private PersonDto tilPersonDto(PersonDokument personDokument) {
        PersonDto dto = new PersonDto();

        dto.fnr = personDokument.fnr;
        dto.sivilstand = personDokument.sivilstand;
        dto.statsborgerskap = personDokument.statsborgerskap;
        dto.kjønn = personDokument.kjønn;
        dto.sammensattNavn = personDokument.sammensattNavn;
        dto.familiemedlemmer = personDokument.familiemedlemmer;
        dto.fødselsdato = personDokument.fødselsdato;
        dto.dødsdato = personDokument.dødsdato;
        dto.diskresjonskode = personDokument.diskresjonskode;
        dto.personstatus = personDokument.personstatus;
        dto.bostedsadresse = personDokument.bostedsadresse;
        dto.postadresse = personDokument.postadresse;
        dto.midlertidigPostadresse = personDokument.midlertidigPostadresse;
        dto.erEgenAnsatt = personDokument.erEgenAnsatt;

        return dto;
    }

    private PersonhistorikkDto tilPersonhistorikkDto(PersonhistorikkDokument personhistorikkDokument) {
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
        return dto;
    }
}
