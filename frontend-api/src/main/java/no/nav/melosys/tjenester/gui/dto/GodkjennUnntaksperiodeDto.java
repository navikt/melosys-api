package no.nav.melosys.tjenester.gui.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class GodkjennUnntaksperiodeDto {

    private final boolean varsleUtland;
    private final String fritekst;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public GodkjennUnntaksperiodeDto(@JsonProperty("varsleUtland") boolean varsleUtland,
                                     @JsonProperty("fritekst") String fritekst) {
        this.varsleUtland = varsleUtland;
        this.fritekst = fritekst;
    }

    public boolean isVarsleUtland() {
        return varsleUtland;
    }

    public String getFritekst() {
        return fritekst;
    }
}
