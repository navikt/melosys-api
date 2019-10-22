package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;

public class EndreVedtakDto {
    private Endretperiode begrunnelseKode;
    private Behandlingstyper behandlingstype;
    private String fritekst;

    public Endretperiode getBegrunnelseKode() {
        return begrunnelseKode;
    }

    public void setBegrunnelseKode(Endretperiode begrunnelseKode) {
        this.begrunnelseKode = begrunnelseKode;
    }

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(final Behandlingstyper behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public String getFritekst() {
        return fritekst;
    }

    public void setFritekst(final String fritekst) {
        this.fritekst = fritekst;
    }
}