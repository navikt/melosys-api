package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum SaksopplysningKilde implements Kodeverk<SaksopplysningKilde> {

    AAREG("AAREG"),
    EREG("EREG"),
    TPS("TPS");
    
    private String kode;

    private SaksopplysningKilde(String kode) {
        this.kode = kode;
    }
    
    @Override
    public String getKode() {
        return kode;
    }

    @Converter
    public static class DbKonverterer extends Kodeverk.DbKonverterer<SaksopplysningKilde> {
        @Override
        protected SaksopplysningKilde[] getLovligeVerdier() {
            return SaksopplysningKilde.values();
        }
    }

}