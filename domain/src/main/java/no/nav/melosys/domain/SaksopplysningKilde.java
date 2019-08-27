package no.nav.melosys.domain;

import javax.persistence.Converter;

import no.nav.melosys.domain.kodeverk.InterntKodeverkTabell;

public enum SaksopplysningKilde implements InterntKodeverkTabell<SaksopplysningKilde> {

    AAREG("AAREG", "Aa-registeret"),
    EESSI("EESSI", "EESSI-prosjektet"),
    EREG("EREG", "Enhetsregisteret"),
    INNTK("INNTK", "Inntektskomponenten"),
    MEDL("MEDL", "Medlemskapsunntak"),
    SBH("SBH", "Saksbehandler"),
    SOB("SOB", "Sak og behandling"),
    TPS("TPS", "Folkeregisteret"),
    UTBETALDATA("UTBETALDATA", "Utbetaldata");


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
    public static class DbKonverterer extends InterntKodeverkTabell.DbKonverterer<SaksopplysningKilde> {
        @Override
        protected SaksopplysningKilde[] getLovligeVerdier() {
            return SaksopplysningKilde.values();
        }
    }

}