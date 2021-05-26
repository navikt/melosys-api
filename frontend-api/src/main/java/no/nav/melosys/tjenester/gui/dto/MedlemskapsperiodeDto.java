package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.InnvilgelsesResultat;
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

    public long getId() {
        return id;
    }

    public String getArbeidsland() {
        return arbeidsland;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    public Folketrygdloven_kap2_bestemmelser getBestemmelse() {
        return bestemmelse;
    }

    public InnvilgelsesResultat getInnvilgelsesResultat() {
        return innvilgelsesResultat;
    }

    public Trygdedekninger getTrygdedekning() {
        return trygdedekning;
    }

    public Medlemskapstyper getMedlemskapstype() {
        return medlemskapstype;
    }

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
