package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;

public class EndreVedtakDto {
    private Endretperiode begrunnelseKode;

    public Endretperiode getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public void setBegrunnelseKode(Endretperiode begrunnelseKode) {
        this.begrunnelseKode = begrunnelseKode;
    }
}