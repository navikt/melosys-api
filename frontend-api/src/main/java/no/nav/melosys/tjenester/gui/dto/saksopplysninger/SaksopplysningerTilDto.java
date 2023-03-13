package no.nav.melosys.tjenester.gui.dto.saksopplysninger;

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
import no.nav.melosys.domain.dokument.sed.SedDokument;
import no.nav.melosys.tjenester.gui.dto.SaksopplysningerDto;
import no.nav.melosys.tjenester.gui.dto.eessi.SedDokumentDto;
import no.nav.melosys.tjenester.gui.dto.inntekt.InntektDto;
import org.springframework.stereotype.Component;


@Component
public class SaksopplysningerTilDto {
    static final Comparator<Medlemsperiode> medlemsperiodeKomparator =
        (o1, o2) -> o2.getPeriode().getFom().compareTo(o1.getPeriode().getFom());

    public SaksopplysningerDto getSaksopplysningerDto(Set<Saksopplysning> saksopplysningSet) {
        SaksopplysningerDto dto = new SaksopplysningerDto();

        for (Saksopplysning saksopplysning : saksopplysningSet) {
            SaksopplysningType type = saksopplysning.getType();
            SaksopplysningDokument dokument = saksopplysning.getDokument();

            switch (type) {
                case ARBFORH -> {
                    ArbeidsforholdDokument arbeidsforholdDokument = (ArbeidsforholdDokument) dokument;
                    if (arbeidsforholdDokument != null && arbeidsforholdDokument.getArbeidsforhold() != null) {
                        arbeidsforholdDokument.getArbeidsforhold().sort(new ArbeidsforholdComparator());
                    }
                    dto.setArbeidsforhold(arbeidsforholdDokument);
                }
                case ORG -> dto.getOrganisasjoner().add((OrganisasjonDokument) dokument);
                case MEDL -> {
                    MedlemskapDokument medlemskapDokument = (MedlemskapDokument) dokument;
                    if (medlemskapDokument != null && medlemskapDokument.getMedlemsperiode() != null) {
                        medlemskapDokument.getMedlemsperiode().sort(Comparator.comparing(Medlemsperiode::getType).thenComparing(medlemsperiodeKomparator));
                    }
                    dto.setMedlemskap(medlemskapDokument);
                }
                case INNTK -> dto.setInntekt(new InntektDto((InntektDokument) dokument));
                case SEDOPPL -> dto.setSed(SedDokumentDto.fra((SedDokument) dokument));
            }
        }
        return dto;
    }

    /**
     * - Åpent arbeidsforhold uten sluttdato sorteres foran/over arbeidsforhold med sluttdato.
     * - Arbeidsforhold må ellers sorteres med nyeste fra-og-med-dato øverst.
     */
    static final class ArbeidsforholdComparator implements Comparator<Arbeidsforhold> {
        @Override
        public int compare(Arbeidsforhold arbeidsforholdA, Arbeidsforhold arbeidsforholdB) {
            if (arbeidsforholdA.getAnsettelsesPeriode().getTom() == null) {
                if (arbeidsforholdB.getAnsettelsesPeriode().getTom() == null) {
                    return arbeidsforholdB.getAnsettelsesPeriode().getFom().compareTo(arbeidsforholdA.getAnsettelsesPeriode().getFom());
                } else {
                    return -1;
                }
            } else if (arbeidsforholdB.getAnsettelsesPeriode().getTom() == null) {
                return 1;
            } else {
                return arbeidsforholdB.getAnsettelsesPeriode().getFom().compareTo(arbeidsforholdA.getAnsettelsesPeriode().getFom());
            }
        }
    }

}
