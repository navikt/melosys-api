package no.nav.melosys.domain;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class KontaktopplysningID implements Serializable {

    @Column(name = "saksnummer", nullable = false)
    private String saksnummer;

    @Column(name = "orgnr", nullable = false)
    private String orgnr;

    public KontaktopplysningID() {
    }

    public KontaktopplysningID(String saksnummer, String orgnr) {
        this.saksnummer = saksnummer;
        this.orgnr = orgnr;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public void setOrgnr(String orgnr) {
        this.orgnr = orgnr;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof KontaktopplysningID)) return false;
        KontaktopplysningID that = (KontaktopplysningID) o;
        return getSaksnummer().equals(that.getSaksnummer()) &&
            getOrgnr().equals(that.getOrgnr());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSaksnummer(), getOrgnr());
    }
}
