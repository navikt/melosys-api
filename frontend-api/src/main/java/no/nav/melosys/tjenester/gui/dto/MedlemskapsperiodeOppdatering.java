package no.nav.melosys.tjenester.gui.dto;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.melosys.domain.InnvilgelsesResultat;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;

public class MedlemskapsperiodeOppdatering {
    private final LocalDate fom;
    private final LocalDate tom;
    private final Trygdedekninger trygdedekning;
    private final InnvilgelsesResultat innvilgelsesResultat;

    @JsonCreator
    public MedlemskapsperiodeOppdatering(@JsonProperty("fom") LocalDate fom,
                                         @JsonProperty("tom") LocalDate tom,
                                         @JsonProperty("trygdedekning") Trygdedekninger trygdedekning,
                                         @JsonProperty("innvilgelsesResultat") InnvilgelsesResultat innvilgelsesResultat) {
        this.fom = fom;
        this.tom = tom;
        this.trygdedekning = trygdedekning;
        this.innvilgelsesResultat = innvilgelsesResultat;
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public Trygdedekninger getTrygdedekning() {
        return trygdedekning;
    }

    public InnvilgelsesResultat getInnvilgelsesResultat() {
        return innvilgelsesResultat;
    }
}
