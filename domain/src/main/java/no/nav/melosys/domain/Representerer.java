package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum Representerer implements InterntKodeverkTabell<Representerer> {
    BRUKER("BRUKER", "Representant representerer bare bruker"),
    ARBEIDSGIVER("ARBEIDSGIVER", "Representant representerer arbeidsgiver"),
    BEGGE("BEGGE", "Representant representerer bruker og arbeidsgiver");

    private String kode;
    private String beskrivelse;

    Representerer(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<Representerer> {
        @Override
        protected Representerer[] getLovligeVerdier() {
            return Representerer.values();
        }
    }

}
