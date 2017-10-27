package no.nav.melosys.domain.dokument.organisasjon.adresse.elektronisk;

import no.nav.melosys.domain.dokument.felles.Periode;

public abstract class ElektroniskAdresse {

    private Periode bruksperiode;

    private Periode gyldighetsperiode;

    public Periode getBruksperiode() {
        return bruksperiode;
    }

    public void setBruksperiode(Periode bruksperiode) {
        this.bruksperiode = bruksperiode;
    }

    public Periode getGyldighetsperiode() {
        return gyldighetsperiode;
    }

    public void setGyldighetsperiode(Periode gyldighetsperiode) {
        this.gyldighetsperiode = gyldighetsperiode;
    }
}
