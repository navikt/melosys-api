package no.nav.melosys.domain.dokument.felles;

import no.nav.melosys.domain.FellesKodeverk;

public abstract class AbstraktKodeverkHjelper implements KodeverkHjelper {
    protected String kode;

    @Override
    public abstract FellesKodeverk hentKodeverkNavn();

    @Override
    public String getKode() {
        return kode;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }
}
