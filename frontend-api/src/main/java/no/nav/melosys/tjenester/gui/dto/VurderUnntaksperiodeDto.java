package no.nav.melosys.tjenester.gui.dto;

import java.util.Set;

import no.nav.melosys.domain.kodeverk.IkkeGodkjentBegrunnelser;

public class VurderUnntaksperiodeDto {

    private final Resultat resultat;
    private final Set<IkkeGodkjentBegrunnelser> ikkeGodkjentBegrunnelseKoder;
    private final String begrunnelseFritekst;

    public VurderUnntaksperiodeDto(Resultat resultat, Set<IkkeGodkjentBegrunnelser> ikkeGodkjentBegrunnelseKoder, String begrunnelseFritekst) {
        this.resultat = resultat;
        this.ikkeGodkjentBegrunnelseKoder = ikkeGodkjentBegrunnelseKoder;
        this.begrunnelseFritekst = begrunnelseFritekst;
    }

    public Resultat getResultat() {
        return resultat;
    }

    public Set<IkkeGodkjentBegrunnelser> getIkkeGodkjentBegrunnelseKoder() {
        return ikkeGodkjentBegrunnelseKoder;
    }

    public String getBegrunnelseFritekst() {
        return begrunnelseFritekst;
    }

    public enum Resultat {
        GODKJENT,
        VURDER_DOKUMENT,
        IKKE_GODKJENT
    }
}
