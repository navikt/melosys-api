package no.nav.melosys.tjenester.gui.dto;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;

public class FattVedtakDto {
    private Behandlingsresultattyper behandlingsresultatTypeKode;
    private String fritekst;
    private String mottakerinstitusjon;
    private Vedtakstyper vedtakstype;
    private String revurderBegrunnelse;

    public Behandlingsresultattyper getBehandlingsresultatTypeKode() {
        return behandlingsresultatTypeKode;
    }

    public void setBehandlingsresultatTypeKode(Behandlingsresultattyper behandlingsresultatTypeKode) {
        this.behandlingsresultatTypeKode = behandlingsresultatTypeKode;
    }

    public String getFritekst() {
        return fritekst;
    }

    public void setFritekst(final String fritekst) {
        this.fritekst = fritekst;
    }

    public String getMottakerinstitusjon() {
        return mottakerinstitusjon;
    }

    public void setMottakerinstitusjon(String mottakerinstitusjon) {
        this.mottakerinstitusjon = mottakerinstitusjon;
    }

    public Vedtakstyper getVedtakstype() {
        return vedtakstype;
    }

    public void setVedtakstype(Vedtakstyper vedtakstype) {
        this.vedtakstype = vedtakstype;
    }

    public String getRevurderBegrunnelse() {
        return revurderBegrunnelse;
    }

    public void setRevurderBegrunnelse(String revurderBegrunnelse) {
        this.revurderBegrunnelse = revurderBegrunnelse;
    }
}
