package no.nav.melosys.domain.dokument.person;

import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.dokument.felles.AbstraktKodeverkHjelper;

public class Diskresjonskode extends AbstraktKodeverkHjelper {

    private static final String SPSF = "SPSF";
    private static final String SPFO = "SPFO";

    @Override
    public FellesKodeverk hentKodeverkNavn() {
        return FellesKodeverk.DISKRESJONSKODER;
    }

    public Diskresjonskode() {
        // Brukes av JAXB
    }

    public Diskresjonskode(String diskresjonskode) {
        this.kode = diskresjonskode;
    }

    public boolean erKode6() {
        return SPSF.equals(kode);
    }

    public boolean erKode7() {
        return SPFO.equals(kode);
    }

    @Override
    public String toString() {
        return kode;
    }
}
