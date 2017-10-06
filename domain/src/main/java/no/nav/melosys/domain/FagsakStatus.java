package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum FagsakStatus implements Kodeverk<FagsakStatus> {

    OPPRETTET("OPPR", "Opprettet"),
    LØPENDE("LOP", "Løpende"),
    OPPHØRT("UBEH", "Opphørt"),
    AVSLUTTET("AVSLU", "Avsluttet");

    private String kode;
    private String beskrivelse;

    private FagsakStatus(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }
    
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }

    @Converter
    public static class DbKonverterer extends Kodeverk.DbKonverterer<FagsakStatus> {
        @Override
        protected FagsakStatus[] getLovligeVerdier() {
            return FagsakStatus.values();
        }
    }

}
