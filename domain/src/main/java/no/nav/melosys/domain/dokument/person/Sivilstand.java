package no.nav.melosys.domain.dokument.person;

import no.nav.melosys.domain.Kodeverk;

/**
 * Denne enumen er en hardkoding av kodeverket Sivilstander.
 */
public enum Sivilstand implements Kodeverk {

    SEPR("SEPR", "Separert"),
    REPA("REPA", "Registrert partner"),
    SKPA("SKPA","Skilt partner"),
    SEPA("SEPA", "Separert partner"),
    UGIF("UGIF", "Ugift"),
    GJPA("GJPA", "Gjenlevende partner"),
    NULL("NULL", "Uoppgitt"),
    GIFT("GIFT", "Gift"),
    SKIL("SKIL", "Skilt"),
    SAMB("SAMB", "Samboer"),
    ENKE("ENKE", "Enke/-mann"),
    GLAD("GLAD", "Gift, lever adskilt");

    private String kode;
    private String beskrivelse;
    
    Sivilstand(String kode, String beskrivelse) {
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
