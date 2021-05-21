package no.nav.melosys.tjenester.gui.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FagsakSokDto {

    private final String ident;
    private final String saksnummer;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public FagsakSokDto(@JsonProperty("ident") String ident, @JsonProperty("saksnummer") String saksnummer) {
        this.ident = ident;
        this.saksnummer = saksnummer;
    }

    public String getIdent() {
        return ident;
    }

    public String getSaksnummer() {
        return saksnummer;
    }
}
