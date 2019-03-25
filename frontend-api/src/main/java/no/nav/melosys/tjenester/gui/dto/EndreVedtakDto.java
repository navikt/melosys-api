package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.kodeverk.Endretperioder;

public class EndreVedtakDto {
    private Endretperioder begrunnelseKode;

    public Endretperioder getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public void setBegrunnelseKode(Endretperioder begrunnelseKode) {
        this.begrunnelseKode = begrunnelseKode;
    }
}