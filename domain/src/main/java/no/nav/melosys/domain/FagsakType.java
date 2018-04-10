package no.nav.melosys.domain;

import javax.persistence.Converter;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * De forskjellige sakstypene Melosys skal kunne behandle.
 * 
 * MERK: Dette kodeverket gjenspeiler IKKE "SakstypeMedOgLov" i den logisk modellen
 */
public enum FagsakType implements KodeverkTabell<FagsakType> {

    EU_EØS("EU_EOS", "EU/EØS"),
    TRYGDEAVTALE("TRG_AVT", "Trygdeavtale"),
    FOLKETRYGD("FLK_TRG", "Folketrygd");


    private String kode;
    private String beskrivelse;

    private FagsakType(String kode, String beskrivelse) {
        this.kode = kode;
        this.beskrivelse = beskrivelse;
    }
    
    @Override
    public String getKode() {
        return kode;
    }

    @Override
    @JsonValue
    public String getBeskrivelse() {
        return beskrivelse;
    }

    @Converter
    public static class DbKonverterer extends KodeverkTabell.DbKonverterer<FagsakType> {
        @Override
        protected FagsakType[] getLovligeVerdier() {
            return FagsakType.values();
        }
    }

}
