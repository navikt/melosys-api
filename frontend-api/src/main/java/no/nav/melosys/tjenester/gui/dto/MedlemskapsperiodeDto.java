package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;

import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.Medlemskapsperiode;
import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Medlemskapstyper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class MedlemskapsperiodeDto {
    private final long id;
    private final String arbeidsland;
    private final LocalDate fomDato;
    private final LocalDate tomDato;
    private final Folketrygdloven_kap2_bestemmelser bestemmelse;
    private final InnvilgelsesResultat innvilgelsesResultat;
    private final Trygdedekninger trygdedekning;
    private final Medlemskapstyper medlemskapstype;

    public MedlemskapsperiodeDto(long id,
                                 String arbeidsland,
                                 LocalDate fomDato,
                                 LocalDate tomDato,
                                 Folketrygdloven_kap2_bestemmelser bestemmelse,
                                 InnvilgelsesResultat innvilgelsesResultat,
                                 Trygdedekninger trygdedekning,
                                 Medlemskapstyper medlemskapstype) {
        this.id = id;
        this.arbeidsland = arbeidsland;
        this.fomDato = fomDato;
        this.tomDato = tomDato;
        this.bestemmelse = bestemmelse;
        this.innvilgelsesResultat = innvilgelsesResultat;
        this.trygdedekning = trygdedekning;
        this.medlemskapstype = medlemskapstype;
    }

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
            medlemskapsperiode.getTrygdedekning(),
            medlemskapsperiode.getMedlemskapstype()
        );
    }
}
