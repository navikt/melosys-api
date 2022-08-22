package no.nav.melosys.service.sak;

import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;

public class EndreSakDto {
    private Sakstyper sakstype;
    private Sakstemaer sakstema;

    public void setSakstype(Sakstyper sakstype) {
        this.sakstype = sakstype;
    }

    public void setSakstema(Sakstemaer sakstema) {
        this.sakstema = sakstema;
    }

    public Sakstyper getSakstype() {
        return sakstype;
    }

    public Sakstemaer getSakstema() {
        return sakstema;
    }
}
