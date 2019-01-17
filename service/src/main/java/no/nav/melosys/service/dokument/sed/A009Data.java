package no.nav.melosys.service.dokument.sed;

import java.util.Collection;

import no.nav.melosys.domain.Lovvalgsperiode;

public class A009Data extends AbstraktSedData {

    private Collection<Lovvalgsperiode> lovvalgsperioder;

    public Collection<Lovvalgsperiode> getLovvalgsperioder() {
        return lovvalgsperioder;
    }

    public void setLovvalgsperioder(Collection<Lovvalgsperiode> lovvalgsperioder) {
        this.lovvalgsperioder = lovvalgsperioder;
    }
}
