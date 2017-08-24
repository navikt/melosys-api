package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum FagsakType implements Kodeverk<FagsakType> {

    SØKNAD_A1("SKNAD_A1");

    private String kode;

    private FagsakType(String kode) {
        this.kode = kode;
    }
    
    @Override
    public String getKode() {
        return kode;
    }

    @Converter
    public static class DbKonverterer extends Kodeverk.DbKonverterer<FagsakType> {
        @Override
        protected FagsakType[] getLovligeVerdier() {
            return FagsakType.values();
        }
    }

}
