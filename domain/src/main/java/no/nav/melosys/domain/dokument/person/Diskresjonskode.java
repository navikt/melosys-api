package no.nav.melosys.domain.dokument.person;

import com.fasterxml.jackson.annotation.JsonValue;
import no.nav.melosys.domain.dokument.KodeverkEnum;

/**
 * Denne enumen er en hardkoding av kodeverket Diskresjonskoder.
 */
public enum Diskresjonskode implements KodeverkEnum<Diskresjonskode> {

    MILI("Militær"),
    UFB("Uten fast bopel"),
    URIK("I utenrikstjeneste"),
    SPSF("Sperret adresse, strengt fortrolig"),
    SVAL("Svalbard"),
    SPFO("Sperret adresse, fortrolig"),
    PEND("Pendler"),
    KLIE("Klientadresse");
    
    private String navn;
    
    private Diskresjonskode(String navn) {
        this.navn = navn;
    }

    @JsonValue
    @Override
    public String getNavn() {
        return navn;
    }

}
