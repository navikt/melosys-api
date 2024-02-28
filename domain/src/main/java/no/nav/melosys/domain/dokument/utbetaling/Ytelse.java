package no.nav.melosys.domain.dokument.utbetaling;

public class Ytelse {

    private String type;

    private Periode periode;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }
}
