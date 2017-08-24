package no.nav.melosys.domain;

import javax.persistence.Converter;

//FIXME (farjam): Ikke revidert for v0

public enum RegelType implements Kodeverk<RegelType> {

    AVKLARE_FAKTA("AVKLARE_FAKTA"),
    BEREGNING("BEREGNING"),
    FORRETNING("FORRETNING"),
    VILKAAR("VILKAAR");

    private String kode;

    private RegelType(String kode) {
        this.kode = kode;
    }
    
    @Override
    public String getKode() {
        return kode;
    }

    @Converter
    public static class DbKonverterer extends Kodeverk.DbKonverterer<RegelType> {
        @Override
        protected RegelType[] getLovligeVerdier() {
            return RegelType.values();
        }
    }

}
