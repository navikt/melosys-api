package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum SaksopplysningKilde implements Kodeverk<SaksopplysningKilde> {

    AAREG("AAREG", "Aa-registeret"),
    EREG("EREG", "Enhetsregisteret"),
    INNTK("INNTK", "Inntektskomponenten"),
    TPS("TPS", "Folkeregisteret");

    private String kode;
    private String beskrivelse;

    private SaksopplysningKilde(String kode, String beskrivelse) {
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
    public static class DbKonverterer extends Kodeverk.DbKonverterer<SaksopplysningKilde> {
        @Override
        protected SaksopplysningKilde[] getLovligeVerdier() {
            return SaksopplysningKilde.values();
        }
    }

}