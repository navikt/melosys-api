package no.nav.melosys.domain.avgift.penger;

import java.math.BigDecimal;

public class Penger {
    private BigDecimal verdi;
    private String valuta;

    private static final String NOK = "NOK";

    public Penger(Double verdi) {
        this(BigDecimal.valueOf(verdi));
    }

    public Penger(BigDecimal verdi) {
        this.verdi = verdi;
        this.valuta = NOK;
    }

    public Penger(BigDecimal verdi, String valuta) {
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
}
