package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl;

import java.time.LocalDate;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import no.nav.melosys.domain.Medlemskapsperiode;

public class Periode {

    @JsonSerialize(using = LocalDateSerializer.class)
    private final LocalDate fom;

    @JsonSerialize(using = LocalDateSerializer.class)
    private final LocalDate tom;

    private final String bestemmelse;
    private final String innvilgelsesResultat;
    private final String medlemskapstype;
    private final String dekning;
    private final String dekningBeskrivelse;

    public Periode(Medlemskapsperiode m) {
        this.fom = m.getFom();
        this.tom = m.getTom();
        this.bestemmelse = m.getBestemmelse().getKode();
        this.innvilgelsesResultat = m.getInnvilgelsesresultat().getKode();
        this.medlemskapstype = m.getMedlemskapstype().getKode();
        this.dekning = m.getTrygdedekning().getKode();
        this.dekningBeskrivelse = m.getTrygdedekning().getBeskrivelse();
    }

    public LocalDate getFom() {
        return fom;
    }

    public LocalDate getTom() {
        return tom;
    }

    public String getBestemmelse() {
        return bestemmelse;
    }

    public String getInnvilgelsesResultat() {
        return innvilgelsesResultat;
    }

    public String getMedlemskapstype() {
        return medlemskapstype;
    }

    public String getDekning() {
        return dekning;
    }

    public String getDekningBeskrivelse() {
        return dekningBeskrivelse;
    }
}
