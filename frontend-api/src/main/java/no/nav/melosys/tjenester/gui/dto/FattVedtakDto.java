package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;

public class FattVedtakDto {
    private Behandlingsresultattyper behandlingsresultatTypeKode;
    private String fritekst;
    private List<String> mottakerinstitusjoner;
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

    public List<String> getMottakerinstitusjoner() {
        return mottakerinstitusjoner;
    }

    public void setMottakerinstitusjoner(List<String> mottakerinstitusjoner) {
        this.mottakerinstitusjoner = mottakerinstitusjoner;
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
