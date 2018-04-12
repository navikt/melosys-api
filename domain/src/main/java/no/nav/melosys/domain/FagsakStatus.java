package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum FagsakStatus implements KodeverkTabell<FagsakStatus> {

    // FIXME (farjam): kodene må fikses. Mismatch mellom logisk modell og hva som kan lagres i gsak (husk også å rette i DB).
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
    public static class DbKonverterer extends KodeverkTabell.DbKonverterer<FagsakStatus> {
        @Override
        protected FagsakStatus[] getLovligeVerdier() {
            return FagsakStatus.values();
        }
    }

}
