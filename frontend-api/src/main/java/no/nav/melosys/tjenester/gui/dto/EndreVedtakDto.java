package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;

public class EndreVedtakDto {
    private Endretperiode begrunnelseKode;
    private String fritekst;
    private String fritekstSed;

    public Endretperiode getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public void setBegrunnelseKode(Endretperiode begrunnelseKode) {
        this.begrunnelseKode = begrunnelseKode;
    }

    public String getFritekst() {
        return fritekst;
    }

    public void setFritekst(final String fritekst) {
        this.fritekst = fritekst;
    }

    public String getFritekstSed() {
        return fritekstSed;
    }

    public void setFritekstSed(String fritekstSed) {
        this.fritekstSed = fritekstSed;
    }
}