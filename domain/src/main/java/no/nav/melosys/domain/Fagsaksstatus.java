package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum Fagsaksstatus implements InterntKodeverkTabell<Fagsaksstatus> {

    OPPRETTET("OPPRETTET", "Opprettet"),
    LOVVALG_AVKLART("LOVVALG AVKLART", "Lovvalg avklart"),
    FORELØPIG_LOVVALG("FORELOEPIG_LOVVALG", "Foreløpig lovvalg"),
    OPPHØRT("OPPHOERT", "Opphørt"),
    AVSLUTTET("AVSLUTTET", "Avsluttet"),
    HENLAGT("HENLAGT", "Henlagt");

    private String kode;
    private String beskrivelse;

    Fagsaksstatus(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<Fagsaksstatus> {
        @Override
        protected Fagsaksstatus[] getLovligeVerdier() {
            return Fagsaksstatus.values();
        }
    }

}
