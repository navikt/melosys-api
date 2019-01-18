package no.nav.melosys.service.dokument.sed;

import no.nav.melosys.domain.Lovvalgsperiode;

public class A009Data extends AbstraktSedData {

    private Lovvalgsperiode lovvalgsperiode;

    public Lovvalgsperiode getLovvalgsperiode() {
        return lovvalgsperiode;
    }

    public void setLovvalgsperioder(Lovvalgsperiode lovvalgsperiode) {
        this.lovvalgsperiode = lovvalgsperiode;
    }
}
