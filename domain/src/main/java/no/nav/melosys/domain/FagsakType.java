package no.nav.melosys.domain;

import com.fasterxml.jackson.annotation.JsonValue;

import javax.persistence.Converter;

/**
 * De forskjellige sakstypene Melosys skal kunne behandle.
 * 
 * MERK: Dette kodeverket gjenspeiler IKKE "SakstypeMedOgLov" i den logisk modellen
 */
public enum FagsakType implements Kodeverk<FagsakType> {

    SØKNAD_A1("A1", "Søknad om A1");

    private String kode;
    private String beskrivelse;

    private FagsakType(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }
    
    @Override
    @JsonValue
    public String getKode() {
        return kode;
    }

    @Override
    public String getBeskrivelse() {
        return beskrivelse;
    }

    @Converter
    public static class DbKonverterer extends Kodeverk.DbKonverterer<FagsakType> {
        @Override
        protected FagsakType[] getLovligeVerdier() {
            return FagsakType.values();
        }
    }

}
