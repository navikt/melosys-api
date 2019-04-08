package no.nav.melosys.domain.dokument.felles;

public abstract class AbstraktKodeverkHjelper implements KodeverkHjelper {
    protected String kode;

    @Override
    public String getKode() {
        return kode;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }
}
