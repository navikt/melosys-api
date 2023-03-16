package no.nav.melosys.tjenester.gui.medlemskapsperiode.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public record MedlemskapsperiodeDto (long id,
                                     String arbeidsland,
                                     LocalDate fomDato,
                                     LocalDate tomDato,
                                     Folketrygdloven_kap2_bestemmelser bestemmelse,
                                     InnvilgelsesResultat innvilgelsesResultat,
                                     Trygdedekninger trygdedekning,
                                     Medlemskapstyper medlemskapstype) {

    public static MedlemskapsperiodeDto av(Medlemskapsperiode medlemskapsperiode) {
        return new MedlemskapsperiodeDto(
            medlemskapsperiode.getId(),
            medlemskapsperiode.getArbeidsland(),
            medlemskapsperiode.getFom(),
            medlemskapsperiode.getTom(),
            medlemskapsperiode.getBestemmelse(),
            medlemskapsperiode.getInnvilgelsesresultat(),
            medlemskapsperiode.getDekning(),
            medlemskapsperiode.getMedlemskapstype()
        );
    }
}
