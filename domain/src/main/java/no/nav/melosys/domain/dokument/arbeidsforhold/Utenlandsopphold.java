package no.nav.melosys.domain.dokument.arbeidsforhold;

import no.nav.melosys.domain.dokument.felles.Periode;

//FIXME (Francois) EESSI2-281 Utenlandsopphold mangler testdata
public class Utenlandsopphold {

    private String land;

    private Periode periode;

    public String getLand() {
        return land;
    }

    public void setLand(String land) {
        this.land = land;
    }

    public Periode getPeriode() {
        return periode;
    }

    public void setPeriode(Periode periode) {
        this.periode = periode;
    }

}
