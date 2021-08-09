package no.nav.melosys.tjenester.gui.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

public class VurderUnntaksperiodeDto {

    private final Set<String> ikkeGodkjentBegrunnelseKoder;
    private final String begrunnelseFritekst;

    @JsonCreator
    public VurderUnntaksperiodeDto(@JsonProperty("ikkeGodkjentBegrunnelseKoder") Set<String> ikkeGodkjentBegrunnelseKoder,
                                   @JsonProperty("begrunnelseFritekst") String begrunnelseFritekst) {
        this.ikkeGodkjentBegrunnelseKoder = ikkeGodkjentBegrunnelseKoder;
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public Set<String> getIkkeGodkjentBegrunnelseKoder() {
        return ikkeGodkjentBegrunnelseKoder;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

}
