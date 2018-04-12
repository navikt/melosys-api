package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum ProsessType implements KodeverkTabell<ProsessType> {

    SØKNAD_A1("SØKNAD_A1", "Søknad A1");

    private String kode;
    private String beskrivelse;

    private ProsessType(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends KodeverkTabell.DbKonverterer<ProsessType> {
        @Override
        protected ProsessType[] getLovligeVerdier() {
            return ProsessType.values();
        }
    }

}
