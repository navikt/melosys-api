package no.nav.melosys.domain.dokument.person;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class Gateadresse {

    private String gatenavn;

    private Integer gatenummer;

    private Integer husnummer;

    private String husbokstav;

    public String getGatenavn() {
        return gatenavn;
    }

    public void setGatenavn(String gatenavn) {
        this.gatenavn = gatenavn;
    }

    public Integer getGatenummer() {
        return gatenummer;
    }

    public void setGatenummer(Integer gatenummer) {
        this.gatenummer = gatenummer;
    }

    public Integer getHusnummer() {
        return husnummer;
    }

    public void setHusnummer(Integer husnummer) {
        this.husnummer = husnummer;
    }

    public String getHusbokstav() {
        return husbokstav;
    }

    public void setHusbokstav(String husbokstav) {
        this.husbokstav = husbokstav;
    }

    @JsonIgnore
    public boolean erTom() {
        return StringUtils.isAllEmpty(gatenavn, husbokstav) &&
            gatenummer == null &&
            husnummer == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {return true;}
        if (!(o instanceof Gateadresse)) {return false;}

        Gateadresse that = (Gateadresse) o;

        return new EqualsBuilder().append(gatenavn, that.gatenavn).append(gatenummer, that.gatenummer).append(husnummer, that.husnummer).append(husbokstav, that.husbokstav).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(gatenavn).append(gatenummer).append(husnummer).append(husbokstav).toHashCode();
    }
}