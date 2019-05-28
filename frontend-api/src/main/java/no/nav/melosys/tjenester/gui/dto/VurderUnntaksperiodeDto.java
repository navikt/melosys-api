package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

public class VurderUnntaksperiodeDto {

    private final Set<String> ikkeGodkjentBegrunnelseKoder;
    private final String begrunnelseFritekst;

    public VurderUnntaksperiodeDto(Set<String> ikkeGodkjentBegrunnelseKoder, String begrunnelseFritekst) {
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
