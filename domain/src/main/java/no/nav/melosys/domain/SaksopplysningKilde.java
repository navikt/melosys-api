package no.nav.melosys.domain;

import javax.persistence.Converter;

public enum SaksopplysningKilde implements KodeverkTabell<SaksopplysningKilde> {

    AAREG("AAREG", "Aa-registeret"),
    EREG("EREG", "Enhetsregisteret"),
    INNTK("INNTK", "Inntektskomponenten"),
    MEDL("MEDL", "Medlemskapsunntak"),
    SBH("SBH", "Saksbehandler"),
    SOB("SOB", "Sak og behandling"),
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
    public static class DbKonverterer extends KodeverkTabell.DbKonverterer<SaksopplysningKilde> {
        @Override
        protected SaksopplysningKilde[] getLovligeVerdier() {
            return SaksopplysningKilde.values();
        }
    }

}