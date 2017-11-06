package no.nav.melosys.domain.dokument.person;

import com.fasterxml.jackson.annotation.JsonValue;

import no.nav.melosys.domain.dokument.KodeverkEnum;

/**
 * Denne enumen er en hardkoding av kodeverket Sivilstander.
 */
public enum Sivilstand implements KodeverkEnum<Sivilstand> {

    SEPR("Separert"),
    REPA("Registrert partner"),
    SKPA("Skilt partner"),
    SEPA("Separert partner"),
    UGIF("Ugift"),
    GJPA("Gjenlevende partner"),
    NULL("Uoppgitt"),
    GIFT("Gift"),
    SKIL("Skilt"),
    SAMB("Samboer"),
    ENKE("Enke/-mann"),
    GLAD("Gift, lever adskilt");
    
    private String navn;
    
    private Sivilstand(String navn) {
        this.navn = navn;
    }

    @Override
    @JsonValue
    public String getNavn() {
        return navn;
    }

}
