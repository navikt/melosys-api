package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum RolleType implements Kodeverk<RolleType> {

    ARBEIDSTAGER("ARBTAG"),
    ARBEIDSGIVER("ARBGIV"),
    FULLMEKTIG("FULLMK");
    
    private String kode;

    private RolleType(String kode) {
        this.kode = kode;
    }
    
    @Override
    public String getKode() {
        return kode;
    }

    @Converter
    public static class DbKonverterer extends Kodeverk.DbKonverterer<RolleType> {
        @Override
        protected RolleType[] getLovligeVerdier() {
            return RolleType.values();
        }
    }

}
