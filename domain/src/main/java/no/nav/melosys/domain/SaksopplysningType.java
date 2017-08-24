package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum SaksopplysningType implements Kodeverk<SaksopplysningType> {
    
    ARBEIDSFORHOLD("ARBFOR"),
    SØKNAD("SOKNAD"),
    PERSONOPPLYSNING("PERSOPL");
    
    private String kode;

    private SaksopplysningType(String kode) {
        this.kode = kode;
    }
    
    @Override
    public String getKode() {
        return kode;
    }

    @Converter
    public static class DbKonverterer extends Kodeverk.DbKonverterer<SaksopplysningType> {
        @Override
        protected SaksopplysningType[] getLovligeVerdier() {
            return SaksopplysningType.values();
        }
    }

}

