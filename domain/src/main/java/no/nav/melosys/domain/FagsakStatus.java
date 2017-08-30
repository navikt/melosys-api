package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum FagsakStatus implements Kodeverk<FagsakStatus> {

    OPPRETTET("OPPR"),
    UBEH("UBEH"),
    LØPENDE("LOP"),
    AVSLUTTET("AVSLU");

    private String kode;

    private FagsakStatus(String kode) {
        this.kode = kode;
    }
    
    @Override
    public String getKode() {
        return kode;
    }

    @Converter
    public static class DbKonverterer extends Kodeverk.DbKonverterer<FagsakStatus> {
        @Override
        protected FagsakStatus[] getLovligeVerdier() {
            return FagsakStatus.values();
        }
    }

}
