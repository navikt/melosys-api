package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class MedlemskapsperiodeOppdatering {
    private final LocalDate fomDato;
    private final LocalDate tomDato;
    private final Trygdedekninger trygdedekning;
    private final InnvilgelsesResultat innvilgelsesResultat;

    @JsonCreator
    public MedlemskapsperiodeOppdatering(@JsonProperty("fom") LocalDate fomDato,
                                         @JsonProperty("tom") LocalDate tomDato,
                                         @JsonProperty("trygdedekning") Trygdedekninger trygdedekning,
                                         @JsonProperty("innvilgelsesResultat") InnvilgelsesResultat innvilgelsesResultat) {
        this.fomDato = fomDato;
        this.tomDato = tomDato;
        this.trygdedekning = trygdedekning;
        this.innvilgelsesResultat = innvilgelsesResultat;
    }

    public LocalDate getFomDato() {
        return fomDato;
    }

    public LocalDate getTomDato() {
        return tomDato;
    }

    public Trygdedekninger getTrygdedekning() {
        return trygdedekning;
    }

    public InnvilgelsesResultat getInnvilgelsesResultat() {
        return innvilgelsesResultat;
    }
}
