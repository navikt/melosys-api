package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum RolleType implements InterntKodeverkTabell<RolleType> {

    BRUKER("BRUKER", "Bruker"), // Aka. arbeidstaker
    ARBEIDSGIVER("ARBGIV", "Arbeidsgiver"),
    REPRESENTANT("REPRES", "Representant"); // Aka. fullmektig
    
    private String kode;
    private String beskrivelse;

    private RolleType(String kode, String beskrivelse) {
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
