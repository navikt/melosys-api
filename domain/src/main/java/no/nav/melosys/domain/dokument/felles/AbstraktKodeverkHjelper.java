package no.nav.melosys.domain.dokument.felles;

import java.util.Objects;

public abstract class AbstraktKodeverkHjelper implements KodeverkHjelper {
    protected String kode;

    @Override
    public String getKode() {
        return kode;
    }

    public void setKode(String kode) {
        this.kode = kode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstraktKodeverkHjelper)) return false;
        AbstraktKodeverkHjelper that = (AbstraktKodeverkHjelper) o;
        return getKode().equals(that.getKode());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKode());
    }
}
