package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum RolleType implements InterntKodeverkTabell<RolleType> {

    BRUKER("BRUKER", "Personen som avklaringen lovvalg eller medlemskap gjelder for"), // Aka. arbeidstaker
    ARBEIDSGIVER("ARBEIDSGIVER", "Arbeidsgiver som sender bruker for arbeid eller oppdrag i utlandet"),
    REPRESENTANT("REPRESENTANT", "Aktøren representerer bruker og/eller arbeidsgiver i saken"), // Aka. fullmektig, verge
    MYNDIGHET("MYNDIGHET", "Myndigheten det sendes til og/eller mottas dokumentasjon fra i saken");
    
    private String kode;
    private String beskrivelse;

    RolleType(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<RolleType> {
        @Override
        protected RolleType[] getLovligeVerdier() {
            return RolleType.values();
        }
    }

}
