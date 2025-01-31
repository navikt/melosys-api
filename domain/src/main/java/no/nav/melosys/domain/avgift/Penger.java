package no.nav.melosys.domain.avgift;

import java.math.BigDecimal;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class Penger {
    @Column(name = "trygdeavgift_beloep_mnd_verdi")
    private BigDecimal verdi;
    @Column(name = "trygdeavgift_beloep_mnd_valuta")
    private String valuta;

    private static final String NOK = "NOK";

    public Penger() {
    }

    public Penger(Double verdi) {
        this(BigDecimal.valueOf(verdi));
    }

    public Penger(BigDecimal verdi) {
        Objects.requireNonNull(verdi, "Penger kan ikke være null");
        this.verdi = verdi;
        this.valuta = NOK;
    }

    public Penger(BigDecimal verdi, String valuta) {
        Objects.requireNonNull(verdi, "Penger kan ikke være null");
        this.verdi = verdi;
        this.valuta = valuta;
    }

    public BigDecimal getVerdi() {
        return verdi;
    }

    public void setVerdi(BigDecimal verdi) {
        this.verdi = verdi;
    }

    public String getValuta() {
        return valuta;
    }

    public void setValuta(String valuta) {
        this.valuta = valuta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Penger that = (Penger) o;
        return verdi.compareTo(that.verdi) == 0
            && Objects.equals(valuta, that.valuta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            verdi != null ? verdi.stripTrailingZeros() : null,
            valuta
        );
    }

    @Override
    public String toString() {
        return "Penger{" + verdi + " " + valuta + '}';
    }
}
