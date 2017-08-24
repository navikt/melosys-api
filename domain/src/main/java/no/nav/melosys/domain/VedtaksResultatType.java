package no.nav.melosys.domain;

import javax.persistence.Converter;
import javax.persistence.Entity;
import javax.persistence.Table;

//FIXME (farjam): Ikke revidert for v0

public enum VedtaksResultatType implements Kodeverk<VedtaksResultatType> {

    INNVILGET("INNVILGET"),
    DELVIS_INNVILGET("DELVIS_INNVILGET"),
    AVSLAG("AVSLAG");

    private String kode;

    private VedtaksResultatType(String kode) {
        this.kode = kode;
    }
    
    @Override
    public String getKode() {
        return kode;
    }

    @Converter
    public static class DbKonverterer extends Kodeverk.DbKonverterer<VedtaksResultatType> {
        @Override
        protected VedtaksResultatType[] getLovligeVerdier() {
            return VedtaksResultatType.values();
        }
    }

}
