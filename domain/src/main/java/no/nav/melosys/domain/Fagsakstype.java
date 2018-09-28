package no.nav.melosys.domain;

import javax.persistence.Converter;

/**
 * De forskjellige sakstypene Melosys skal kunne behandle.
 * 
 * MERK: Dette kodeverket gjenspeiler IKKE "SakstypeMedOgLov" i den logisk modellen
 */
public enum Fagsakstype implements InterntKodeverkTabell<Fagsakstype> {

    EU_EØS("EU_EOS", "EU/EØS"),
    TRYGDEAVTALE("TRYGDEAVTALE", "Trygdeavtale"),
    FOLKETRYGD("FTRL", "Folketrygdloven");

    private String kode;
    private String beskrivelse;

    Fagsakstype(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<Fagsakstype> {
        @Override
        protected Fagsakstype[] getLovligeVerdier() {
            return Fagsakstype.values();
        }
    }

}
