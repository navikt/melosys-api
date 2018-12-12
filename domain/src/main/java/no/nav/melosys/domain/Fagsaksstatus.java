package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum Fagsaksstatus implements InterntKodeverkTabell<Fagsaksstatus> {

    OPPRETTET("OPPRETTET", "Saken er opprettet"),
    LOVVALG_AVKLART("LOVVALG_AVKLART", "Lovvalget er avklart"),
    AVSLUTTET("AVSLUTTET", "Saken er avsluttet"),
    HENLAGT("HENLAGT", "Saken er henlagt");

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
