package no.nav.melosys.domain;

import javax.persistence.Converter;

//FIXME (farjam): Ikke revidert for v0

public enum RettighetsType implements Kodeverk<RettighetsType> {

    LOVVALGSLAND("LOVVALGSLAND"),
    FRIVILIG_MEDLEMSKAP("FRIVILIG_MEDL"),
    UNNTAK_MEDLEMSKAP("UNNTAK_MEDL");

    private String kode;

    private RettighetsType(String kode) {
        this.kode = kode;
    }
    
    @Override
    public String getKode() {
        return kode;
    }

    @Converter
    public static class DbKonverterer extends Kodeverk.DbKonverterer<RettighetsType> {
        @Override
        protected RettighetsType[] getLovligeVerdier() {
            return RettighetsType.values();
        }
    }

}
