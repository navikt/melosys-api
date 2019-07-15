package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.Kodeverk;

public enum SaksopplysningKilde implements Kodeverk {

    AAREG("AAREG", "Aa-registeret"),
    EESSI("EESSI", "EESSI-prosjektet"),
    EREG("EREG", "Enhetsregisteret"),
    INNTK("INNTK", "Inntektskomponenten"),
    MEDL("MEDL", "Medlemskapsunntak"),
    SBH("SBH", "Saksbehandler"),
    SOB("SOB", "Sak og behandling"),
    TPS("TPS", "Folkeregisteret");


    private String kode;
    private String beskrivelse;

    SaksopplysningKilde(String kode, String beskrivelse) {
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
}