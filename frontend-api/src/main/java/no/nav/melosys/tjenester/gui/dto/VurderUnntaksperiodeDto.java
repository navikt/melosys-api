package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

import no.nav.melosys.domain.kodeverk.IkkeGodkjentBegrunnelser;

public class VurderUnntaksperiodeDto {

    private final Set<IkkeGodkjentBegrunnelser> ikkeGodkjentBegrunnelseKoder;
    private final String begrunnelseFritekst;

    public VurderUnntaksperiodeDto(Set<IkkeGodkjentBegrunnelser> ikkeGodkjentBegrunnelseKoder, String begrunnelseFritekst) {
        this.ikkeGodkjentBegrunnelseKoder = ikkeGodkjentBegrunnelseKoder;
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public Set<IkkeGodkjentBegrunnelser> getIkkeGodkjentBegrunnelseKoder() {
        return ikkeGodkjentBegrunnelseKoder;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

}
